/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  basemod.helpers.SuperclassFinder
 *  com.esotericsoftware.spine.Skeleton
 *  com.evacipated.cardcrawl.modthespire.lib.SpirePatch
 *  com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch
 *  com.megacrit.cardcrawl.cards.DamageInfo
 *  com.megacrit.cardcrawl.characters.AbstractPlayer
 *  com.megacrit.cardcrawl.characters.Ironclad
 *  javassist.CannotCompileException
 *  javassist.expr.ExprEditor
 *  javassist.expr.MethodCall
 */
package feminization.patches;

import basemod.helpers.SuperclassFinder;
import com.esotericsoftware.spine.Skeleton;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.characters.Ironclad;
import feminization.FeminizationMod;
import feminization.utils.FeminizationUtil;
import java.lang.reflect.Field;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class IroncladPatch {

    @SpirePatch(clz=AbstractPlayer.class, method="heal")
    public static class heal {
        @SpirePostfixPatch
        public static void postfix(AbstractPlayer _inst, int healAmount) throws Exception {
            if (!(_inst instanceof Ironclad)) {
                return;
            }
            if (!FeminizationUtil.ironcladSelections.get((int)FeminizationUtil.ironcladSelected).canHalfHealth) {
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

    @SpirePatch(clz=Ironclad.class, method="damage")
    public static class damage {
        @SpirePostfixPatch
        public static void postfix(Ironclad _inst, DamageInfo info) throws Exception {
            if (!FeminizationMod.activeH) {
                return;
            }
            if (!FeminizationUtil.ironcladSelections.get((int)FeminizationUtil.ironcladSelected).canHalfHealth) {
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

    @SpirePatch(clz=Ironclad.class, method="<ctor>")
    public static class Resize {
        public static ExprEditor Instrument() {
            return new ExprEditor(){

                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getClassName().equals(Ironclad.class.getName()) && m.getMethodName().equals("loadAnimation")) {
                        m.replace("{$proceed($1, $2, 1.0f);}");
                    }
                }
            };
        }
    }
}

