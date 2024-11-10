package kz.ilotterytea.maxon.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import kz.ilotterytea.javaextra.tuples.Pair;
import kz.ilotterytea.maxon.MaxonGame;
import kz.ilotterytea.maxon.constants.SettingsConstants;
import kz.ilotterytea.maxon.localization.LineId;
import kz.ilotterytea.maxon.player.Savegame;
import kz.ilotterytea.maxon.screens.game.GameScreen;
import kz.ilotterytea.maxon.tasks.MultiplierTask;
import kz.ilotterytea.maxon.utils.OsUtils;
import kz.ilotterytea.maxon.utils.formatters.NumberFormatter;
import kz.ilotterytea.maxon.utils.math.Math;

import java.util.ArrayList;

public class SlotsMinigameScreen implements Screen {
    private enum Slot {
        Arbuz(5),
        Icecream(30),
        Kochan(80),
        Buter(120),
        Corn(200),
        Kebab(500),
        Onions(1000),
        Treat(2500)
        ;

        private final int multiplier;

        Slot(int multiplier) {
            this.multiplier = multiplier;
        }

        public int getMultiplier() {
            return multiplier;
        }
    }

    private static class SlotImage extends Image {
        public SlotImage(Slot slot, AssetManager assetManager) {
            super(assetManager.get(
                    String.format("sprites/minigames/slots/%s.png", slot.name().toLowerCase()),
                    Texture.class
            ));
        }
    }

    private Savegame savegame;
    private MaxonGame game;

    private Stage stage;
    private TextButton spinButton, exitButton;
    private Label prizeLabel, moneyLabel;
    private TextField stakeField;
    private Table columns;

    private double prize, stake;
    private int loseStreak, maxLoseStreak, lockedColumns;
    private boolean disabled;

    private Slot loseSlot;

    private ArrayList<Slot> columnSlots;
    private ArrayList<Pair<Timer.Task, Float>> tasks;

    private MultiplierTask multiplierTask;

    private Music audioLoop;
    private float soundVolume;

    @Override
    public void show() {
        this.savegame = Savegame.getInstance();
        this.game = MaxonGame.getInstance();
        this.columnSlots = new ArrayList<>();
        this.tasks = new ArrayList<>();
        this.multiplierTask = new MultiplierTask(savegame);
        this.loseSlot = Slot.values()[0];

        Viewport viewport;

        if (OsUtils.isMobile) {
            viewport = new ScreenViewport();
        } else {
            viewport = new FitViewport(800f, 600f);
        }

        this.stage = new Stage(viewport);

        Skin skin = game.assetManager.get("sprites/gui/ui.skin");

        Table table = new Table();
        table.setFillParent(true);
        table.align(Align.center);

        if (OsUtils.isMobile) table.pad(64f);

        stage.addActor(table);

        // Background
        if (OsUtils.isPC) {
            Image background = new Image(game.assetManager.get("sprites/minigames/slots/background.png", Texture.class));
            background.setZIndex(2);

            table.add(background);
        }

        String styleName = OsUtils.isMobile ? "defaultMobile" : "default";

        // Buttons
        spinButton = new TextButton(game.getLocale().getLine(LineId.MinigameSlotsSpinbutton), skin, styleName);
        spinButton.setDisabled(true);
        spinButton.setWidth(420f);
        spinButton.setPosition(62f, 60f);
        spinButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (spinButton.isDisabled()) return;
                super.clicked(event, x, y);
                restart();
            }
        });

        exitButton = new TextButton(game.getLocale().getLine(LineId.MinigameSlotsExitbutton), skin, styleName);
        exitButton.setPosition(62f, stage.getHeight() / 2f - 150f);
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (exitButton.isDisabled()) return;
                super.clicked(event, x, y);
                game.setScreen(new GameScreen());
            }
        });

        // Labels
        styleName = OsUtils.isMobile ? "slotsMobile" : "slots";

        prizeLabel = new Label("", skin, styleName);
        prizeLabel.setAlignment(Align.center);
        prizeLabel.setPosition(stage.getWidth() / 2f - 180f, stage.getHeight() / 2f + 80f);

        Image moneyIcon = new Image(game.assetManager.get("sprites/gui/player_icons.atlas", TextureAtlas.class).findRegion("points"));
        moneyIcon.setSize(20f, 20f);
        moneyIcon.setPosition(stage.getWidth() / 2f + 60f, stage.getHeight() / 2f - 180f);

        moneyLabel = new Label(NumberFormatter.format(savegame.getMoney()), skin, styleName);
        moneyLabel.setAlignment(Align.right);
        moneyLabel.setPosition(stage.getWidth() / 2f, stage.getHeight() / 2f - 180f);

        Label stakeLabel = new Label(game.getLocale().getLine(LineId.MinigameSlotsBet), skin, styleName);
        stakeLabel.setAlignment(Align.center);
        stakeLabel.setPosition(stage.getWidth() / 2f - 40f, stage.getHeight() / 2f - 100f);

        stakeField = new TextField("", skin, OsUtils.isMobile ? "defaultMobile" : "default");
        stakeField.setMessageText("---");
        stakeField.setTextFieldFilter((x, c) -> String.valueOf(c).matches("^[0-9]*$"));
        stakeField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (actor instanceof TextField tf) {
                    if (tf.isDisabled()) return;

                    String value = tf.getText();

                    try {
                        long v = Long.parseLong(value);

                        if (v > savegame.getMoney()) {
                            v = Double.valueOf(savegame.getMoney()).longValue();
                        }

                        tf.setText(Long.toString(v));
                        stake = (double) v;

                        spinButton.setDisabled(stake <= 0.0);
                    } catch (Exception ignored) {}
                }
            }
        });
        stakeField.setPosition(stage.getWidth() / 2f - 70f, stage.getHeight() / 2f - 150f);

        // Slot columns
        columns = new Table();

        if (OsUtils.isPC) {
            columns.setX(62f);
            columns.setY(stage.getHeight() / 2f);
            columns.setWidth(424f);
        }

        for (int i = 0; i < 3; i++) {
            columnSlots.add(Slot.values()[Math.getRandomNumber(0, Slot.values().length)]);
        }

        reRoll();

        Timer.Task updateTask = new Timer.Task() {
            @Override
            public void run() {
                reRoll();
            }
        };

        tasks.add(new Pair<>(updateTask, 0.1f));

        Timer.Task lockColumnTask = new Timer.Task() {
            @Override
            public void run() {
                Sound sound = game.assetManager.get("sfx/minigames/slots/slots_lock.ogg", Sound.class);
                sound.play(soundVolume);

                lockedColumns += 1;

                if (lockedColumns > 1) {
                    finish();
                }
            }
        };

        tasks.add(new Pair<>(lockColumnTask, 2f));

        disableSlotMachineIfNoStake();

        audioLoop = game.assetManager.get("mus/minigames/slots/slots_loop.mp3");
        audioLoop.setLooping(true);
        audioLoop.setVolume(game.prefs.getInteger(SettingsConstants.MUSIC_NAME, 10) / 10f);
        soundVolume = game.prefs.getInteger(SettingsConstants.SFX_NAME, 10) / 10f;

        Timer.schedule(multiplierTask, 0.1f, 0.1f);

        if (OsUtils.isMobile) {
            Image title = new Image(game.assetManager.get("sprites/minigames/slots/title.png", Texture.class));
            table.add(title).growX().height(
                    title.getHeight() * (this.stage.getWidth() - 64f) / title.getWidth()
            ).padBottom(64f).row();

            table.add(prizeLabel).expandX().padBottom(64f).row();

            table.add(columns).growX().padBottom(64f).row();
            columns.align(Align.center);

            table.add(stakeLabel).growX().padBottom(32f).row();
            stakeLabel.setAlignment(Align.right);

            Table table2 = new Table();
            table2.add(exitButton).align(Align.left).expandX();
            table2.add(stakeField).align(Align.right).minWidth(300f);
            table.add(table2).growX().padBottom(32f).row();

            Table table3 = new Table();
            table3.align(Align.right);
            table3.add(moneyLabel).padRight(16f);
            table3.add(moneyIcon);
            table.add(table3).growX().padBottom(64f).row();

            table.add(spinButton).growX();
        } else {
            stage.addActor(spinButton);
            stage.addActor(exitButton);
            stage.addActor(prizeLabel);
            stage.addActor(moneyIcon);
            stage.addActor(moneyLabel);
            stage.addActor(stakeLabel);
            stage.addActor(stakeField);
            stage.addActor(columns);
        }

        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        if (OsUtils.isMobile) {
            Gdx.gl.glClearColor(0.12f, 0.12f, 0.15f, 1f);
        } else {
            Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        }

        stage.act(delta);
        stage.draw();

        moneyLabel.setText(NumberFormatter.format(savegame.getMoney(), false));
    }


    private void finish() {
        if (audioLoop.isPlaying()) audioLoop.stop();

        stakeField.setDisabled(false);
        exitButton.setDisabled(false);
        spinButton.setDisabled(false);

        for (Pair<Timer.Task, Float> x : tasks) {
            x.getFirst().cancel();
        }

        giveReward();
        updateLabels();
        disableSlotMachineIfNoStake();
    }

    private void restart() {
        audioLoop.play();

        Sound sound = game.assetManager.get("sfx/minigames/slots/slots_start.ogg");
        sound.play(soundVolume);

        prizeLabel.setText("");

        exitButton.setDisabled(true);
        spinButton.setDisabled(true);
        stakeField.setDisabled(true);

        loseSlot = Slot.values()[Math.getRandomNumber(0,3)];
        lockedColumns = -1;
        loseStreak = 0;
        prize = 0.0;
        maxLoseStreak = Math.getRandomNumber(20, 50);

        reRoll();

        for (Pair<Timer.Task, Float> task : tasks) {
            Timer.schedule(task.getFirst(), task.getSecond(), task.getSecond());
        }
    }

    private void reRoll() {
        ArrayList<Slot> array = new ArrayList<>();

        for (int i = 0; i < columnSlots.size(); i++) {
            while (true) {
                Slot x = columnSlots.get(i);

                if (i <= lockedColumns) {
                    if (loseStreak >= maxLoseStreak) {
                        x = loseSlot;
                    }

                    array.add(x);
                    break;
                }

                Slot slot = Slot.values()[Math.getRandomNumber(0, Slot.values().length)];

                if (x.ordinal() != slot.ordinal()) {
                    array.add(slot);
                    break;
                }
            }
        }

        columnSlots.clear();
        columns.clear();

        float size = (OsUtils.isMobile ? 300f : 100f) * game.prefs.getFloat("guiScale", SettingsConstants.UI_DEFAULT_SCALE);

        for (Slot x : array) {
            columnSlots.add(x);
            columns.add(new SlotImage(x, game.assetManager))
                    .size(size, size)
                    .expandX();
        }
    }

    private void disableSlotMachineIfNoStake() {
        if ((long) savegame.getMoney() > 0) {
            return;
        }

        disabled = true;

        stakeField.setMessageText("---");
        stakeField.setText("---");
        stakeField.setDisabled(disabled);
        spinButton.setDisabled(disabled);

        for (Pair<Timer.Task, Float> x : tasks) {
            x.getFirst().cancel();
        }
    }

    private void giveReward() {
        Slot first = columnSlots.get(0);
        boolean same = false;

        for (Slot x : columnSlots) {
            same = x.ordinal() == first.ordinal();

            if (!same) {
                break;
            }
        }

        playRewardSound(same, first);

        savegame.setSlotsTotalSpins(savegame.getSlotsTotalSpins() + 1);

        if (!same) {
            loseStreak++;
            savegame.decreaseMoney(stake);
            return;
        }

        prize = stake * first.multiplier;
        savegame.increaseMoney(prize);
        savegame.setSlotsWins(savegame.getSlotsWins() + 1);
    }

    private void updateLabels() {
        String prizeText;

        if (prize == 0.0) {
            prizeText = game.getLocale().getLine(LineId.MinigameSlotsNothing);
        } else {
            prizeText = game.getLocale().getFormattedLine(LineId.MinigameSlotsPrize, NumberFormatter.format(prize, false));
        }

        prizeLabel.setText(prizeText);

        if (stake > savegame.getMoney()) {
            stake = savegame.getMoney();

            long money = (long) savegame.getMoney();
            String stakeText;
            if (money <= 0) {
                stakeText = "---";
            } else {
                stakeText = String.valueOf(money);
            }

            stakeField.setText(stakeText);
        }

        moneyLabel.setText(NumberFormatter.format(savegame.getMoney(), false));
    }

    private void playRewardSound(boolean same, Slot slot) {
        String path;
        if (!same) {
            path = "fail";
        } else {
            path = switch (slot.ordinal()) {
                case 0, 1 -> "small_win";
                case 2, 3, 4 -> "medium_win";
                default -> "big_win";
            };
        }

        Sound sound = game.assetManager.get(String.format("sfx/minigames/slots/slots_%s.ogg", path));
        sound.play(soundVolume);
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
        dispose();
    }

    @Override
    public void resume() {
        show();
    }

    @Override
    public void hide() {
        savegame.save();
        dispose();
    }

    @Override
    public void dispose() {
        for (Pair<Timer.Task, Float> x : tasks) {
            x.getFirst().cancel();
        }

        tasks.clear();
        multiplierTask.cancel();
        stage.dispose();
        audioLoop.stop();

        Gdx.input.setOnscreenKeyboardVisible(false);
        Gdx.input.setInputProcessor(null);
    }
}
