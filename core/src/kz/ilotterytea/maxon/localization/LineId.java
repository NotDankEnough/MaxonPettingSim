package kz.ilotterytea.maxon.localization;

import org.slf4j.LoggerFactory;

import java.util.Locale;

public enum LineId {
    UpdaterInfo,

    MenuNewgame,
    MenuContinue,
    MenuReset,

    SoundsTitle,
    SoundsMusic,
    SoundsSfx,

    GiftboxOpen,

    MinigameSlotsSpinbutton,
    MinigameSlotsExitbutton,
    MinigameSlotsBet,
    MinigameSlotsNothing,
    MinigameSlotsPrize,

    StoreTitle,
    StoreBuy,
    StoreSell,
    StoreX1,
    StoreX10,
    StorePetlocked,

    PetBrorName,
    PetBrorDesc,
    PetSandwichName,
    PetSandwichDesc,
    PetManlooshkaName,
    PetManlooshkaDesc,
    PetThirstyName,
    PetThirstyDesc,
    PetFuriosName,
    PetFuriosDesc,
    PetTvcatName,
    PetTvcatDesc,
    PetProgcatName,
    PetProgcatDesc,
    PetScreamcatName,
    PetScreamcatDesc,
    PetHellcatName,
    PetHellcatDesc,
    PetLurkerName,
    PetLurkerDesc,
    PetPianoName,
    PetPianoDesc,
    PetBeeName,
    PetBeeDesc,
    PetBusyName,
    PetBusyDesc,
    PetAeaeName,
    PetAeaeDesc,
    PetSuccatName,
    PetSuccatDesc,
    ;

    public static LineId fromJson(String value) {
        StringBuilder result = new StringBuilder();
        String[] chunks = value.split("\\.");

        for (String chunk : chunks) {
            chunk = chunk.replace("_", "");

            String firstLetter = chunk.substring(0, 1).toUpperCase(Locale.ROOT);
            String otherPart = chunk.substring(1);

            result.append(firstLetter).append(otherPart);
        }

        value = result.toString();

        try {
            return LineId.valueOf(value);
        } catch (Exception e) {
            LoggerFactory.getLogger(LineId.class.getName()).error("The key '{}' not registered in LineId enum", value);
            return null;
        }
    }
}
