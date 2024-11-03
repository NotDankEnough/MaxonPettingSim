package kz.ilotterytea.maxon.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.*;
import kz.ilotterytea.javaextra.tuples.Triple;
import kz.ilotterytea.maxon.MaxonConstants;
import kz.ilotterytea.maxon.assets.AssetUtils;
import kz.ilotterytea.maxon.MaxonGame;
import kz.ilotterytea.maxon.ui.MovingChessBackground;
import kz.ilotterytea.maxon.utils.OsUtils;

import java.util.ArrayList;

public class SplashScreen implements Screen {
    private MaxonGame game = MaxonGame.getInstance();

    private Stage stage;

    private MovingChessBackground background;

    private TextureAtlas brandAtlas;
    private Skin contributorSkin;

    private Image backgroundTint;
    private ProgressBar bar;

    private boolean assetsLoaded = false;

    @Override public void show() {
        this.game = MaxonGame.getInstance();

        this.stage = new Stage(new FitViewport(800, 600));
        Skin skin = new Skin(Gdx.files.internal("sprites/gui/ui.skin"));

        Table logoTable = new Table();
        logoTable.setFillParent(true);
        logoTable.align(Align.center);

        backgroundTint = new Image(skin, "black");
        backgroundTint.setFillParent(true);
        backgroundTint.addAction(Actions.alpha(0.5f));
        stage.addActor(backgroundTint);

        brandAtlas = new TextureAtlas(Gdx.files.internal("sprites/gui/ilotterytea.atlas"));
        Image image = new Image(brandAtlas.findRegion("devOld"));

        float stageWidth;

        if (OsUtils.isMobile) {
            stageWidth = this.stage.getWidth() - 20f;
        } else {
            stageWidth = 700f;
        }

        float difference = stageWidth / image.getWidth();
        image.setSize(stageWidth, image.getHeight() * difference);

        logoTable.add(image).size(image.getWidth(), image.getHeight()).padBottom(30f).row();

        // Extracting drawables of contributors
        ArrayList<Drawable> contributors = new ArrayList<>();
        contributorSkin = new Skin(Gdx.files.internal("sprites/gui/friends.skin"));

        for (Triple<String, String, Integer> contributor : MaxonConstants.GAME_DEVELOPERS) {
            String name = contributor.getFirst();
            Drawable icon = contributorSkin.getDrawable(name);
            contributors.add(icon);
        }

        // Background
        background = new MovingChessBackground(1f, 0f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), contributors);

        // Progress bar
        bar = new ProgressBar(0f, 100f, 1f, false, skin);
        logoTable.add(bar).width(image.getWidth());

        stage.addActor(logoTable);

        Gdx.input.setInputProcessor(stage);

        AssetUtils.setup(game.assetManager);
        AssetUtils.queue(game.assetManager);
    }

    private void update() {
        if (game.assetManager.update() && !assetsLoaded) {
            backgroundTint.addAction(
                    Actions.sequence(
                            Actions.alpha(1.0f, 1f),
                            new Action() {
                                @Override
                                public boolean act(float v) {
                                    game.getPetManager().load();
                                    game.setScreen(new MenuScreen());
                                    dispose();
                                    return false;
                                }
                            }
                    )
            );
            assetsLoaded = true;
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.batch.begin();
        background.draw(game.batch);
        game.batch.end();

        stage.draw();
        stage.act(delta);

        update();
        bar.setValue(100f / (game.assetManager.getQueuedAssets() + 1));
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        background.update(width, height);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() { dispose(); }
    @Override public void dispose() {
        brandAtlas.dispose();
        contributorSkin.dispose();
    }
}
