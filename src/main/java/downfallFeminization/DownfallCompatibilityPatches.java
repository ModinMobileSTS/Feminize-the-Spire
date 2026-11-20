package downfallFeminization;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.AnimationStateData;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.SkeletonData;
import com.esotericsoftware.spine.SkeletonJson;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.screens.charSelect.CharacterOption;
import com.megacrit.cardcrawl.screens.charSelect.CharacterSelectScreen;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoom;
import feminization.utils.ResourceUtil;
import java.lang.reflect.Field;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import npcFeminization.NpcFeminizationMod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DownfallCompatibilityPatches {
    private static final String DOWNFALL_MOD_ID = "downfall";
    private static final String OVERRIDE_PREFIX = "feminizethespire/downfall_overrides/";
    private static final String MONSTER_OVERRIDE_PREFIX = "feminizethespire/monster_overrides/";
    private static final String NPC_OVERRIDE_PREFIX = "feminizethespire/npc_overrides/";
    private static final String MERCHANT_NPC_BASE = "images/npcs/merchant/skeleton";
    private static final String NEOW_NPC_BASE = "images/npcs/neow/skeleton";
    // Prebuilt comparison targets; building these per call allocated garbage on every animation load.
    private static final String MERCHANT_NPC_ATLAS = MERCHANT_NPC_BASE + ".atlas";
    private static final String MERCHANT_NPC_JSON = MERCHANT_NPC_BASE + ".json";
    private static final String NEOW_NPC_ATLAS = NEOW_NPC_BASE + ".atlas";
    private static final String NEOW_NPC_JSON = NEOW_NPC_BASE + ".json";
    private static final String SLIMEBOUND_CHARACTER_BASE = "slimeboundResources/SlimeboundImages/char/skeleton";
    private static final String SLIMEBOUND_OVERRIDE_BASE = OVERRIDE_PREFIX + SLIMEBOUND_CHARACTER_BASE;
    private static final String SLIMEBOUND_ATLAS_URL = SLIMEBOUND_OVERRIDE_BASE + ".atlas";
    private static final String SLIMEBOUND_JSON_URL = SLIMEBOUND_OVERRIDE_BASE + ".json";
    private static final String SLIMEBOUND_CHARACTER_ATLAS = SLIMEBOUND_CHARACTER_BASE + ".atlas";
    private static final String SLIMEBOUND_CHARACTER_JSON = SLIMEBOUND_CHARACTER_BASE + ".json";
    private static final String HEXAGHOST_DEFAULT_ATLAS = OVERRIDE_PREFIX + "downfallFeminizationResources/images/hexaghost/liuhuo.atlas";
    private static final String HEXAGHOST_DEFAULT_JSON = OVERRIDE_PREFIX + "downfallFeminizationResources/images/hexaghost/liuhuo.json";
    private static final String CUSTOM_FEMINIZED_PORTRAIT = OVERRIDE_PREFIX + "downfallFeminizationResources/images/portraits/custom_feminized_portrait.png";
    private static final String HEXAGHOST_FEMINIZED_PORTRAIT = OVERRIDE_PREFIX + "downfallFeminizationResources/images/portraits/hexaghost_feminized_portrait.png";
    private static final String GREMLIN_PORTRAIT = OVERRIDE_PREFIX + "downfallFeminizationResources/images/portraits/gremlin_feminized_portrait.png";
    private static final String CONFIG_MOD_NAME = "Feminization";
    private static final String CONFIG_FILE_NAME = "Common";
    private static final String GREMLIN_FEMINIZED_KEY = "downfallGremlinFeminizedSkin";
    private static final String SLIMEBOUND_FEMINIZED_SKIN_NAME = "娘化";
    private static final String HEXAGHOST_FEMINIZED_SKIN_NAME = "六火娘";
    private static final String GREMLIN_FEMINIZED_SKIN_NAME = "地精娘小队";
    private static final String FEMINIZED_SKIN_DESCRIPTION = "anaertailin 带来的娘化皮肤";
    private static final boolean DEBUG_LOGS = false;
    private static final Color HOVER_COLOR = new Color(1.0f, 1.0f, 1.0f, 0.5f);
    private static final Logger logger = LogManager.getLogger((String)DownfallCompatibilityPatches.class.getName());
    private static final Method LOAD_ANIMATION = findLoadAnimationMethod();
    private static final ThreadLocal<Boolean> REDIRECTING_ANIMATION = new ThreadLocal<Boolean>();
    private static final Map<String, GremlinAssets> GREMLIN_ASSETS = new HashMap<String, GremlinAssets>();
    private static final Set<String> LOGGED_ANIMATION_PATHS = new HashSet<String>();
    private static final Set<String> LOGGED_RESOURCE_PATHS = new HashSet<String>();
    private static final Set<Object> EMBEDDED_SKINS = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
    public static boolean activeFeminization = true;
    private static boolean resetSlimeOrbRender;
    private static Texture hiddenCharSelectBg;
    private static Texture gremlinPortrait;
    private static Hitbox gremlinLeftHb;
    private static Hitbox gremlinRightHb;
    private static boolean gremlinFeminizedSelected = loadGremlinFeminizedSelection();
    private static boolean loggedDisabled;
    private static String lastSkinStateSnapshot = "";
    private static int characterSelectLogCount;
    /** Set once both embedded skins are in place, so the per-frame append pass can bail out early. */
    private static boolean embeddedSkinsResolved;
    /**
     * Set once a full pass over the reskin characters made no changes, so the per-frame unlock scan
     * (a reflective read per character) stops repeating itself. {@code reskinUnlock} never reverts,
     * and the character array is fixed for the session, so one clean pass is conclusive.
     */
    private static boolean alternateSkinsUnlockResolved;
    /** Cached {@code Class#field -> Field} lookups; a null value records a confirmed miss. */
    private static volatile Map<String, Field> fieldCache = new HashMap<String, Field>();
    private static Field skinCharactersField;
    private static boolean skinCharactersFieldResolved;
    /** Cached {@code orb class -> is a Slimebound SpawnedSlime}; consulted per orb per frame. */
    private static volatile Map<Class<?>, Boolean> spawnedSlimeCache = new IdentityHashMap<Class<?>, Boolean>();

    private DownfallCompatibilityPatches() {
    }

    @SpirePatch(cls = "reskinContent.reskinContent", method = "loadSettings", requiredModId = DOWNFALL_MOD_ID, optional = true)
    public static class DefaultSkinPatch {
        @SpirePostfixPatch
        public static void postfix() {
            if (!activeFeminization) {
                logDisabled("reskinContent.loadSettings");
                return;
            }
            debug("reskinContent.loadSettings postfix entered");
            if (unlockAlternateSkins()) {
                debug("Unlocked Downfall reskin skins after loadSettings; saving Downfall reskin settings");
                saveReskinSettings();
            }
            appendEmbeddedSkins();
            logSkinState("after reskinContent.loadSettings", true);
        }
    }

    @SpirePatch2(clz = AbstractCreature.class, method = "loadAnimation", paramtypez = {String.class, String.class, float.class}, requiredModId = DOWNFALL_MOD_ID, optional = true)
    public static class DownfallAnimationResourcePatch {
        @SpirePrefixPatch
        public static SpireReturn<Void> prefix(AbstractCreature __instance, String atlasUrl, String skeletonUrl, float scale) {
            if (!activeFeminization) {
                logDisabled("AbstractCreature.loadAnimation");
                return SpireReturn.Continue();
            }
            if (Boolean.TRUE.equals(REDIRECTING_ANIMATION.get()) || LOAD_ANIMATION == null) {
                return SpireReturn.Continue();
            }

            String redirectedAtlas = redirectAnimationPath(atlasUrl);
            String redirectedSkeleton = redirectAnimationPath(skeletonUrl);
            logAnimationLoad(__instance, atlasUrl, skeletonUrl, scale, redirectedAtlas, redirectedSkeleton);
            boolean atlasRedirected = !redirectedAtlas.equals(atlasUrl);
            boolean skeletonRedirected = !redirectedSkeleton.equals(skeletonUrl);
            if (!atlasRedirected && !skeletonRedirected) {
                return SpireReturn.Continue();
            }
            if (atlasRedirected != skeletonRedirected) {
                debug("Redirect skipped because only one animation resource path changed: atlas=" + atlasUrl + " -> " + redirectedAtlas + ", json=" + skeletonUrl + " -> " + redirectedSkeleton);
                return SpireReturn.Continue();
            }
            if (!exists(redirectedAtlas) || !exists(redirectedSkeleton)) {
                debug("Redirect skipped because redirected animation resources are missing: atlas=" + redirectedAtlas + " exists=" + exists(redirectedAtlas) + ", json=" + redirectedSkeleton + " exists=" + exists(redirectedSkeleton));
                return SpireReturn.Continue();
            }

            REDIRECTING_ANIMATION.set(Boolean.TRUE);
            try {
                debug("Redirecting loadAnimation for " + __instance.getClass().getName() + ": " + atlasUrl + " -> " + redirectedAtlas + ", " + skeletonUrl + " -> " + redirectedSkeleton);
                ResourceUtil.invokeLoadAnimation(__instance, LOAD_ANIMATION, redirectedAtlas, redirectedSkeleton, scale);
                applyNpcCombatSkin(__instance, atlasUrl, skeletonUrl, redirectedAtlas, redirectedSkeleton);
                return SpireReturn.Return(null);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                REDIRECTING_ANIMATION.remove();
            }
            return SpireReturn.Continue();
        }

        @SpirePostfixPatch
        public static void postfix(AbstractCreature __instance, String atlasUrl, String skeletonUrl, float scale) {
            if (activeFeminization) {
                applyNpcCombatSkin(__instance, atlasUrl, skeletonUrl, atlasUrl, skeletonUrl);
            }
        }
    }

    @SpirePatch2(clz = AbstractMonster.class, method = "update", requiredModId = DOWNFALL_MOD_ID, optional = true)
    public static class NeowBossR18Patch {
        @SpirePostfixPatch
        public static void postfix(AbstractMonster __instance) {
            applyNeowBossR18Skin(__instance);
        }
    }

    @SpirePatch2(clz = CharacterSelectScreen.class, method = "update", requiredModId = DOWNFALL_MOD_ID, optional = true)
    public static class CharacterSelectUnlockPatch {
        @SpirePrefixPatch
        public static void prefix(CharacterSelectScreen __instance) {
            if (!activeFeminization) {
                logDisabled("CharacterSelectScreen.update");
                return;
            }
            if (unlockAlternateSkins()) {
                debug("Unlocked Downfall reskin skins during character select update; saving Downfall reskin settings");
                saveReskinSettings();
            }
            appendEmbeddedSkins();
            updateGremlinSkinControls(__instance);
            if (DEBUG_LOGS) {
                if (characterSelectLogCount < 5) {
                    ++characterSelectLogCount;
                    logSkinState("character select update #" + characterSelectLogCount, true);
                } else {
                    logSkinState("character select update changed", false);
                }
            }
        }
    }

    @SpirePatch2(clz = CharacterSelectScreen.class, method = "render", paramtypez = {SpriteBatch.class}, requiredModId = DOWNFALL_MOD_ID, optional = true)
    public static class CharacterSelectFullScreenPortraitPatch {
        @SpirePrefixPatch
        public static void prefix(CharacterSelectScreen __instance, SpriteBatch sb) {
            if (!activeFeminization) {
                return;
            }

            Texture portrait = getSelectedCoverPortrait(__instance);
            if (portrait == null) {
                return;
            }

            hiddenCharSelectBg = __instance.bgCharImg;
            __instance.bgCharImg = null;
            renderCover(sb, portrait);
        }

        @SpirePostfixPatch
        public static void postfix(CharacterSelectScreen __instance, SpriteBatch sb) {
            if (hiddenCharSelectBg != null) {
                __instance.bgCharImg = hiddenCharSelectBg;
                hiddenCharSelectBg = null;
            }
        }

        @SpirePostfixPatch
        public static void renderGremlinControls(CharacterSelectScreen __instance, SpriteBatch sb) {
            if (activeFeminization && isGremlinSelected(__instance)) {
                renderGremlinSkinControls(sb);
            }
        }
    }

    @SpirePatch(cls = "slimebound.characters.SlimeboundCharacter", method = "reloadAnimation", requiredModId = DOWNFALL_MOD_ID, optional = true)
    public static class SlimeboundReloadAnimationPatch {
        @SpirePrefixPatch
        public static void prefix(Object __instance) {
            if (!activeFeminization) {
                logDisabled("slimebound.characters.SlimeboundCharacter.reloadAnimation");
                return;
            }
            appendEmbeddedSkins();
            debugUnique(LOGGED_RESOURCE_PATHS, "slimebound-reload-current", "Slimebound reloadAnimation about to use reskinCount[1]=" + getReskinCount(1) + ", selectedSkin=" + getSelectedSkinClassName(1));
        }
    }

    @SpirePatch(cls = "theHexaghost.TheHexaghost", method = "reloadAnimation", requiredModId = DOWNFALL_MOD_ID, optional = true)
    public static class HexaghostReloadResourcePatch {
        @SpirePrefixPatch
        public static void prefix(Object __instance) {
            if (!activeFeminization) {
                logDisabled("theHexaghost.TheHexaghost.reloadAnimation resource override");
                return;
            }
            appendEmbeddedSkins();
            debugUnique(LOGGED_RESOURCE_PATHS, "hexaghost-reload-current", "Hexaghost reloadAnimation about to use reskinCount[2]=" + getReskinCount(2) + ", selectedSkin=" + getSelectedSkinClassName(2));
        }
    }

    @SpirePatch(cls = "gremlin.GremlinMod", method = "getGremlinSkeleton", paramtypez = {String.class}, requiredModId = DOWNFALL_MOD_ID, optional = true)
    public static class GremlinSkeletonPatch {
        @SpirePrefixPatch
        public static SpireReturn<Skeleton> prefix(String assetFolder) {
            if (!activeFeminization) {
                logDisabled("gremlin.GremlinMod.getGremlinSkeleton");
                return SpireReturn.Continue();
            }
            if (!gremlinFeminizedSelected) {
                return SpireReturn.Continue();
            }

            String base = OVERRIDE_PREFIX + "gremlinResources/images/char/" + assetFolder + "/skeleton";
            if (DEBUG_LOGS) {
                debugUnique(LOGGED_RESOURCE_PATHS, "gremlin:" + assetFolder, "Gremlin skeleton request assetFolder=" + assetFolder + ", overrideAtlas=" + base + ".atlas exists=" + exists(base + ".atlas") + ", overrideJson=" + base + ".json exists=" + exists(base + ".json"));
            }
            if (!exists(base + ".atlas") || !exists(base + ".json")) {
                return SpireReturn.Continue();
            }

            TextureAtlas atlas = null;
            try {
                Class<?> gremlinMod = Class.forName("gremlin.GremlinMod");
                Map<String, Skeleton> skeletonMap = getStaticField(gremlinMod, "gremlinSkeletonMap");
                Map<String, AnimationState> stateMap = getStaticField(gremlinMod, "gremlinAnimationStateMap");
                GremlinAssets cached = GREMLIN_ASSETS.get(assetFolder);
                if (cached != null) {
                    skeletonMap.put(assetFolder, cached.skeleton);
                    stateMap.put(assetFolder, cached.state);
                    return SpireReturn.Return(cached.skeleton);
                }

                atlas = ResourceUtil.loadAtlas(base + ".atlas");
                SkeletonJson json = new SkeletonJson(atlas);
                json.setScale(Settings.scale);
                SkeletonData skeletonData = json.readSkeletonData(Gdx.files.internal(base + ".json"));
                Skeleton skeleton = new Skeleton(skeletonData);
                skeleton.setColor(Color.WHITE);
                AnimationStateData stateData = new AnimationStateData(skeletonData);
                AnimationState state = new AnimationState(stateData);
                GremlinAssets assets = new GremlinAssets(atlas, skeleton, state,
                        skeletonMap.get(assetFolder), stateMap.get(assetFolder));
                GREMLIN_ASSETS.put(assetFolder, assets);
                skeletonMap.put(assetFolder, skeleton);
                stateMap.put(assetFolder, state);
                debug("Loaded feminized Gremlin skeleton for assetFolder=" + assetFolder + " from " + base);
                return SpireReturn.Return(skeleton);
            } catch (Exception e) {
                ResourceUtil.disposeAtlas(atlas);
                debug("Failed to load feminized Gremlin skeleton for assetFolder=" + assetFolder);
                e.printStackTrace();
                return SpireReturn.Continue();
            }
        }
    }

    @SpirePatch(cls = "gremlin.characters.GremlinCharacter", method = "swapBody", paramtypez = {String.class, String.class}, requiredModId = DOWNFALL_MOD_ID, optional = true)
    public static class GremlinCharacterSwapBodyPatch {
        // Also clean up the owned body when switching back to the native skin. The prefix below
        // handles the enabled path; the native swapBody must not leak our last body on disable.
        public static ExprEditor Instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if ("loadAnimation".equals(call.getMethodName())
                            && "(Ljava/lang/String;Ljava/lang/String;F)V".equals(call.getSignature())) {
                        call.replace("{ feminization.utils.ResourceUtil.reloadAnimation(this, $1, $2, $3); }");
                    }
                }
            };
        }

        @SpirePrefixPatch
        public static SpireReturn<String> prefix(Object __instance, String assetFolder, String animationName) {
            if (!activeFeminization || !gremlinFeminizedSelected || !(__instance instanceof AbstractPlayer) || LOAD_ANIMATION == null) {
                return SpireReturn.Continue();
            }

            String base = OVERRIDE_PREFIX + "gremlinResources/images/char/" + assetFolder + "/skeleton";
            if (!exists(base + ".atlas") || !exists(base + ".json")) {
                return SpireReturn.Continue();
            }

            try {
                AbstractPlayer player = (AbstractPlayer)__instance;
                String previousGremlin = getStringField(__instance, "currentGremlin");
                ResourceUtil.invokeLoadAnimation(player, LOAD_ANIMATION, base + ".atlas", base + ".json", 1.0f);
                AnimationState.TrackEntry entry = player.state.setAnimation(0, animationName, true);
                if (entry != null) {
                    entry.setTime(entry.getEndTime() * MathUtils.random());
                }
                setFieldValue(__instance, "currentGremlin", assetFolder);
                setFieldValue(__instance, "currentAnimation", animationName);
                return SpireReturn.Return(previousGremlin);
            } catch (Exception e) {
                debug("Failed to swap GremlinCharacter body to embedded skin: " + e.getClass().getName() + ": " + e.getMessage());
                e.printStackTrace();
                return SpireReturn.Continue();
            }
        }
    }

    @SpirePatch(cls = "slimebound.characters.SlimeboundCharacter", method = SpirePatch.CONSTRUCTOR, paramtypez = {String.class, AbstractPlayer.PlayerClass.class}, requiredModId = DOWNFALL_MOD_ID, optional = true)
    public static class SlimeboundHitboxPatch {
        @SpirePostfixPatch
        public static void postfix(Object __instance, String playerName, AbstractPlayer.PlayerClass playerClass) {
            if (!activeFeminization) {
                logDisabled("slimebound.characters.SlimeboundCharacter.<ctor>");
                return;
            }
            int reskinCount = getReskinCount(1);
            if (DEBUG_LOGS) {
                debugUnique(LOGGED_RESOURCE_PATHS, "slimebound-hitbox:" + reskinCount, "SlimeboundCharacter constructor: reskinCount[1]=" + reskinCount + ", instance=" + __instance.getClass().getName());
            }
            if (!shouldUseEmbeddedSlimeboundAnimation() || !(__instance instanceof AbstractPlayer)) {
                return;
            }

            AbstractPlayer player = (AbstractPlayer)__instance;
            player.hb_h = 360.0f * Settings.xScale;
            player.hb_y = -20.0f * Settings.scale;
            player.hb.resize(player.hb_w, player.hb_h);
            player.hb.move(player.drawX + player.hb_x + player.animX, player.drawY + player.hb_y + player.hb_h / 2.0f);
            player.healthHb.move(player.hb.cX, player.hb.cY - player.hb_h / 2.0f - player.healthHb.height / 2.0f);
        }
    }

    @SpirePatch(cls = "slimebound.orbs.SpawnedSlime", method = SpirePatch.CONSTRUCTOR, requiredModId = DOWNFALL_MOD_ID, optional = true)
    public static class SpawnedSlimeResourcePatch {
        public static ExprEditor Instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall methodCall) throws CannotCompileException {
                    if ("com.badlogic.gdx.Files".equals(methodCall.getClassName()) && "internal".equals(methodCall.getMethodName())) {
                        methodCall.replace("{ $_ = $proceed(downfallFeminization.DownfallCompatibilityPatches.redirectSlimeboundResource($1)); }");
                    }
                }
            };
        }
    }

    @SpirePatch(cls = "slimebound.orbs.SpawnedSlime", method = SpirePatch.CONSTRUCTOR, requiredModId = DOWNFALL_MOD_ID, optional = true)
    public static class SpawnedSlimeScalePatch {
        // The previous SpireInsertPatch(rloc = 38) hardcoded a bytecode offset inside Downfall's
        // constructor; any Downfall update shifted it. Intercepting SkeletonJson.setScale instead
        // survives reordering, and silently no-ops if the call ever disappears.
        public static ExprEditor Instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall methodCall) throws CannotCompileException {
                    if ("com.esotericsoftware.spine.SkeletonJson".equals(methodCall.getClassName())
                            && "setScale".equals(methodCall.getMethodName())) {
                        methodCall.replace("{ $proceed($1 * (downfallFeminization.DownfallCompatibilityPatches.shouldScaleSpawnedSlime() ? 0.75f : 1.0f)); }");
                    }
                }
            };
        }
    }

    @SpirePatch(cls = "reskinContent.skinCharacter.AbstractSkin", method = "loadAnimation", requiredModId = DOWNFALL_MOD_ID, optional = true)
    public static class SkinAnimationDisposePatch {
        @SpirePrefixPatch
        public static void prefix(Object __instance) {
            if (!activeFeminization) {
                logDisabled("reskinContent.skinCharacter.AbstractSkin.loadAnimation");
                return;
            }
            logSkinObject("AbstractSkin.loadAnimation", __instance);
            disposeAtlas(getFieldValue(__instance, "portraitAtlas"));
            setFieldValue(__instance, "portraitAtlas", null);
            setFieldValue(__instance, "portraitSkeleton", null);
            setFieldValue(__instance, "portraitState", null);
            setFieldValue(__instance, "portraitStateData", null);
            setFieldValue(__instance, "portraitData", null);
        }
    }

    @SpirePatch(cls = "reskinContent.skinCharacter.skins.Slimebound.Slaifu", method = "loadAnimation", requiredModId = DOWNFALL_MOD_ID, optional = true)
    public static class SlaifuAnimationDisposePatch {
        @SpirePrefixPatch
        public static void prefix() {
            if (!activeFeminization) {
                logDisabled("Slaifu.loadAnimation");
                return;
            }
            debug("Slaifu.loadAnimation prefix entered; disposing static sneckoAtlas cache if present");
            disposeSneckoAtlas("reskinContent.skinCharacter.skins.Slimebound.Slaifu");
        }
    }

    @SpirePatch(cls = "reskinContent.skinCharacter.skins.Snecko.SSSSnecko", method = "loadAnimation", requiredModId = DOWNFALL_MOD_ID, optional = true)
    public static class SneckoAnimationDisposePatch {
        @SpirePrefixPatch
        public static void prefix() {
            if (!activeFeminization) {
                logDisabled("SSSSnecko.loadAnimation");
                return;
            }
            debug("SSSSnecko.loadAnimation prefix entered; disposing static sneckoAtlas cache if present");
            disposeSneckoAtlas("reskinContent.skinCharacter.skins.Snecko.SSSSnecko");
        }
    }

    @SpirePatch(cls = "theHexaghost.TheHexaghost", method = "reloadAnimation", requiredModId = DOWNFALL_MOD_ID, optional = true)
    public static class TheHexaghostReloadPatch {
        @SpirePrefixPatch
        public static SpireReturn<Void> prefix(Object __instance) {
            if (!activeFeminization) {
                logDisabled("theHexaghost.TheHexaghost.reloadAnimation");
                return SpireReturn.Continue();
            }
            int reskinCount = getReskinCount(2);
            if (DEBUG_LOGS) {
                debugUnique(LOGGED_RESOURCE_PATHS, "hexaghost-reload:" + reskinCount, "Hexaghost reloadAnimation: reskinCount[2]=" + reskinCount + ", instance=" + __instance.getClass().getName() + ", overrideAtlasExists=" + exists(HEXAGHOST_DEFAULT_ATLAS) + ", overrideJsonExists=" + exists(HEXAGHOST_DEFAULT_JSON));
            }
            if (!shouldUseEmbeddedHexaghostAnimation() || !(__instance instanceof AbstractPlayer) || LOAD_ANIMATION == null || !exists(HEXAGHOST_DEFAULT_ATLAS) || !exists(HEXAGHOST_DEFAULT_JSON)) {
                return SpireReturn.Continue();
            }

            try {
                AbstractPlayer player = (AbstractPlayer)__instance;
                ResourceUtil.invokeLoadAnimation(player, LOAD_ANIMATION, HEXAGHOST_DEFAULT_ATLAS, HEXAGHOST_DEFAULT_JSON, 1.0f);
                AnimationState.TrackEntry entry = player.state.setAnimation(0, "animation", true);
                if (entry != null) {
                    entry.setTime(entry.getEndTime() * MathUtils.random());
                }
                return SpireReturn.Return(null);
            } catch (Exception e) {
                e.printStackTrace();
                return SpireReturn.Continue();
            }
        }
    }

    /**
     * Downfall's playable Hexaghost draws its ghost-flame body through {@code theHexaghost.vfx.MyBody}
     * (field {@code TheHexaghost.myBody}), entirely separate from the player's spine skeleton. Swapping
     * the skeleton alone therefore leaves the vanilla flame silhouette rendering behind the feminized
     * skin. Cancel it while our embedded skin is worn — but only for the local player, mirroring how
     * skindex scopes its own MyBodyRenderPatch so remote players keep their own visuals.
     */
    @SpirePatch(cls = "theHexaghost.vfx.MyBody", method = "render", paramtypez = {SpriteBatch.class}, requiredModId = DOWNFALL_MOD_ID, optional = true)
    public static class HexaghostMyBodyRenderPatch {
        @SpirePrefixPatch
        public static SpireReturn<Void> prefix(Object __instance, SpriteBatch sb) {
            if (!activeFeminization || !shouldUseEmbeddedHexaghostAnimation()) {
                return SpireReturn.Continue();
            }
            if (isLocalPlayerBody(__instance)) {
                return SpireReturn.Return(null);
            }
            return SpireReturn.Continue();
        }
    }

    private static volatile Field myBodyField;
    private static volatile boolean myBodyFieldResolved;

    private static boolean isLocalPlayerBody(Object candidate) {
        if (!myBodyFieldResolved) {
            try {
                myBodyField = findField(Class.forName("theHexaghost.TheHexaghost"), "myBody");
            } catch (Exception e) {
                myBodyField = null;
            } finally {
                myBodyFieldResolved = true;
            }
        }
        Field field = myBodyField;
        if (field == null) {
            return false;
        }
        Object player = AbstractDungeon.player;
        if (player == null || !"theHexaghost.TheHexaghost".equals(player.getClass().getName())) {
            return false;
        }
        try {
            return candidate == field.get(player);
        } catch (Exception ignored) {
            return false;
        }
    }

    @SpirePatch(clz = AbstractPlayer.class, method = "render", paramtypez = {SpriteBatch.class}, requiredModId = DOWNFALL_MOD_ID, optional = true)
    public static class SlimeOrbRenderPrefixPatch {
        @SpirePrefixPatch
        public static void prefix(AbstractPlayer __instance, SpriteBatch sb) {
            if (!activeFeminization || !shouldUseEmbeddedSlimeboundAnimation() || __instance.orbs.isEmpty()) {
                if (!activeFeminization) {
                    logDisabled("AbstractPlayer.render slime orb prefix");
                }
                return;
            }

            for (AbstractOrb orb : __instance.orbs) {
                if (isSpawnedSlime(orb) && !getNoRender(orb)) {
                    setNoRender(orb, true);
                    resetSlimeOrbRender = true;
                }
            }
        }
    }

    @SpirePatch(clz = AbstractPlayer.class, method = "render", paramtypez = {SpriteBatch.class}, requiredModId = DOWNFALL_MOD_ID, optional = true)
    public static class SlimeOrbRenderPostfixPatch {
        @SpirePostfixPatch
        public static void postfix(AbstractPlayer __instance, SpriteBatch sb) {
            boolean canDrawSlimes = activeFeminization && shouldUseEmbeddedSlimeboundAnimation() && isInCombatRoom() && !__instance.isDead;
            // Un-hiding must be unconditional: the prefix may have hidden orbs on a frame whose tail
            // conditions (dead player, non-combat room) would otherwise bail out below and leave
            // those orbs permanently invisible.
            if (canDrawSlimes) {
                for (AbstractOrb orb : __instance.orbs) {
                    if (isSpawnedSlime(orb)) {
                        if (resetSlimeOrbRender) {
                            setNoRender(orb, false);
                        }
                        orb.render(sb);
                    }
                }
            } else if (resetSlimeOrbRender && !__instance.orbs.isEmpty()) {
                for (AbstractOrb orb : __instance.orbs) {
                    if (isSpawnedSlime(orb)) {
                        setNoRender(orb, false);
                    }
                }
            }
            resetSlimeOrbRender = false;
        }
    }

    private static boolean unlockAlternateSkins() {
        if (alternateSkinsUnlockResolved) {
            return false;
        }
        Object[] characters = getSkinCharacters();
        if (characters == null || characters.length == 0) {
            return false;
        }

        boolean changed = false;
        for (Object character : characters) {
            if (character == null) {
                continue;
            }
            try {
                Object skins = findField(character.getClass(), "skins").get(character);
                if (skins instanceof Object[] && ((Object[])skins).length > 1) {
                    Field unlockField = findField(character.getClass(), "reskinUnlock");
                    if (!unlockField.getBoolean(character)) {
                        unlockField.setBoolean(character, true);
                        changed = true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (!changed) {
            // Every character is unlocked (or has nothing to unlock); stop rescanning each frame.
            alternateSkinsUnlockResolved = true;
        }
        return changed;
    }

    private static void saveReskinSettings() {
        try {
            Class<?> reskinContent = Class.forName("reskinContent.reskinContent");
            Method saveSettings = reskinContent.getDeclaredMethod("saveSettings");
            saveSettings.setAccessible(true);
            debug("Calling reskinContent.reskinContent.saveSettings()");
            saveSettings.invoke(null);
            debug("reskinContent.reskinContent.saveSettings() completed");
        } catch (Exception e) {
            debug("Failed to call reskinContent.reskinContent.saveSettings()");
            e.printStackTrace();
        }
    }

    private static boolean loadGremlinFeminizedSelection() {
        try {
            SpireConfig config = new SpireConfig(CONFIG_MOD_NAME, CONFIG_FILE_NAME);
            if (!config.has(GREMLIN_FEMINIZED_KEY)) {
                config.setBool(GREMLIN_FEMINIZED_KEY, false);
                config.save();
                return false;
            }
            return config.getBool(GREMLIN_FEMINIZED_KEY);
        } catch (Exception e) {
            // Runs from the static initializer; a corrupt properties value must not blow up class
            // loading with an ExceptionInInitializerError.
            debug("Failed to load Gremlin feminized skin selection: " + e.getClass().getName() + ": " + e.getMessage());
            return false;
        }
    }

    private static void saveGremlinFeminizedSelection() {
        try {
            SpireConfig config = new SpireConfig(CONFIG_MOD_NAME, CONFIG_FILE_NAME);
            config.setBool(GREMLIN_FEMINIZED_KEY, gremlinFeminizedSelected);
            config.save();
        } catch (Exception e) {
            debug("Failed to save Gremlin feminized skin selection: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void clearGremlinSkeletonCaches() {
        try {
            Class<?> gremlinMod = Class.forName("gremlin.GremlinMod");
            Map<String, Skeleton> skeletonMap = getStaticField(gremlinMod, "gremlinSkeletonMap");
            Map<String, AnimationState> stateMap = getStaticField(gremlinMod, "gremlinAnimationStateMap");
            for (Map.Entry<String, GremlinAssets> entry : GREMLIN_ASSETS.entrySet()) {
                String key = entry.getKey();
                GremlinAssets assets = entry.getValue();
                // Preserve native/third-party caches. Restore only entries still pointing at ours.
                if (skeletonMap.get(key) == assets.skeleton) {
                    if (assets.previousSkeleton == null) {
                        skeletonMap.remove(key);
                    } else {
                        skeletonMap.put(key, assets.previousSkeleton);
                    }
                }
                if (stateMap.get(key) == assets.state) {
                    if (assets.previousState == null) {
                        stateMap.remove(key);
                    } else {
                        stateMap.put(key, assets.previousState);
                    }
                }
                ResourceUtil.disposeAtlas(assets.atlas);
            }
            GREMLIN_ASSETS.clear();
        } catch (Exception e) {
            debug("Failed to clear Gremlin skeleton caches: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private static final class GremlinAssets {
        private final TextureAtlas atlas;
        private final Skeleton skeleton;
        private final AnimationState state;
        private final Skeleton previousSkeleton;
        private final AnimationState previousState;

        private GremlinAssets(TextureAtlas atlas, Skeleton skeleton, AnimationState state,
                Skeleton previousSkeleton, AnimationState previousState) {
            this.atlas = atlas;
            this.skeleton = skeleton;
            this.state = state;
            this.previousSkeleton = previousSkeleton;
            this.previousState = previousState;
        }
    }

    private static void appendEmbeddedSkins() {
        // Called every frame from CharacterSelectScreen.update. Once both skins are resolved there is
        // nothing left to do, so short-circuit instead of re-running the filesystem probes and the
        // linear scan over the skin array on every frame.
        if (embeddedSkinsResolved) {
            return;
        }
        boolean slimebound = appendEmbeddedSkin(1, "reskinContent.skinCharacter.skins.Slimebound.Slaifu", SLIMEBOUND_ATLAS_URL, SLIMEBOUND_JSON_URL, CUSTOM_FEMINIZED_PORTRAIT, 1.2f, SLIMEBOUND_FEMINIZED_SKIN_NAME, FEMINIZED_SKIN_DESCRIPTION);
        boolean hexaghost = appendEmbeddedSkin(2, "reskinContent.skinCharacter.skins.Hexaghost.HexaghostOriginal", HEXAGHOST_DEFAULT_ATLAS, HEXAGHOST_DEFAULT_JSON, HEXAGHOST_FEMINIZED_PORTRAIT, 1.0f, HEXAGHOST_FEMINIZED_SKIN_NAME, FEMINIZED_SKIN_DESCRIPTION);
        embeddedSkinsResolved = slimebound && hexaghost;
    }

    /**
     * @return true once this skin is present in the character's skin array, meaning no further work is
     *         needed on later frames. False while Downfall's own data is not ready yet.
     */
    private static boolean appendEmbeddedSkin(int characterIndex, String templateSkinClassName, String atlasUrl, String jsonUrl, String placeholderPng, float renderScale, String skinName, String skinDescription) {
        Object character = getSkinCharacter(characterIndex);
        if (character == null) {
            return false;
        }
        Object skinsObject = getFieldValue(character, "skins");
        if (!(skinsObject instanceof Object[])) {
            return false;
        }
        if (!exists(atlasUrl) || !exists(jsonUrl) || !exists(placeholderPng)) {
            if (DEBUG_LOGS) {
                debugUnique(LOGGED_RESOURCE_PATHS, "missing-embedded-skin:" + templateSkinClassName, "Embedded Downfall skin resources missing for " + templateSkinClassName + ": atlasExists=" + exists(atlasUrl) + ", jsonExists=" + exists(jsonUrl) + ", placeholderExists=" + exists(placeholderPng));
            }
            // Resources will not appear later in this session; stop retrying.
            return true;
        }

        Object[] skins = (Object[])skinsObject;
        Object existingSkin = findEmbeddedSkin(skins, atlasUrl, jsonUrl);
        if (existingSkin != null) {
            EMBEDDED_SKINS.add(existingSkin);
            return true;
        }

        try {
            Object skin = Class.forName(templateSkinClassName).getDeclaredConstructor().newInstance();
            configureEmbeddedSkin(skin, atlasUrl, jsonUrl, placeholderPng, renderScale, skinName, skinDescription);
            EMBEDDED_SKINS.add(skin);
            Object expanded = Array.newInstance(skins.getClass().getComponentType(), skins.length + 1);
            System.arraycopy(skins, 0, expanded, 0, skins.length);
            Array.set(expanded, skins.length, skin);
            findField(character.getClass(), "skins").set(character, expanded);
            clampReskinCount(character, skins.length + 1);
            debug("Appended embedded Downfall skin to " + character.getClass().getName() + ": template=" + templateSkinClassName + ", index=" + skins.length + ", atlasURL=" + atlasUrl + ", jsonURL=" + jsonUrl + ", placeholder=" + placeholderPng);
            return true;
        } catch (Exception e) {
            debug("Failed to append embedded Downfall skin from template " + templateSkinClassName + ": " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            // The template class is missing or incompatible; retrying every frame cannot fix that.
            return true;
        }
    }

    private static void clampReskinCount(Object character, int skinCount) {
        try {
            Field reskinCountField = findField(character.getClass(), "reskinCount");
            int reskinCount = reskinCountField.getInt(character);
            if (reskinCount < 0 || reskinCount >= skinCount) {
                reskinCountField.setInt(character, 0);
            }
        } catch (Exception e) {
            debug("Failed to clamp Downfall reskinCount: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private static Object findEmbeddedSkin(Object[] skins, String atlasUrl, String jsonUrl) {
        for (Object skin : skins) {
            if (skin == null) {
                continue;
            }
            if (atlasUrl.equals(getFieldValue(skin, "atlasURL")) && jsonUrl.equals(getFieldValue(skin, "jsonURL"))) {
                return skin;
            }
        }
        return null;
    }

    private static void configureEmbeddedSkin(Object skin, String atlasUrl, String jsonUrl, String placeholderPng, float renderScale, String skinName, String skinDescription) {
        setFieldValue(skin, "NAME", skinName);
        setFieldValue(skin, "DESCRIPTION", skinDescription);
        setFieldValue(skin, "atlasURL", atlasUrl);
        setFieldValue(skin, "jsonURL", jsonUrl);
        setFloatFieldIfDifferent(skin, "renderscale", renderScale);
        setFieldValue(skin, "portraitAtlasPath", null);
        setFieldValue(skin, "portraitAnimationType", 0);
        Texture placeholder = loadTexture(placeholderPng);
        setFieldValue(skin, "portraitStatic_IMG", placeholder);
        setFieldValue(skin, "portraitAnimation_IMG", placeholder);
        disposeAtlas(getFieldValue(skin, "portraitAtlas"));
        setFieldValue(skin, "portraitAtlas", null);
        setFieldValue(skin, "portraitSkeleton", null);
        setFieldValue(skin, "portraitState", null);
        setFieldValue(skin, "portraitStateData", null);
        setFieldValue(skin, "portraitData", null);
    }

    private static Texture loadTexture(String path) {
        try {
            Texture texture = new Texture(Gdx.files.internal(path));
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            return texture;
        } catch (Exception e) {
            debug("Failed to load placeholder texture " + path + ": " + e.getClass().getName() + ": " + e.getMessage());
            return null;
        }
    }

    private static String redirectAnimationPath(String path) {
        String npcRedirect = redirectNpcAnimationPath(path);
        if (npcRedirect != path) {
            return npcRedirect;
        }
        String gremlinRedirect = redirectGremlinCharacterAnimationPath(path);
        if (gremlinRedirect != path) {
            return gremlinRedirect;
        }
        if (shouldUseEmbeddedSlimeboundAnimation() && (SLIMEBOUND_CHARACTER_ATLAS.equals(path) || SLIMEBOUND_CHARACTER_JSON.equals(path))) {
            return OVERRIDE_PREFIX + path;
        }
        return path;
    }

    private static String redirectNpcAnimationPath(String path) {
        if (!NpcFeminizationMod.activeFeminization || !isNpcAnimationPath(path)) {
            return path;
        }
        String redirected = NPC_OVERRIDE_PREFIX + path;
        return exists(redirected) ? redirected : path;
    }

    private static boolean isNpcAnimationPath(String path) {
        return MERCHANT_NPC_ATLAS.equals(path)
                || MERCHANT_NPC_JSON.equals(path)
                || NEOW_NPC_ATLAS.equals(path)
                || NEOW_NPC_JSON.equals(path);
    }

    private static void applyNpcCombatSkin(AbstractCreature creature, String atlasUrl, String skeletonUrl, String redirectedAtlas, String redirectedSkeleton) {
        if (creature == null || creature.isPlayer || !NpcFeminizationMod.activeFeminization) {
            return;
        }
        if (!isNpcAnimationPath(atlasUrl) && !isNpcAnimationPath(skeletonUrl) && !isNpcOverridePath(redirectedAtlas) && !isNpcOverridePath(redirectedSkeleton)) {
            return;
        }
        if (isNeowBoss(creature) || !NpcFeminizationMod.activeH) {
            return;
        }
        try {
            Object skeleton = findField(creature.getClass(), "skeleton").get(creature);
            if (skeleton instanceof Skeleton) {
                ((Skeleton)skeleton).setSkin("nsfw");
            }
        } catch (Exception ignored) {
        }
    }

    private static void applyNeowBossR18Skin(AbstractMonster boss) {
        if (boss == null || !isNeowBoss(boss) || !NpcFeminizationMod.activeFeminization || !NpcFeminizationMod.activeH) {
            return;
        }
        try {
            // currentHealth/maxHealth are public on AbstractCreature; reflecting for them meant walking
            // the hierarchy from NeowBoss on every frame of the fight.
            if (boss.maxHealth <= 0 || boss.currentHealth * 2 > boss.maxHealth) {
                return;
            }
            Object skeleton = findField(boss.getClass(), "skeleton").get(boss);
            if (skeleton instanceof Skeleton) {
                Skeleton spine = (Skeleton)skeleton;
                if (spine.getSkin() == null || !"nsfw".equals(spine.getSkin().getName())) {
                    spine.setSkin("nsfw");
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static boolean isNeowBoss(Object creature) {
        if (creature == null) {
            return false;
        }
        String className = creature.getClass().getName();
        return "downfall.monsters.NeowBoss".equals(className)
                || "downfall.monsters.NeowBossFinal".equals(className)
                || "downfall.monsters.OldNeow".equals(className);
    }

    private static boolean isNpcOverridePath(String path) {
        return path != null && path.startsWith(NPC_OVERRIDE_PREFIX) && isNpcAnimationPath(path.substring(NPC_OVERRIDE_PREFIX.length()));
    }

    private static String redirectGremlinCharacterAnimationPath(String path) {
        if (!gremlinFeminizedSelected || path == null || !path.startsWith("gremlinResources/images/char/") || !path.endsWith("/skeleton.atlas") && !path.endsWith("/skeleton.json")) {
            return path;
        }
        String redirected = OVERRIDE_PREFIX + path;
        return exists(redirected) ? redirected : path;
    }

    private static boolean shouldUseEmbeddedHexaghostAnimation() {
        return isSkinSelected(2, HEXAGHOST_DEFAULT_ATLAS, HEXAGHOST_DEFAULT_JSON);
    }

    /** Called from instrumented SpawnedSlime bytecode; must stay cheap and never throw. */
    public static boolean shouldScaleSpawnedSlime() {
        return activeFeminization && shouldUseEmbeddedSlimeboundAnimation();
    }

    private static boolean shouldUseEmbeddedSlimeboundAnimation() {
        return isSkinSelected(1, SLIMEBOUND_ATLAS_URL, SLIMEBOUND_JSON_URL);
    }

    private static boolean isSkinSelected(int characterIndex, String atlasUrl, String jsonUrl) {
        Object skin = getSelectedSkin(characterIndex);
        return skin != null && atlasUrl.equals(getFieldValue(skin, "atlasURL")) && jsonUrl.equals(getFieldValue(skin, "jsonURL"));
    }

    private static boolean isEmbeddedSkinSelected(CharacterSelectScreen screen) {
        Object skin = getSelectedDownfallSkin(screen);
        return skin != null && EMBEDDED_SKINS.contains(skin);
    }

    private static Texture getSelectedCoverPortrait(CharacterSelectScreen screen) {
        if (isGremlinSelected(screen) && gremlinFeminizedSelected) {
            return getGremlinPortrait();
        }
        if (isEmbeddedSkinSelected(screen)) {
            return getSelectedEmbeddedPortrait(screen);
        }
        return null;
    }

    private static Texture getSelectedEmbeddedPortrait(CharacterSelectScreen screen) {
        Object skin = getSelectedDownfallSkin(screen);
        Object texture = getFieldValue(skin, "portraitStatic_IMG");
        return texture instanceof Texture ? (Texture)texture : null;
    }

    private static Object getSelectedDownfallSkin(CharacterSelectScreen screen) {
        if (screen == null || screen.options == null) {
            return null;
        }

        CharacterOption selectedOption = null;
        for (CharacterOption option : screen.options) {
            if (option.selected) {
                selectedOption = option;
                break;
            }
        }
        if (selectedOption == null) {
            return null;
        }

        Object[] characters = getSkinCharacters();
        if (characters == null) {
            return null;
        }

        for (Object character : characters) {
            if (!selectedOption.name.equals(getFieldValue(character, "id"))) {
                continue;
            }
            Object skin = getSelectedSkinFromCharacter(character);
            if (skin != null && EMBEDDED_SKINS.contains(skin)) {
                return skin;
            }
        }
        return null;
    }

    private static boolean isGremlinSelected(CharacterSelectScreen screen) {
        CharacterOption option = getSelectedOption(screen);
        return option != null && isGremlinOption(option);
    }

    private static CharacterOption getSelectedOption(CharacterSelectScreen screen) {
        if (screen == null || screen.options == null) {
            return null;
        }
        for (CharacterOption option : screen.options) {
            if (option.selected) {
                return option;
            }
        }
        return null;
    }

    private static boolean isGremlinOption(CharacterOption option) {
        if (option == null) {
            return false;
        }
        if (isGremlinCharacter(option.c)) {
            return true;
        }
        String gremlinName = getGremlinLocalizedName();
        return gremlinName != null && gremlinName.equals(option.name);
    }

    private static boolean isGremlinCharacter(Object character) {
        return character != null && "gremlin.characters.GremlinCharacter".equals(character.getClass().getName());
    }

    /** Cached localized Gremlin name; resolved at most once, including a confirmed miss. */
    private static String gremlinLocalizedName;
    private static boolean gremlinLocalizedNameResolved;

    private static String getGremlinLocalizedName() {
        // Consulted every frame while any non-Gremlin character option is selected on the character
        // select screen; the reflective resolution must happen once, not per frame.
        if (gremlinLocalizedNameResolved) {
            return gremlinLocalizedName;
        }
        try {
            Class<?> gremlinCharacter = Class.forName("gremlin.characters.GremlinCharacter");
            Object strings = findField(gremlinCharacter, "characterStrings").get(null);
            Object names = findField(strings.getClass(), "NAMES").get(strings);
            if (names instanceof String[] && ((String[])names).length > 0) {
                gremlinLocalizedName = ((String[])names)[0];
            }
        } catch (Exception ignored) {
        } finally {
            gremlinLocalizedNameResolved = true;
        }
        return gremlinLocalizedName;
    }

    private static void updateGremlinSkinControls(CharacterSelectScreen screen) {
        if (!isGremlinSelected(screen)) {
            return;
        }
        ensureGremlinHitboxes();
        updateGremlinButton(gremlinLeftHb);
        updateGremlinButton(gremlinRightHb);
        if (gremlinLeftHb.clicked || gremlinRightHb.clicked) {
            gremlinLeftHb.clicked = false;
            gremlinRightHb.clicked = false;
            gremlinFeminizedSelected = !gremlinFeminizedSelected;
            saveGremlinFeminizedSelection();
            clearGremlinSkeletonCaches();
            CharacterOption option = getSelectedOption(screen);
            if (option != null && option.c != null) {
                refreshGremlinFrontBody(option.c);
                option.c.doCharSelectScreenSelectEffect();
            }
        }
    }

    private static void refreshGremlinFrontBody(Object character) {
        if (!isGremlinCharacter(character)) {
            return;
        }
        String assetFolder = getStringField(character, "currentGremlin");
        String animationName = getStringField(character, "currentAnimation");
        Object mobState = getFieldValue(character, "mobState");
        if ((assetFolder == null || assetFolder.length() == 0) && mobState != null) {
            assetFolder = invokeStringMethod(mobState, "getFrontGremlin");
        }
        if ((animationName == null || animationName.length() == 0) && mobState != null) {
            animationName = invokeStringMethod(mobState, "getFrontAnimation");
        }
        if (assetFolder == null || animationName == null) {
            return;
        }
        try {
            Method swapBody = character.getClass().getDeclaredMethod("swapBody", String.class, String.class);
            swapBody.setAccessible(true);
            swapBody.invoke(character, assetFolder, animationName);
        } catch (Exception e) {
            debug("Failed to refresh Gremlin front body after skin toggle: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private static void updateGremlinButton(Hitbox hitbox) {
        hitbox.update();
        if (hitbox.justHovered) {
            CardCrawlGame.sound.playA("UI_HOVER", -0.3f);
        }
        if (InputHelper.justClickedLeft && hitbox.hovered) {
            CardCrawlGame.sound.playA("UI_CLICK_1", -0.4f);
            hitbox.clickStarted = true;
        }
    }

    private static void renderGremlinSkinControls(SpriteBatch sb) {
        ensureGremlinHitboxes();
        renderArrow(sb, gremlinLeftHb, false);
        String label = gremlinFeminizedSelected ? GREMLIN_FEMINIZED_SKIN_NAME : "Original";
        FontHelper.renderSmartText(sb, (BitmapFont)FontHelper.buttonLabelFont, label, 250.0f * Settings.scale, (float)Settings.HEIGHT / 2.0f + 280.0f * Settings.scale, 10000.0f, 40.0f * Settings.scale, Settings.CREAM_COLOR, 1.0f);
        if (gremlinFeminizedSelected) {
            FontHelper.renderSmartText(sb, (BitmapFont)FontHelper.charDescFont, FEMINIZED_SKIN_DESCRIPTION, 250.0f * Settings.scale, (float)Settings.HEIGHT / 2.0f + 235.0f * Settings.scale, 10000.0f, 32.0f * Settings.scale, Settings.CREAM_COLOR, 1.0f);
        }
        renderArrow(sb, gremlinRightHb, true);
    }

    private static void renderArrow(SpriteBatch sb, Hitbox hitbox, boolean flipX) {
        sb.draw(ImageMaster.POPUP_ARROW, hitbox.cX - 40.0f, hitbox.cY - 40.0f, 20.0f, 20.0f, 80.0f, 80.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 256, 256, flipX, false);
        if (hitbox.hovered) {
            sb.setBlendFunction(770, 1);
            sb.setColor(HOVER_COLOR);
            sb.draw(ImageMaster.POPUP_ARROW, hitbox.cX - 40.0f, hitbox.cY - 40.0f, 20.0f, 20.0f, 80.0f, 80.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 256, 256, flipX, false);
            sb.setColor(Color.WHITE);
            sb.setBlendFunction(770, 771);
        }
    }

    private static void ensureGremlinHitboxes() {
        float infoX = 200.0f * Settings.scale;
        float infoY = (float)Settings.HEIGHT / 2.0f;
        if (gremlinLeftHb == null) {
            gremlinLeftHb = new Hitbox(80.0f * Settings.scale, 80.0f * Settings.scale);
        }
        if (gremlinRightHb == null) {
            gremlinRightHb = new Hitbox(80.0f * Settings.scale, 80.0f * Settings.scale);
        }
        gremlinLeftHb.move(infoX, infoY + 272.0f * Settings.scale);
        gremlinRightHb.move(infoX + 250.0f * Settings.scale, infoY + 272.0f * Settings.scale);
    }

    private static Texture getGremlinPortrait() {
        if (gremlinPortrait == null) {
            gremlinPortrait = loadTexture(GREMLIN_PORTRAIT);
        }
        return gremlinPortrait;
    }

    private static void renderCover(SpriteBatch sb, Texture texture) {
        float screenW = (float)Settings.WIDTH;
        float screenH = (float)Settings.HEIGHT;
        float scale = Math.max(screenW / texture.getWidth(), screenH / texture.getHeight());
        float drawW = texture.getWidth() * scale;
        float drawH = texture.getHeight() * scale;
        float x = (screenW - drawW) / 2.0f;
        float y = (screenH - drawH) / 2.0f;
        sb.setColor(Color.WHITE);
        sb.draw(texture, x, y, drawW, drawH);
    }

    public static String redirectSlimeboundResource(String path) {
        if (!activeFeminization) {
            logDisabled("SpawnedSlime resource redirect");
            return path;
        }
        if (!shouldUseEmbeddedSlimeboundAnimation()) {
            return path;
        }
        String monsterRedirected = redirectSpawnedSlimeMonsterResource(path);
        if (monsterRedirected != path) {
            return monsterRedirected;
        }
        String redirected = redirectSlimeboundOrbResource(path);
        if (redirected != path) {
            return redirected;
        }
        return path;
    }

    private static String redirectSpawnedSlimeMonsterResource(String path) {
        if (!isSpawnedSlimeMonsterResource(path)) {
            return path;
        }
        String redirected = MONSTER_OVERRIDE_PREFIX + path;
        if (!exists(redirected)) {
            return path;
        }
        if (DEBUG_LOGS) {
            debugUnique(LOGGED_RESOURCE_PATHS, "spawned-slime-monster:" + path, "SpawnedSlime monster resource path=" + path + ", redirected=" + redirected + ", redirectedExists=true");
        }
        return redirected;
    }

    private static boolean isSpawnedSlimeMonsterResource(String path) {
        if (path == null || !path.startsWith("images/monsters/theBottom/slime")) {
            return false;
        }
        return path.endsWith("/skeleton.atlas") || path.endsWith("/skeleton.json");
    }

    private static String redirectSlimeboundOrbResource(String path) {
        String orbPrefix = "slimeboundResources/SlimeboundImages/orbs/";
        if (path == null || !path.startsWith(orbPrefix)) {
            return path;
        }

        String extension = getResourceExtension(path);
        if (".json".equals(extension)) {
            return path;
        }
        String redirected = OVERRIDE_PREFIX + path;
        // Single probe, shared by the redirect decision and any debug output below.
        boolean redirectedExists = exists(redirected);
        if (DEBUG_LOGS) {
            debugUnique(LOGGED_RESOURCE_PATHS, "slime-orb:" + path, "SpawnedSlime resource path=" + path + ", redirected=" + redirected + ", redirectedExists=" + redirectedExists);
        }
        if (redirectedExists && canRedirectSlimeOrb(path, redirected, extension)) {
            return redirected;
        }

        String orbName = getSlimeOrbName(path.substring(orbPrefix.length()));
        if (orbName == null || extension == null) {
            return path;
        }

        String flatRedirected = OVERRIDE_PREFIX + orbPrefix + canonicalSlimeOrbName(orbName) + extension;
        boolean flatRedirectedExists = exists(flatRedirected);
        if (DEBUG_LOGS) {
            debugUnique(LOGGED_RESOURCE_PATHS, "slime-orb:" + path, "SpawnedSlime resource path=" + path + ", redirected=" + flatRedirected + ", redirectedExists=" + flatRedirectedExists);
        }
        if (flatRedirectedExists && canRedirectSlimeOrb(path, flatRedirected, extension)) {
            return flatRedirected;
        }
        return path;
    }

    private static boolean canRedirectSlimeOrb(String originalPath, String redirectedPath, String extension) {
        if (".atlas".equals(extension)) {
            return true;
        }
        if (".json".equals(extension)) {
            return false;
        }
        return true;
    }

    private static String getResourceExtension(String path) {
        int dot = path.lastIndexOf('.');
        if (dot < 0) {
            return null;
        }
        String extension = path.substring(dot);
        if (".atlas".equals(extension) || ".json".equals(extension) || ".png".equals(extension)) {
            return extension;
        }
        return null;
    }

    private static String getSlimeOrbName(String relativePath) {
        if (relativePath == null || relativePath.length() == 0) {
            return null;
        }
        String normalized = relativePath.replace('\\', '/');
        int slash = normalized.indexOf('/');
        if (slash > 0) {
            return normalized.substring(0, slash);
        }
        int dot = normalized.lastIndexOf('.');
        if (dot > 0) {
            return normalized.substring(0, dot);
        }
        return null;
    }

    private static String canonicalSlimeOrbName(String orbName) {
        if ("poisonslime".equalsIgnoreCase(orbName)) {
            return "PoisonSlime";
        }
        if ("darklingslime".equalsIgnoreCase(orbName)) {
            return "DarklingSlime";
        }
        return orbName;
    }

    private static int getReskinCount(int characterIndex) {
        Object character = getSkinCharacter(characterIndex);
        if (character == null) {
            return -1;
        }
        try {
            return findField(character.getClass(), "reskinCount").getInt(character);
        } catch (Exception e) {
            return -1;
        }
    }

    private static Object getSkinCharacter(int index) {
        Object[] characters = getSkinCharacters();
        if (characters == null || index < 0 || index >= characters.length) {
            return null;
        }
        return characters[index];
    }

    private static String getSelectedSkinClassName(int characterIndex) {
        Object skin = getSelectedSkin(characterIndex);
        return skin == null ? "<missing-selected-skin>" : skin.getClass().getName();
    }

    private static Object getSelectedSkin(int characterIndex) {
        Object character = getSkinCharacter(characterIndex);
        return getSelectedSkinFromCharacter(character);
    }

    private static Object getSelectedSkinFromCharacter(Object character) {
        if (character == null) {
            return null;
        }
        Object skinsObject = getFieldValue(character, "skins");
        int reskinCount = getIntFieldDebug(character, "reskinCount", -1);
        if (!(skinsObject instanceof Object[])) {
            return null;
        }
        Object[] skins = (Object[])skinsObject;
        if (reskinCount < 0 || reskinCount >= skins.length) {
            return null;
        }
        return skins[reskinCount];
    }

    private static Object[] getSkinCharacters() {
        // Reached twice per frame from AbstractPlayer.render while Downfall is installed, so the
        // Class.forName and the field lookup are resolved once and reused.
        if (skinCharactersFieldResolved) {
            if (skinCharactersField == null) {
                return null;
            }
            try {
                Object characters = skinCharactersField.get(null);
                return characters instanceof Object[] ? (Object[])characters : null;
            } catch (Exception e) {
                return null;
            }
        }

        try {
            Class<?> patches = Class.forName("reskinContent.patches.CharacterSelectScreenPatches");
            skinCharactersField = findField(patches, "characters");
            skinCharactersFieldResolved = true;
            Object characters = skinCharactersField.get(null);
            return characters instanceof Object[] ? (Object[])characters : null;
        } catch (Exception e) {
            skinCharactersFieldResolved = true;
            skinCharactersField = null;
            debug("Unable to read reskinContent.patches.CharacterSelectScreenPatches.characters: " + e.getClass().getName() + ": " + e.getMessage());
            return null;
        }
    }

    private static void logAnimationLoad(AbstractCreature instance, String atlasUrl, String skeletonUrl, float scale, String redirectedAtlas, String redirectedSkeleton) {
        // The message below probes the filesystem four times; none of that may run with logs off.
        if (!DEBUG_LOGS) {
            return;
        }
        if (!isRelevantDownfallPath(atlasUrl) && !isRelevantDownfallPath(skeletonUrl) && redirectedAtlas == atlasUrl && redirectedSkeleton == skeletonUrl) {
            return;
        }
        String key = instance.getClass().getName() + "|" + atlasUrl + "|" + skeletonUrl;
        debugUnique(LOGGED_ANIMATION_PATHS, key, "AbstractCreature.loadAnimation request: class=" + instance.getClass().getName() + ", scale=" + scale + ", atlas=" + atlasUrl + ", json=" + skeletonUrl + ", redirectedAtlas=" + redirectedAtlas + ", redirectedJson=" + redirectedSkeleton + ", atlasExists=" + exists(atlasUrl) + ", jsonExists=" + exists(skeletonUrl) + ", redirectedAtlasExists=" + exists(redirectedAtlas) + ", redirectedJsonExists=" + exists(redirectedSkeleton));
    }

    private static void logSkinObject(String context, Object skin) {
        if (!DEBUG_LOGS || skin == null) {
            return;
        }
        String className = skin.getClass().getName();
        debug(context + ": skinClass=" + className + ", NAME=" + getFieldDebug(skin, "NAME") + ", portraitAtlasPath=" + getFieldDebug(skin, "portraitAtlasPath") + ", portraitAnimationType=" + getFieldDebug(skin, "portraitAnimationType") + ", atlasURL=" + getFieldDebug(skin, "atlasURL") + ", jsonURL=" + getFieldDebug(skin, "jsonURL") + ", SHOULDER1=" + getFieldDebug(skin, "SHOULDER1") + ", SHOULDER2=" + getFieldDebug(skin, "SHOULDER2") + ", CORPSE=" + getFieldDebug(skin, "CORPSE"));
    }

    private static void logSkinState(String context, boolean force) {
        // Runs from CharacterSelectScreen.update, i.e. every frame. Building the snapshot walks every
        // character and reflects roughly ten fields each, so it must not happen unless logging is on.
        if (!DEBUG_LOGS) {
            return;
        }
        String snapshot = buildSkinStateSnapshot();
        if (force || !snapshot.equals(lastSkinStateSnapshot)) {
            lastSkinStateSnapshot = snapshot;
            debug(context + ": active=" + activeFeminization + ", saveSlot=" + com.megacrit.cardcrawl.core.CardCrawlGame.saveSlot + ", " + snapshot);
        }
    }

    private static String buildSkinStateSnapshot() {
        Object[] characters = getSkinCharacters();
        if (characters == null) {
            return "characters=null";
        }
        StringBuilder builder = new StringBuilder("characters=");
        builder.append(characters.length).append(" [");
        for (int i = 0; i < characters.length; ++i) {
            Object character = characters[i];
            if (i > 0) {
                builder.append(" | ");
            }
            if (character == null) {
                builder.append(i).append(":null");
                continue;
            }
            builder.append(i).append(":charClass=").append(character.getClass().getName());
            builder.append(", id=").append(getFieldDebug(character, "id"));
            builder.append(", unlock=").append(getFieldDebug(character, "reskinUnlock"));
            int reskinCount = getIntFieldDebug(character, "reskinCount", -999);
            builder.append(", count=").append(reskinCount);
            Object skinsObject = getFieldValue(character, "skins");
            if (skinsObject instanceof Object[]) {
                Object[] skins = (Object[])skinsObject;
                builder.append(", skinCount=").append(skins.length);
                if (reskinCount >= 0 && reskinCount < skins.length && skins[reskinCount] != null) {
                    Object currentSkin = skins[reskinCount];
                    builder.append(", currentSkin=").append(currentSkin.getClass().getName());
                    builder.append(", currentName=").append(getFieldDebug(currentSkin, "NAME"));
                    builder.append(", atlasURL=").append(getFieldDebug(currentSkin, "atlasURL"));
                    builder.append(", jsonURL=").append(getFieldDebug(currentSkin, "jsonURL"));
                    builder.append(", portraitAtlasPath=").append(getFieldDebug(currentSkin, "portraitAtlasPath"));
                    builder.append(", shoulder1=").append(getFieldDebug(currentSkin, "SHOULDER1"));
                    builder.append(", shoulder2=").append(getFieldDebug(currentSkin, "SHOULDER2"));
                }
            } else {
                builder.append(", skins=").append(skinsObject);
            }
        }
        builder.append("]");
        return builder.toString();
    }

    private static boolean isRelevantDownfallPath(String path) {
        return path != null && (path.startsWith("slimeboundResources/") || path.startsWith("gremlinResources/") || path.startsWith("downfallFeminizationResources/") || isNpcAnimationPath(path) || isNpcOverridePath(path) || path.startsWith("reskinContent/img/Slimebound/") || path.startsWith("reskinContent/img/HexaghostMod/") || path.startsWith("reskinContent/img/SneckoMod/") || path.startsWith("reskinContent/img/ChampMod/") || path.startsWith("reskinContent/img/BronzeMod/") || path.startsWith("reskinContent/img/GuardianMod/"));
    }

    private static void debugUnique(Set<String> set, String key, String message) {
        // Arguments are evaluated eagerly by callers, some of them building strings with filesystem
        // probes. Bailing here is a safety net; call sites on warm paths additionally guard their own
        // argument construction with DEBUG_LOGS.
        if (!DEBUG_LOGS || !set.add(key)) {
            return;
        }
        debug(message);
    }

    private static void logDisabled(String context) {
        if (!loggedDisabled) {
            loggedDisabled = true;
            debug(context + " skipped because activeFeminization=false");
        }
    }

    private static void debug(String message) {
        if (DEBUG_LOGS) {
            logger.info("[DownfallFeminizationDebug] " + message);
        }
    }

    private static String getFieldDebug(Object owner, String fieldName) {
        Object value = getFieldValue(owner, fieldName);
        return String.valueOf(value);
    }

    private static int getIntFieldDebug(Object owner, String fieldName, int fallback) {
        if (owner == null) {
            return fallback;
        }
        try {
            return findField(owner.getClass(), fieldName).getInt(owner);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static boolean isSpawnedSlime(AbstractOrb orb) {
        // Runs for every orb on every frame of the fight; the answer is fixed per class.
        Class<?> orbClass = orb.getClass();
        Boolean cached = spawnedSlimeCache.get(orbClass);
        if (cached != null) {
            return cached.booleanValue();
        }

        boolean spawnedSlime = false;
        Class<?> current = orbClass;
        while (current != null) {
            if ("slimebound.orbs.SpawnedSlime".equals(current.getName())) {
                spawnedSlime = true;
                break;
            }
            current = current.getSuperclass();
        }

        synchronized (DownfallCompatibilityPatches.class) {
            Map<Class<?>, Boolean> updated = new IdentityHashMap<Class<?>, Boolean>(spawnedSlimeCache);
            updated.put(orbClass, Boolean.valueOf(spawnedSlime));
            spawnedSlimeCache = updated;
        }
        return spawnedSlime;
    }

    private static boolean getNoRender(AbstractOrb orb) {
        try {
            return findField(orb.getClass(), "noRender").getBoolean(orb);
        } catch (Exception e) {
            return false;
        }
    }

    private static void setNoRender(AbstractOrb orb, boolean noRender) {
        try {
            findField(orb.getClass(), "noRender").setBoolean(orb, noRender);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean isInCombatRoom() {
        AbstractRoom room = AbstractDungeon.getCurrRoom();
        return room != null && (room.phase == AbstractRoom.RoomPhase.COMBAT || room instanceof MonsterRoom);
    }

    private static boolean exists(String path) {
        try {
            return Gdx.files.internal(path).exists();
        } catch (Exception e) {
            return false;
        }
    }

    private static void disposeSneckoAtlas(String className) {
        try {
            Class<?> skinClass = Class.forName(className);
            disposeAtlas(findField(skinClass, "sneckoAtlas").get(null));
            findField(skinClass, "sneckoAtlas").set(null, null);
            findField(skinClass, "sneckoSkeleton").set(null, null);
            findField(skinClass, "sneckoState").set(null, null);
            findField(skinClass, "sneckoStateData").set(null, null);
            findField(skinClass, "sneckoData").set(null, null);
        } catch (Exception ignored) {
        }
    }

    private static Object getFieldValue(Object owner, String fieldName) {
        if (owner == null) {
            return null;
        }
        try {
            return findField(owner.getClass(), fieldName).get(owner);
        } catch (Exception e) {
            return null;
        }
    }

    private static void setFieldValue(Object owner, String fieldName, Object value) {
        if (owner == null) {
            return;
        }
        try {
            findField(owner.getClass(), fieldName).set(owner, value);
        } catch (Exception ignored) {
        }
    }

    private static String getStringField(Object owner, String fieldName) {
        Object value = getFieldValue(owner, fieldName);
        return value instanceof String ? (String)value : null;
    }

    private static String invokeStringMethod(Object owner, String methodName) {
        if (owner == null) {
            return null;
        }
        try {
            Method method = owner.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            Object value = method.invoke(owner);
            return value instanceof String ? (String)value : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean setFloatFieldIfDifferent(Object owner, String fieldName, float value) {
        if (owner == null) {
            return false;
        }
        try {
            Field field = findField(owner.getClass(), fieldName);
            if (Float.compare(field.getFloat(owner), value) == 0) {
                return false;
            }
            field.setFloat(owner, value);
            return true;
        } catch (Exception e) {
            debug("Failed to set float field " + fieldName + " on " + owner.getClass().getName() + ": " + e.getClass().getName() + ": " + e.getMessage());
            return false;
        }
    }

    private static void disposeAtlas(Object atlas) {
        if (atlas instanceof TextureAtlas) {
            ResourceUtil.disposeAtlas((TextureAtlas)atlas);
        }
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

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        // Walking the hierarchy costs a thrown NoSuchFieldException per superclass, and callers sit on
        // per-frame paths. Results (including misses) are cached; the class/field pairs are fixed at
        // runtime, so the map stays small and never needs invalidation.
        String key = type.getName() + '#' + name;
        Map<String, Field> cache = fieldCache;
        Field cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        if (cache.containsKey(key)) {
            // A recorded miss; the second lookup only happens once per key.
            throw new NoSuchFieldException(name);
        }

        Field found = null;
        Class<?> current = type;
        while (current != null) {
            try {
                found = current.getDeclaredField(name);
                found.setAccessible(true);
                break;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }

        synchronized (DownfallCompatibilityPatches.class) {
            Map<String, Field> updated = new HashMap<String, Field>(fieldCache);
            updated.put(key, found);
            fieldCache = updated;
        }

        if (found == null) {
            throw new NoSuchFieldException(name);
        }
        return found;
    }

    @SuppressWarnings("unchecked")
    private static <T> T getStaticField(Class<?> type, String name) throws Exception {
        return (T)findField(type, name).get(null);
    }
}
