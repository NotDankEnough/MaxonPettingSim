package com.ilotterytea.maxoning.screens.game.shop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.ilotterytea.maxoning.player.MaxonItem;
import com.ilotterytea.maxoning.player.MaxonItemRegister;
import com.ilotterytea.maxoning.player.MaxonSavegame;
import com.ilotterytea.maxoning.ui.PurchaseItem;
import com.ilotterytea.maxoning.utils.math.Math;

import java.util.ArrayList;

public class ShopUI {
    private final Table table, mainTable;
    private final Skin skin;
    private final TextureAtlas atlas;
    private final ArrayList<MaxonItem> items;

    private ShopMode mode;
    private ShopMultiplier multiplier;

    private final MaxonSavegame savegame;
    private Label pointsLabel, multiplierLabel;

    public ShopUI(final MaxonSavegame savegame, Stage stage, Skin skin, TextureAtlas atlas) {
        this.savegame = savegame;

        this.skin = skin;
        this.atlas = atlas;
        this.mode = ShopMode.BUY;
        this.multiplier = ShopMultiplier.X1;
        this.items = MaxonItemRegister.getItems();

        this.table = new Table(skin);
        this.table.setBackground("board");

        this.mainTable = new Table(this.skin);
        mainTable.setFillParent(true);
        mainTable.align(Align.center | Align.left);

        mainTable.add(this.table).growY().width(Math.percentFromValue(25f, Gdx.graphics.getWidth()));
        stage.addActor(mainTable);
    }

    public void createSavegameUI() {
        Table table = new Table(this.skin);

        table.align(Align.center | Align.left);
        table.pad(10f);

        // Setting up the points
        Table pointsTable = new Table();

        Image pointsImage = new Image(this.atlas.findRegion("points"));
        this.pointsLabel = new Label(String.valueOf(savegame.points), this.skin);

        pointsTable.add(pointsImage);
        pointsTable.add(pointsLabel).padLeft(15f);

        table.add(pointsTable).padBottom(10f).row();

        // Setting up the multiplier
        Table multiplierTable = new Table();

        Image multiplierImage = new Image(this.atlas.findRegion("multiplier"));
        this.multiplierLabel = new Label(String.format("%s/s", savegame.multiplier), this.skin);

        multiplierTable.add(multiplierImage);
        multiplierTable.add(multiplierLabel).padLeft(15f);

        table.add(multiplierTable);

        this.table.add(table).grow();
    }

    public void createShopTitleUI() {
        Table table = new Table(this.skin);

        table.align(Align.center);
        table.pad(10f);

        Label label = new Label("Store", skin);
        table.add(label);

        this.table.add(table).grow().row();
    }

    public void createShopControlUI() {
        Table table = new Table(this.skin);

        table.align(Align.center);
        table.pad(10f);

        // Mode changer
        Table modeTable = new Table();

        TextButton buyButton = new TextButton("Buy", this.skin);
        buyButton.setDisabled(true);
        modeTable.add(buyButton).growX().row();

        TextButton sellButton = new TextButton("Sell", this.skin);
        modeTable.add(sellButton).growX();

        sellButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                mode = ShopMode.SELL;
                sellButton.setDisabled(true);
                buyButton.setDisabled(false);
            }
        });

        buyButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                mode = ShopMode.BUY;
                sellButton.setDisabled(false);
                buyButton.setDisabled(true);
            }
        });

        table.add(modeTable).grow();

        // Multiplier changer
        Table multiplierTable = new Table();

        TextButton x1Button = new TextButton("1x", this.skin);
        x1Button.setDisabled(true);
        multiplierTable.add(x1Button).width(64f).height(64f).padRight(10f);

        TextButton x10Button = new TextButton("10x", this.skin);
        multiplierTable.add(x10Button).width(64f).height(64f);

        x1Button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                multiplier = ShopMultiplier.X1;
                x1Button.setDisabled(true);
                x10Button.setDisabled(false);
            }
        });

        x10Button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                multiplier = ShopMultiplier.X10;
                x1Button.setDisabled(false);
                x10Button.setDisabled(true);
            }
        });

        table.add(multiplierTable).grow();

        this.table.add(table).grow().row();
    }

    public void createShopListUI() {
        Table table = new Table(this.skin);
        table.setBackground("shop_list");

        for (final MaxonItem item : this.items) {
            int amount = (int) savegame.inv.stream().filter(c -> c == item.id).count();

            double price = item.price * java.lang.Math.pow(1.15f, amount + this.multiplier.getMultiplier());
            item.price = (float) price;
            PurchaseItem purchaseItem = new PurchaseItem(this.skin, item);

            table.add(purchaseItem).growX().padBottom(5f).row();
        }

        ScrollPane scrollPane = new ScrollPane(table);
        scrollPane.setScrollingDisabled(true, false);

        Table scrollPaneTable = new Table(this.skin);
        scrollPaneTable.setBackground("shop_list");
        scrollPaneTable.pad(1f, 5f, 1f, 5f);
        scrollPaneTable.add(scrollPane).grow();

        this.table.add(scrollPaneTable).grow().row();
    }

    public void render() {
        this.pointsLabel.setText(String.valueOf(savegame.points));
        this.multiplierLabel.setText(String.format("%s/s", savegame.multiplier));
    }

    public void update() {
        this.mainTable.clear();
        this.mainTable.add(this.table).growY().width(Math.percentFromValue(30f, Gdx.graphics.getWidth()));
    }
}
