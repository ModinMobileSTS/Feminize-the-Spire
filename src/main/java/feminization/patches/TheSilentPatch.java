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
 *  com.megacrit.cardcrawl.characters.TheSilent
 */
package feminization.patches;

import basemod.helpers.SuperclassFinder;
import com.esotericsoftware.spine.Skeleton;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.characters.TheSilent;
import feminization.FeminizationMod;
import feminization.utils.FeminizationUtil;
import java.lang.reflect.Field;

public class TheSilentPatch {

    @SpirePatch(clz=AbstractPlayer.class, method="heal")
    public static class heal {
        @SpirePostfixPatch
        public static void postfix(AbstractPlayer _inst, int healAmount) throws Exception {
            if (!(_inst instanceof TheSilent)) {
                return;
            }
            if (!FeminizationUtil.silentSelections.get((int)FeminizationUtil.silentSelected).canHalfHealth) {
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

    @SpirePatch(clz=TheSilent.class, method="damage")
    public static class damage2 {
        @SpirePostfixPatch
        public static void postfix(TheSilent _inst, DamageInfo info) throws Exception {
            if (!FeminizationMod.activeH) {
                return;
            }
            if (!FeminizationUtil.silentSelections.get((int)FeminizationUtil.silentSelected).canHalfHealth) {
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
}

