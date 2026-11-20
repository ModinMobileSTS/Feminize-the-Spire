/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  basemod.BaseMod
 *  basemod.IUIElement
 *  basemod.ModLabeledToggleButton
 *  basemod.ModPanel
 *  basemod.interfaces.EditStringsSubscriber
 *  basemod.interfaces.ISubscriber
 *  basemod.interfaces.PostInitializeSubscriber
 *  com.badlogic.gdx.Gdx
 *  com.badlogic.gdx.graphics.Texture
 *  com.evacipated.cardcrawl.modthespire.lib.SpireConfig
 *  com.evacipated.cardcrawl.modthespire.lib.SpireInitializer
 *  com.megacrit.cardcrawl.core.CardCrawlGame
 *  com.megacrit.cardcrawl.core.Settings
 *  com.megacrit.cardcrawl.core.Settings$GameLanguage
 *  com.megacrit.cardcrawl.helpers.FontHelper
 *  com.megacrit.cardcrawl.localization.UIStrings
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package npcFeminization;

import basemod.BaseMod;
import basemod.IUIElement;
import basemod.ModLabeledToggleButton;
import basemod.ModPanel;
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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SpireInitializer
public class NpcFeminizationMod
implements PostInitializeSubscriber,
EditStringsSubscriber {
    private static final String MODNAME = "Npc Feminization";
    private static final String AUTHOR = "anaer, hoykj";
    private static final String DESCRIPTION = "";
    private static final String ACTIVE_H_KEY = "activeH";
    private static final String ACTIVE_NPC_NSFW_KEY = "activeNpcNsfw";
    private static final String ACTIVE_NPC_FEMINIZATION_KEY = "activeNpcFeminization";
    public static final Logger logger = LogManager.getLogger((String)NpcFeminizationMod.class.getName());
    public static boolean activeH;
    public static boolean activeFeminization = true;

    public static void initialize() {
        NpcFeminizationMod npcFeminizationMod = new NpcFeminizationMod();
        try {
            Properties defaults = new Properties();
            defaults.setProperty(ACTIVE_H_KEY, "false");
            defaults.setProperty(ACTIVE_NPC_NSFW_KEY, "false");
            defaults.setProperty(ACTIVE_NPC_FEMINIZATION_KEY, "true");
            SpireConfig config = new SpireConfig("Feminization", "Common", defaults);
            if (!config.has(ACTIVE_NPC_NSFW_KEY)) {
                config.setBool(ACTIVE_NPC_NSFW_KEY, config.getBool(ACTIVE_H_KEY));
                config.save();
            }
            activeH = config.getBool(ACTIVE_NPC_NSFW_KEY);
            activeFeminization = config.getBool(ACTIVE_NPC_FEMINIZATION_KEY);
        }
        catch (Exception e) {
            // SpireConfig parsing can throw NumberFormat/RuntimeException on a corrupt properties
            // file; a broken value must disable the feature instead of aborting mod bootstrap.
            e.printStackTrace();
        }
    }

    public NpcFeminizationMod() {
        BaseMod.subscribe((ISubscriber)this);
    }

    public void receivePostInitialize() {
    }

    private void CreatePanel() throws IOException {
        UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("Main_Feminization");
        String[] TEXT = uiStrings.TEXT;
        SpireConfig spireConfig = new SpireConfig("Feminization", "Common");
        ModPanel settingsPanel = new ModPanel();
        ModLabeledToggleButton activeHToggle = new ModLabeledToggleButton(TEXT[0], 400.0f, 700.0f, Settings.CREAM_COLOR, FontHelper.charDescFont, NpcFeminizationMod.activeH, settingsPanel, label -> {}, button -> {
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
        settingsPanel.addUIElement((IUIElement)activeHToggle);
        Texture badgeTexture = new Texture(Gdx.files.internal("npcFeminizationResources/images/feminization.png"));
        BaseMod.registerModBadge((Texture)badgeTexture, (String)MODNAME, (String)AUTHOR, (String)DESCRIPTION, (ModPanel)settingsPanel);
    }

    public void receiveEditStrings() {
        if (Settings.language == Settings.GameLanguage.ZHS || Settings.language == Settings.GameLanguage.ZHT) {
            String uiStrings = Gdx.files.internal("npcFeminizationResources/localization/zhs/ui.json").readString(String.valueOf(StandardCharsets.UTF_8));
            BaseMod.loadCustomStrings(UIStrings.class, (String)uiStrings);
        } else {
            String uiStrings = Gdx.files.internal("npcFeminizationResources/localization/eng/ui.json").readString(String.valueOf(StandardCharsets.UTF_8));
            BaseMod.loadCustomStrings(UIStrings.class, (String)uiStrings);
        }
    }
}
