package extraFeminization.patch;

import com.esotericsoftware.spine.AnimationStateData;
import com.esotericsoftware.spine.Skeleton;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.characters.Watcher;
import com.megacrit.cardcrawl.core.AbstractCreature;
import feminization.FeminizationMod;
import feminization.utils.FeminizationUtil;
import feminization.utils.ResourceUtil;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class NobraWatcherSuccubusPatch {
    private static final String WATCHER_SUCCUBUS_ID = "watcher_succubus_nun";
    private static final String DEFAULT_ATLAS = "extraFeminizationResources/images/characters/watcher2/idle/nun.atlas";
    private static final String DEFAULT_JSON = "extraFeminizationResources/images/characters/watcher2/idle/nun.json";
    private static final String NOBRA_ATLAS = "feminizethespire/nobra/extraFeminizationResources/images/characters/watcher2/idle/nun.atlas";
    private static final String NOBRA_JSON = "feminizethespire/nobra/extraFeminizationResources/images/characters/watcher2/idle/nun.json";
    private static final Method LOAD_ANIMATION = findLoadAnimationMethod();
    private static final Field STATE_DATA = findStateDataField();
    private static final Field SKELETON = findOptionalField(AbstractCreature.class, "skeleton");
    private static final Field EYE_BONE = findOptionalField(Watcher.class, "eyeBone");
    private static AbstractPlayer currentPlayer;
    private static boolean currentUsingNobra;

    private NobraWatcherSuccubusPatch() {
    }

    @SpirePatch(clz = Watcher.class, method = "damage")
    public static class Damage {
        @SpirePostfixPatch
        public static void postfix(Watcher __instance, DamageInfo info) {
            update(__instance);
        }
    }

    @SpirePatch(clz = AbstractPlayer.class, method = "heal", paramtypez = {int.class})
    public static class Heal {
        @SpirePostfixPatch
        public static void postfix(AbstractPlayer __instance, int healAmount) {
            update(__instance);
        }
    }

    @SpirePatch(clz = AbstractPlayer.class, method = "update")
    public static class Update {
        @SpirePostfixPatch
        public static void postfix(AbstractPlayer __instance) {
            update(__instance);
        }
    }

    private static void update(AbstractPlayer player) {
        if (!(player instanceof Watcher) || LOAD_ANIMATION == null || STATE_DATA == null) {
            return;
        }

        if (!WATCHER_SUCCUBUS_ID.equals(FeminizationUtil.watcherId)) {
            if (player == currentPlayer) {
                currentPlayer = null;
                currentUsingNobra = false;
            }
            return;
        }

        boolean shouldUseNobra = shouldUseNobra(player);
        if (player != currentPlayer) {
            currentPlayer = player;
            currentUsingNobra = false;
            if (!shouldUseNobra) {
                return;
            }
        }
        if (player == currentPlayer && shouldUseNobra == currentUsingNobra) {
            return;
        }

        try {
            ResourceUtil.invokeLoadAnimation(player, LOAD_ANIMATION, shouldUseNobra ? NOBRA_ATLAS : DEFAULT_ATLAS, shouldUseNobra ? NOBRA_JSON : DEFAULT_JSON, 1.0f);
            updateEyeBone(player);
            player.state.setAnimation(0, "Idle", true);
            AnimationStateData stateData = (AnimationStateData) STATE_DATA.get(player);
            stateData.setMix("Hit", "Idle", 0.1f);
            currentPlayer = player;
            currentUsingNobra = shouldUseNobra;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean shouldUseNobra(AbstractPlayer player) {
        return FeminizationMod.activeH && player.currentHealth * 2 <= player.maxHealth;
    }

    private static void updateEyeBone(AbstractPlayer player) throws IllegalAccessException {
        if (SKELETON == null || EYE_BONE == null) {
            return;
        }
        Skeleton skeleton = (Skeleton) SKELETON.get(player);
        EYE_BONE.set(player, skeleton.findBone("eye_anchor"));
    }

    private static Method findLoadAnimationMethod() {
        try {
            Method method = AbstractCreature.class.getDeclaredMethod("loadAnimation", String.class, String.class, float.class);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Field findStateDataField() {
        try {
            Field field = findField(AbstractPlayer.class, "stateData");
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Field findOptionalField(Class<?> type, String name) {
        try {
            Field field = findField(type, name);
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
