/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  basemod.BaseMod
 *  basemod.helpers.SuperclassFinder
 *  com.badlogic.gdx.Gdx
 *  com.esotericsoftware.spine.Skeleton
 *  com.megacrit.cardcrawl.characters.Ironclad
 *  com.megacrit.cardcrawl.characters.Watcher
 *  com.megacrit.cardcrawl.core.CardCrawlGame
 *  com.megacrit.cardcrawl.core.Settings
 *  com.megacrit.cardcrawl.core.Settings$GameLanguage
 *  com.megacrit.cardcrawl.helpers.ImageMaster
 *  com.megacrit.cardcrawl.localization.UIStrings
 */
package extraFeminization.helper;

import basemod.BaseMod;
import basemod.helpers.SuperclassFinder;
import com.badlogic.gdx.Gdx;
import com.esotericsoftware.spine.Skeleton;
import com.megacrit.cardcrawl.characters.Ironclad;
import com.megacrit.cardcrawl.characters.Watcher;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.UIStrings;
import feminization.FeminizationMod;
import feminization.utils.CharacterSelectionInfo;
import feminization.utils.FeminizationUtil;
import feminization.utils.ResourceUtil;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

public class ExtraFeminizationHelper {
    public static void addStrings() {
        if (Settings.language == Settings.GameLanguage.ZHS || Settings.language == Settings.GameLanguage.ZHT) {
            String uiStrings = Gdx.files.internal("extraFeminizationResources/localization/zhs/ui.json").readString(String.valueOf(StandardCharsets.UTF_8));
            BaseMod.loadCustomStrings(UIStrings.class, (String)uiStrings);
        } else {
            String uiStrings = Gdx.files.internal("extraFeminizationResources/localization/eng/ui.json").readString(String.valueOf(StandardCharsets.UTF_8));
            BaseMod.loadCustomStrings(UIStrings.class, (String)uiStrings);
        }
    }

    public static void addCharacterSkins() {
        UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("CharacterSkin_ExtraFeminization");
        String[] TEXT = uiStrings.TEXT;
        FeminizationUtil.watcherSelections.add(new CharacterSelectionInfo.Builder().name(TEXT[0]).bgCharImg("extraFeminizationResources/images/ui/charSelect/watcherPortrait.jpg").shoulderImg("extraFeminizationResources/images/characters/watcher/shoulder.png").shoulder2Img("extraFeminizationResources/images/characters/watcher/shoulder2.png").atlas("extraFeminizationResources/images/characters/watcher/idle/qyb.atlas").json("extraFeminizationResources/images/characters/watcher/idle/qyb.json").customAnim(new CharacterSelectionInfo.CharacterRunnable(){

            @Override
            public void run() {
                if (!(this.character instanceof Watcher)) {
                    return;
                }
                Watcher _inst = (Watcher)this.character;
                _inst.corpseImg = ResourceUtil.replaceOwnedTexture(_inst.corpseImg, FeminizationMod.activeH ? "extraFeminizationResources/images/characters/watcher/corpse_nsfw.png" : "extraFeminizationResources/images/characters/watcher/corpse.png");
                try {
                    Field eyeBoneField = SuperclassFinder.getSuperclassField(_inst.getClass(), (String)"eyeBone");
                    Field skeletonField = SuperclassFinder.getSuperclassField(_inst.getClass(), (String)"skeleton");
                    eyeBoneField.setAccessible(true);
                    skeletonField.setAccessible(true);
                    Skeleton skeleton = (Skeleton)skeletonField.get(_inst);
                    eyeBoneField.set(_inst, skeleton.findBone("eye_anchor"));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                _inst.hb_w += 80.0f * Settings.xScale;
                _inst.hb_h += 120.0f * Settings.xScale;
                _inst.hb.resize(_inst.hb_w, _inst.hb_h);
                _inst.hb.move(_inst.drawX + _inst.hb_x + _inst.animX, _inst.drawY + _inst.hb_y + _inst.hb_h / 2.0f);
                _inst.healthHb.move(_inst.hb.cX, _inst.hb.cY - _inst.hb_h / 2.0f - _inst.healthHb.height / 2.0f);
            }
        }).canHalfHealth(true).create());
        FeminizationUtil.silentSelections.add(new CharacterSelectionInfo.Builder().name(TEXT[1]).bgCharImg("extraFeminizationResources/images/ui/charSelect/jackPortrait.jpg").shoulderImg("extraFeminizationResources/images/characters/theSilent/shoulder.png").shoulder2Img("extraFeminizationResources/images/characters/theSilent/shoulder2.png").corpseImg("extraFeminizationResources/images/characters/theSilent/corpse.png").atlas("extraFeminizationResources/images/characters/theSilent/idle/jake2.atlas").json("extraFeminizationResources/images/characters/theSilent/idle/jake2.json").canHalfHealth(true).create());
        FeminizationUtil.silentSelections.add(new CharacterSelectionInfo.Builder().name(TEXT[4]).bgCharImg("extraFeminizationResources/images/ui/charSelect/silentPortrait.jpg").atlas("extraFeminizationResources/images/characters/theSilent2/idle/hornet2.atlas").json("extraFeminizationResources/images/characters/theSilent2/idle/hornet2.json").create());
        FeminizationUtil.silentSelections.add(new CharacterSelectionInfo.Builder().name(TEXT[5]).bgCharImg("extraFeminizationResources/images/ui/charSelect/silentPortrait2.png").atlas("extraFeminizationResources/images/characters/theSilent3/idle/turehornet.atlas").json("extraFeminizationResources/images/characters/theSilent3/idle/turehornet.json").canHalfHealth(true).create());
        FeminizationUtil.ironcladSelections.add(new CharacterSelectionInfo.Builder().name(TEXT[2]).id("ironclad_lily").bgCharImg("extraFeminizationResources/images/ui/charSelect/lilyPortrait.png").atlas("extraFeminizationResources/images/characters/ironclad/idle/lily.atlas").corpseImg("extraFeminizationResources/images/characters/ironclad/corpse.png").json("extraFeminizationResources/images/characters/ironclad/idle/lily.json").customAnim(new CharacterSelectionInfo.CharacterRunnable(){

            @Override
            public void run() {
                if (!(this.character instanceof Ironclad)) {
                    return;
                }
                this.character.state.getData().setMix("attack1", "Idle", 0.2f);
                this.character.state.getData().setMix("attack123", "Idle", 0.2f);
                this.character.state.getData().setMix("attack_ranged", "Idle", 0.2f);
                this.character.state.getData().setMix("attack_rush", "Idle", 0.2f);
                this.character.state.getData().setMix("buff", "Idle", 0.2f);
            }
        }).canHalfHealth(false).create());
        FeminizationUtil.defectSelections.add(new CharacterSelectionInfo.Builder().name(TEXT[6]).bgCharImg("extraFeminizationResources/images/ui/charSelect/defectPortrait.jpg").corpseImg("extraFeminizationResources/images/characters/defect/dead.png").atlas("extraFeminizationResources/images/characters/defect/idle/STSaliceS.atlas").json("extraFeminizationResources/images/characters/defect/idle/STSaliceS.json").create());
        FeminizationUtil.watcherSelections.add(new CharacterSelectionInfo.Builder().name(TEXT[3]).id("watcher_succubus_nun").bgCharImg("extraFeminizationResources/images/ui/charSelect/watcherPortrait2.jpg").corpseImg("extraFeminizationResources/images/characters/watcher2/corpse.png").shoulderImg("extraFeminizationResources/images/characters/watcher2/shoulder.png").shoulder2Img("extraFeminizationResources/images/characters/watcher2/shoulder2.png").atlas("extraFeminizationResources/images/characters/watcher2/idle/nun.atlas").json("extraFeminizationResources/images/characters/watcher2/idle/nun.json").customAnim(new CharacterSelectionInfo.CharacterRunnable(){

            @Override
            public void run() {
                if (!(this.character instanceof Watcher)) {
                    return;
                }
                Watcher _inst = (Watcher)this.character;
                try {
                    Field eyeBoneField = SuperclassFinder.getSuperclassField(_inst.getClass(), (String)"eyeBone");
                    Field skeletonField = SuperclassFinder.getSuperclassField(_inst.getClass(), (String)"skeleton");
                    eyeBoneField.setAccessible(true);
                    skeletonField.setAccessible(true);
                    Skeleton skeleton = (Skeleton)skeletonField.get(_inst);
                    eyeBoneField.set(_inst, skeleton.findBone("eye_anchor"));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                this.character.state.getData().setMix("attack_artillery", "Idle", 0.2f);
                this.character.state.getData().setMix("attack_bandage", "Idle", 0.2f);
                this.character.state.getData().setMix("attack_point", "Idle", 0.2f);
                this.character.state.getData().setMix("attack_ranged", "Idle", 0.2f);
            }
        }).canHalfHealth(false).create());
    }
}
