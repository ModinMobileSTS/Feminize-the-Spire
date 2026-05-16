/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  basemod.helpers.SuperclassFinder
 */
package feminization.utils;

import basemod.helpers.SuperclassFinder;
import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import feminization.FeminizationMod;
import feminization.utils.CharacterSelectionInfo;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;

public class FeminizationUtil {
    public static int ironcladSelected = 1;
    public static int silentSelected = 1;
    public static int defectSelected = 1;
    public static int watcherSelected = 1;
    public static ArrayList<CharacterSelectionInfo> ironcladSelections = new ArrayList();
    public static ArrayList<CharacterSelectionInfo> silentSelections = new ArrayList();
    public static ArrayList<CharacterSelectionInfo> defectSelections = new ArrayList();
    public static ArrayList<CharacterSelectionInfo> watcherSelections = new ArrayList();
    public static String ironcladId = "";
    public static String watcherId = "";

    public static void saveSelectedSkins() {
        try {
            SpireConfig config = new SpireConfig(FeminizationMod.MODNAME, "Common");
            config.setInt("ironcladSelected", ironcladSelected);
            config.setInt("silentSelected", silentSelected);
            config.setInt("defectSelected", defectSelected);
            config.setInt("watcherSelected", watcherSelected);
            config.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void normalizeSelectedSkins() {
        ironcladSelected = normalizeSelected(ironcladSelected, ironcladSelections.size());
        silentSelected = normalizeSelected(silentSelected, silentSelections.size());
        defectSelected = normalizeSelected(defectSelected, defectSelections.size());
        watcherSelected = normalizeSelected(watcherSelected, watcherSelections.size());
    }

    private static int normalizeSelected(int selected, int size) {
        if (size <= 0) {
            return 0;
        }
        selected %= size;
        return selected < 0 ? selected + size : selected;
    }

    public static <T> T getField(Object o, String name) throws Exception {
        Field field = SuperclassFinder.getSuperclassField(o.getClass(), (String)name);
        field.setAccessible(true);
        return (T)field.get(o);
    }
}
