/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  basemod.BaseMod
 *  basemod.helpers.SuperclassFinder
 *  com.badlogic.gdx.Gdx
 *  com.badlogic.gdx.graphics.Color
 *  com.badlogic.gdx.graphics.g2d.TextureAtlas
 *  com.esotericsoftware.spine.AnimationState
 *  com.esotericsoftware.spine.AnimationStateData
 *  com.esotericsoftware.spine.Skeleton
 *  com.esotericsoftware.spine.SkeletonData
 *  com.esotericsoftware.spine.SkeletonJson
 *  com.megacrit.cardcrawl.characters.TheSilent
 *  com.megacrit.cardcrawl.characters.Watcher
 *  com.megacrit.cardcrawl.core.CardCrawlGame
 *  com.megacrit.cardcrawl.core.Settings
 *  com.megacrit.cardcrawl.core.Settings$GameLanguage
 *  com.megacrit.cardcrawl.helpers.ImageMaster
 *  com.megacrit.cardcrawl.localization.UIStrings
 */
package feminization.helper;

import basemod.BaseMod;
import basemod.helpers.SuperclassFinder;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.AnimationStateData;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.SkeletonData;
import com.esotericsoftware.spine.SkeletonJson;
import com.megacrit.cardcrawl.characters.TheSilent;
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

public class FeminizationHelper {
    public static void addStrings() {
        if (Settings.language == Settings.GameLanguage.ZHS || Settings.language == Settings.GameLanguage.ZHT) {
            String uiStrings = Gdx.files.internal("feminizationResources/localization/zhs/ui.json").readString(String.valueOf(StandardCharsets.UTF_8));
            BaseMod.loadCustomStrings(UIStrings.class, (String)uiStrings);
        } else {
            String uiStrings = Gdx.files.internal("feminizationResources/localization/eng/ui.json").readString(String.valueOf(StandardCharsets.UTF_8));
            BaseMod.loadCustomStrings(UIStrings.class, (String)uiStrings);
        }
    }

    public static void addCharacterSkins() {
        UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("CharacterSkin_Feminization");
        String[] TEXT = uiStrings.TEXT;
        FeminizationUtil.ironcladSelections.add(0, new CharacterSelectionInfo.Builder().name(TEXT[0]).create());
        FeminizationUtil.ironcladSelections.add(1, new CharacterSelectionInfo.Builder().name(TEXT[1]).bgCharImg("feminizationResources/images/ui/charSelect/ironcladPortrait-2.jpg").shoulderImg("feminizationResources/images/characters/ironclad-2/shoulder.png").shoulder2Img("feminizationResources/images/characters/ironclad-2/shoulder2.png").corpseImg("feminizationResources/images/characters/ironclad-2/corpse.png").atlas("feminizationResources/images/characters/ironclad-2/idle/skeleton.atlas").json("feminizationResources/images/characters/ironclad-2/idle/skeleton.json").canHalfHealth(true).create());
        FeminizationUtil.silentSelections.add(0, new CharacterSelectionInfo.Builder().name(TEXT[0]).create());
        FeminizationUtil.silentSelections.add(1, new CharacterSelectionInfo.Builder().name(TEXT[2]).bgCharImg("feminizationResources/images/ui/charSelect/silentPortrait.png").bgCharImgNsfw("feminizationResources/images/ui/charSelect/silentPortrait2.png").shoulderImg("feminizationResources/images/characters/theSilent-2/shoulder.png").shoulder2Img("feminizationResources/images/characters/theSilent-2/shoulder2.png").atlas("feminizationResources/images/characters/theSilent-2/idle/hunter2.atlas").json("feminizationResources/images/characters/theSilent-2/idle/hunter2.json").customAnim(new CharacterSelectionInfo.CharacterRunnable(){

            @Override
            public void run() {
                if (!(this.character instanceof TheSilent)) {
                    return;
                }
                TheSilent _inst = (TheSilent)this.character;
                _inst.corpseImg = ResourceUtil.replaceOwnedTexture(_inst.corpseImg, FeminizationMod.activeH ? "feminizationResources/images/characters/theSilent-2/corpse_nsfw.png" : "feminizationResources/images/characters/theSilent-2/corpse.png");
            }
        }).canHalfHealth(true).create());
        FeminizationUtil.defectSelections.add(0, new CharacterSelectionInfo.Builder().name(TEXT[0]).create());
        FeminizationUtil.defectSelections.add(1, new CharacterSelectionInfo.Builder().name(TEXT[3]).bgCharImg("feminizationResources/images/ui/charSelect/defectPortrait.jpg").shoulderImg("feminizationResources/images/characters/defect-2/shoulder.png").shoulder2Img("feminizationResources/images/characters/defect-2/shoulder2.png").corpseImg("feminizationResources/images/characters/defect-2/corpse.png").atlas("feminizationResources/images/characters/defect-2/idle/skeleton.atlas").json("feminizationResources/images/characters/defect-2/idle/skeleton.json").canHalfHealth(true).create());
        FeminizationUtil.watcherSelections.add(0, new CharacterSelectionInfo.Builder().name(TEXT[0]).create());
        FeminizationUtil.watcherSelections.add(1, new CharacterSelectionInfo.Builder().name(TEXT[4]).bgCharImg("feminizationResources/images/ui/charSelect/watcherPortrait.jpg").shoulderImg("feminizationResources/images/characters/watcher-2/shoulder.png").shoulder2Img("feminizationResources/images/characters/watcher-2/shoulder2.png").atlas("feminizationResources/images/characters/watcher-2/idle/skeleton.atlas").json("feminizationResources/images/characters/watcher-2/idle/skeleton.json").customAnim(new CharacterSelectionInfo.CharacterRunnable(){

            @Override
            public void run() {
                if (!(this.character instanceof Watcher)) {
                    return;
                }
                Watcher _inst = (Watcher)this.character;
                try {
                    Field eyeAtlasField = SuperclassFinder.getSuperclassField(_inst.getClass(), (String)"eyeAtlas");
                    Field eyeSkeletonField = SuperclassFinder.getSuperclassField(_inst.getClass(), (String)"eyeSkeleton");
                    Field eyeStateDataField = SuperclassFinder.getSuperclassField(_inst.getClass(), (String)"eyeStateData");
                    Field eyeBoneField = SuperclassFinder.getSuperclassField(_inst.getClass(), (String)"eyeBone");
                    Field skeletonField = SuperclassFinder.getSuperclassField(_inst.getClass(), (String)"skeleton");
                    eyeAtlasField.setAccessible(true);
                    eyeSkeletonField.setAccessible(true);
                    eyeStateDataField.setAccessible(true);
                    eyeBoneField.setAccessible(true);
                    skeletonField.setAccessible(true);
                    Skeleton skeleton = (Skeleton)skeletonField.get(_inst);
                    com.esotericsoftware.spine.Bone eyeBone = skeleton.findBone("eye_anchor");
                    TextureAtlas oldEyeAtlas = (TextureAtlas)eyeAtlasField.get(_inst);
                    TextureAtlas newEyeAtlas = ResourceUtil.loadAtlas("feminizationResources/images/characters/watcher-2/eye_anim/skeleton.atlas");
                    try {
                        SkeletonJson json = new SkeletonJson(newEyeAtlas);
                        json.setScale(Settings.scale / 1.0f);
                        SkeletonData skeletonData = json.readSkeletonData(Gdx.files.internal("feminizationResources/images/characters/watcher-2/eye_anim/skeleton.json"));
                        Skeleton eyeSkeleton = new Skeleton(skeletonData);
                        eyeSkeleton.setColor(Color.WHITE);
                        AnimationStateData eyeStateData = new AnimationStateData(skeletonData);
                        AnimationState eyeState = new AnimationState(eyeStateData);
                        eyeStateData.setDefaultMix(0.2f);
                        eyeState.setAnimation(0, "None", true);
                        eyeAtlasField.set(_inst, newEyeAtlas);
                        eyeSkeletonField.set(_inst, eyeSkeleton);
                        eyeStateDataField.set(_inst, eyeStateData);
                        _inst.eyeState = eyeState;
                        eyeBoneField.set(_inst, eyeBone);
                    } catch (Exception exception) {
                        ResourceUtil.disposeAtlas(newEyeAtlas);
                        throw exception;
                    }
                    if (oldEyeAtlas != newEyeAtlas) {
                        ResourceUtil.disposeAtlas(oldEyeAtlas);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).canHalfHealth(false).create());
    }
}
