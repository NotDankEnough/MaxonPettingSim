package com.ilotterytea.maxoning.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.RepeatAction;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.ilotterytea.maxoning.MaxonConstants;
import com.ilotterytea.maxoning.MaxonGame;
import com.ilotterytea.maxoning.player.MaxonSavegame;
import com.ilotterytea.maxoning.ui.*;
import com.ilotterytea.maxoning.utils.I18N;
import com.ilotterytea.maxoning.utils.serialization.GameDataSystem;

import java.util.ArrayList;

public class MenuScreen implements Screen {
    private final MaxonGame game;

    private final Stage stage;
    private final Music menuMusic;

    MaxonSavegame sav;

    public MenuScreen(final MaxonGame game) {
        this.game = game;

        // Stage and skin:
        this.stage = new Stage(new ScreenViewport());
        this.stage.addAction(Actions.sequence(Actions.alpha(0.0f), Actions.alpha(1.0f, 1f)));

        Skin skin = game.assetManager.get("MainSpritesheet.skin", Skin.class);
        Skin widgetSkin = game.assetManager.get("sprites/gui/widgets.skin", Skin.class);
        TextureAtlas brandAtlas = game.assetManager.get("sprites/gui/brand.atlas", TextureAtlas.class);
        TextureAtlas widgetAtlas = game.assetManager.get("sprites/gui/widgets.atlas", TextureAtlas.class);

        sav = GameDataSystem.load("00.maxon");

        // Main Menu music:
        this.menuMusic = game.assetManager.get("mus/menu/mus_menu_loop.ogg", Music.class);
        menuMusic.setLooping(true);

        // - - - - - -  U I  - - - - - -
        Table menuTable = new Table();
        menuTable.setFillParent(true);

        // - - -  Brand - - -
        Table brandTable = new Table();

        Image logo = new Image(brandAtlas.findRegion("brand"));

        logo.setOrigin(
                logo.getWidth() / 2f,
                logo.getHeight() / 2f
        );

        logo.addAction(
                Actions.repeat(
                        RepeatAction.FOREVER,
                        Actions.sequence(
                                Actions.parallel(
                                        Actions.rotateTo(-5f, 5f, Interpolation.smoother),
                                        Actions.scaleTo(0.9f, 0.9f, 5f, Interpolation.smoother)
                                ),
                                Actions.parallel(
                                        Actions.rotateTo(5f, 5f, Interpolation.smoother),
                                        Actions.scaleTo(1.1f, 1.1f, 5f, Interpolation.smoother)
                                )
                        )
                )
        );

        brandTable.add(logo);

        // - - -  Menu control (quit, options, etc.) - - -
        Table controlTable = new Table(skin);
        controlTable.align(Align.top | Align.center);
        controlTable.pad(6f);

        // Left part of menu control
        Table leftGameControlTable = new Table();
        leftGameControlTable.align(Align.left);

        ImageButton quitButton = new ImageButton(widgetSkin, "quit");

        quitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                Gdx.app.exit();
            }
        });

        leftGameControlTable.add(quitButton).padRight(12f);

        // Right part of menu control
        Table rightGameControlTable = new Table();
        rightGameControlTable.align(Align.right);

        // Localization
        String[] fh4Locale = game.locale.getFileHandle().nameWithoutExtension().split("_");
        String localeButtonStyleName = "locale_" + fh4Locale[0];
        ImageButton localeButton = new ImageButton(widgetSkin, localeButtonStyleName);

        localeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);

                int index = 0;
                ArrayList<FileHandle> fhArray = new ArrayList<>();
                fhArray.add(MaxonConstants.FILE_RU_RU);
                fhArray.add(MaxonConstants.FILE_EN_US);

                if (fhArray.indexOf(game.locale.getFileHandle()) + 1 < fhArray.size()) {
                    index = fhArray.indexOf(game.locale.getFileHandle()) + 1;
                }

                FileHandle fhNext = fhArray.get(index);

                game.locale = new I18N(fhNext);
                game.prefs.putString("lang", fhNext.nameWithoutExtension());
                game.prefs.flush();

                game.setScreen(new SplashScreen(game));
                menuMusic.stop();
            }
        });


        rightGameControlTable.add(localeButton).padRight(16f);

        // Music button
        String musicButtonStyleName;

        if (game.prefs.getBoolean("music")) {
            musicButtonStyleName = "music_on";
            menuMusic.play();
        } else {
            musicButtonStyleName = "music_off";
        }

        ImageButton musicButton = new ImageButton(widgetSkin, musicButtonStyleName);
        musicButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);

                Button.ButtonStyle style;

                if (game.prefs.getBoolean("music")) {
                    style = widgetSkin.get("music_off", ImageButton.ImageButtonStyle.class);
                    menuMusic.pause();
                } else {
                    style = widgetSkin.get("music_on", ImageButton.ImageButtonStyle.class);
                    menuMusic.play();
                }

                game.prefs.putBoolean("music", !game.prefs.getBoolean("music"));
                game.prefs.flush();

                musicButton.setStyle(style);
            }
        });
        rightGameControlTable.add(musicButton).padRight(16f);

        // Resolution button
        String resolutionButtonStyleName;

        if (game.prefs.getBoolean("fullscreen")) {
            resolutionButtonStyleName = "windowed";
        } else {
            resolutionButtonStyleName = "fullscreen";
        }

        ImageButton resolutionButton = new ImageButton(widgetSkin, resolutionButtonStyleName);
        resolutionButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);

                Button.ButtonStyle style;

                if (game.prefs.getBoolean("fullscreen")) {
                    style = widgetSkin.get("fullscreen", ImageButton.ImageButtonStyle.class);
                    Gdx.graphics.setWindowedMode(game.prefs.getInteger("width", 800), game.prefs.getInteger("height", 600));
                } else {
                    style = widgetSkin.get("windowed", ImageButton.ImageButtonStyle.class);
                    Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
                }

                game.prefs.putBoolean("fullscreen", !game.prefs.getBoolean("fullscreen"));
                game.prefs.flush();

                resolutionButton.setStyle(style);
            }
        });
        rightGameControlTable.add(resolutionButton);

        controlTable.add(leftGameControlTable).grow();
        controlTable.add(rightGameControlTable).grow();

        // - - -  Savegame  - - -
        Table savegameTable = new Table();
        SavegameWidget info = new SavegameWidget(this.game, skin, sav);

        savegameTable.add(info).minSize(640f, 240f);

        // Adding tables into the main UI table
        menuTable.add(brandTable).grow().row();
        menuTable.add(savegameTable).grow().row();
        menuTable.add(controlTable).growX();

        this.stage.addActor(menuTable);

        Gdx.input.setInputProcessor(stage);
    }

    @Override public void show() {
        if (game.prefs.getBoolean("music", true)) menuMusic.play();

        // Start to render:
        render(Gdx.graphics.getDeltaTime());
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(1, 1, 1, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {
        menuMusic.stop();
        dispose();
    }
    @Override public void dispose() {
        stage.dispose();
    }
}
