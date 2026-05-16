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
package monsterFeminization;

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
public class MonsterFeminizationMod
implements PostInitializeSubscriber,
EditStringsSubscriber {
    private static final String MODNAME = "Feminization";
    private static final String AUTHOR = "anaer, hoykj";
    private static final String DESCRIPTION = "";
    private static final String ACTIVE_H_KEY = "activeH";
    private static final String ACTIVE_MONSTER_NSFW_KEY = "activeMonsterNsfw";
    private static final String ACTIVE_MONSTER_FEMINIZATION_KEY = "activeMonsterFeminization";
    public static final Logger logger = LogManager.getLogger((String)MonsterFeminizationMod.class.getName());
    public static boolean activeH;
    public static boolean activeFeminization = true;

    public static void initialize() {
        MonsterFeminizationMod monsterFeminizationMod = new MonsterFeminizationMod();
        try {
            Properties defaults = new Properties();
            defaults.setProperty(ACTIVE_H_KEY, "false");
            defaults.setProperty(ACTIVE_MONSTER_NSFW_KEY, "false");
            defaults.setProperty(ACTIVE_MONSTER_FEMINIZATION_KEY, "true");
            SpireConfig config = new SpireConfig(MODNAME, "Common", defaults);
            if (!config.has(ACTIVE_MONSTER_NSFW_KEY)) {
                config.setBool(ACTIVE_MONSTER_NSFW_KEY, config.getBool(ACTIVE_H_KEY));
                config.save();
            }
            activeH = config.getBool(ACTIVE_MONSTER_NSFW_KEY);
            activeFeminization = config.getBool(ACTIVE_MONSTER_FEMINIZATION_KEY);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public MonsterFeminizationMod() {
        BaseMod.subscribe((ISubscriber)this);
    }

    public void receivePostInitialize() {
    }

    private void CreatePanel() throws IOException {
        UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("Main_Feminization");
        String[] TEXT = uiStrings.TEXT;
        SpireConfig spireConfig = new SpireConfig(MODNAME, "Common");
        ModPanel settingsPanel = new ModPanel();
        ModLabeledToggleButton activeHToggle = new ModLabeledToggleButton(TEXT[0], 400.0f, 700.0f, Settings.CREAM_COLOR, FontHelper.charDescFont, MonsterFeminizationMod.activeH, settingsPanel, label -> {}, button -> {
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
        settingsPanel.addUIElement((IUIElement)activeHToggle);
        Texture badgeTexture = new Texture(Gdx.files.internal("monsterFeminizationResources/images/feminization.png"));
        BaseMod.registerModBadge((Texture)badgeTexture, (String)MODNAME, (String)AUTHOR, (String)DESCRIPTION, (ModPanel)settingsPanel);
    }

    public void receiveEditStrings() {
        if (Settings.language == Settings.GameLanguage.ZHS || Settings.language == Settings.GameLanguage.ZHT) {
            String uiStrings = Gdx.files.internal("monsterFeminizationResources/localization/zhs/ui.json").readString(String.valueOf(StandardCharsets.UTF_8));
            BaseMod.loadCustomStrings(UIStrings.class, (String)uiStrings);
        } else {
            String uiStrings = Gdx.files.internal("monsterFeminizationResources/localization/eng/ui.json").readString(String.valueOf(StandardCharsets.UTF_8));
            BaseMod.loadCustomStrings(UIStrings.class, (String)uiStrings);
        }
    }
}
