/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  basemod.helpers.SuperclassFinder
 *  com.esotericsoftware.spine.AnimationState$TrackEntry
 *  com.esotericsoftware.spine.Skeleton
 *  com.evacipated.cardcrawl.modthespire.lib.SpirePatch
 *  com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch
 *  com.megacrit.cardcrawl.cards.DamageInfo
 *  com.megacrit.cardcrawl.characters.AbstractPlayer
 *  com.megacrit.cardcrawl.characters.Watcher
 *  javassist.CannotCompileException
 *  javassist.expr.ExprEditor
 *  javassist.expr.MethodCall
 */
package feminization.patches;

import basemod.helpers.SuperclassFinder;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.Skeleton;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.characters.Watcher;
import feminization.FeminizationMod;
import feminization.utils.FeminizationUtil;
import java.lang.reflect.Field;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class WatcherPatch {

    @SpirePatch(clz=AbstractPlayer.class, method="heal")
    public static class heal {
        @SpirePostfixPatch
        public static void postfix(AbstractPlayer _inst, int healAmount) throws Exception {
            if (!(_inst instanceof Watcher)) {
                return;
            }
            if (!FeminizationUtil.watcherSelections.get((int)FeminizationUtil.watcherSelected).canHalfHealth) {
                return;
            }
            if (_inst.currentHealth * 2 > _inst.maxHealth) {
                Field skeletonFiled = SuperclassFinder.getSuperclassField(_inst.getClass(), (String)"skeleton");
                skeletonFiled.setAccessible(true);
                Skeleton skeleton = (Skeleton)skeletonFiled.get(_inst);
                skeleton.setSkin("default");
            }
        }
    }

    @SpirePatch(clz=Watcher.class, method="damage")
    public static class damage2 {
        @SpirePostfixPatch
        public static void postfix(Watcher _inst, DamageInfo info) throws Exception {
            if (!FeminizationMod.activeH) {
                return;
            }
            if (!FeminizationUtil.watcherSelections.get((int)FeminizationUtil.watcherSelected).canHalfHealth) {
                return;
            }
            if (_inst.currentHealth * 2 <= _inst.maxHealth) {
                Field skeletonFiled = SuperclassFinder.getSuperclassField(_inst.getClass(), (String)"skeleton");
                skeletonFiled.setAccessible(true);
                Skeleton skeleton = (Skeleton)skeletonFiled.get(_inst);
                skeleton.setSkin("damaged");
            }
        }
    }

    @SpirePatch(clz=Watcher.class, method="damage")
    public static class damage {
        public static ExprEditor Instrument() {
            return new ExprEditor(){

                public void edit(MethodCall m) throws CannotCompileException {
                    if (FeminizationUtil.watcherSelected > 0 && m.getClassName().equals(AnimationState.TrackEntry.class.getName()) && m.getMethodName().equals("setTime")) {
                        m.replace("{$proceed(0.0f);}");
                    }
                }
            };
        }
    }
}

