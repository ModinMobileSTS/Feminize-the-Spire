package monsterFeminization;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.core.AbstractCreature;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import feminization.utils.ResourceUtil;

@SpirePatch2(clz = AbstractCreature.class, method = "loadAnimation", paramtypez = {String.class, String.class, float.class})
public class MonsterAnimationResourcePatch {
    private static final String OVERRIDE_PREFIX = "feminizethespire/monster_overrides/";
    private static final Set<String> OVERRIDDEN_RESOURCE_BASES = buildOverriddenResourceBases();
    private static final ThreadLocal<Boolean> REDIRECTING = new ThreadLocal<Boolean>();
    private static final Method LOAD_ANIMATION = findLoadAnimationMethod();

    private MonsterAnimationResourcePatch() {
    }

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(AbstractCreature __instance, String atlasUrl, String skeletonUrl, float scale) {
        if (!MonsterFeminizationMod.activeFeminization || Boolean.TRUE.equals(REDIRECTING.get()) || __instance == null || __instance.isPlayer || LOAD_ANIMATION == null) {
            return SpireReturn.Continue();
        }

        String atlasBase = stripExtension(atlasUrl, ".atlas");
        String skeletonBase = stripExtension(skeletonUrl, ".json");
        if (atlasBase == null || !atlasBase.equals(skeletonBase) || !OVERRIDDEN_RESOURCE_BASES.contains(atlasBase)) {
            return SpireReturn.Continue();
        }

        REDIRECTING.set(Boolean.TRUE);
        try {
            ResourceUtil.invokeLoadAnimation(__instance, LOAD_ANIMATION, OVERRIDE_PREFIX + atlasUrl, OVERRIDE_PREFIX + skeletonUrl, scale);
            return SpireReturn.Return(null);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            REDIRECTING.remove();
        }
        return SpireReturn.Continue();
    }

    private static String stripExtension(String path, String extension) {
        return path.endsWith(extension) ? path.substring(0, path.length() - extension.length()) : null;
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

    private static Set<String> buildOverriddenResourceBases() {
        HashSet<String> bases = new HashSet<String>();
        bases.add("images/monsters/theBottom/angryGremlin/skeleton");
        bases.add("images/monsters/theBottom/blueSlaver/skeleton");
        bases.add("images/monsters/theBottom/boss/slime/skeleton");
        bases.add("images/monsters/theBottom/cultist/skeleton");
        bases.add("images/monsters/theBottom/fatGremlin/skeleton");
        bases.add("images/monsters/theBottom/femaleGremlin/skeleton");
        bases.add("images/monsters/theBottom/fungi/skeleton");
        bases.add("images/monsters/theBottom/jawWorm/skeleton");
        bases.add("images/monsters/theBottom/lagavulin/skeleton");
        bases.add("images/monsters/theBottom/looter/skeleton");
        bases.add("images/monsters/theBottom/louseGreen/skeleton");
        bases.add("images/monsters/theBottom/louseRed/skeleton");
        bases.add("images/monsters/theBottom/nobGremlin/skeleton");
        bases.add("images/monsters/theBottom/redSlaver/skeleton");
        bases.add("images/monsters/theBottom/sentry/skeleton");
        bases.add("images/monsters/theBottom/slimeAltL/skeleton");
        bases.add("images/monsters/theBottom/slimeAltM/skeleton");
        bases.add("images/monsters/theBottom/slimeAltS/skeleton");
        bases.add("images/monsters/theBottom/slimeL/skeleton");
        bases.add("images/monsters/theBottom/slimeM/skeleton");
        bases.add("images/monsters/theBottom/slimeS/skeleton");
        bases.add("images/monsters/theBottom/thiefGremlin/skeleton");
        bases.add("images/monsters/theBottom/wizardGremlin/skeleton");
        bases.add("images/monsters/theCity/gremlinleader/skeleton");
        bases.add("images/monsters/theCity/healer/skeleton");
        bases.add("images/monsters/theCity/looterAlt/skeleton");
        bases.add("images/monsters/theCity/reptile/skeleton");
        bases.add("images/monsters/theCity/snakePlant/skeleton");
        bases.add("images/monsters/theCity/tank/skeleton");
        return Collections.unmodifiableSet(bases);
    }
}
