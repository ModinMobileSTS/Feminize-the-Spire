/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  basemod.BaseMod
 *  basemod.IUIElement
 *  basemod.ModLabeledToggleButton
 *  basemod.ModPanel
 *  basemod.interfaces.EditCharactersSubscriber
 *  basemod.interfaces.EditStringsSubscriber
 *  basemod.interfaces.ISubscriber
 *  basemod.interfaces.PostInitializeSubscriber
 *  com.badlogic.gdx.Gdx
 *  com.badlogic.gdx.graphics.Texture
 *  com.evacipated.cardcrawl.modthespire.lib.SpireConfig
 *  com.evacipated.cardcrawl.modthespire.lib.SpireInitializer
 *  com.megacrit.cardcrawl.core.CardCrawlGame
 *  com.megacrit.cardcrawl.core.Settings
 *  com.megacrit.cardcrawl.helpers.FontHelper
 *  com.megacrit.cardcrawl.localization.UIStrings
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package feminization;

import basemod.BaseMod;
import basemod.IUIElement;
import basemod.ModLabeledToggleButton;
import basemod.ModPanel;
import basemod.interfaces.EditCharactersSubscriber;
import basemod.interfaces.EditStringsSubscriber;
import basemod.interfaces.ISubscriber;
import basemod.interfaces.PostInitializeSubscriber;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.localization.UIStrings;
import downfallFeminization.DownfallCompatibilityPatches;
import feminization.cardArt.CardArtFeminization;
import feminization.helper.FeminizationHelper;
import feminization.utils.FeminizationUtil;
import java.io.IOException;
import java.util.Properties;
import monsterFeminization.MonsterFeminizationMod;
import npcFeminization.NpcFeminizationMod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SpireInitializer
public class FeminizationMod
implements PostInitializeSubscriber,
EditCharactersSubscriber,
EditStringsSubscriber {
    public static final String MODNAME = "Feminization";
    private static final String AUTHOR = "anaer, hoykj";
    private static final String DESCRIPTION = "";
    private static final String ACTIVE_H_KEY = "activeH";
    private static final String ACTIVE_CHARACTER_NSFW_KEY = "activeCharacterNsfw";
    private static final String ACTIVE_NPC_NSFW_KEY = "activeNpcNsfw";
    private static final String ACTIVE_MONSTER_NSFW_KEY = "activeMonsterNsfw";
    private static final String ACTIVE_NPC_FEMINIZATION_KEY = "activeNpcFeminization";
    private static final String ACTIVE_MONSTER_FEMINIZATION_KEY = "activeMonsterFeminization";
    private static final String ACTIVE_DOWNFALL_FEMINIZATION_KEY = "activeDownfallFeminization";
    private static final String ACTIVE_OTHER_MOD_FEMINIZATION_KEY = "activeOtherModFeminization";
    private static final String ACTIVE_IRONCLAD_CARD_FEMINIZATION_KEY = "activeIroncladCardFeminization";
    private static final String ACTIVE_SILENT_CARD_FEMINIZATION_KEY = "activeSilentCardFeminization";
    private static final String ACTIVE_DEFECT_CARD_FEMINIZATION_KEY = "activeDefectCardFeminization";
    private static final String ACTIVE_WATCHER_CARD_FEMINIZATION_KEY = "activeWatcherCardFeminization";
    private static final String ACTIVE_CURSE_CARD_FEMINIZATION_KEY = "activeCurseCardFeminization";
    private static final String ACTIVE_COLORLESS_CARD_FEMINIZATION_KEY = "activeColorlessCardFeminization";
    public static final Logger logger = LogManager.getLogger((String)FeminizationMod.class.getName());
    public static boolean activeH;
    public static boolean activeOtherModFeminization;

    public static void initialize() {
        FeminizationMod feminizationMod = new FeminizationMod();
        try {
            Properties defaults = new Properties();
            defaults.setProperty(ACTIVE_H_KEY, "false");
            defaults.setProperty(ACTIVE_CHARACTER_NSFW_KEY, "false");
            defaults.setProperty(ACTIVE_NPC_NSFW_KEY, "false");
            defaults.setProperty(ACTIVE_MONSTER_NSFW_KEY, "false");
            defaults.setProperty(ACTIVE_NPC_FEMINIZATION_KEY, "true");
            defaults.setProperty(ACTIVE_MONSTER_FEMINIZATION_KEY, "true");
            defaults.setProperty(ACTIVE_DOWNFALL_FEMINIZATION_KEY, "true");
            defaults.setProperty(ACTIVE_OTHER_MOD_FEMINIZATION_KEY, "false");
            defaults.setProperty(ACTIVE_IRONCLAD_CARD_FEMINIZATION_KEY, "true");
            defaults.setProperty(ACTIVE_SILENT_CARD_FEMINIZATION_KEY, "true");
            defaults.setProperty(ACTIVE_DEFECT_CARD_FEMINIZATION_KEY, "true");
            defaults.setProperty(ACTIVE_WATCHER_CARD_FEMINIZATION_KEY, "true");
            defaults.setProperty(ACTIVE_CURSE_CARD_FEMINIZATION_KEY, "true");
            defaults.setProperty(ACTIVE_COLORLESS_CARD_FEMINIZATION_KEY, "true");
            defaults.setProperty("ironcladSelected", "1");
            defaults.setProperty("silentSelected", "1");
            defaults.setProperty("defectSelected", "1");
            defaults.setProperty("watcherSelected", "1");
            SpireConfig config = new SpireConfig(MODNAME, "Common", defaults);
            boolean legacyActiveH = config.getBool(ACTIVE_H_KEY);
            boolean migratedNsfwConfig = false;
            if (!config.has(ACTIVE_CHARACTER_NSFW_KEY)) {
                config.setBool(ACTIVE_CHARACTER_NSFW_KEY, legacyActiveH);
                migratedNsfwConfig = true;
            }
            if (!config.has(ACTIVE_NPC_NSFW_KEY)) {
                config.setBool(ACTIVE_NPC_NSFW_KEY, legacyActiveH);
                migratedNsfwConfig = true;
            }
            if (!config.has(ACTIVE_MONSTER_NSFW_KEY)) {
                config.setBool(ACTIVE_MONSTER_NSFW_KEY, legacyActiveH);
                migratedNsfwConfig = true;
            }
            if (!config.has(ACTIVE_DOWNFALL_FEMINIZATION_KEY)) {
                config.setBool(ACTIVE_DOWNFALL_FEMINIZATION_KEY, true);
                migratedNsfwConfig = true;
            }
            if (!config.has(ACTIVE_OTHER_MOD_FEMINIZATION_KEY)) {
                config.setBool(ACTIVE_OTHER_MOD_FEMINIZATION_KEY, false);
                migratedNsfwConfig = true;
            }
            if (!config.has(ACTIVE_IRONCLAD_CARD_FEMINIZATION_KEY)) {
                config.setBool(ACTIVE_IRONCLAD_CARD_FEMINIZATION_KEY, true);
                migratedNsfwConfig = true;
            }
            if (!config.has(ACTIVE_SILENT_CARD_FEMINIZATION_KEY)) {
                config.setBool(ACTIVE_SILENT_CARD_FEMINIZATION_KEY, true);
                migratedNsfwConfig = true;
            }
            if (!config.has(ACTIVE_DEFECT_CARD_FEMINIZATION_KEY)) {
                config.setBool(ACTIVE_DEFECT_CARD_FEMINIZATION_KEY, true);
                migratedNsfwConfig = true;
            }
            if (!config.has(ACTIVE_WATCHER_CARD_FEMINIZATION_KEY)) {
                config.setBool(ACTIVE_WATCHER_CARD_FEMINIZATION_KEY, true);
                migratedNsfwConfig = true;
            }
            if (!config.has(ACTIVE_CURSE_CARD_FEMINIZATION_KEY)) {
                config.setBool(ACTIVE_CURSE_CARD_FEMINIZATION_KEY, true);
                migratedNsfwConfig = true;
            }
            if (!config.has(ACTIVE_COLORLESS_CARD_FEMINIZATION_KEY)) {
                config.setBool(ACTIVE_COLORLESS_CARD_FEMINIZATION_KEY, true);
                migratedNsfwConfig = true;
            }
            activeH = config.getBool(ACTIVE_CHARACTER_NSFW_KEY);
            FeminizationUtil.ironcladSelected = config.getInt("ironcladSelected");
            FeminizationUtil.silentSelected = config.getInt("silentSelected");
            FeminizationUtil.defectSelected = config.getInt("defectSelected");
            FeminizationUtil.watcherSelected = config.getInt("watcherSelected");
            NpcFeminizationMod.activeH = config.getBool(ACTIVE_NPC_NSFW_KEY);
            MonsterFeminizationMod.activeH = config.getBool(ACTIVE_MONSTER_NSFW_KEY);
            NpcFeminizationMod.activeFeminization = config.getBool(ACTIVE_NPC_FEMINIZATION_KEY);
            MonsterFeminizationMod.activeFeminization = config.getBool(ACTIVE_MONSTER_FEMINIZATION_KEY);
            DownfallCompatibilityPatches.activeFeminization = config.getBool(ACTIVE_DOWNFALL_FEMINIZATION_KEY);
            activeOtherModFeminization = config.getBool(ACTIVE_OTHER_MOD_FEMINIZATION_KEY);
            CardArtFeminization.activeIroncladCardFeminization = config.getBool(ACTIVE_IRONCLAD_CARD_FEMINIZATION_KEY);
            CardArtFeminization.activeSilentCardFeminization = config.getBool(ACTIVE_SILENT_CARD_FEMINIZATION_KEY);
            CardArtFeminization.activeDefectCardFeminization = config.getBool(ACTIVE_DEFECT_CARD_FEMINIZATION_KEY);
            CardArtFeminization.activeWatcherCardFeminization = config.getBool(ACTIVE_WATCHER_CARD_FEMINIZATION_KEY);
            CardArtFeminization.activeCurseCardFeminization = config.getBool(ACTIVE_CURSE_CARD_FEMINIZATION_KEY);
            CardArtFeminization.activeColorlessCardFeminization = config.getBool(ACTIVE_COLORLESS_CARD_FEMINIZATION_KEY);
            if (migratedNsfwConfig) {
                config.save();
            }
        }
        catch (Exception e) {
            // SpireConfig parsing can throw NumberFormat/RuntimeException on a corrupt properties
            // file; a broken value must disable the feature instead of aborting mod bootstrap.
            e.printStackTrace();
        }
    }

    public FeminizationMod() {
        BaseMod.subscribe((ISubscriber)this);
    }

    public void receivePostInitialize() {
        CardArtFeminization.preloadCardAtlases();
        try {
            this.CreatePanel();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void CreatePanel() throws IOException {
        UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("Config_Feminization");
        String[] TEXT = uiStrings.TEXT;
        SpireConfig spireConfig = new SpireConfig(MODNAME, "Common");
        ModPanel settingsPanel = new ModPanel();
        ModLabeledToggleButton activeHToggle = new ModLabeledToggleButton(FeminizationMod.configText(TEXT, 0, "允许角色NSFW内容", "Enable character NSFW content"), 400.0f, 700.0f, Settings.CREAM_COLOR, FontHelper.charDescFont, FeminizationMod.activeH, settingsPanel, label -> {}, button -> {
            FeminizationMod.activeH = button.enabled;
            spireConfig.setBool(ACTIVE_H_KEY, FeminizationMod.activeH);
            spireConfig.setBool(ACTIVE_CHARACTER_NSFW_KEY, FeminizationMod.activeH);
            CardCrawlGame.mainMenuScreen.optionPanel.effects.clear();
            try {
                spireConfig.save();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        });
        settingsPanel.addUIElement((IUIElement)activeHToggle);
        ModLabeledToggleButton npcFeminizationToggle = new ModLabeledToggleButton(FeminizationMod.configText(TEXT, 1, "启用NPC娘化", "Enable NPC feminization"), 400.0f, 650.0f, Settings.CREAM_COLOR, FontHelper.charDescFont, NpcFeminizationMod.activeFeminization, settingsPanel, label -> {}, button -> {
            NpcFeminizationMod.activeFeminization = button.enabled;
            spireConfig.setBool(ACTIVE_NPC_FEMINIZATION_KEY, NpcFeminizationMod.activeFeminization);
            CardCrawlGame.mainMenuScreen.optionPanel.effects.clear();
            try {
                spireConfig.save();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        });
        settingsPanel.addUIElement((IUIElement)npcFeminizationToggle);
        ModLabeledToggleButton npcNsfwToggle = new ModLabeledToggleButton(FeminizationMod.configText(TEXT, 2, "允许NPC NSFW内容", "Enable NPC NSFW content"), 400.0f, 600.0f, Settings.CREAM_COLOR, FontHelper.charDescFont, NpcFeminizationMod.activeH, settingsPanel, label -> {}, button -> {
            NpcFeminizationMod.activeH = button.enabled;
            spireConfig.setBool(ACTIVE_NPC_NSFW_KEY, NpcFeminizationMod.activeH);
            CardCrawlGame.mainMenuScreen.optionPanel.effects.clear();
            try {
                spireConfig.save();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        });
        settingsPanel.addUIElement((IUIElement)npcNsfwToggle);
        ModLabeledToggleButton monsterFeminizationToggle = new ModLabeledToggleButton(FeminizationMod.configText(TEXT, 3, "启用怪物娘化", "Enable monster feminization"), 400.0f, 550.0f, Settings.CREAM_COLOR, FontHelper.charDescFont, MonsterFeminizationMod.activeFeminization, settingsPanel, label -> {}, button -> {
            MonsterFeminizationMod.activeFeminization = button.enabled;
            spireConfig.setBool(ACTIVE_MONSTER_FEMINIZATION_KEY, MonsterFeminizationMod.activeFeminization);
            CardCrawlGame.mainMenuScreen.optionPanel.effects.clear();
            try {
                spireConfig.save();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        });
        settingsPanel.addUIElement((IUIElement)monsterFeminizationToggle);
        ModLabeledToggleButton monsterNsfwToggle = new ModLabeledToggleButton(FeminizationMod.configText(TEXT, 4, "允许怪物NSFW内容", "Enable monster NSFW content"), 400.0f, 500.0f, Settings.CREAM_COLOR, FontHelper.charDescFont, MonsterFeminizationMod.activeH, settingsPanel, label -> {}, button -> {
            MonsterFeminizationMod.activeH = button.enabled;
            spireConfig.setBool(ACTIVE_MONSTER_NSFW_KEY, MonsterFeminizationMod.activeH);
            CardCrawlGame.mainMenuScreen.optionPanel.effects.clear();
            try {
                spireConfig.save();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        });
        settingsPanel.addUIElement((IUIElement)monsterNsfwToggle);
        ModLabeledToggleButton downfallFeminizationToggle = new ModLabeledToggleButton(FeminizationMod.configText(TEXT, 5, "启用崩坠娘化", "Enable Downfall feminization"), 400.0f, 450.0f, Settings.CREAM_COLOR, FontHelper.charDescFont, DownfallCompatibilityPatches.activeFeminization, settingsPanel, label -> {}, button -> {
            DownfallCompatibilityPatches.activeFeminization = button.enabled;
            spireConfig.setBool(ACTIVE_DOWNFALL_FEMINIZATION_KEY, DownfallCompatibilityPatches.activeFeminization);
            CardCrawlGame.mainMenuScreen.optionPanel.effects.clear();
            try {
                spireConfig.save();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        });
        settingsPanel.addUIElement((IUIElement)downfallFeminizationToggle);
        ModLabeledToggleButton otherModFeminizationToggle = new ModLabeledToggleButton(FeminizationMod.configText(TEXT, 6, "尝试娘化其他类似模组", "Try to feminize compatible cards and characters from other mods"), 400.0f, 400.0f, Settings.CREAM_COLOR, FontHelper.charDescFont, activeOtherModFeminization, settingsPanel, label -> {}, button -> {
            activeOtherModFeminization = button.enabled;
            spireConfig.setBool(ACTIVE_OTHER_MOD_FEMINIZATION_KEY, activeOtherModFeminization);
            CardCrawlGame.mainMenuScreen.optionPanel.effects.clear();
            try {
                spireConfig.save();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        });
        settingsPanel.addUIElement((IUIElement)otherModFeminizationToggle);
        ModLabeledToggleButton ironcladCardFeminizationToggle = new ModLabeledToggleButton(FeminizationMod.configText(TEXT, 7, "启用铁甲战士卡图娘化", "Enable Ironclad card art feminization"), 400.0f, 350.0f, Settings.CREAM_COLOR, FontHelper.charDescFont, CardArtFeminization.activeIroncladCardFeminization, settingsPanel, label -> {}, button -> {
            CardArtFeminization.activeIroncladCardFeminization = button.enabled;
            CardArtFeminization.onSettingsChanged();
            spireConfig.setBool(ACTIVE_IRONCLAD_CARD_FEMINIZATION_KEY, CardArtFeminization.activeIroncladCardFeminization);
            CardCrawlGame.mainMenuScreen.optionPanel.effects.clear();
            try {
                spireConfig.save();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        });
        settingsPanel.addUIElement((IUIElement)ironcladCardFeminizationToggle);
        ModLabeledToggleButton silentCardFeminizationToggle = new ModLabeledToggleButton(FeminizationMod.configText(TEXT, 8, "启用静默猎手卡图娘化", "Enable Silent card art feminization"), 400.0f, 300.0f, Settings.CREAM_COLOR, FontHelper.charDescFont, CardArtFeminization.activeSilentCardFeminization, settingsPanel, label -> {}, button -> {
            CardArtFeminization.activeSilentCardFeminization = button.enabled;
            CardArtFeminization.onSettingsChanged();
            spireConfig.setBool(ACTIVE_SILENT_CARD_FEMINIZATION_KEY, CardArtFeminization.activeSilentCardFeminization);
            CardCrawlGame.mainMenuScreen.optionPanel.effects.clear();
            try {
                spireConfig.save();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        });
        settingsPanel.addUIElement((IUIElement)silentCardFeminizationToggle);
        ModLabeledToggleButton defectCardFeminizationToggle = new ModLabeledToggleButton(FeminizationMod.configText(TEXT, 9, "启用故障机器人卡图娘化", "Enable Defect card art feminization"), 400.0f, 250.0f, Settings.CREAM_COLOR, FontHelper.charDescFont, CardArtFeminization.activeDefectCardFeminization, settingsPanel, label -> {}, button -> {
            CardArtFeminization.activeDefectCardFeminization = button.enabled;
            CardArtFeminization.onSettingsChanged();
            spireConfig.setBool(ACTIVE_DEFECT_CARD_FEMINIZATION_KEY, CardArtFeminization.activeDefectCardFeminization);
            CardCrawlGame.mainMenuScreen.optionPanel.effects.clear();
            try {
                spireConfig.save();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        });
        settingsPanel.addUIElement((IUIElement)defectCardFeminizationToggle);
        ModLabeledToggleButton watcherCardFeminizationToggle = new ModLabeledToggleButton(FeminizationMod.configText(TEXT, 10, "启用观者卡图娘化", "Enable Watcher card art feminization"), 400.0f, 200.0f, Settings.CREAM_COLOR, FontHelper.charDescFont, CardArtFeminization.activeWatcherCardFeminization, settingsPanel, label -> {}, button -> {
            CardArtFeminization.activeWatcherCardFeminization = button.enabled;
            CardArtFeminization.onSettingsChanged();
            spireConfig.setBool(ACTIVE_WATCHER_CARD_FEMINIZATION_KEY, CardArtFeminization.activeWatcherCardFeminization);
            CardCrawlGame.mainMenuScreen.optionPanel.effects.clear();
            try {
                spireConfig.save();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        });
        settingsPanel.addUIElement((IUIElement)watcherCardFeminizationToggle);
        ModLabeledToggleButton curseCardFeminizationToggle = new ModLabeledToggleButton(FeminizationMod.configText(TEXT, 11, "启用诅咒牌卡图娘化", "Enable curse card art feminization"), 400.0f, 150.0f, Settings.CREAM_COLOR, FontHelper.charDescFont, CardArtFeminization.activeCurseCardFeminization, settingsPanel, label -> {}, button -> {
            CardArtFeminization.activeCurseCardFeminization = button.enabled;
            CardArtFeminization.onSettingsChanged();
            spireConfig.setBool(ACTIVE_CURSE_CARD_FEMINIZATION_KEY, CardArtFeminization.activeCurseCardFeminization);
            CardCrawlGame.mainMenuScreen.optionPanel.effects.clear();
            try {
                spireConfig.save();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        });
        settingsPanel.addUIElement((IUIElement)curseCardFeminizationToggle);
        ModLabeledToggleButton colorlessCardFeminizationToggle = new ModLabeledToggleButton(FeminizationMod.configText(TEXT, 12, "启用无色牌卡图娘化", "Enable colorless card art feminization"), 400.0f, 100.0f, Settings.CREAM_COLOR, FontHelper.charDescFont, CardArtFeminization.activeColorlessCardFeminization, settingsPanel, label -> {}, button -> {
            CardArtFeminization.activeColorlessCardFeminization = button.enabled;
            CardArtFeminization.onSettingsChanged();
            spireConfig.setBool(ACTIVE_COLORLESS_CARD_FEMINIZATION_KEY, CardArtFeminization.activeColorlessCardFeminization);
            CardCrawlGame.mainMenuScreen.optionPanel.effects.clear();
            try {
                spireConfig.save();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        });
        settingsPanel.addUIElement((IUIElement)colorlessCardFeminizationToggle);
        Texture badgeTexture = new Texture(Gdx.files.internal("feminizationResources/images/feminization.png"));
        BaseMod.registerModBadge((Texture)badgeTexture, (String)MODNAME, (String)AUTHOR, (String)DESCRIPTION, (ModPanel)settingsPanel);
    }

    private static String configText(String[] text, int index, String zhsFallback, String engFallback) {
        if (text != null && text.length > index) {
            return text[index];
        }
        return Settings.language == Settings.GameLanguage.ZHS || Settings.language == Settings.GameLanguage.ZHT ? zhsFallback : engFallback;
    }

    public void receiveEditStrings() {
        FeminizationHelper.addStrings();
    }

    public void receiveEditCharacters() {
        FeminizationHelper.addCharacterSkins();
        FeminizationUtil.normalizeSelectedSkins();
    }
}
