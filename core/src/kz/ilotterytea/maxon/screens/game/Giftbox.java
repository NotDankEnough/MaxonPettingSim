package kz.ilotterytea.maxon.screens.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Timer;
import kz.ilotterytea.javaextra.tuples.Triple;
import kz.ilotterytea.maxon.MaxonGame;
import kz.ilotterytea.javaextra.comparators.MapValueKeyComparator;
import kz.ilotterytea.maxon.anim.SpriteUtils;
import kz.ilotterytea.maxon.constants.SettingsConstants;
import kz.ilotterytea.maxon.localization.LineId;
import kz.ilotterytea.maxon.pets.Pet;
import kz.ilotterytea.maxon.pets.PetManager;
import kz.ilotterytea.maxon.player.Savegame;
import kz.ilotterytea.maxon.ui.AnimatedImage;
import kz.ilotterytea.maxon.utils.OsUtils;
import kz.ilotterytea.maxon.utils.formatters.NumberFormatter;
import kz.ilotterytea.maxon.utils.math.Math;
import net.mgsx.gltf.scene3d.lights.PointLightEx;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;

import java.util.*;
import java.util.List;

public class Giftbox implements Disposable {
    private final Stage stage;
    private final Skin skin;
    private final AssetManager assetManager;
    private final SceneManager sceneManager;

    private final Sound openedSound;
    private final Music openedMusic;
    private final float soundVolume;

    private final AnimatedImage boxImage;

    private Scene scene;
    private final BoundingBox box;
    private final PointLightEx light;

    private final Timer.Task task;

    private final Vector3 boxPosition, boxScale;
    private final float boxZPosition;

    private float totalDelta;

    private boolean isActive;

    public Giftbox(Stage stage, Skin skin, AssetManager assetManager, SceneManager sceneManager) {
        this.stage = stage;
        this.skin = skin;
        this.assetManager = assetManager;
        this.sceneManager = sceneManager;

        this.boxImage = new AnimatedImage(
                SpriteUtils.splitToTextureRegions(
                        assetManager.get("sprites/giftbox/gift.png"),
                        256, 256
                ),
                10
        );
        boxImage.setScale(0.8f);
        boxImage.setZIndex(1000);
        boxImage.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                Triple<Integer, Double, String> gift = giveGift();
                displayGift(gift.getFirst(), gift.getSecond(), gift.getThird());
                updateBox(false);
            }
        });

        if (OsUtils.isMobile) {
            this.openedSound = assetManager.get("sfx/giftbox/giftbox_flying.ogg");
        } else {
            this.openedSound = assetManager.get("sfx/giftbox/giftbox_click.ogg");
        }

        this.soundVolume = MaxonGame.getInstance().prefs.getInteger(SettingsConstants.SFX_NAME, 10) / 10f;

        this.openedMusic = assetManager.get("mus/giftbox/giftbox_opened.mp3");
        openedMusic.setLooping(true);

        float musicVolume = MaxonGame.getInstance().prefs.getInteger(SettingsConstants.MUSIC_NAME, 10) / 10f;
        openedMusic.setVolume(musicVolume);

        this.boxPosition = new Vector3(3.3f, 0f, 0.4f);
        this.boxScale = new Vector3(2f, 2f, 2f);
        this.boxZPosition = 180f;

        this.isActive = false;
        this.totalDelta = 0f;

        this.light = new PointLightEx();
        this.light.set(Color.ORANGE, new Vector3(boxPosition.x, boxPosition.y + 2f, boxPosition.z - 1f), 80f, 100f);

        this.box = new BoundingBox(
                new Vector3(boxPosition.x - 0.5f, boxPosition.y + 0.5f, boxPosition.z - 1f),
                new Vector3(boxPosition.x, boxPosition.y + 3f, boxPosition.z + 2f)
        );

        task = new Timer.Task() {
            @Override
            public void run() {
                if (!isActive) {
                    updateBox(true);
                }
            }
        };

        updateBox(false);
    }

    public void update(float delta, Camera camera) {
        if (OsUtils.isPC && checkCollision(camera)) {
            Triple<Integer, Double, String> gift = giveGift();
            displayGift(gift.getFirst(), gift.getSecond(), gift.getThird());
            updateBox(false);
        } else if (OsUtils.isMobile && isActive) {
            updateGiftboxPosition(delta);
        }
    }

    private void updateBox(boolean open) {
        if (OsUtils.isMobile) {
            updateBoxMobile(open);
        } else {
            updateBoxPC(open);
        }
    }

    private void updateBoxPC(boolean open) {
        String path;

        if (open) {
            path = "models/props/giftbox/giftbox_opened.glb";
            sceneManager.environment.add(light);

            if (!openedMusic.isPlaying()) {
                openedMusic.play();
            }
        } else {
            path = "models/props/giftbox/giftbox_closed.glb";
            sceneManager.environment.remove(light);
            restartTimer();

            if (openedMusic.isPlaying()) {
                openedMusic.stop();
            }

            if (isActive) {
                openedSound.play(soundVolume);
            }
        }

        isActive = open;

        if (scene != null) {
            sceneManager.removeScene(scene);
        }

        SceneAsset asset = assetManager.get(path, SceneAsset.class);
        scene = new Scene(asset.scene);

        scene.modelInstance.transform.setToScaling(boxScale);
        scene.modelInstance.transform.translate(boxPosition);
        scene.modelInstance.transform.rotate(Vector3.Y, boxZPosition);

        sceneManager.addScene(scene);
    }

    private void updateBoxMobile(boolean open) {
        if (open) {
            boxImage.setX(-boxImage.getWidth());
            stage.addActor(boxImage);

            openedSound.play(soundVolume);
        } else {
            restartTimer();
            boxImage.remove();
            totalDelta = 0f;
        }

        isActive = open;
    }

    private boolean checkCollision(Camera camera) {
        if (!isActive) {
            return false;
        }

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

    private void restartTimer() {
        int delaySeconds = Math.getRandomNumber(300, 600);
        Timer.schedule(task, delaySeconds);
    }

    private Triple<Integer, Double, String> giveGift() {
        Savegame savegame = Savegame.getInstance();

        int choice = Math.getRandomNumber(1, 3);
        double value;
        String petName = null;

        switch (choice) {
            // Pet
            case 1: {
                HashMap<String, Integer> pets = savegame.getPurchasedPets();
                List<Map.Entry<String, Integer>> list = new ArrayList<>(pets.entrySet());
                Collections.sort(list, new MapValueKeyComparator<>());

                try {
                    Map.Entry<String, Integer> pet = list.get(0);

                    PetManager petManager = MaxonGame.getInstance().getPetManager();
                    Pet petData = petManager.getPet(pet.getKey());

                    if (petData == null) {
                        value = 0.0;
                        break;
                    }

                    value = 1.0;

                    pets.put(pet.getKey(), pet.getValue() + (int) value);
                    savegame.increaseMultiplier(petData.getMultiplier());
                    petName = pet.getKey();
                } catch (Exception e) {
                    value = 0.0;
                }
                break;
            }
            // Multiplier
            case 2: {
                double v = Math.getRandomNumber(1, 5) / 100f;
                value = savegame.getMultiplier() * v;
                savegame.increaseMultiplier(value);
                break;
            }
            // Money
            default: {
                double v = Math.getRandomNumber(1, 5) / 1000f;
                value = savegame.getMoney() * v;
                savegame.increaseMoney(value);
                break;
            }
        }

        return new Triple<>(choice, value, petName);
    }

    private void displayGift(int choice, double value, String petName) {
        Table mainTable = new Table(skin);
        mainTable.setBackground("halftransparentblack");
        mainTable.setFillParent(true);
        mainTable.align(Align.center);

        mainTable.addAction(
                Actions.sequence(
                        Actions.alpha(0f),
                        Actions.alpha(1f, 0.25f),
                        Actions.delay(1f),
                        Actions.alpha(0f, 0.25f),
                        Actions.run(mainTable::clear)
                )
        );

        stage.addActor(mainTable);

        Table table = new Table(skin);
        table.setSize(300f, 500f);
        table.setBackground("bg");
        table.align(Align.center);
        table.pad(15f);
        mainTable.add(table);

        // Adding the title
        Label title = new Label(MaxonGame.getInstance().getLocale().getLine(LineId.GiftboxOpen), skin);
        table.add(title).row();

        String regionName;

        switch (choice) {
            case 1:
                regionName = petName != null ? petName : "pets";
                break;
            case 2:
                regionName = "multiplier";
                break;
            default:
                regionName = "points";
                break;
        }

        // Adding the icon
        TextureRegion region;

        if (petName != null && choice == 1) {
            PetManager petManager = MaxonGame.getInstance().getPetManager();
            Pet petData = petManager.getPet(regionName);

            region = petData.getIcon().getFrame(0);
        } else {
            TextureAtlas atlas = assetManager.get("sprites/gui/player_icons.atlas", TextureAtlas.class);
            region = atlas.findRegion(regionName);
        }

        Image icon = new Image(region);
        icon.setSize(64f, 64f);
        table.add(icon).pad(8f).row();

        // Adding the value
        Label amount = new Label(NumberFormatter.format((long) value), skin);
        table.add(amount).pad(8f).row();
    }

    private void updateGiftboxPosition(float delta) {
        if (boxImage.getX() - boxImage.getWidth() / 2f > stage.getWidth()) {
            updateBoxMobile(false);
            return;
        }

        totalDelta += delta;
        boxImage.setX(boxImage.getX() + 150f * delta);
        boxImage.setY(stage.getHeight() / 2f + 50f * (float) java.lang.Math.sin(2f * java.lang.Math.PI * totalDelta));
    }

    @Override
    public void dispose() {
        openedMusic.stop();
        openedSound.stop();
        task.cancel();
    }
}
