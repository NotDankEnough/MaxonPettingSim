package kz.ilotterytea.maxon;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import kz.ilotterytea.maxon.localization.LocalizationManager;
import kz.ilotterytea.maxon.pets.PetManager;
import kz.ilotterytea.maxon.screens.SplashScreen;
import kz.ilotterytea.maxon.session.SessionClient;
import kz.ilotterytea.maxon.utils.GameUpdater;

public class MaxonGame extends Game {
    public SpriteBatch batch;
    public AssetManager assetManager;
    public Preferences prefs;

    private LocalizationManager locale;
    private PetManager petManager;

    private DiscordActivityClient discordActivityClient;
    private SessionClient sessionClient;

    private static MaxonGame instance;

    public static MaxonGame getInstance() {
        if (instance == null) {
            instance = new MaxonGame();
        }
        return instance;
    }

    public PetManager getPetManager() {
        return petManager;
    }

    public DiscordActivityClient getDiscordActivityClient() {
        return discordActivityClient;
    }

    public SessionClient getSessionClient() {
        return sessionClient;
    }

    public LocalizationManager getLocale() {
        return locale;
    }

    public void setLocale(LocalizationManager locale) {
        this.locale = locale;
    }

    @Override
    public void create() {
        // Check the latest version
        new GameUpdater().checkLatestUpdate();

        sessionClient = new SessionClient(Gdx.app.getPreferences("kz.ilotterytea.SigninSession"));
        batch = new SpriteBatch();
        prefs = Gdx.app.getPreferences(MaxonConstants.GAME_APP_PACKAGE);
        locale = new LocalizationManager(Gdx.files.internal("i18n/" + prefs.getString("lang", "en_us") + ".json"));

        Gdx.graphics.setVSync(prefs.getBoolean("vsync", true));

        if (prefs.getBoolean("fullscreen", false)) {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        } else if (
                prefs.contains("width") ||
                        prefs.contains("height")
        ) {
            int width = prefs.getInteger("width", 800);

            if (width < 800) {
                width = 800;
                prefs.putInteger("width", width);
            }

            int height = prefs.getInteger("height", 600);

            if (height < 600) {
                height = 600;
                prefs.putInteger("height", height);
            }

            prefs.flush();
            Gdx.graphics.setWindowedMode(width, height);
        }

        assetManager = new AssetManager();
        petManager = new PetManager(assetManager);

        discordActivityClient = new DiscordActivityClient();

        this.setScreen(new SplashScreen());
    }

    @Override
    public void dispose() {
        batch.dispose();
        for (String name : assetManager.getAssetNames()) {
            assetManager.unload(name);
        }
        assetManager.dispose();
        discordActivityClient.dispose();
    }
}
