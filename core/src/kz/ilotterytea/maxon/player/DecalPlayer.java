package kz.ilotterytea.maxon.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Timer;
import kz.ilotterytea.maxon.MaxonGame;
import kz.ilotterytea.maxon.utils.OsUtils;

import java.util.ArrayList;

public class DecalPlayer implements Disposable {
    private final ArrayList<TextureRegion> regions;
    private int regionIndex;
    private final Decal decal;
    private final BoundingBox box;
    private final Savegame savegame;

    private int clickStreak;
    private final Timer.Task delayTask;

    public DecalPlayer(Savegame savegame, ArrayList<TextureRegion> regions) {
        this.savegame = savegame;

        this.regions = regions;
        this.regionIndex = 0;

        this.decal = Decal.newDecal(this.regions.get(this.regionIndex));
        this.decal.setScale(0.025f);

        if (OsUtils.isMobile) {
            this.decal.setPosition(-5f, 1.75f, 4f);
        } else {
            this.decal.setPosition(2f, 1.75f, 2f);
        }

        float width = this.decal.getWidth() / (this.decal.getScaleX() * 1000f);
        float height = this.decal.getHeight() / (this.decal.getScaleY() * 1000f);

        Vector3 position = this.decal.getPosition();
        Vector3 minBox = new Vector3(position.x - width / 3, position.y - height / 3, position.z - width / 3);
        Vector3 maxBox = new Vector3(position.x + width / 3, position.y + height / 3, position.z + width / 3);

        this.box = new BoundingBox(minBox, maxBox);

        clickStreak = 1;
        this.delayTask = new Timer.Task() {
            @Override
            public void run() {
                if (clickStreak == 1) return;

                clickStreak -= 1;
            }
        };
        Timer.schedule(delayTask, 0.5f, 0.5f);
    }

    public void render(Camera camera) {
        if (checkCollisions(camera) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) pet();
    }

    private boolean checkCollisions(Camera camera) {
        Ray ray = null;

        if (Gdx.input.justTouched()) {
            ray = camera.getPickRay(Gdx.input.getX(), Gdx.input.getY());
        }

        if (ray == null) {
            return false;
        }

        Vector3 intersection = new Vector3();

        return Intersector.intersectRayBounds(ray, box, intersection);
    }

    private void pet() {
        updateTextureRegion();
        savegame.increaseMoney(1);

        Sound sound = MaxonGame.getInstance().assetManager.get("sfx/player/purr.ogg", Sound.class);
        sound.play();

        clickStreak++;
    }

    private void updateTextureRegion() {
        this.regionIndex++;
        TextureRegion region;

        try {
            region = this.regions.get(this.regionIndex);
        } catch (Exception ignored) {
            this.regionIndex = 0;
            region = this.regions.get(regionIndex);
        }

        this.decal.setTextureRegion(region);
    }

    public Decal getDecal() {
        return this.decal;
    }

    public int getClickStreak() {
        return clickStreak;
    }

    @Override
    public void dispose() {
        delayTask.cancel();
    }
}
