/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  basemod.BaseMod
 *  basemod.interfaces.EditCharactersSubscriber
 *  basemod.interfaces.EditStringsSubscriber
 *  basemod.interfaces.ISubscriber
 *  basemod.interfaces.PostInitializeSubscriber
 *  com.evacipated.cardcrawl.modthespire.lib.SpireInitializer
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package extraFeminization;

import basemod.BaseMod;
import basemod.interfaces.EditCharactersSubscriber;
import basemod.interfaces.EditStringsSubscriber;
import basemod.interfaces.ISubscriber;
import basemod.interfaces.PostInitializeSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import extraFeminization.helper.ExtraFeminizationHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SpireInitializer
public class ExtraFeminizationMod
implements PostInitializeSubscriber,
EditCharactersSubscriber,
EditStringsSubscriber {
    private static final String MODNAME = "Feminization";
    private static final String AUTHOR = "anaer, hoykj";
    private static final String DESCRIPTION = "";
    public static final Logger logger = LogManager.getLogger((String)ExtraFeminizationMod.class.getName());

    public static void initialize() {
        ExtraFeminizationMod extraFeminizationMod = new ExtraFeminizationMod();
    }

    public ExtraFeminizationMod() {
        BaseMod.subscribe((ISubscriber)this);
    }

    public void receivePostInitialize() {
    }

    public void receiveEditStrings() {
        ExtraFeminizationHelper.addStrings();
    }

    public void receiveEditCharacters() {
        ExtraFeminizationHelper.addCharacterSkins();
    }
}

