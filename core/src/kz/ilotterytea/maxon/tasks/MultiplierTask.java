package kz.ilotterytea.maxon.tasks;

import com.badlogic.gdx.utils.Timer;
import kz.ilotterytea.maxon.MaxonGame;
import kz.ilotterytea.maxon.pets.Pet;
import kz.ilotterytea.maxon.player.Savegame;

public class MultiplierTask extends Timer.Task {
    private final MaxonGame game = MaxonGame.getInstance();
    private final Savegame savegame;

    public MultiplierTask(Savegame savegame) {
        this.savegame = savegame;
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

        savegame.increaseMoney(multiplier);
    }
}
