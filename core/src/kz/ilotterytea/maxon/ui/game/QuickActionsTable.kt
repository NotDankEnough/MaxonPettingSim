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

class QuickActionsTable(skin: Skin) : Table() {
    init {
        val game = MaxonGame.getInstance()
        val clickSound = game.assetManager.get("sfx/ui/click.ogg", Sound::class.java)
        val soundVolume = game.prefs.getInteger("sfx", 10) / 10f

        val slotsButton = ShakingImageButton(skin, "slots")
        slotsButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                super.clicked(event, x, y)
                clickSound.play(soundVolume)
                game.screen = SlotsMinigameScreen()
            }
        })
        add(slotsButton).height(64f).width(64f).padRight(8f)

        val quitButton = ShakingImageButton(skin, "exit")
        quitButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                super.clicked(event, x, y)
                clickSound.play(soundVolume)
                game.screen = MenuScreen()
            }
        })
        add(quitButton).height(64f).width(64f)
    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        // i'm not sure how much does it affect on performance
        setX(Gdx.graphics.width - 36f * 2f, Align.left)
        setY(Gdx.graphics.height - 36f, Align.top)
    }
}