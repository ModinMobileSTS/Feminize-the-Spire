/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  basemod.helpers.SuperclassFinder
 *  com.badlogic.gdx.graphics.g2d.SpriteBatch
 *  com.esotericsoftware.spine.AnimationState$TrackEntry
 *  com.esotericsoftware.spine.AnimationStateData
 *  com.evacipated.cardcrawl.modthespire.lib.SpirePatch
 *  com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch
 *  com.megacrit.cardcrawl.characters.AbstractPlayer$PlayerClass
 *  com.megacrit.cardcrawl.characters.Watcher
 *  com.megacrit.cardcrawl.core.CardCrawlGame
 *  com.megacrit.cardcrawl.helpers.ImageMaster
 *  com.megacrit.cardcrawl.screens.charSelect.CharacterOption
 *  com.megacrit.cardcrawl.screens.charSelect.CharacterSelectScreen
 */
package feminization.patches.charSelect;

import basemod.helpers.SuperclassFinder;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.AnimationStateData;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.characters.Watcher;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.screens.charSelect.CharacterOption;
import com.megacrit.cardcrawl.screens.charSelect.CharacterSelectScreen;
import feminization.charSelect.WatcherChangePart;
import feminization.utils.CharacterSelectionInfo;
import feminization.utils.FeminizationUtil;
import feminization.utils.ResourceUtil;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class WatcherChangePatch {
    public static WatcherChangePart watcherPart = new WatcherChangePart();

    @SpirePatch(clz=Watcher.class, method="<ctor>")
    public static class changeAnim {
        @SpirePostfixPatch
        public static void postfix(Watcher _inst, String playerName) throws Exception {
            if (FeminizationUtil.watcherSelections.size() < 1) {
                return;
            }
            CharacterSelectionInfo info = FeminizationUtil.watcherSelections.get(FeminizationUtil.watcherSelected);
            FeminizationUtil.watcherId = info.id;
            if (info.shoulderImg != null) {
                _inst.shoulderImg = ResourceUtil.replaceTexture(_inst.shoulderImg, info.shoulderImg);
            }
            if (info.shoulder2Img != null) {
                _inst.shoulder2Img = ResourceUtil.replaceTexture(_inst.shoulder2Img, info.shoulder2Img);
            }
            if (info.corpseImg != null) {
                _inst.corpseImg = ResourceUtil.replaceTexture(_inst.corpseImg, info.corpseImg);
            }
            if (info.atlas != null && info.json != null) {
                Method loadAnimationMethod = SuperclassFinder.getSuperClassMethod(_inst.getClass(), (String)"loadAnimation", (Class[])new Class[]{String.class, String.class, Float.TYPE});
                loadAnimationMethod.setAccessible(true);
                ResourceUtil.invokeLoadAnimation(_inst, loadAnimationMethod, info.atlas, info.json, 1.0f);
                AnimationState.TrackEntry e = _inst.state.setAnimation(0, "Idle", true);
                Field stateDataField = SuperclassFinder.getSuperclassField(_inst.getClass(), (String)"stateData");
                stateDataField.setAccessible(true);
                AnimationStateData stateData = (AnimationStateData)stateDataField.get(_inst);
                stateData.setMix("Hit", "Idle", 0.1f);
                e.setTimeScale(0.7f);
            }
            if (info.customAnim != null) {
                info.customAnim.character = _inst;
                info.customAnim.run();
            }
        }
    }

    @SpirePatch(clz=CharacterSelectScreen.class, method="render")
    public static class render {
        @SpirePostfixPatch
        public static void postfix(CharacterSelectScreen _inst, SpriteBatch sb) {
            if (CardCrawlGame.chosenCharacter == AbstractPlayer.PlayerClass.WATCHER) {
                for (CharacterOption option : _inst.options) {
                    if (option.c.chosenClass != CardCrawlGame.chosenCharacter) continue;
                    if (!option.selected) break;
                    watcherPart.render(sb);
                    break;
                }
            }
        }
    }

    @SpirePatch(clz=CharacterSelectScreen.class, method="update")
    public static class update {
        @SpirePostfixPatch
        public static void postfix(CharacterSelectScreen _inst) {
            if (CardCrawlGame.chosenCharacter == AbstractPlayer.PlayerClass.WATCHER) {
                for (CharacterOption option : _inst.options) {
                    if (option.c.chosenClass != CardCrawlGame.chosenCharacter) continue;
                    if (option.selected) {
                        watcherPart.update(option);
                    } else {
                        watcherPart.disable();
                    }
                    break;
                }
            } else {
                watcherPart.disable();
            }
        }
    }
}
