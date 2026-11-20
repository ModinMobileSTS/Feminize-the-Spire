package feminization.compat;

import basemod.BaseMod;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import feminization.utils.FeminizationUtil;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Registers this mod's character skins with Together in Spire's bundled Skindex API.
 *
 * <p>The API is deliberately accessed through reflection. Together in Spire remains optional,
 * and an installation without it never attempts to load one of its classes.</p>
 */
public final class TogetherInSpireSkindexCompatibility {
    private static final String TOGETHER_IN_SPIRE_MOD_ID = "spireTogether";
    private static final String REGISTRY_CLASS = "skindex.registering.SkindexRegistry";
    private static final String SUBSCRIBER_CLASS = "skindex.registering.ISkindexSubscriber";
    private static final String PLAYER_REGISTRANT_CLASS = "skindex.registering.SkindexPlayerSkinRegistrant";
    private static final String PLAYER_SKIN_CLASS = "skindex.skins.player.PlayerSkin";
    private static final String ATLAS_SKIN_DATA_CLASS = "skindex.skins.player.PlayerAtlasSkinData";
    private static final String ATLAS_SKIN_CLASS = "skindex.skins.player.PlayerAtlasSkin";
    private static final String WATCHER_ATLAS_SKIN_DATA_CLASS = "skindex.skins.player.watcher.WatcherAtlasSkinData";
    private static final String TOGETHER_IN_SPIRE_MAIN_CLASS = "spireTogether.SpireTogetherMod";
    private static final String VANILLA_WATCHER_EYE_DIRECTORY = "images/characters/watcher/eye_anim/";
    private static final String VANILLA_WATCHER_EYE_ATLAS = "images/characters/watcher/eye_anim/skeleton.atlas";
    private static final String VANILLA_WATCHER_EYE_SKELETON = "images/characters/watcher/eye_anim/skeleton.json";
    private static final Logger logger = LogManager.getLogger(TogetherInSpireSkindexCompatibility.class.getName());

    private static final SkinDefinition[] SKINS = {
        new SkinDefinition(
            "FeminizeTheSpire:ironclad_feminized",
            "Feminized Ironclad",
            "IRONCLAD",
            1,
            "",
            "feminizationResources/images/characters/ironclad-2/idle/",
            "feminizationResources/images/characters/ironclad-2/idle/skeleton.atlas",
            "feminizationResources/images/characters/ironclad-2/idle/skeleton.json",
            "feminizationResources/images/characters/ironclad-2/shoulder.png",
            "feminizationResources/images/characters/ironclad-2/shoulder2.png",
            "feminizationResources/images/characters/ironclad-2/corpse.png",
            0.6f,
            false,
            null,
            null,
            null
        ),
        new SkinDefinition(
            "FeminizeTheSpire:silent_feminized",
            "Feminized Silent",
            "THE_SILENT",
            1,
            "",
            "feminizationResources/images/characters/theSilent-2/idle/",
            "feminizationResources/images/characters/theSilent-2/idle/hunter2.atlas",
            "feminizationResources/images/characters/theSilent-2/idle/hunter2.json",
            "feminizationResources/images/characters/theSilent-2/shoulder.png",
            "feminizationResources/images/characters/theSilent-2/shoulder2.png",
            "feminizationResources/images/characters/theSilent-2/corpse.png",
            0.9f,
            false,
            null,
            null,
            null
        ),
        new SkinDefinition(
            "FeminizeTheSpire:defect_feminized",
            "Feminized Defect",
            "DEFECT",
            1,
            "",
            "feminizationResources/images/characters/defect-2/idle/",
            "feminizationResources/images/characters/defect-2/idle/skeleton.atlas",
            "feminizationResources/images/characters/defect-2/idle/skeleton.json",
            "feminizationResources/images/characters/defect-2/shoulder.png",
            "feminizationResources/images/characters/defect-2/shoulder2.png",
            "feminizationResources/images/characters/defect-2/corpse.png",
            0.6f,
            false,
            null,
            null,
            null
        ),
        new SkinDefinition(
            "FeminizeTheSpire:watcher_feminized",
            "Feminized Watcher",
            "WATCHER",
            1,
            "",
            "feminizationResources/images/characters/watcher-2/idle/",
            "feminizationResources/images/characters/watcher-2/idle/skeleton.atlas",
            "feminizationResources/images/characters/watcher-2/idle/skeleton.json",
            "feminizationResources/images/characters/watcher-2/shoulder.png",
            "feminizationResources/images/characters/watcher-2/shoulder2.png",
            null,
            0.7f,
            true,
            "feminizationResources/images/characters/watcher-2/eye_anim/",
            "feminizationResources/images/characters/watcher-2/eye_anim/skeleton.atlas",
            "feminizationResources/images/characters/watcher-2/eye_anim/skeleton.json"
        ),
        new SkinDefinition(
            "FeminizeTheSpire:ironclad_lily",
            "Lily",
            "IRONCLAD",
            2,
            "ironclad_lily",
            "extraFeminizationResources/images/characters/ironclad/idle/",
            "extraFeminizationResources/images/characters/ironclad/idle/lily.atlas",
            "extraFeminizationResources/images/characters/ironclad/idle/lily.json",
            null,
            null,
            "extraFeminizationResources/images/characters/ironclad/corpse.png",
            0.6f,
            false,
            null,
            null,
            null
        ),
        new SkinDefinition(
            "FeminizeTheSpire:silent_jack",
            "Jack",
            "THE_SILENT",
            2,
            "",
            "extraFeminizationResources/images/characters/theSilent/idle/",
            "extraFeminizationResources/images/characters/theSilent/idle/jake2.atlas",
            "extraFeminizationResources/images/characters/theSilent/idle/jake2.json",
            "extraFeminizationResources/images/characters/theSilent/shoulder.png",
            "extraFeminizationResources/images/characters/theSilent/shoulder2.png",
            "extraFeminizationResources/images/characters/theSilent/corpse.png",
            0.9f,
            false,
            null,
            null,
            null
        ),
        new SkinDefinition(
            "FeminizeTheSpire:silent_hornet",
            "Hornet",
            "THE_SILENT",
            3,
            "",
            "extraFeminizationResources/images/characters/theSilent2/idle/",
            "extraFeminizationResources/images/characters/theSilent2/idle/hornet2.atlas",
            "extraFeminizationResources/images/characters/theSilent2/idle/hornet2.json",
            null,
            null,
            null,
            0.9f,
            false,
            null,
            null,
            null
        ),
        new SkinDefinition(
            "FeminizeTheSpire:silent_hornet_2",
            "Hornet 2",
            "THE_SILENT",
            4,
            "",
            "extraFeminizationResources/images/characters/theSilent3/idle/",
            "extraFeminizationResources/images/characters/theSilent3/idle/turehornet.atlas",
            "extraFeminizationResources/images/characters/theSilent3/idle/turehornet.json",
            null,
            null,
            null,
            0.9f,
            false,
            null,
            null,
            null
        ),
        new SkinDefinition(
            "FeminizeTheSpire:defect_alice",
            "Alice",
            "DEFECT",
            2,
            "",
            "extraFeminizationResources/images/characters/defect/idle/",
            "extraFeminizationResources/images/characters/defect/idle/STSaliceS.atlas",
            "extraFeminizationResources/images/characters/defect/idle/STSaliceS.json",
            null,
            null,
            "extraFeminizationResources/images/characters/defect/dead.png",
            0.6f,
            false,
            null,
            null,
            null
        ),
        new SkinDefinition(
            "FeminizeTheSpire:watcher_shulker",
            "Shulker",
            "WATCHER",
            2,
            "",
            "extraFeminizationResources/images/characters/watcher/idle/",
            "extraFeminizationResources/images/characters/watcher/idle/qyb.atlas",
            "extraFeminizationResources/images/characters/watcher/idle/qyb.json",
            "extraFeminizationResources/images/characters/watcher/shoulder.png",
            "extraFeminizationResources/images/characters/watcher/shoulder2.png",
            "extraFeminizationResources/images/characters/watcher/corpse.png",
            0.7f,
            true,
            VANILLA_WATCHER_EYE_DIRECTORY,
            VANILLA_WATCHER_EYE_ATLAS,
            VANILLA_WATCHER_EYE_SKELETON
        ),
        new SkinDefinition(
            "FeminizeTheSpire:watcher_succubus_nun",
            "Succubus Nun",
            "WATCHER",
            3,
            "watcher_succubus_nun",
            "extraFeminizationResources/images/characters/watcher2/idle/",
            "extraFeminizationResources/images/characters/watcher2/idle/nun.atlas",
            "extraFeminizationResources/images/characters/watcher2/idle/nun.json",
            "extraFeminizationResources/images/characters/watcher2/shoulder.png",
            "extraFeminizationResources/images/characters/watcher2/shoulder2.png",
            "extraFeminizationResources/images/characters/watcher2/corpse.png",
            0.7f,
            true,
            VANILLA_WATCHER_EYE_DIRECTORY,
            VANILLA_WATCHER_EYE_ATLAS,
            VANILLA_WATCHER_EYE_SKELETON
        )
    };

    private static Object registrant;
    private static List<Object> playerSkins;
    private static boolean registrationFailureLogged;
    private static boolean runtimeFailureLogged;
    private static ClassLoader skindexClassLoader;

    private TogetherInSpireSkindexCompatibility() {
    }

    /**
     * This runs before Skindex processes subscriptions at the end of BaseMod post-initialization.
     */
    @SpirePatch2(clz = BaseMod.class, method = "publishPostInitialize", requiredModId = TOGETHER_IN_SPIRE_MOD_ID, optional = true)
    public static class RegisterSkinsBeforeSkindexRegistryPatch {
        @SpirePrefixPatch
        public static void prefix() {
            registerSkins();
        }
    }

    /**
     * Skindex applies its queued skin during BaseMod's start-game callback. Reapply the native
     * Feminize the Spire selection after all subscribers have run, so the normal character
     * selector remains authoritative in a solo run.
     */
    @SpirePatch2(clz = BaseMod.class, method = "publishStartGame", requiredModId = TOGETHER_IN_SPIRE_MOD_ID, optional = true)
    public static class RestoreSoloSkinAfterSkindexPatch {
        @SpirePostfixPatch
        public static void postfix() {
            restoreSkinAfterStartGame();
        }
    }

    /** Keeps special animation IDs in sync when the Skindex wardrobe switches a local skin. */
    @SpirePatch2(cls = PLAYER_SKIN_CLASS, method = "loadOnPlayer", requiredModId = TOGETHER_IN_SPIRE_MOD_ID, optional = true)
    public static class SynchronizeSkinStatePatch {
        @SpirePostfixPatch
        public static void postfix(Object __instance, boolean __result) {
            if (__result) {
                synchronizeSkinState(__instance);
            }
        }
    }

    /** Applies the extra attack-animation mix rules after an atlas skin has reloaded the body. */
    @SpirePatch2(cls = ATLAS_SKIN_CLASS, method = "loadOnPlayer", requiredModId = TOGETHER_IN_SPIRE_MOD_ID, optional = true)
    public static class ConfigureAtlasSkinPatch {
        @SpirePostfixPatch
        public static void postfix(Object __instance, boolean __result) {
            if (__result) {
                configureSpecialAnimations(__instance);
            }
        }
    }

    public static synchronized void registerSkins() {
        if (registrant != null) {
            return;
        }

        try {
            Class<?> registryClass = Class.forName(REGISTRY_CLASS);
            ClassLoader classLoader = registryClass.getClassLoader();
            Class<?> subscriberClass = Class.forName(SUBSCRIBER_CLASS, false, classLoader);
            Class<?> playerRegistrantClass = Class.forName(PLAYER_REGISTRANT_CLASS, false, classLoader);
            Object proxy = Proxy.newProxyInstance(
                classLoader,
                new Class<?>[] {playerRegistrantClass},
                new PlayerSkinRegistrantHandler()
            );
            Method subscribe = registryClass.getMethod("subscribe", subscriberClass);
            subscribe.invoke(null, proxy);
            skindexClassLoader = classLoader;
            registrant = proxy;
        } catch (ClassNotFoundException ignored) {
            // Together in Spire is not installed or has no compatible Skindex implementation.
        } catch (Exception | LinkageError exception) {
            logRegistrationFailure(exception);
        }
    }

    private static synchronized List<Object> getPlayerSkins() {
        if (playerSkins != null) {
            return playerSkins;
        }

        try {
            Class<?> atlasSkinDataClass = Class.forName(ATLAS_SKIN_DATA_CLASS, true, skindexClassLoader);
            List<Object> skins = new ArrayList<Object>(SKINS.length);
            for (SkinDefinition definition : SKINS) {
                skins.add(createPlayerSkin(definition, atlasSkinDataClass));
            }
            playerSkins = Collections.unmodifiableList(skins);
        } catch (Exception | LinkageError exception) {
            logRegistrationFailure(exception);
            playerSkins = Collections.emptyList();
        }
        return playerSkins;
    }

    private static Object createPlayerSkin(SkinDefinition definition, Class<?> atlasSkinDataClass) throws Exception {
        Class<?> dataClass = atlasSkinDataClass;
        if (definition.watcher) {
            try {
                dataClass = Class.forName(WATCHER_ATLAS_SKIN_DATA_CLASS, true, skindexClassLoader);
            } catch (ClassNotFoundException ignored) {
                // Older Together in Spire versions can still render the body with the generic atlas skin.
            }
        }

        Object data = dataClass.newInstance();
        setRequiredField(data, "id", definition.id);
        setRequiredField(data, "name", definition.name);
        setRequiredField(data, "unlockMethod", "FREE");
        setRequiredField(data, "playerClass", definition.playerClass);
        setRequiredField(data, "resourceDirectoryUrl", definition.resourceDirectory);
        setRequiredField(data, "scale", Float.valueOf(1.0f));
        setRequiredField(data, "atlasUrl", definition.atlas);
        setRequiredField(data, "skeletonUrl", definition.skeleton);
        setRequiredField(data, "defaultAnimName", "Idle");
        setRequiredField(data, "defaultTimeScale", Float.valueOf(definition.timeScale));
        setOptionalField(data, "shoulderIMG", definition.shoulder);
        setOptionalField(data, "shoulder2IMG", definition.shoulder2);
        setOptionalField(data, "corpseIMG", definition.corpse);
        setOptionalField(data, "eyeResourceDirectoryUrl", definition.eyeResourceDirectory);
        setOptionalField(data, "eyeAtlasUrl", definition.eyeAtlas);
        setOptionalField(data, "eyeSkeletonUrl", definition.eyeSkeleton);
        return dataClass.getMethod("createPlayerSkin").invoke(data);
    }

    private static void restoreSkinAfterStartGame() {
        if (!CardCrawlGame.isInARun() || AbstractDungeon.player == null) {
            return;
        }

        try {
            Boolean multiplayer = isMultiplayerGame();
            // Do not make a local decision if Together in Spire cannot report its connection state.
            if (!Boolean.FALSE.equals(multiplayer)) {
                return;
            }
            if (AbstractDungeon.player.chosenClass == null
                || !isSupportedNativePlayerClass(AbstractDungeon.player.chosenClass.name())) {
                return;
            }

            SkinDefinition selectedDefinition = getSelectedSoloSkin();
            Object selectedSkin = selectedDefinition == null
                ? getDefaultSkinForCurrentPlayer()
                : getRegisteredSkin(selectedDefinition);
            invokeLoadOnPlayer(selectedSkin);
        } catch (Exception | LinkageError exception) {
            logRuntimeFailure(exception);
        }
    }

    private static Object getDefaultSkinForCurrentPlayer() throws Exception {
        Class<?> registryClass = Class.forName(REGISTRY_CLASS, true, skindexClassLoader);
        Method getDefaultSkin = registryClass.getMethod(
            "getDefaultPlayerSkinByClass",
            AbstractDungeon.player.chosenClass.getClass(),
            Boolean.TYPE
        );
        return getDefaultSkin.invoke(null, AbstractDungeon.player.chosenClass, Boolean.FALSE);
    }

    private static Boolean isMultiplayerGame() {
        try {
            Class<?> togetherInSpireClass = Class.forName(TOGETHER_IN_SPIRE_MAIN_CLASS, true, skindexClassLoader);
            Object connected = togetherInSpireClass.getMethod("isConnectedToGame").invoke(null);
            return connected instanceof Boolean ? (Boolean)connected : null;
        } catch (Exception | LinkageError ignored) {
            return null;
        }
    }

    private static boolean isSupportedNativePlayerClass(String playerClass) {
        return "IRONCLAD".equals(playerClass)
            || "THE_SILENT".equals(playerClass)
            || "DEFECT".equals(playerClass)
            || "WATCHER".equals(playerClass);
    }

    private static SkinDefinition getSelectedSoloSkin() {
        if (AbstractDungeon.player == null || AbstractDungeon.player.chosenClass == null) {
            return null;
        }

        String playerClass = AbstractDungeon.player.chosenClass.name();
        int selected;
        if ("IRONCLAD".equals(playerClass)) {
            selected = FeminizationUtil.ironcladSelected;
        } else if ("THE_SILENT".equals(playerClass)) {
            selected = FeminizationUtil.silentSelected;
        } else if ("DEFECT".equals(playerClass)) {
            selected = FeminizationUtil.defectSelected;
        } else if ("WATCHER".equals(playerClass)) {
            selected = FeminizationUtil.watcherSelected;
        } else {
            return null;
        }

        for (SkinDefinition definition : SKINS) {
            if (definition.playerClass.equals(playerClass) && definition.nativeSelectionIndex == selected) {
                return definition;
            }
        }
        return null;
    }

    private static Object getRegisteredSkin(SkinDefinition definition) {
        if (definition == null) {
            return null;
        }

        List<Object> skins = getPlayerSkins();
        for (int i = 0; i < SKINS.length && i < skins.size(); ++i) {
            if (SKINS[i] == definition) {
                return skins.get(i);
            }
        }
        return null;
    }

    private static void invokeLoadOnPlayer(Object skin) throws Exception {
        if (skin != null) {
            skin.getClass().getMethod("loadOnPlayer").invoke(skin);
        }
    }

    private static void synchronizeSkinState(Object skin) {
        try {
            SkinDefinition definition = findDefinition(getSkinId(skin));
            String playerClass = AbstractDungeon.player == null || AbstractDungeon.player.chosenClass == null
                ? null
                : AbstractDungeon.player.chosenClass.name();
            if (definition != null) {
                synchronizeSelectedSkin(definition);
                playerClass = definition.playerClass;
            }
            synchronizeSpecialAnimationId(playerClass, definition == null ? "" : definition.runtimeSkinId);
        } catch (Exception | LinkageError exception) {
            logRuntimeFailure(exception);
        }
    }

    private static void synchronizeSelectedSkin(SkinDefinition definition) {
        boolean changed = false;
        if ("IRONCLAD".equals(definition.playerClass)) {
            changed = FeminizationUtil.ironcladSelected != definition.nativeSelectionIndex;
            FeminizationUtil.ironcladSelected = definition.nativeSelectionIndex;
        } else if ("THE_SILENT".equals(definition.playerClass)) {
            changed = FeminizationUtil.silentSelected != definition.nativeSelectionIndex;
            FeminizationUtil.silentSelected = definition.nativeSelectionIndex;
        } else if ("DEFECT".equals(definition.playerClass)) {
            changed = FeminizationUtil.defectSelected != definition.nativeSelectionIndex;
            FeminizationUtil.defectSelected = definition.nativeSelectionIndex;
        } else if ("WATCHER".equals(definition.playerClass)) {
            changed = FeminizationUtil.watcherSelected != definition.nativeSelectionIndex;
            FeminizationUtil.watcherSelected = definition.nativeSelectionIndex;
        }
        if (changed) {
            FeminizationUtil.saveSelectedSkins();
        }
    }

    private static void synchronizeSpecialAnimationId(String playerClass, String runtimeSkinId) {
        if ("IRONCLAD".equals(playerClass)) {
            FeminizationUtil.ironcladId = runtimeSkinId;
        } else if ("WATCHER".equals(playerClass)) {
            FeminizationUtil.watcherId = runtimeSkinId;
        }
    }

    private static void configureSpecialAnimations(Object skin) {
        try {
            SkinDefinition definition = findDefinition(getSkinId(skin));
            if (definition == null || AbstractDungeon.player == null || AbstractDungeon.player.state == null) {
                return;
            }

            if ("ironclad_lily".equals(definition.runtimeSkinId)) {
                AbstractDungeon.player.state.getData().setMix("attack1", "Idle", 0.2f);
                AbstractDungeon.player.state.getData().setMix("attack123", "Idle", 0.2f);
                AbstractDungeon.player.state.getData().setMix("attack_ranged", "Idle", 0.2f);
                AbstractDungeon.player.state.getData().setMix("attack_rush", "Idle", 0.2f);
                AbstractDungeon.player.state.getData().setMix("buff", "Idle", 0.2f);
            } else if ("watcher_succubus_nun".equals(definition.runtimeSkinId)) {
                AbstractDungeon.player.state.getData().setMix("attack_artillery", "Idle", 0.2f);
                AbstractDungeon.player.state.getData().setMix("attack_bandage", "Idle", 0.2f);
                AbstractDungeon.player.state.getData().setMix("attack_point", "Idle", 0.2f);
                AbstractDungeon.player.state.getData().setMix("attack_ranged", "Idle", 0.2f);
            }
        } catch (Exception | LinkageError exception) {
            logRuntimeFailure(exception);
        }
    }

    private static SkinDefinition findDefinition(String id) {
        if (id == null) {
            return null;
        }
        for (SkinDefinition definition : SKINS) {
            if (definition.id.equals(id)) {
                return definition;
            }
        }
        return null;
    }

    private static String getSkinId(Object skin) throws Exception {
        if (skin == null) {
            return null;
        }
        Object id = skin.getClass().getMethod("getId").invoke(skin);
        return id instanceof String ? (String)id : null;
    }

    private static void setRequiredField(Object target, String name, Object value) throws Exception {
        if (!setFields(target, name, value)) {
            throw new NoSuchFieldException(name);
        }
    }

    private static void setOptionalField(Object target, String name, Object value) throws Exception {
        if (value != null) {
            setFields(target, name, value);
        }
    }

    private static boolean setFields(Object target, String name, Object value) throws Exception {
        boolean found = false;
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(target, value);
                found = true;
            } catch (NoSuchFieldException ignored) {
                // The field can live on any superclass in Skindex's data object hierarchy.
            }
            type = type.getSuperclass();
        }
        return found;
    }

    private static void logRegistrationFailure(Throwable exception) {
        if (!registrationFailureLogged) {
            registrationFailureLogged = true;
            logger.warn("Unable to register Feminize the Spire skins with Together in Spire; multiplayer will use its normal skins.", exception);
        }
    }

    private static void logRuntimeFailure(Throwable exception) {
        if (!runtimeFailureLogged) {
            runtimeFailureLogged = true;
            logger.warn("Unable to apply the selected Feminize the Spire skin through Together in Spire; the normal skin will be used.", exception);
        }
    }

    private static final class PlayerSkinRegistrantHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] arguments) {
            String methodName = method.getName();
            if ("getDefaultPlayerSkinsToRegister".equals(methodName)) {
                return Collections.emptyList();
            }
            if ("getPlayerSkinsToRegister".equals(methodName)) {
                return getPlayerSkins();
            }
            if ("toString".equals(methodName)) {
                return "FeminizeTheSpireSkindexRegistrant";
            }
            if ("hashCode".equals(methodName)) {
                return Integer.valueOf(System.identityHashCode(proxy));
            }
            if ("equals".equals(methodName)) {
                return Boolean.valueOf(arguments != null && arguments.length == 1 && proxy == arguments[0]);
            }
            return null;
        }
    }

    private static final class SkinDefinition {
        private final String id;
        private final String name;
        private final String playerClass;
        private final int nativeSelectionIndex;
        private final String runtimeSkinId;
        private final String resourceDirectory;
        private final String atlas;
        private final String skeleton;
        private final String shoulder;
        private final String shoulder2;
        private final String corpse;
        private final float timeScale;
        private final boolean watcher;
        private final String eyeResourceDirectory;
        private final String eyeAtlas;
        private final String eyeSkeleton;

        private SkinDefinition(
            String id,
            String name,
            String playerClass,
            int nativeSelectionIndex,
            String runtimeSkinId,
            String resourceDirectory,
            String atlas,
            String skeleton,
            String shoulder,
            String shoulder2,
            String corpse,
            float timeScale,
            boolean watcher,
            String eyeResourceDirectory,
            String eyeAtlas,
            String eyeSkeleton
        ) {
            this.id = id;
            this.name = name;
            this.playerClass = playerClass;
            this.nativeSelectionIndex = nativeSelectionIndex;
            this.runtimeSkinId = runtimeSkinId;
            this.resourceDirectory = resourceDirectory;
            this.atlas = atlas;
            this.skeleton = skeleton;
            this.shoulder = shoulder;
            this.shoulder2 = shoulder2;
            this.corpse = corpse;
            this.timeScale = timeScale;
            this.watcher = watcher;
            this.eyeResourceDirectory = eyeResourceDirectory;
            this.eyeAtlas = eyeAtlas;
            this.eyeSkeleton = eyeSkeleton;
        }
    }
}
