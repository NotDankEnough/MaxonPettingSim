package kz.ilotterytea.maxon.screens.game;

import com.badlogic.gdx.*;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import kz.ilotterytea.maxon.MaxonGame;
import kz.ilotterytea.maxon.anim.SpriteUtils;
import kz.ilotterytea.maxon.audio.Playlist;
import kz.ilotterytea.maxon.constants.SettingsConstants;
import kz.ilotterytea.maxon.inputprocessors.CrossProcessor;
import kz.ilotterytea.maxon.pets.Pet;
import kz.ilotterytea.maxon.pets.PetManager;
import kz.ilotterytea.maxon.player.DecalPlayer;
import kz.ilotterytea.maxon.player.Savegame;
import kz.ilotterytea.maxon.screens.MenuScreen;
import kz.ilotterytea.maxon.screens.game.shop.ShopUI;
import kz.ilotterytea.maxon.tasks.MultiplierTask;
import kz.ilotterytea.maxon.ui.*;
import kz.ilotterytea.maxon.ui.game.QuickActionsTable;
import kz.ilotterytea.maxon.utils.OsUtils;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalShadowLight;
import net.mgsx.gltf.scene3d.lights.PointLightEx;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.scene.SceneSkybox;
import net.mgsx.gltf.scene3d.utils.EnvironmentUtil;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;

import java.util.ArrayList;

public class GameScreen implements Screen, InputProcessor {
    private MaxonGame game;
    private long playTimestamp;

    private final Savegame savegame = Savegame.getInstance();

    private Stage stage;
    private Skin uiSkin;

    private Playlist playlist;

    private ShopUI shopUI;

    private SceneManager sceneManager;
    private PerspectiveCamera camera;

    private DecalBatch decalBatch;
    private ArrayList<Decal> decals;
    private DecalPlayer decalPlayer;

    private float elapsedTime = 0.0f;

    private Giftbox giftbox;

    private final ArrayList<Timer.Task> tasks = new ArrayList<>();

    @Override
    public void show() {
        this.game = MaxonGame.getInstance();
        this.playTimestamp = System.currentTimeMillis();
        if (savegame.isNewlyCreated()) savegame.setNewlyCreated(false);

        create3D();

        decalBatch = new DecalBatch(new CameraGroupStrategy(camera));
        decals = new ArrayList<>();

        ArrayList<TextureRegion> playerTextureRegions = SpriteUtils.splitToTextureRegions(game.assetManager.get("sprites/sheet/loadingCircle.png", Texture.class), 112, 112);
        decalPlayer = new DecalPlayer(savegame, playerTextureRegions);
        decals.add(decalPlayer.getDecal());

        playlist = new Playlist(
                game.assetManager.get("mus/game/onwards.mp3", Music.class),
                game.assetManager.get("mus/game/paris.mp3", Music.class),
                game.assetManager.get("mus/game/adieu.mp3", Music.class),
                game.assetManager.get("mus/game/shopping_spree.mp3", Music.class)
        );
        playlist.setShuffleMode(true);
        playlist.setVolume(game.prefs.getInteger(SettingsConstants.MUSIC_NAME, 10) / 10f);
        playlist.next();

        createStageUI();

        giftbox = new Giftbox(stage, uiSkin, game.assetManager, sceneManager);

        Gdx.input.setInputProcessor(new InputMultiplexer(this, new CrossProcessor(), stage));

        tasks.add(Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                savegame.save();
            }
        }, 10, 10));

        // Add a 1/10th multiplier to the money every 1/10th of a second.
        tasks.add(Timer.schedule(new MultiplierTask(savegame, decalPlayer, shopUI.getMultiplierLabel()), 0.1f, 0.1f));

        camera.update();
        render(Gdx.graphics.getDeltaTime());
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        if (!playlist.getPlayingNow().isPlaying()) {
            playlist.next();
        }

        // Render 3D
        sceneManager.update(Gdx.graphics.getDeltaTime());
        sceneManager.render();

        this.giftbox.update(delta, this.camera);

        if (!shopUI.isShopListOpened()) {
            this.decalPlayer.render(this.camera);
        }

        for (Decal decal : this.decals) {
            decal.lookAt(this.camera.position, this.camera.up);
            this.decalBatch.add(decal);
        }

        // - - -  R E N D E R I N G  P E T S  - - -
        ArrayList<Decal> petDecals = new ArrayList<>();

        // Getting the pet decals
        for (String id : savegame.getPurchasedPets().keySet()) {
            PetManager petManager = game.getPetManager();
            Pet pet = petManager.getPet(id);

            if (pet == null) {
                continue;
            }

            int amount = savegame.getPurchasedPets().get(id);
            int majorAmount = amount / 10;
            int minorAmount = amount % 10;

            Color color = switch (majorAmount % 10) {
                case 1, 2 -> Color.BLUE;
                case 3, 4 -> Color.PINK;
                case 5, 6 -> Color.ORANGE;
                case 7, 8 -> Color.GREEN;
                case 9 -> Color.GRAY;
                default -> Color.WHITE;
            };

            for (int i = 0; i < majorAmount; i++) {
                Decal decal = pet.getDecal();
                decal = Decal.newDecal(decal.getWidth(), decal.getHeight(), decal.getTextureRegion());
                decal.setColor(color);

                float multiplier = majorAmount * 0.1f;

                decal.setScale(1.0f + multiplier);
                decal.setY(decal.getY() + multiplier * 2.5f);

                petDecals.add(decal);
            }

            for (int i = 0; i < minorAmount; i++) {
                Decal decal = pet.getDecal();
                decal = Decal.newDecal(decal.getWidth(), decal.getHeight(), decal.getTextureRegion());
                petDecals.add(decal);
            }
        }

        elapsedTime += delta * decalPlayer.getClickStreak() / 10f;

        // Rendering the pet decals
        for (int i = 0; i < petDecals.size(); i++) {
            Decal decal = petDecals.get(i);

            float angle = elapsedTime + (i * MathUtils.PI2 / petDecals.size());
            float radius = 2.0f;
            float x = MathUtils.cos(angle) * radius;
            float z = MathUtils.sin(angle) * radius;

            decal.setPosition(decalPlayer.getDecal().getX() + x, 0.5f, decalPlayer.getDecal().getZ() + z);
            decal.lookAt(this.camera.position, this.camera.up);
            this.decalBatch.add(decal);
        }

        this.decalBatch.flush();

        stage.act(delta);
        stage.draw();

        shopUI.render();
    }

    @Override
    public void resize(int width, int height) {
        this.stage.getViewport().update(width, height, true);
        sceneManager.updateViewport(width, height);
        this.shopUI.resize(stage.getWidth());
    }

    @Override public void pause() {}

    @Override public void resume() {}

    @Override public void hide() {
        for (Timer.Task task : tasks) {
            task.cancel();
        }
        tasks.clear();

        playlist.getPlayingNow().stop();

        savegame.setElapsedTime((System.currentTimeMillis() - playTimestamp) + savegame.getElapsedTime());
        savegame.save();

        dispose();
    }

    @Override
    public void dispose() {
        giftbox.dispose();
        stage.clear();
        decalPlayer.dispose();
        sceneManager.dispose();
    }

    @Override
    public boolean keyDown(int keycode) {
        if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MenuScreen());
            return true;
        }
        return false;
    }

    private void create3D() {
        sceneManager = new SceneManager();

        if (!OsUtils.isMobile) {
            SceneAsset sceneAsset = game.assetManager.get("models/scenes/living_room.glb", SceneAsset.class);
            Scene scene = new Scene(sceneAsset.scene);
            sceneManager.addScene(scene);
        }

        camera = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 1f;
        camera.far = 300f;
        camera.position.set(-3f, 2f, -0.3f);

        float angle = 256f;

        if (OsUtils.isMobile) {
            angle = 155f;
        }

        camera.rotate(angle, 0f, 1f, 0f);

        camera.update();

        sceneManager.setCamera(camera);

        DirectionalShadowLight light = new DirectionalShadowLight(1024, 1024, 60f, 60f, 1f, 300f);
        light.set(new Color(0xdcccffff), -1f, -0.8f, -0.2f);
        light.intensity = 5f;
        sceneManager.environment.add(light);
        sceneManager.environment.shadowMap = light;

        PointLightEx signLight = new PointLightEx();
        signLight.set(Color.PINK, new Vector3(2f, 6f, 2f), 80f, 100f);

        PointLightEx windowLight = new PointLightEx();
        windowLight.set(Color.BLUE, new Vector3(-1.1f, 7.3f, 0.5f), 80f, 100f);

        sceneManager.environment.add(windowLight, signLight);

        // setup quick IBL (image based lighting)
        IBLBuilder iblBuilder = IBLBuilder.createOutdoor(light);

        Cubemap environmentCubemap;

        if (OsUtils.isMobile) {
            environmentCubemap = EnvironmentUtil.createCubemap(
                    new InternalFileHandleResolver(),
                    "skyboxes/game/",
                    ".png",
                    EnvironmentUtil.FACE_NAMES_NEG_POS
            );
        } else {
            environmentCubemap = iblBuilder.buildEnvMap(1000);
        }

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

    private void createStageUI() {
        this.stage = new Stage(new ScreenViewport());
        TextureAtlas playerIconAtlas = this.game.assetManager.get("sprites/gui/player_icons.atlas", TextureAtlas.class);

        this.uiSkin = this.game.assetManager.get("sprites/gui/ui.skin", Skin.class);

        this.shopUI = new ShopUI(savegame, this.stage, this.uiSkin, playerIconAtlas);

        if (OsUtils.isMobile) {
            shopUI.createSavegameUI();
        }

        if (OsUtils.isMobile) shopUI.createShopTitleUI();
        shopUI.createShopMerchant();
        shopUI.createShopControlUI();
        shopUI.createShopListUI();

        if (!OsUtils.isMobile) {
            shopUI.createSavegameUI();
        }

        DebugWidget debugWidget = new DebugWidget(uiSkin);
        this.stage.addActor(debugWidget);

        QuickActionsTable quickActionsTable = new QuickActionsTable(this.game.assetManager.get("sprites/gui/widgets.skin", Skin.class), uiSkin);
        if (OsUtils.isMobile) {
            quickActionsTable.setFillParent(true);
            quickActionsTable.align(Align.bottom);
        }

        this.stage.addActor(quickActionsTable);
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchCancelled(int i, int i1, int i2, int i3) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }
}
