/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  basemod.helpers.SuperclassFinder
 *  com.esotericsoftware.spine.Skeleton
 *  com.evacipated.cardcrawl.modthespire.lib.SpirePatch
 *  com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch
 *  com.megacrit.cardcrawl.cards.DamageInfo
 *  com.megacrit.cardcrawl.monsters.AbstractMonster
 *  com.megacrit.cardcrawl.monsters.city.Centurion
 *  com.megacrit.cardcrawl.monsters.city.GremlinLeader
 *  com.megacrit.cardcrawl.monsters.city.Snecko
 *  com.megacrit.cardcrawl.monsters.exordium.AcidSlime_L
 *  com.megacrit.cardcrawl.monsters.exordium.AcidSlime_M
 *  com.megacrit.cardcrawl.monsters.exordium.Cultist
 *  com.megacrit.cardcrawl.monsters.exordium.GremlinNob
 *  com.megacrit.cardcrawl.monsters.exordium.GremlinTsundere
 *  com.megacrit.cardcrawl.monsters.exordium.JawWorm
 *  com.megacrit.cardcrawl.monsters.exordium.SlimeBoss
 *  com.megacrit.cardcrawl.monsters.exordium.SpikeSlime_L
 *  com.megacrit.cardcrawl.monsters.exordium.SpikeSlime_M
 */
package monsterFeminization.patch;

import basemod.helpers.SuperclassFinder;
import com.esotericsoftware.spine.Skeleton;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.city.Centurion;
import com.megacrit.cardcrawl.monsters.city.GremlinLeader;
import com.megacrit.cardcrawl.monsters.city.Snecko;
import com.megacrit.cardcrawl.monsters.exordium.AcidSlime_L;
import com.megacrit.cardcrawl.monsters.exordium.AcidSlime_M;
import com.megacrit.cardcrawl.monsters.exordium.Cultist;
import com.megacrit.cardcrawl.monsters.exordium.GremlinNob;
import com.megacrit.cardcrawl.monsters.exordium.GremlinTsundere;
import com.megacrit.cardcrawl.monsters.exordium.JawWorm;
import com.megacrit.cardcrawl.monsters.exordium.SlimeBoss;
import com.megacrit.cardcrawl.monsters.exordium.SpikeSlime_L;
import com.megacrit.cardcrawl.monsters.exordium.SpikeSlime_M;
import java.lang.reflect.Field;
import monsterFeminization.MonsterFeminizationMod;

public class HalfLifePatch {
    public static boolean canSwitch(AbstractMonster _inst) {
        return !(_inst instanceof Cultist) && !(_inst instanceof GremlinNob) && !(_inst instanceof JawWorm) && !(_inst instanceof GremlinTsundere) && !(_inst instanceof GremlinLeader) && !(_inst instanceof SlimeBoss) && !(_inst instanceof AcidSlime_M) && !(_inst instanceof AcidSlime_L) && !(_inst instanceof SpikeSlime_L) && !(_inst instanceof SpikeSlime_M) && !(_inst instanceof Centurion) && !(_inst instanceof Snecko);
    }

    @SpirePatch(clz=AbstractMonster.class, method="heal")
    public static class heal {
        @SpirePostfixPatch
        public static void postfix(AbstractMonster _inst, int healAmount) throws Exception {
            if (!MonsterFeminizationMod.activeFeminization) {
                return;
            }
            if (HalfLifePatch.canSwitch(_inst)) {
                return;
            }
            try {
                if (_inst.currentHealth * 2 > _inst.maxHealth) {
                    Field skeletonFiled = SuperclassFinder.getSuperclassField(_inst.getClass(), (String)"skeleton");
                    skeletonFiled.setAccessible(true);
                    Skeleton skeleton = (Skeleton)skeletonFiled.get(_inst);
                    skeleton.setSkin("default");
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    @SpirePatch(clz=AbstractMonster.class, method="damage")
    public static class damage2 {
        @SpirePostfixPatch
        public static void postfix(AbstractMonster _inst, DamageInfo info) throws Exception {
            if (!MonsterFeminizationMod.activeFeminization || !MonsterFeminizationMod.activeH) {
                return;
            }
            if (HalfLifePatch.canSwitch(_inst)) {
                return;
            }
            MonsterFeminizationMod.logger.info("damaged");
            try {
                if (_inst.currentHealth * 2 <= _inst.maxHealth) {
                    Field skeletonFiled = SuperclassFinder.getSuperclassField(_inst.getClass(), (String)"skeleton");
                    skeletonFiled.setAccessible(true);
                    Skeleton skeleton = (Skeleton)skeletonFiled.get(_inst);
                    skeleton.setSkin("damaged");
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }
}
