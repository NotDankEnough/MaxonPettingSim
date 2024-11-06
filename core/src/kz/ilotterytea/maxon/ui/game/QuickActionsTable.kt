package kz.ilotterytea.maxon.ui.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import kz.ilotterytea.maxon.MaxonGame
import kz.ilotterytea.maxon.screens.MenuScreen
import kz.ilotterytea.maxon.screens.SlotsMinigameScreen
import kz.ilotterytea.maxon.ui.ShakingImageButton
import kz.ilotterytea.maxon.utils.OsUtils

class QuickActionsTable(widgetSkin: Skin, uiSkin: Skin) : Table(uiSkin) {
    init {
        val game = MaxonGame.getInstance()
        val clickSound = game.assetManager.get("sfx/ui/click.ogg", Sound::class.java)
        val soundVolume = game.prefs.getInteger("sfx", 10) / 10f
        val iconSize = if (OsUtils.isMobile) {
            256f
        } else {
            64f
        }

        val slotsButton = ShakingImageButton(widgetSkin, "slots")
        slotsButton.setOrigin(iconSize / 2f, iconSize / 2f)
        slotsButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                super.clicked(event, x, y)
                clickSound.play(soundVolume)
                game.screen = SlotsMinigameScreen()
            }
        })
        val slotsCell = add(slotsButton).size(iconSize).padRight(8f)

        val quitButton = ShakingImageButton(widgetSkin, "exit")
        quitButton.setOrigin(iconSize / 2f, iconSize / 2f)
        quitButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                super.clicked(event, x, y)
                clickSound.play(soundVolume)
                game.screen = MenuScreen()
            }
        })
        val quitCell = add(quitButton).size(iconSize)

        if (OsUtils.isMobile) {
            slotsCell.expandX()
            quitCell.expandX()
        }
    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        if (OsUtils.isMobile) return

        // i'm not sure how much does it affect on performance
        setX(Gdx.graphics.width - 36f * 2f, Align.left)
        setY(Gdx.graphics.height - 36f, Align.top)
    }
}