package kz.ilotterytea.maxon.ui.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import kz.ilotterytea.maxon.MaxonGame;
import kz.ilotterytea.maxon.constants.SettingsConstants;
import kz.ilotterytea.maxon.screens.MenuScreen;
import kz.ilotterytea.maxon.screens.SlotsMinigameScreen;
import kz.ilotterytea.maxon.ui.ShakingImageButton;
import kz.ilotterytea.maxon.utils.OsUtils;

public class QuickActionsTable extends Table {
    public QuickActionsTable(Skin widgetSkin, Skin uiSkin) {
        super(uiSkin);

        MaxonGame game = MaxonGame.getInstance();

        Sound clickSound = game.assetManager.get("sfx/ui/click.ogg");
        float soundVolume = game.prefs.getInteger(SettingsConstants.SFX_NAME, 10) / 10f;

        float iconSize = (OsUtils.isMobile ? 256f : 64f)
        * game.prefs.getFloat("guiScale", SettingsConstants.UI_DEFAULT_SCALE);

        ShakingImageButton slotsButton = new ShakingImageButton(widgetSkin, "slots");
        slotsButton.setOrigin(iconSize / 2f, iconSize / 2f);
        slotsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                clickSound.play(soundVolume);
                game.setScreen(new SlotsMinigameScreen());
            }
        });
        Cell<ShakingImageButton> slotsCell = add(slotsButton).size(iconSize).padRight(8f);

        ShakingImageButton quitButton = new ShakingImageButton(widgetSkin, "exit");
        quitButton.setOrigin(iconSize / 2f, iconSize / 2f);
        quitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                clickSound.play(soundVolume);
                game.setScreen(new MenuScreen());
            }
        });
        Cell<ShakingImageButton> quitCell = add(quitButton).size(iconSize);

        if (OsUtils.isMobile) {
            slotsCell.expandX();
            quitCell.expandX();
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);

        if (OsUtils.isMobile) return;

        // i'm not sure how much does it affect on performance
        setX(Gdx.graphics.getWidth() - 36f * 2f, Align.left);
        setY(Gdx.graphics.getHeight() - 36f, Align.top);
    }
}
