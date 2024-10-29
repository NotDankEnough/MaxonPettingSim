package kz.ilotterytea.maxon.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.RepeatAction;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import kz.ilotterytea.maxon.MaxonConstants;
import kz.ilotterytea.maxon.assets.AssetUtils;
import kz.ilotterytea.maxon.MaxonGame;
import kz.ilotterytea.maxon.utils.OsUtils;

public class SplashScreen implements Screen {
    private MaxonGame game = MaxonGame.getInstance();

    private Stage stage;

    private TextureAtlas brandAtlas;
    private Skin contributorsSkin;
    private Sound clickSound;
    private float soundVolume;

    private ProgressBar bar;

    private boolean assetsLoaded = false;

    @Override public void show() {
        this.game = MaxonGame.getInstance();
        clickSound = Gdx.audio.newSound(Gdx.files.internal("sfx/ui/click.ogg"));
        soundVolume = game.prefs.getInteger("sfx", 10) / 10f;

        this.stage = new Stage(new FitViewport(800, 600));
        Skin skin = new Skin(Gdx.files.internal("sprites/gui/ui.skin"));

        Table logoTable = new Table();
        logoTable.setFillParent(true);
        logoTable.align(Align.center);

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

        // Showing contributors
        Table contributorsTable = new Table();
        contributorsSkin = new Skin(Gdx.files.internal("sprites/gui/friends.skin"));

        for (int i = 0; i < MaxonConstants.GAME_DEVELOPERS.length; i++) {
            String name = MaxonConstants.GAME_DEVELOPERS[i][0];
            String url = MaxonConstants.GAME_DEVELOPERS[i][1];

            ImageButton imageButton = new ImageButton(contributorsSkin, name);

            imageButton.addAction(
                    Actions.sequence(
                            Actions.delay(0.5f * i),
                            Actions.repeat(
                                    RepeatAction.FOREVER,
                                    Actions.sequence(
                                            Actions.moveBy(0f, 20f, 2f, Interpolation.smoother),
                                            Actions.moveBy(0f, -20f, 2f, Interpolation.smoother)
                                    )
                            )
                    )
            );

            imageButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    super.clicked(event, x, y);
                    Gdx.net.openURI(url);
                    clickSound.play(soundVolume);
                }
            });

            Cell<ImageButton> cell = contributorsTable.add(imageButton).size(OsUtils.isMobile ? 64f : 32f);
            if (i + 1 < MaxonConstants.GAME_DEVELOPERS.length) cell.padRight(OsUtils.isMobile? 32f : 8f);
        }

        logoTable.add(contributorsTable).width(image.getWidth()).padBottom(30f).row();

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
            stage.addAction(
                    Actions.sequence(
                            Actions.alpha(0.0f, 1f),
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
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.draw();
        stage.act(delta);

        update();
        bar.setValue(100f / (game.assetManager.getQueuedAssets() + 1));
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() { dispose(); }
    @Override public void dispose() {
        brandAtlas.dispose();
        contributorsSkin.dispose();
        clickSound.dispose();
    }
}
