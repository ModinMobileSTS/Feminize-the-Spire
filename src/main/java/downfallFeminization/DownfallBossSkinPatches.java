package downfallFeminization;

import com.badlogic.gdx.Gdx;
import com.evacipated.cardcrawl.modthespire.lib.ByRef;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import feminization.utils.CharacterSelectionInfo;
import feminization.utils.FeminizationUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public final class DownfallBossSkinPatches {
    private static final String DOWNFALL_MOD_ID = "downfall";
    private static final String IRONCLAD = "charbosses.bosses.Ironclad.CharBossIronclad";
    private static final String SILENT = "charbosses.bosses.Silent.CharBossSilent";
    private static final String DEFECT = "charbosses.bosses.Defect.CharBossDefect";
    private static final String WATCHER = "charbosses.bosses.Watcher.CharBossWatcher";
    private static final String SILENT_MIRROR = "charbosses.monsters.MirrorImageSilent";
    private static final String BOSS_VARIANT_PREFIX = "expansioncontentResources/images/bosses/";
    private static final String VANILLA_EYE_BASE = "images/characters/watcher/eye_anim/skeleton";
    private static final String FEMINIZED_EYE_BASE = "feminizationResources/images/characters/watcher-2/eye_anim/skeleton";
    private static final String FEMINIZED_WATCHER_JSON = "feminizationResources/images/characters/watcher-2/idle/skeleton.json";

    // Cosmetic randomness must not advance Downfall's combat, card or monster RNG streams.
    private static final Random SKIN_RANDOM = new Random();
    // Keep a skin for the lifetime of each boss, without retaining creatures from previous combats.
    private static final Map<AbstractCreature, CharacterSelectionInfo> BOSS_SKINS = new WeakHashMap<AbstractCreature, CharacterSelectionInfo>();

    private DownfallBossSkinPatches() {
    }

    @SpirePatch2(clz = AbstractCreature.class, method = "loadAnimation", paramtypez = {String.class, String.class, float.class}, requiredModId = DOWNFALL_MOD_ID, optional = true)
    public static class BossAnimationPatch {
        @SpirePrefixPatch
        public static void prefix(AbstractCreature __instance, @ByRef String[] atlasUrl, @ByRef String[] skeletonUrl, @ByRef float[] scale) {
            if (!DownfallCompatibilityPatches.activeFeminization || __instance == null || __instance.isPlayer
                    || !isBossBodyAnimation(atlasUrl[0], skeletonUrl[0])) {
                return;
            }

            CharacterSelectionInfo skin = BOSS_SKINS.get(__instance);
            if (skin == null) {
                List<CharacterSelectionInfo> selections = getSelections(__instance);
                if (selections == null) {
                    return;
                }
                if (isClass(__instance, SILENT_MIRROR)) {
                    // A different random skin would reveal which Silent is the real boss.
                    skin = findSilentBossSkin();
                }
                if (skin == null) {
                    skin = chooseSkin(selections);
                }
                if (skin == null) {
                    return;
                }
                BOSS_SKINS.put(__instance, skin);
            }

            // Change the original call's arguments instead of loading a second atlas/skeleton.
            // Player-only customAnim callbacks must not run on these AbstractMonster instances.
            atlasUrl[0] = skin.atlas;
            skeletonUrl[0] = skin.json;
            scale[0] = 1.0f;
        }
    }

    @SpirePatch(cls = WATCHER, method = "loadEyeAnimation", requiredModId = DOWNFALL_MOD_ID, optional = true)
    public static class WatcherEyeAnimationPatch {
        public static ExprEditor Instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if ("com.badlogic.gdx.Files".equals(call.getClassName()) && "internal".equals(call.getMethodName())) {
                        call.replace("{ $_ = $proceed(downfallFeminization.DownfallBossSkinPatches.redirectWatcherEyeResource(this, $1)); }");
                    }
                }
            };
        }
    }

    public static String redirectWatcherEyeResource(AbstractCreature boss, String path) {
        if (!DownfallCompatibilityPatches.activeFeminization) {
            return path;
        }
        CharacterSelectionInfo skin = BOSS_SKINS.get(boss);
        if (skin == null || !FEMINIZED_WATCHER_JSON.equals(skin.json)) {
            return path;
        }
        // The other Watcher skins use the original eye effects and retain their eye_anchor bone.
        if ((VANILLA_EYE_BASE + ".atlas").equals(path) || (VANILLA_EYE_BASE + ".json").equals(path)) {
            if (exists(FEMINIZED_EYE_BASE + ".atlas") && exists(FEMINIZED_EYE_BASE + ".json")) {
                return FEMINIZED_EYE_BASE + path.substring(VANILLA_EYE_BASE.length());
            }
        }
        return path;
    }

    private static boolean isBossBodyAnimation(String atlas, String json) {
        if (atlas == null || json == null || !atlas.endsWith(".atlas") || !json.endsWith(".json")) {
            return false;
        }
        String base = atlas.substring(0, atlas.length() - ".atlas".length());
        if (!json.equals(base + ".json")) {
            return false;
        }
        // NewAge and Simpler archetypes reload a variant body in initialize()/addedPreBattle(),
        // after the constructor's vanilla body was already replaced (e.g. act 1 Poison_Silent).
        // Intercept those reloads too, reusing BOSS_SKINS so the skin never reverts or rerolls.
        // getSelections still restricts this to the four character bosses and Silent's mirror.
        return (base.startsWith("images/characters/") && base.endsWith("/idle/skeleton"))
                || base.startsWith(BOSS_VARIANT_PREFIX);
    }

    private static List<CharacterSelectionInfo> getSelections(AbstractCreature creature) {
        if (isClass(creature, IRONCLAD)) {
            return FeminizationUtil.ironcladSelections;
        }
        if (isClass(creature, SILENT) || isClass(creature, SILENT_MIRROR)) {
            return FeminizationUtil.silentSelections;
        }
        if (isClass(creature, DEFECT)) {
            return FeminizationUtil.defectSelections;
        }
        if (isClass(creature, WATCHER)) {
            return FeminizationUtil.watcherSelections;
        }
        return null;
    }

    private static boolean isClass(Object object, String className) {
        for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
            if (className.equals(type.getName())) {
                return true;
            }
        }
        return false;
    }

    private static CharacterSelectionInfo chooseSkin(List<CharacterSelectionInfo> selections) {
        List<CharacterSelectionInfo> candidates = new ArrayList<CharacterSelectionInfo>();
        for (CharacterSelectionInfo skin : selections) {
            // The vanilla/default entry has no animation paths and is never a candidate.
            if (skin != null && exists(skin.atlas) && exists(skin.json)) {
                candidates.add(skin);
            }
        }
        return candidates.isEmpty() ? null : candidates.get(SKIN_RANDOM.nextInt(candidates.size()));
    }

    private static CharacterSelectionInfo findSilentBossSkin() {
        AbstractRoom room = AbstractDungeon.getCurrRoom();
        if (room == null || room.monsters == null) {
            return null;
        }
        for (AbstractMonster monster : room.monsters.monsters) {
            if (isClass(monster, SILENT)) {
                CharacterSelectionInfo skin = BOSS_SKINS.get(monster);
                if (skin != null) {
                    return skin;
                }
            }
        }
        return null;
    }

    private static boolean exists(String path) {
        try {
            return path != null && Gdx.files.internal(path).exists();
        } catch (Exception ignored) {
            return false;
        }
    }
}
