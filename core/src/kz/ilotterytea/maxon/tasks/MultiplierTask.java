package kz.ilotterytea.maxon.tasks;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Timer;
import kz.ilotterytea.maxon.MaxonGame;
import kz.ilotterytea.maxon.pets.Pet;
import kz.ilotterytea.maxon.player.DecalPlayer;
import kz.ilotterytea.maxon.player.Savegame;
import kz.ilotterytea.maxon.utils.formatters.NumberFormatter;

public class MultiplierTask extends Timer.Task {
    private final MaxonGame game = MaxonGame.getInstance();
    private final Savegame savegame;
    private final DecalPlayer player;
    private final Label multiplierLabel;

    public MultiplierTask(Savegame savegame) {
        this.savegame = savegame;
        this.player = null;
        this.multiplierLabel = null;
    }

    public MultiplierTask(Savegame savegame, DecalPlayer player, Label multiplierLabel) {
        this.savegame = savegame;
        this.player = player;
        this.multiplierLabel = multiplierLabel;
    }

    @Override
    public void run() {
        double multiplier = 0.0f;

        for (String id : savegame.getPurchasedPets().keySet()) {
            Pet pet = game.getPetManager().getPet(id);

            if (pet == null) {
                continue;
            }

            int amount = savegame.getPurchasedPets().get(id);

            double m = pet.getMultiplier() * amount;
            multiplier += m;
        }

        multiplier /= 10f;

        if (player != null && multiplierLabel != null) {
            multiplier *= 1.0 + player.getClickStreak() / 10000.0;
            multiplierLabel.setText(NumberFormatter.format(multiplier * 10f) + "/s");
        }

        savegame.increaseMoney(multiplier);
    }
}
