package kz.ilotterytea.maxon.player;


import com.badlogic.gdx.Gdx;
import com.google.gson.Gson;
import kz.ilotterytea.maxon.MaxonConstants;
import kz.ilotterytea.maxon.utils.OsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Savegame implements Serializable {
    private static final File directory = new File(MaxonConstants.GAME_SAVEGAME_FOLDER);
    private static final File file = new File(
            String.format(
                    "%s/savegame.maxon",
                    (OsUtils.isAndroid || OsUtils.isIos)
                            ? Gdx.files.getExternalStoragePath()
                            : directory.getAbsolutePath()
            )
    );
    private static final Gson gson = new Gson();
    private static final Logger logger = LoggerFactory.getLogger(Savegame.class);

    private double money, multiplier;
    private final HashMap<String, Integer> purchasedPets = new HashMap<>();
    private final ArrayList<String> unlockedPets = new ArrayList<>();
    private String name;
    private long elapsedTime, slotsWins, slotsTotalSpins;
    private boolean isNewlyCreated;

    private static Savegame savegame;

    public static Savegame getInstance() {
        if (savegame == null) {
            savegame = load();
        }

        return savegame;
    }

    private Savegame() {
        setDefaultValues();
    }

    public static Savegame load() {
        if (!file.exists()) {
            return new Savegame();
        }

        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);

            Savegame savegame = gson.fromJson(ois.readUTF(), Savegame.class);
            ois.close();

            savegame.isNewlyCreated = false;

            logger.info("Loaded the savegame");

            return savegame;
        } catch (IOException e) {
            logger.error("Failed to load a save: {}", e.toString());
            return new Savegame();
        }
    }

    public void save() {
        if (OsUtils.isPC && !directory.exists()) {
            directory.mkdirs();
        }

        try {
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            oos.writeUTF(gson.toJson(this));
            oos.close();

            logger.info("Saved the game");
        } catch (IOException e) {
            logger.error("Failed to save the game: {}", e.toString());
        }
    }

    public void delete() {
        if (file.delete()) {
            setDefaultValues();
        }
    }

    private void setDefaultValues() {
        money = 0.0f;
        multiplier = 0.0f;
        purchasedPets.clear();
        unlockedPets.clear();
        name = OsUtils.isPC ? System.getProperty("user.name", "Maxon") : "Maxon";
        elapsedTime = 0;
        slotsWins = 0;
        slotsTotalSpins = 0;
        isNewlyCreated = true;
    }

    public double getMoney() {
        return money;
    }

    public void setMoney(double money) {
        this.money = money;
    }

    public void increaseMoney(double money) {
        this.money += money;
    }

    public void decreaseMoney(double money) {
        this.money -= money;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void increaseMultiplier(double multiplier) {
        this.multiplier += multiplier;
    }

    public void decreaseMultiplier(double multiplier) {
        this.multiplier -= multiplier;
    }

    public HashMap<String, Integer> getPurchasedPets() {
        return purchasedPets;
    }

    public Integer getAllPetAmount() {
        Integer sum = 0;

        for (Integer v : getPurchasedPets().values()) {
            sum += v;
        }

        return sum;
    }

    public ArrayList<String> getUnlockedPets() {
        return unlockedPets;
    }

    public String getName() {
        return name;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(long elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public long getSlotsTotalSpins() {
        return slotsTotalSpins;
    }

    public void setSlotsTotalSpins(long slotsTotalSpins) {
        this.slotsTotalSpins = slotsTotalSpins;
    }

    public long getSlotsWins() {
        return slotsWins;
    }

    public void setSlotsWins(long slotsWins) {
        this.slotsWins = slotsWins;
    }

    public boolean isNewlyCreated() {
        return isNewlyCreated;
    }

    public void setNewlyCreated(boolean newlyCreated) {
        isNewlyCreated = newlyCreated;
    }
}
