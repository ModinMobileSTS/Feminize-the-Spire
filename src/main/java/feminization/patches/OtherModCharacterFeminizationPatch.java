package feminization.patches;

import com.badlogic.gdx.Gdx;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import feminization.FeminizationMod;
import feminization.utils.CharacterSelectionInfo;
import feminization.utils.FeminizationUtil;
import feminization.utils.ResourceUtil;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

@SpirePatch2(clz = AbstractCreature.class, method = "loadAnimation", paramtypez = {String.class, String.class, float.class})
public class OtherModCharacterFeminizationPatch {
    private static final String VANILLA_CHARACTER_PACKAGE_PREFIX = "com.megacrit.cardcrawl.characters.";
    private static final ThreadLocal<Boolean> REDIRECTING = new ThreadLocal<Boolean>();
    private static final Method LOAD_ANIMATION = findLoadAnimationMethod();

    private OtherModCharacterFeminizationPatch() {
    }

    @SpirePrefixPatch
    public static SpireReturn<Void> prefix(AbstractCreature __instance, String atlasUrl, String skeletonUrl, float scale) {
        if (!FeminizationMod.activeOtherModFeminization
                || Boolean.TRUE.equals(REDIRECTING.get())
                || !(__instance instanceof AbstractPlayer)
                || isVanillaCharacter(__instance)
                || LOAD_ANIMATION == null) {
            return SpireReturn.Continue();
        }

        CharacterSelectionInfo selection = findCompatibleSelection(__instance, atlasUrl, skeletonUrl);
        if (selection == null || selection.atlas == null || selection.json == null
                || !exists(selection.atlas) || !exists(selection.json)) {
            return SpireReturn.Continue();
        }

        REDIRECTING.set(Boolean.TRUE);
        try {
            ResourceUtil.invokeLoadAnimation(__instance, LOAD_ANIMATION, selection.atlas, selection.json, scale);
            return SpireReturn.Return(null);
        } catch (Exception ignored) {
            return SpireReturn.Continue();
        } finally {
            REDIRECTING.remove();
        }
    }

    private static CharacterSelectionInfo findCompatibleSelection(AbstractCreature creature, String atlasUrl, String skeletonUrl) {
        String characterKey = findCharacterKey(creature, atlasUrl, skeletonUrl);
        if ("ironclad".equals(characterKey)) {
            return selectedInfo(FeminizationUtil.ironcladSelections, FeminizationUtil.ironcladSelected);
        }
        if ("silent".equals(characterKey)) {
            return selectedInfo(FeminizationUtil.silentSelections, FeminizationUtil.silentSelected);
        }
        if ("defect".equals(characterKey)) {
            return selectedInfo(FeminizationUtil.defectSelections, FeminizationUtil.defectSelected);
        }
        if ("watcher".equals(characterKey)) {
            return selectedInfo(FeminizationUtil.watcherSelections, FeminizationUtil.watcherSelected);
        }
        return null;
    }

    private static CharacterSelectionInfo selectedInfo(List<CharacterSelectionInfo> selections, int selected) {
        if (selections == null || selected < 0 || selected >= selections.size()) {
            return null;
        }
        return selections.get(selected);
    }

    private static String findCharacterKey(AbstractCreature creature, String atlasUrl, String skeletonUrl) {
        String className = creature.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        String atlasPath = normalize(atlasUrl);
        String skeletonPath = normalize(skeletonUrl);
        if (matches(className, atlasPath, skeletonPath, "ironclad")) {
            return "ironclad";
        }
        if (matches(className, atlasPath, skeletonPath, "thesilent") || matches(className, atlasPath, skeletonPath, "silent")) {
            return "silent";
        }
        if (matches(className, atlasPath, skeletonPath, "defect")) {
            return "defect";
        }
        if (matches(className, atlasPath, skeletonPath, "watcher")) {
            return "watcher";
        }
        return null;
    }

    private static boolean matches(String className, String atlasPath, String skeletonPath, String key) {
        return className.equals(key)
                || className.equals("the" + key)
                || (atlasPath.contains("/" + key + "/") && skeletonPath.contains("/" + key + "/"));
    }

    private static String normalize(String path) {
        return path == null ? "" : path.replace('\\', '/').toLowerCase(Locale.ROOT);
    }

    private static boolean isVanillaCharacter(AbstractCreature creature) {
        Package characterPackage = creature.getClass().getPackage();
        return characterPackage != null && characterPackage.getName().startsWith(VANILLA_CHARACTER_PACKAGE_PREFIX);
    }

    private static boolean exists(String path) {
        try {
            return Gdx.files.internal(path).exists();
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Method findLoadAnimationMethod() {
        try {
            Method method = AbstractCreature.class.getDeclaredMethod("loadAnimation", String.class, String.class, float.class);
            method.setAccessible(true);
            return method;
        } catch (Exception ignored) {
            return null;
        }
    }
}
