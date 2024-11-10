package kz.ilotterytea.maxon.screens.game.shop;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import kz.ilotterytea.maxon.MaxonGame;
import kz.ilotterytea.maxon.anim.SpriteUtils;
import kz.ilotterytea.maxon.constants.SettingsConstants;
import kz.ilotterytea.maxon.ui.AnimatedImage;
import kz.ilotterytea.maxon.utils.OsUtils;

import java.util.ArrayList;

public class ShopMerchant extends Stack {
    public ShopMerchant() {
        super();
        MaxonGame game = MaxonGame.getInstance();

        // Background
        Table backgroundTable = new Table();
        backgroundTable.setFillParent(true);

        Image backgroundImage = new Image(game.assetManager.get("sprites/merchant/merchant_mobile_background.png", Texture.class));
        backgroundTable.add(backgroundImage).grow();

        super.add(backgroundTable);

        // Merchant
        ArrayList<TextureRegion> regions = SpriteUtils.splitToTextureRegions(game.assetManager.get("sprites/merchant/merchant.png"), 150, 100);
        AnimatedImage merchantImage = new AnimatedImage(regions, 5);

        Table merchantTable = new Table();
        merchantTable.setFillParent(true);
        merchantTable.align(Align.bottom);

        float size = OsUtils.isMobile ? 3f : 1.25f;

        merchantTable.add(merchantImage).size(merchantImage.getWidth() * size, merchantImage.getHeight() * size);

        super.add(merchantTable);

        // Merchant sound
        Sound sound = game.assetManager.get("sfx/extra/honk.ogg");

        merchantImage.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                sound.play(game.prefs.getInteger(SettingsConstants.SFX_NAME, 10) / 10f);
            }
        });
    }
}
