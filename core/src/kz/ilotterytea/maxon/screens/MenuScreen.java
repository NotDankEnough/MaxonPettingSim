package kz.ilotterytea.maxon.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.RepeatAction;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import kz.ilotterytea.javaextra.tuples.Triple;
import kz.ilotterytea.maxon.MaxonConstants;
import kz.ilotterytea.maxon.MaxonGame;
import kz.ilotterytea.maxon.localization.LineId;
import kz.ilotterytea.maxon.localization.LocalizationManager;
import kz.ilotterytea.maxon.player.Savegame;
import kz.ilotterytea.maxon.ui.*;
import kz.ilotterytea.maxon.utils.GameUpdater;
import kz.ilotterytea.maxon.utils.OsUtils;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalShadowLight;
import net.mgsx.gltf.scene3d.lights.PointLightEx;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.scene.SceneSkybox;
import net.mgsx.gltf.scene3d.utils.EnvironmentUtil;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;

import java.util.ArrayList;

public class MenuScreen implements Screen {
    private MaxonGame game;

    private Stage stage;
    private Skin uiSkin;
    private Music menuMusic;

    private final Savegame savegame = Savegame.getInstance();

    private SceneManager sceneManager;
    private PerspectiveCamera camera;

    private final ArrayList<Timer.Task> tasks = new ArrayList<>();
    private Sound clickSound;
    private float soundVolume;

    @Override public void show() {
        this.game = MaxonGame.getInstance();
        game.getDiscordActivityClient().runThread();

        // Stage and skin:
        this.stage = new Stage(new ScreenViewport());
        this.stage.addAction(Actions.sequence(Actions.alpha(0.0f), Actions.alpha(1.0f, 1f)));

        this.uiSkin = game.assetManager.get("sprites/gui/ui.skin", Skin.class);
        Skin widgetSkin = game.assetManager.get("sprites/gui/widgets.skin", Skin.class);
        TextureAtlas brandAtlas = game.assetManager.get("sprites/gui/brand.atlas", TextureAtlas.class);

        // Main Menu music:
        this.menuMusic = game.assetManager.get("mus/menu/mus_menu_loop.mp3", Music.class);
        menuMusic.setLooping(true);

        clickSound = game.assetManager.get("sfx/ui/click.ogg", Sound.class);
        soundVolume = game.prefs.getInteger("sfx", 10) / 10f;

        // - - - - - -  U I  - - - - - -
        float iconSize = OsUtils.isMobile ? 256f : 64f;

        Table menuTable = new Table();
        menuTable.setFillParent(true);

        // - - -  Brand - - -
        Table brandTable = new Table();
        brandTable.align(Align.center);

        Image logo = new Image(brandAtlas.findRegion("brand"));

        logo.setOrigin(
                logo.getWidth() / 2f,
                logo.getHeight() / 2f
        );

        if (OsUtils.isMobile) {
            logo.addAction(
                    Actions.repeat(
                            RepeatAction.FOREVER,
                            Actions.sequence(
                                    Actions.scaleTo(0.9f, 0.9f, 5f, Interpolation.smoother),
                                    Actions.scaleTo(1.0f, 1.0f, 5f, Interpolation.smoother)
                            )
                    )
            );

            float stageWidth = this.stage.getWidth() - 10f;
            float difference = stageWidth / logo.getWidth();

            brandTable.add(logo).size(stageWidth, logo.getHeight() * difference);
        } else {
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
        }

        logo.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                openCredits();
            }
        });

        Image updateLogo;

        if (OsUtils.isMobile) {
            updateLogo = new Image(brandAtlas.findRegion("beta"));
        } else {
            updateLogo = new Image(brandAtlas.findRegion("update"));
        }

        updateLogo.setOrigin(
                updateLogo.getWidth() / 2f,
                updateLogo.getHeight() / 2f
        );

        updateLogo.setRotation(10f);

        updateLogo.addAction(
                Actions.repeat(
                        RepeatAction.FOREVER,
                        Actions.sequence(
                                Actions.scaleTo(0.9f, 0.9f, 0.25f, Interpolation.circleIn),
                                Actions.scaleTo(1f, 1f, 0.25f, Interpolation.circleOut)
                        )
                )
        );

        brandTable.add(updateLogo)
                .size(updateLogo.getWidth() / 2f, updateLogo.getHeight() / 2f)
                .padLeft(updateLogo.getWidth() / -2f)
                .padBottom(-86f);

        // - - -  Menu control (quit, options, etc.) - - -
        Table controlTable = new Table();
        controlTable.align(Align.top | Align.center);
        controlTable.pad(6f);

        // Left part of menu control
        Table leftGameControlTable = new Table();
        leftGameControlTable.align(Align.left);

        ShakingImageButton quitButton = new ShakingImageButton(widgetSkin, "exit");
        quitButton.setOrigin(iconSize / 2f, iconSize / 2f);

        quitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                clickSound.play(soundVolume);
                Gdx.app.exit();
            }
        });

        leftGameControlTable.add(quitButton).size(iconSize).padRight(12f);

        // Right part of menu control
        Table rightGameControlTable = new Table();
        if (OsUtils.isMobile) {
            rightGameControlTable.align(Align.center);
        } else {
            rightGameControlTable.align(Align.right);
        }

        // Localization
        String[] fh4Locale = game.getLocale().getHandle().nameWithoutExtension().split("_");
        String localeButtonStyleName = "locale_" + fh4Locale[0];
        ShakingImageButton localeButton = new ShakingImageButton(widgetSkin, localeButtonStyleName);
        localeButton.setOrigin(iconSize / 2f, iconSize / 2f);

        localeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);

                int index = 0;
                ArrayList<FileHandle> fhArray = new ArrayList<>();
                fhArray.add(MaxonConstants.FILE_RU_RU);
                fhArray.add(MaxonConstants.FILE_EN_US);

                if (fhArray.indexOf(game.getLocale().getHandle()) + 1 < fhArray.size()) {
                    index = fhArray.indexOf(game.getLocale().getHandle()) + 1;
                }

                FileHandle fhNext = fhArray.get(index);

                game.setLocale(new LocalizationManager(fhNext));
                game.prefs.putString("lang", fhNext.nameWithoutExtension());
                game.prefs.flush();

                game.setScreen(new SplashScreen());
                menuMusic.stop();
                clickSound.play(soundVolume);
            }
        });

        // Music button
        menuMusic.setVolume(game.prefs.getInteger("music", 10) / 10f);
        String musicButtonStyleName;

        if (OsUtils.isPC || game.prefs.getInteger("music", 10) > 0) {
            musicButtonStyleName = "music_on";
            menuMusic.play();
        } else {
            musicButtonStyleName = "music_off";
        }

        ShakingImageButton musicButton = new ShakingImageButton(widgetSkin, musicButtonStyleName);
        musicButton.setOrigin(iconSize / 2f, iconSize / 2f);
        musicButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);

                if (OsUtils.isPC) {
                    openSoundControl();
                    return;
                }

                String style;
                int v = game.prefs.getInteger("music", 10);

                if (v > 0) {
                    style = "music_off";
                    menuMusic.pause();
                } else {
                    style = "music_on";
                    menuMusic.play();
                }

                game.prefs.putInteger("music", game.prefs.getInteger("music", 10) > 0 ? 0 : 10);
                game.prefs.flush();

                musicButton.setDrawable(widgetSkin, style);
                clickSound.play(soundVolume);
            }
        });

        if (!OsUtils.isMobile) {
            rightGameControlTable.add(localeButton).size(iconSize).padRight(16f);
            rightGameControlTable.add(musicButton).size(iconSize).padRight(16f);

            // Resolution button
            String resolutionButtonStyleName;

            if (game.prefs.getBoolean("fullscreen")) {
                resolutionButtonStyleName = "windowed";
            } else {
                resolutionButtonStyleName = "fullscreen";
            }

            ShakingImageButton resolutionButton = new ShakingImageButton(widgetSkin, resolutionButtonStyleName);
            resolutionButton.setOrigin(iconSize / 2f, iconSize / 2f);
            resolutionButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    super.clicked(event, x, y);

                    String style;

                    if (game.prefs.getBoolean("fullscreen")) {
                        style = "fullscreen";
                        Gdx.graphics.setWindowedMode(game.prefs.getInteger("width", 800), game.prefs.getInteger("height", 600));
                    } else {
                        style = "windowed";
                        Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
                    }

                    game.prefs.putBoolean("fullscreen", !game.prefs.getBoolean("fullscreen"));
                    game.prefs.flush();

                    resolutionButton.setDrawable(widgetSkin, style);
                    clickSound.play(soundVolume);
                }
            });
            rightGameControlTable.add(resolutionButton).size(iconSize);

            controlTable.add(leftGameControlTable).grow();
        } else {
            rightGameControlTable.add(localeButton).size(iconSize).expand();
            rightGameControlTable.add(musicButton).size(iconSize).expand();
        }

        controlTable.add(rightGameControlTable).grow();

        // - - -  Savegame  - - -
        Table savegameTable = new Table();
        SavegameWidget info = new SavegameWidget(this.game, uiSkin, stage, savegame);

        if (OsUtils.isMobile) {
            savegameTable.add(info).growX().minHeight(240f).pad(16f);
        } else {
            savegameTable.add(info).minSize(640f, 240f);
        }

        // Suggest an update
        if (!GameUpdater.CLIENT_IS_ON_LATEST_VERSION && OsUtils.isPC) {
            TextButton updateButton = new TextButton(game.getLocale().getLine(LineId.UpdaterInfo), uiSkin, "link");

            updateButton.setPosition(8f, stage.getHeight() - 32f);

            updateButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    super.clicked(event, x, y);
                    Gdx.net.openURI(MaxonConstants.GAME_APP_URL);
                }
            });

            menuTable.add(updateButton).pad(6f).left().row();
        }

        // Adding tables into the main UI table
        menuTable.add(brandTable).grow().row();
        menuTable.add(savegameTable).grow().row();
        menuTable.add(controlTable).growX();

        this.stage.addActor(menuTable);

        DebugWidget debugWidget = new DebugWidget(uiSkin);
        this.stage.addActor(debugWidget);

        create3D();
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        sceneManager.update(delta);
        sceneManager.render();

        camera.rotate(2 * delta, 0, 1, 0);
        camera.update();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        sceneManager.updateViewport(width, height);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {
        for (Timer.Task task : tasks) {
            task.cancel();
        }
        tasks.clear();

        menuMusic.stop();
        dispose();
    }
    @Override public void dispose() {
        stage.dispose();
        sceneManager.dispose();
    }

    private void create3D() {
        sceneManager = new SceneManager();

        camera = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 1f;
        camera.far = 300f;
        camera.position.set(0f, 5f, 0f);

        camera.update();

        sceneManager.setCamera(camera);

        DirectionalShadowLight light = new DirectionalShadowLight(1024, 1024, 60f, 60f, 1f, 300f);
        light.set(new Color(0xdcccffff), -1f, -0.8f, -0.2f);
        light.intensity = 5f;
        sceneManager.environment.add(light);
        sceneManager.environment.shadowMap = light;

        PointLightEx signLight = new PointLightEx();

        if (!savegame.isNewlyCreated()) {
            signLight.set(Color.PINK, new Vector3(2f, 6f, 2f), 80f, 100f);
        }

        PointLightEx windowLight = new PointLightEx();
        windowLight.set(Color.BLUE, new Vector3(-1.1f, 7.3f, 0.5f), 80f, 100f);

        sceneManager.environment.add(windowLight, signLight);

        // setup quick IBL (image based lighting)
        IBLBuilder iblBuilder = IBLBuilder.createOutdoor(light);

        Cubemap environmentCubemap = EnvironmentUtil.createCubemap(
                new InternalFileHandleResolver(),
                "skyboxes/menu/",
                ".png",
                EnvironmentUtil.FACE_NAMES_NEG_POS
        );

        Cubemap diffuseCubemap = iblBuilder.buildIrradianceMap(256);
        Cubemap specularCubemap = iblBuilder.buildRadianceMap(10);
        iblBuilder.dispose();

        Texture brdfLUT = new Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png"));

        sceneManager.setAmbientLight(1f);
        sceneManager.environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT));
        sceneManager.environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap));
        sceneManager.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap));

        sceneManager.setSkyBox(new SceneSkybox(environmentCubemap));
    }

    private void openSoundControl() {
        Image bgTint = new Image(uiSkin, "halftransparentblack");
        bgTint.setFillParent(true);
        stage.addActor(bgTint);

        // Table
        Table table = new Table();
        table.setFillParent(true);
        table.align(Align.center);

        stage.addActor(table);

        // Window
        Table window = new Table(uiSkin);
        window.setBackground("bg");
        window.align(Align.center);
        table.add(window).size(400f, 250f);

        // Table for title and close button
        Table titleTable = new Table(uiSkin);
        titleTable.setBackground("bg2");
        window.add(titleTable).growX().row();

        // Title
        Label titleLabel = new Label(game.getLocale().getLine(LineId.SoundsTitle), uiSkin);
        titleTable.add(titleLabel).pad(8f, 16f, 8f, 16f).growX();

        // Close button
        TextButton closeButton = new TextButton(" X ", uiSkin);
        closeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                table.remove();
                bgTint.remove();
                clickSound.play(soundVolume);
            }
        });
        titleTable.add(closeButton).pad(8f, 16f, 8f, 16f);

        // Table for sliders
        Table contentTable = new Table();
        window.add(contentTable).pad(16f).grow().row();

        // Creating sliders
        String[] sliderNames = {"music", "sfx"};

        for (String sliderName : sliderNames) {
            Label name = new Label(game.getLocale().getLine(LineId.fromJson("sounds." + sliderName)), uiSkin);
            Slider slider = new Slider(0f, 10f, 1f, false, uiSkin);
            slider.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (actor instanceof Slider s) {
                        int v = (int) s.getValue();
                        game.prefs.putInteger(sliderName, v);
                        game.prefs.flush();

                        switch (sliderName) {
                            case "music":
                                menuMusic.setVolume(v / 10f);
                                break;
                            case "sfx":
                                soundVolume = v / 10f;
                                break;
                            default:
                                break;
                        }

                        clickSound.play(soundVolume);
                    }
                }
            });

            int value = game.prefs.getInteger(sliderName, 10);

            if (value > 10) value = 10;
            else if (value < 0) value = 0;

            slider.setValue(value);
            game.prefs.putInteger(sliderName, value);
            game.prefs.flush();

            contentTable.add(name).grow();
            contentTable.add(slider).grow().row();
        }
    }

    private void openCredits() {
        String labelStyleName = OsUtils.isMobile ? "defaultMobile" : "default";

        Image bgTint = new Image(uiSkin, "halftransparentblack");
        bgTint.setFillParent(true);
        stage.addActor(bgTint);

        // Table
        Table table = new Table();
        table.setFillParent(true);
        table.align(Align.center);

        stage.addActor(table);

        // Window
        Table window = new Table(uiSkin);
        window.setBackground("bg");
        window.align(Align.center);
        Cell<Table> tableCell = table.add(window);

        if (OsUtils.isMobile) {
            tableCell.growX();
        } else {
            tableCell.size(650f, 450f);
        }

        // Table for title and close button
        Table titleTable = new Table(uiSkin);
        titleTable.setBackground("bg2");
        window.add(titleTable).growX().row();

        // Title
        Label titleLabel = new Label(game.getLocale().getLine(LineId.CreditsTitle), uiSkin, labelStyleName);
        titleTable.add(titleLabel).pad(8f, 16f, 8f, 16f).growX();

        // Close button
        TextButton closeButton = new TextButton(" X ", uiSkin, labelStyleName);
        closeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                table.remove();
                bgTint.remove();
                clickSound.play(soundVolume);
            }
        });
        titleTable.add(closeButton).pad(8f, 16f, 8f, 16f);

        // Table for contributors
        Table contributorTable = new Table();
        ScrollPane contributorPane = new ScrollPane(contributorTable, uiSkin);
        contributorPane.setScrollingDisabled(true, false);
        contributorPane.setFadeScrollBars(false);
        window.add(contributorPane).pad(16f).grow().row();

        // Creating contributors
        Skin friendSkin = game.assetManager.get("sprites/gui/friends.skin");
        for (int i = 0; i < MaxonConstants.GAME_DEVELOPERS.size(); i++) {
            Triple<String, String, Integer> contributor = MaxonConstants.GAME_DEVELOPERS.get(i);

            // Widget
            Table widget = new Table();
            widget.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    super.clicked(event, x, y);
                    Gdx.net.openURI(contributor.getSecond());
                    clickSound.play(soundVolume);
                }
            });
            widget.align(Align.center);
            contributorTable.add(widget).padBottom(16f);

            // Name
            Label name = new Label(contributor.getFirst(), uiSkin, OsUtils.isMobile ? "credits_name_mobile" : "credits_name");
            name.setAlignment(Align.center);
            widget.add(name).grow().padBottom(8f).row();

            // Icon
            Image icon = new Image(friendSkin.getDrawable(contributor.getFirst()));
            widget.add(icon).size(OsUtils.isMobile ? 128f : 64f).padBottom(8f).row();

            // Role
            LineId id = switch (contributor.getThird()) {
                case 1 -> LineId.CreditsDeveloper;
                case 2 -> LineId.CreditsContributor;
                case 3 -> LineId.CreditsMusic;
                case 4 -> LineId.CreditsIdea;
                case 5 -> LineId.CreditsTester;
                case 6 -> LineId.CreditsMoral;
                default -> LineId.CreditsMaxon;
            };

            Label role = new Label(game.getLocale().getLine(id), uiSkin, OsUtils.isMobile ? "credits_role_mobile" : "credits_role");
            role.setAlignment(Align.center);
            widget.add(role).grow().row();

            if (i % 3 == 2) contributorTable.row();
        }

        // Game engine credits
        Table engineCredits = new Table(uiSkin);
        engineCredits.setBackground("bg2");
        window.add(engineCredits).growX().row();

        Label engineLabel = new Label("Made with", uiSkin, labelStyleName);
        engineCredits.add(engineLabel).padRight(16f);

        TextureAtlas brandAtlas = game.assetManager.get("sprites/gui/brand.atlas");
        Image engineImage = new Image(brandAtlas.findRegion("engine"));
        engineImage.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                Gdx.net.openURI("https://libgdx.com");
                clickSound.play(soundVolume);
            }
        });
        engineCredits.add(engineImage).height(OsUtils.isMobile ? 64f : 32f).width(OsUtils.isMobile ? 360f : 180f);
    }
}
