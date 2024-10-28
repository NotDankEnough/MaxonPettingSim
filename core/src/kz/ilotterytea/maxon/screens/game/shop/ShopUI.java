package kz.ilotterytea.maxon.screens.game.shop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import kz.ilotterytea.maxon.MaxonGame;
import kz.ilotterytea.maxon.localization.LineId;
import kz.ilotterytea.maxon.pets.Pet;
import kz.ilotterytea.maxon.pets.PetWidget;
import kz.ilotterytea.maxon.player.Savegame;
import kz.ilotterytea.maxon.utils.OsUtils;
import kz.ilotterytea.maxon.utils.formatters.NumberFormatter;
import kz.ilotterytea.maxon.utils.math.Math;

import java.util.ArrayList;

public class ShopUI {
    private final Table table, mainTable;
    private Table controlTable, shopListTable;

    private final Skin skin;
    private final TextureAtlas atlas;

    private ShopMode mode;
    private ShopMultiplier multiplier;

    private boolean isShopListOpened = false;

    private final Savegame savegame;
    private Label pointsLabel, multiplierLabel;

    private final ArrayList<PetWidget> petWidgets = new ArrayList<>();

    private final Sound clickSound, notEnoughMoneySound, purchaseSound, sellSound;

    private final String styleName = OsUtils.isMobile ? "defaultMobile" : "default";

    public ShopUI(final Savegame savegame, Stage stage, Skin skin, TextureAtlas atlas) {
        this.savegame = savegame;
        MaxonGame game = MaxonGame.getInstance();
        this.clickSound = game.assetManager.get("sfx/ui/click.ogg", Sound.class);
        this.notEnoughMoneySound = game.assetManager.get("sfx/shop/not_enough_money.ogg", Sound.class);
        this.purchaseSound = game.assetManager.get("sfx/shop/purchase.ogg", Sound.class);
        this.sellSound = game.assetManager.get("sfx/shop/sell.ogg", Sound.class);

        this.skin = skin;
        this.atlas = atlas;
        this.mode = ShopMode.BUY;
        this.multiplier = ShopMultiplier.X1;

        this.table = new Table(skin);
        this.table.setBackground("store");

        this.mainTable = new Table(this.skin);
        mainTable.setFillParent(true);

        if (OsUtils.isMobile) {
            mainTable.align(Align.center | Align.top);
            mainTable.add(this.table).growX();
        } else {
            mainTable.align(Align.center | Align.left);
            mainTable.add(this.table).growY().width(Math.percentFromValue(25f, Gdx.graphics.getWidth()));
        }

        stage.addActor(mainTable);
    }

    public void createSavegameUI() {
        Table table = new Table(this.skin);

        table.align(Align.center | Align.left);
        table.pad(10f);

        // Setting up the points
        Table pointsTable = new Table();
        pointsTable.align(Align.left);

        Image pointsImage = new Image(this.atlas.findRegion("points"));
        this.pointsLabel = new Label(String.valueOf(savegame.getMoney()), this.skin, styleName);
        pointsLabel.setAlignment(Align.left);

        if (OsUtils.isMobile) {
            pointsTable.add(pointsImage).size(38f, 38f).padRight(15f);
        } else {
            pointsTable.add(pointsImage).size(64f, 64f).padRight(15f);
        }

        pointsTable.add(pointsLabel).grow();

        table.add(pointsTable).grow().padBottom(5f).row();

        // Setting up the multiplier
        Table multiplierTable = new Table();
        multiplierTable.align(Align.left);

        Image multiplierImage = new Image(this.atlas.findRegion("multiplier"));
        this.multiplierLabel = new Label(String.format("%s/s", savegame.getMultiplier()), this.skin, styleName);
        multiplierLabel.setAlignment(Align.left);

        if (OsUtils.isMobile) {
            multiplierTable.add(multiplierImage).size(38f, 38f).padRight(15f);
        } else {
            multiplierTable.add(multiplierImage).size(64f, 64f).padRight(15f);
        }

        multiplierTable.add(multiplierLabel).grow();

        table.add(multiplierTable).grow();

        this.table.add(table).grow().row();
    }

    public void createShopTitleUI() {
        Table titleTable = new Table(skin);
        titleTable.setBackground("store_control");

        Label label = new Label(MaxonGame.getInstance().getLocale().getLine(LineId.StoreTitle), skin, styleName);
        label.setAlignment(Align.center);
        titleTable.add(label).pad(10f).grow();

        this.table.add(titleTable).growX().row();

        if (OsUtils.isMobile) {
            titleTable.addCaptureListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    super.clicked(event, x, y);

                    isShopListOpened = !isShopListOpened;

                    if (isShopListOpened) {
                        table.add(controlTable).grow().row();
                        table.add(shopListTable).grow().row();
                    } else {
                        table.removeActor(controlTable);
                        table.removeActor(shopListTable);
                    }
                }
            });
        }
    }

    public void createShopControlUI() {
        controlTable = new Table(this.skin);
        controlTable.setBackground("store_control");

        controlTable.align(Align.center);
        controlTable.pad(10f);

        String styleName = OsUtils.isMobile ? "store_control_mobile" : "store_control";

        // Mode changer
        Table modeTable = new Table();

        TextButton buyButton = new TextButton(MaxonGame.getInstance().getLocale().getLine(LineId.StoreBuy), this.skin, OsUtils.isMobile ? "store_buy_mobile" : "store_buy");
        buyButton.setDisabled(true);
        modeTable.add(buyButton).padBottom(5f).growX().row();

        TextButton sellButton = new TextButton(MaxonGame.getInstance().getLocale().getLine(LineId.StoreSell), this.skin, OsUtils.isMobile ? "store_sell_mobile" : "store_sell");
        modeTable.add(sellButton).growX();

        sellButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                if (!sellButton.isDisabled()) {
                    clickSound.play();
                }

                mode = ShopMode.SELL;
                sellButton.setDisabled(true);
                buyButton.setDisabled(false);
            }
        });

        buyButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                if (!buyButton.isDisabled()) {
                    clickSound.play();
                }

                mode = ShopMode.BUY;
                sellButton.setDisabled(false);
                buyButton.setDisabled(true);
            }
        });

        controlTable.add(modeTable).padRight(5f).grow();

        // Multiplier changer
        Table multiplierTable = new Table();
        multiplierTable.align(Align.left);

        TextButton x1Button = new TextButton(MaxonGame.getInstance().getLocale().getLine(LineId.StoreX1), this.skin, styleName);
        x1Button.setDisabled(true);
        multiplierTable.add(x1Button).width(64f).height(64f).padRight(10f);

        TextButton x10Button = new TextButton(MaxonGame.getInstance().getLocale().getLine(LineId.StoreX10), this.skin, styleName);
        multiplierTable.add(x10Button).width(64f).height(64f);

        x1Button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                if (!x1Button.isDisabled()) {
                    clickSound.play();
                }

                multiplier = ShopMultiplier.X1;
                x1Button.setDisabled(true);
                x10Button.setDisabled(false);
            }
        });

        x10Button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                if (!x10Button.isDisabled()) {
                    clickSound.play();
                }

                multiplier = ShopMultiplier.X10;
                x1Button.setDisabled(false);
                x10Button.setDisabled(true);
            }
        });

        controlTable.add(multiplierTable).grow();

        if (!OsUtils.isMobile) {
            this.table.add(controlTable).grow().row();
        }
    }

    public void createShopListUI() {
        Table table = new Table(this.skin);
        ArrayList<Pet> pets = MaxonGame.getInstance().getPetManager().getPets();

        for (Pet pet : pets) {
            PetWidget widget = new PetWidget(this.skin, pet, this.atlas);
            widget.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    super.clicked(event, x, y);

                    if (widget.isDisabled()) {
                        notEnoughMoneySound.play();
                        return;
                    }

                    if (mode == ShopMode.BUY) {
                        Integer amount = savegame.getPurchasedPets().get(pet.getId());

                        if (amount == null) {
                            amount = 0;
                        }

                        savegame.decreaseMoney(widget.getPrice());
                        savegame.increaseMultiplier(pet.getMultiplier() * multiplier.getMultiplier());
                        savegame.getPurchasedPets().put(
                                pet.getId(),
                                amount + multiplier.getMultiplier()
                        );
                        purchaseSound.play();
                    } else {
                        savegame.increaseMoney(widget.getPrice());
                        savegame.decreaseMultiplier(pet.getMultiplier() * multiplier.getMultiplier());
                        savegame.getPurchasedPets().put(
                                pet.getId(),
                                savegame.getPurchasedPets().get(pet.getId())
                                        - multiplier.getMultiplier()
                        );
                        sellSound.play();
                    }
                }
            });

            petWidgets.add(widget);
            table.add(widget).growX().padBottom(5f).row();
        }

        ScrollPane scrollPane = new ScrollPane(table);
        scrollPane.setScrollingDisabled(true, false);

        shopListTable = new Table(this.skin);
        shopListTable.setBackground("store_list");
        shopListTable.pad(4f, 0f, 4f, 0f);
        shopListTable.add(scrollPane).grow();

        if (!OsUtils.isMobile) {
            this.table.add(shopListTable).grow().row();
        }
    }

    private void updatePurchaseItems() {
        for (final PetWidget widget : this.petWidgets) {
            if (!savegame.getUnlockedPets().contains(widget.getPet().getId())) {
                double price = widget.getPrice() / 4.0f;
                double price2 = widget.getPrice() / 1.5f;

                if (!widget.isLocked()) {
                    widget.setLocked(true);
                }

                widget.setDisabled(true);

                if (price > savegame.getMoney()) {
                    widget.setVisible(false);
                } else if (price < savegame.getMoney() && price2 > savegame.getMoney()){
                    widget.setVisible(true);
                } else {
                    widget.setVisible(true);
                    widget.setLocked(false);
                    savegame.getUnlockedPets().add(widget.getPet().getId());

                    Sound sound = MaxonGame.getInstance().assetManager.get("sfx/shop/unlocked.ogg");
                    sound.play();
                }

                continue;
            }

            Integer amount = savegame.getPurchasedPets().get(widget.getPet().getId());

            if (amount == null) amount = 0;

            double price = widget.getPet().getPrice() * java.lang.Math.pow(1.15f, amount) * multiplier.getMultiplier();

            if (mode == ShopMode.SELL) {
                price /= 4;
            }

            widget.setPrice(price);

            if (mode == ShopMode.BUY) {
                if (price > savegame.getMoney() || savegame.getMoney() - price < 0) {
                    widget.setDisabled(true);
                } else if (widget.isDisabled()) {
                    widget.setDisabled(false);
                }
            } else {
                if (amount - multiplier.getMultiplier() < 0) {
                    widget.setDisabled(true);
                } else if (widget.isDisabled()) {
                    widget.setDisabled(false);
                }
            }
        }
    }

    public void render() {
        this.pointsLabel.setText(NumberFormatter.format(savegame.getMoney(), false));
        //this.multiplierLabel.setText(String.format("%s/s", NumberFormatter.format(savegame.getMultiplier())));
        updatePurchaseItems();
    }

    public void update() {
        if (OsUtils.isMobile) {
            return;
        }

        this.mainTable.clear();
        this.mainTable.add(this.table).growY().width(Math.percentFromValue(30f, Gdx.graphics.getWidth()));
    }

    public boolean isShopListOpened() {
        return isShopListOpened;
    }

    public Label getMultiplierLabel() {
        return multiplierLabel;
    }
}
