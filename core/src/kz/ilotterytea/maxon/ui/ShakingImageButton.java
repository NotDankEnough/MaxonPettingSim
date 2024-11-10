package kz.ilotterytea.maxon.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.RepeatAction;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class ShakingImageButton extends Image {
    public ShakingImageButton(Skin skin, String style) {
        super(skin.getRegion(style));

        this.setOrigin(getWidth() / 2f, getHeight() / 2f);

        this.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                super.enter(event, x, y, pointer, fromActor);

                addAction(
                        Actions.repeat(
                                RepeatAction.FOREVER,
                                Actions.sequence(
                                        Actions.rotateTo(-2f, 0.1f),
                                        Actions.rotateTo(2f, 0.1f)
                                )
                        )
                );
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                super.exit(event, x, y, pointer, toActor);

                clearActions();
                addAction(Actions.rotateTo(0f, 0.1f));
            }
        });
    }
}
