/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  basemod.helpers.SuperclassFinder
 */
package feminization.utils;

import basemod.helpers.SuperclassFinder;
import com.esotericsoftware.spine.Skeleton;
import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import feminization.FeminizationMod;
import feminization.utils.CharacterSelectionInfo;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;

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

    private static final String SKELETON_FIELD_NAME = "skeleton";
    /**
     * Cache of {@code creature class -> skeleton Field}. SuperclassFinder walks the hierarchy and
     * throws a NoSuchFieldException per level; damage/heal patches hit this on every event, so the
     * resolved field (and confirmed misses) are cached per concrete class.
     */
    private static volatile Map<Class<?>, Field> skeletonFieldCache = new IdentityHashMap<Class<?>, Field>();

    /**
     * Bounds-safe accessor for the currently selected skin. A stale or corrupt config value, an
     * out-of-range index written by another mod, or an empty list must degrade to {@code null}
     * instead of throwing {@link IndexOutOfBoundsException} inside a patched constructor.
     */
    public static CharacterSelectionInfo getIroncladSelection() {
        return normalizeSelection(ironcladSelections, ironcladSelected);
    }

    public static CharacterSelectionInfo getSilentSelection() {
        return normalizeSelection(silentSelections, silentSelected);
    }

    public static CharacterSelectionInfo getDefectSelection() {
        return normalizeSelection(defectSelections, defectSelected);
    }

    public static CharacterSelectionInfo getWatcherSelection() {
        return normalizeSelection(watcherSelections, watcherSelected);
    }

    private static CharacterSelectionInfo normalizeSelection(ArrayList<CharacterSelectionInfo> selections, int selected) {
        if (selections == null || selections.isEmpty()) {
            return null;
        }
        int index = selected % selections.size();
        if (index < 0) {
            index += selections.size();
        }
        return selections.get(index);
    }

    public static Skeleton getSkeleton(Object creature) {
        if (creature == null) {
            return null;
        }
        Class<?> creatureClass = creature.getClass();
        Field field = skeletonFieldCache.get(creatureClass);
        if (field == null && !skeletonFieldCache.containsKey(creatureClass)) {
            try {
                field = SuperclassFinder.getSuperclassField(creatureClass, (String)SKELETON_FIELD_NAME);
            } catch (Exception ignored) {
                field = null;
            }
            if (field != null) {
                field.setAccessible(true);
                synchronized (FeminizationUtil.class) {
                    Map<Class<?>, Field> updated = new IdentityHashMap<Class<?>, Field>(skeletonFieldCache);
                    updated.put(creatureClass, field);
                    skeletonFieldCache = updated;
                }
            }
        }
        if (field == null) {
            return null;
        }
        try {
            return (Skeleton)field.get(creature);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

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
