@file:Suppress("unused", "unused", "unused", "unused", "unused", "unused", "unused", "unused", "unused", "unused",
    "unused", "unused", "unused", "unused", "unused", "unused", "unused", "unused"
)

package kz.ilotterytea.maxon.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Timer
import com.badlogic.gdx.utils.Timer.Task
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.ScreenViewport
import kz.ilotterytea.maxon.MaxonGame
import kz.ilotterytea.maxon.constants.SettingsConstants
import kz.ilotterytea.maxon.localization.LineId
import kz.ilotterytea.maxon.player.Savegame
import kz.ilotterytea.maxon.screens.game.GameScreen
import kz.ilotterytea.maxon.tasks.MultiplierTask
import kz.ilotterytea.maxon.utils.OsUtils
import kz.ilotterytea.maxon.utils.formatters.NumberFormatter
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.random.Random

private enum class Slot(val multiplier: Int) {
    Arbuz(5),
    Icecream(30),
    Kochan(80),
    Buter(120),
    Corn(200),
    Kebab(500),
    Onions(1000),
    Treat(2500),
}

private class SlotImage(slot: Slot, assetManager: AssetManager) : Image(assetManager.get("sprites/minigames/slots/${slot.name.lowercase()}.png", Texture::class.java))

@Suppress("unused", "unused", "unused", "unused", "unused", "unused", "unused", "unused", "unused", "unused", "unused",
    "unused", "unused", "unused", "unused", "unused", "unused", "unused"
)
class SlotsMinigameScreen : Screen {
    private val savegame = Savegame.getInstance()

    private val game = MaxonGame.getInstance()
    private val stage = Stage(if (OsUtils.isMobile) {
        ScreenViewport()
    } else {
        FitViewport(800f, 600f)
    })

    private var spinButton: TextButton? = null
    private var exitButton: TextButton? = null

    private var prize = 0.0
    private var prizeLabel: Label? = null
    private var moneyLabel: Label? = null

    private var stake = 0.0
    private var stakeField: TextField? = null

    private var loseSlot = Slot.values()[0]
    private var loseStreak = 0
    private var maxLoseStreak = Random.nextInt(20, 50)

    private var disabled = false
    private var lockedColumns = -1
    private val columnSlots = arrayListOf<Slot>()
    private val columns = Table()

    private val tasks = arrayListOf<Pair<Task, Float>>()

    private val audioLoop: Music = game.assetManager.get("mus/minigames/slots/slots_loop.mp3")
    
    private val soundVolume = game.prefs.getInteger("sfx", 10) / 10f

    private val multiplierTask = MultiplierTask(savegame)

    override fun show() {
        // Skins
        val skin = game.assetManager.get("sprites/gui/ui.skin", Skin::class.java)

        // Main table
        val table = Table()
        table.setFillParent(true)
        table.align(Align.center)

        if (OsUtils.isMobile) table.pad(64f)

        stage.addActor(table)

        // Background
        if (OsUtils.isPC) {
            val background = Image(game.assetManager.get("sprites/minigames/slots/background.png", Texture::class.java))
            background.zIndex = 2

            table.add(background)
        }

        var styleName = if (OsUtils.isMobile) "defaultMobile" else "default"

        // Buttons
        spinButton = TextButton(game.locale.getLine(LineId.MinigameSlotsSpinbutton), skin, styleName)
        spinButton?.isDisabled = true
        spinButton?.width = 420f
        spinButton?.setPosition(62f, 60f)
        spinButton?.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (spinButton?.isDisabled == true) return
                super.clicked(event, x, y)
                restart()
            }
        })

        exitButton = TextButton(game.locale.getLine(LineId.MinigameSlotsExitbutton), skin, styleName)
        exitButton?.setPosition(62f, stage.height / 2f - 150f)
        exitButton?.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (exitButton?.isDisabled == true) return
                super.clicked(event, x, y)
                game.screen = GameScreen()
            }
        })

        // Labels
        styleName = if (OsUtils.isMobile) "slotsMobile" else "slots"

        prizeLabel = Label("", skin, styleName)
        prizeLabel?.setAlignment(Align.center)
        prizeLabel?.setPosition(stage.width / 2f - 180f, stage.height / 2f + 80f)

        val moneyIcon = Image(game.assetManager.get("sprites/gui/player_icons.atlas", TextureAtlas::class.java).findRegion("points"))
        moneyIcon.setSize(20f, 20f)
        moneyIcon.setPosition(stage.width / 2f + 60f, stage.height / 2f - 180f)

        moneyLabel = Label(NumberFormatter.format(savegame.money), skin, styleName)
        moneyLabel?.setAlignment(Align.right)
        moneyLabel?.setPosition(stage.width / 2f, stage.height / 2f - 180f)

        val stakeLabel = Label(game.locale.getLine(LineId.MinigameSlotsBet), skin, styleName)
        stakeLabel.setAlignment(Align.center)
        stakeLabel.setPosition(stage.width / 2f - 40f, stage.height / 2f - 100f)

        stakeField = TextField("", skin, if (OsUtils.isMobile) "defaultMobile" else "default")
        stakeField?.messageText = "---"
        stakeField?.setTextFieldFilter { _, c -> c.toString().matches(Regex("^[0-9]*\$")) }
        stakeField?.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                val textField = actor as TextField
                if (textField.isDisabled) return

                var value = textField.text?.toLongOrNull()

                if (value != null) {
                    if (value > savegame.money.toLong()) {
                        value = savegame.money.roundToLong()
                    }

                    textField.text = value.toString()
                    stake = value.toDouble()

                    spinButton?.isDisabled = stake <= 0.0
                }
            }
        })
        stakeField?.setPosition(stage.width / 2f - 70f, stage.height / 2f - 150f)

        // Slot columns
        if (OsUtils.isPC) {
            columns.x = 62f
            columns.y = stage.height / 2f
            columns.width = 424f
        }

        for (i in 0..2) {
            columnSlots.add(Slot.values()[Random.nextInt(0, Slot.values().size)])
        }

        reRoll()

        val updateTask = object : Task() {
            override fun run() {
                reRoll()
            }
        }

        tasks.add(Pair(updateTask, 0.1f))

        val lockColumnTask = object : Task() {
            override fun run() {
                val sound = game.assetManager.get("sfx/minigames/slots/slots_lock.ogg", Sound::class.java)
                sound.play(soundVolume)

                lockedColumns += 1

                if (lockedColumns > 1) {
                    finish()
                }
            }
        }

        tasks.add(Pair(lockColumnTask, 2f))

        disableSlotMachineIfNoStake()

        audioLoop.isLooping = true
        audioLoop.volume = game.prefs.getInteger("music", 10) / 10f

        Timer.schedule(multiplierTask, 0.1f, 0.1f)

        if (OsUtils.isMobile) {
            val title = Image(game.assetManager.get("sprites/minigames/slots/title.png", Texture::class.java))
            table.add(title).growX().height(title.height * (this.stage.width - 64f) / title.width).padBottom(64f).row()

            table.add(prizeLabel).expandX().padBottom(64f).row()

            table.add(columns).growX().padBottom(64f).row()
            columns.align(Align.center)

            table.add(stakeLabel).growX().padBottom(32f).row()
            stakeLabel.setAlignment(Align.right)

            val table2 = Table()
            table2.add(exitButton).align(Align.left).expandX()
            table2.add(stakeField).align(Align.right).minWidth(300f)
            table.add(table2).growX().padBottom(32f).row()

            val table3 = Table()
            table3.align(Align.right)
            table3.add(moneyLabel).padRight(16f)
            table3.add(moneyIcon)
            table.add(table3).growX().padBottom(64f).row()

            table.add(spinButton).growX()
        } else {
            stage.addActor(spinButton)
            stage.addActor(exitButton)
            stage.addActor(prizeLabel)
            stage.addActor(moneyIcon)
            stage.addActor(moneyLabel)
            stage.addActor(stakeLabel)
            stage.addActor(stakeField)
            stage.addActor(columns)
        }

        Gdx.input.inputProcessor = stage
    }

    override fun render(delta: Float) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        if (OsUtils.isMobile) {
            Gdx.gl.glClearColor(0.12f, 0.12f, 0.15f, 1f)
        } else {
            Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        }

        stage.act(delta)
        stage.draw()

        moneyLabel?.setText(NumberFormatter.format(savegame.money, false))
    }

    private fun reRoll() {
        val array = arrayListOf<Slot>()

        for (i in 0 until columnSlots.size) {
            while (true) {
                var x = columnSlots[i]

                if (i <= lockedColumns) {
                    if (loseStreak >= maxLoseStreak) {
                        x = loseSlot
                    }

                    array.add(x)
                    break
                }

                val slot = Slot.values()[Random.nextInt(0, Slot.values().size)]

                if (x.ordinal != slot.ordinal) {
                    array.add(slot)
                    break
                }
            }
        }

        columnSlots.clear()
        columns.clear()

        val size = (if (OsUtils.isMobile) 300f else 100f) * game.prefs.getFloat("guiScale", SettingsConstants.UI_DEFAULT_SCALE)

        for (x in array) {
            columnSlots.add(x)
            columns.add(SlotImage(x, game.assetManager))
                .size(size, size)
                .expandX()
        }
    }


    private fun finish() {
        if (audioLoop.isPlaying) audioLoop.stop()

        stakeField?.isDisabled = false
        exitButton?.isDisabled = false
        spinButton?.isDisabled = false

        for (x in tasks) {
            x.first.cancel()
        }

        giveReward()
        updateLabels()
        disableSlotMachineIfNoStake()
    }

    private fun restart() {
        audioLoop.play()

        val sound = game.assetManager.get<Sound>("sfx/minigames/slots/slots_start.ogg")
        sound.play(soundVolume)

        prizeLabel?.setText("")

        exitButton?.isDisabled = true
        spinButton?.isDisabled = true
        stakeField?.isDisabled = true

        loseSlot = Slot.values()[Random.nextInt(0,3)]
        lockedColumns = -1
        loseStreak = 0
        prize = 0.0
        maxLoseStreak = Random.nextInt(20, 50)

        reRoll()

        for (task in tasks) {
            Timer.schedule(task.first, task.second, task.second)
        }
    }

    private fun giveReward() {
        val first = columnSlots[0]
        var same = false

        for (x in columnSlots) {
            same = x.ordinal == first.ordinal

            if (!same) {
                break
            }
        }

        playRewardSound(same, first)

        savegame.slotsTotalSpins++

        if (!same) {
            loseStreak++
            savegame.money -= stake
            return
        }

        prize = stake * first.multiplier
        savegame.money += prize
        savegame.slotsWins++
    }

    private fun updateLabels() {
        val prizeText = if (prize == 0.0) {
            game.locale.getLine(LineId.MinigameSlotsNothing)
        } else {
            game.locale.getFormattedLine(LineId.MinigameSlotsPrize, NumberFormatter.format(prize, false))
        }

        prizeLabel?.setText(prizeText)

        if (stake.toLong() > savegame.money.toLong()) {
            stake = savegame.money

            val stakeText = if (savegame.money.roundToInt() <= 0) {
                "---"
            } else {
                savegame.money.roundToInt().toString()
            }

            stakeField?.text = stakeText
        }

        moneyLabel?.setText(NumberFormatter.format(savegame.money, false))
    }

    private fun disableSlotMachineIfNoStake() {
        if (savegame.money.toLong() > 0) {
            return
        }

        disabled = true

        stakeField?.messageText = "---"
        stakeField?.text = "---"
        stakeField?.isDisabled = disabled
        spinButton?.isDisabled = disabled

        for (x in tasks) {
            x.first.cancel()
        }
    }

    private fun playRewardSound(same: Boolean, slot: Slot) {
        val path = if (!same) {
            "fail"
        } else {
            when (slot.ordinal) {
                in 0..1 -> "small_win"
                in 2..4 -> "medium_win"
                else -> "big_win"
            }
        }

        val sound = game.assetManager.get("sfx/minigames/slots/slots_$path.ogg", Sound::class.java)
        sound.play(soundVolume)
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun pause() {
        dispose()
    }

    override fun resume() {
        show()
    }

    override fun hide() {
        savegame.save()
        dispose()
    }

    override fun dispose() {
        for (x in tasks) {
            x.first.cancel()
        }

        tasks.clear()
        multiplierTask.cancel()
        stage.dispose()
        audioLoop.stop()

        Gdx.input.setOnscreenKeyboardVisible(false)
        Gdx.input.inputProcessor = null
    }
}