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
import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Map;
import monsterFeminization.MonsterFeminizationMod;

public class HalfLifePatch {
    private static final String DEFAULT_SKIN = "default";
    private static final String DAMAGED_SKIN = "damaged";
    /**
     * SuperclassFinder walks the hierarchy and throws a NoSuchFieldException per level, and this runs on
     * every heal and every damage event. The skeleton field per monster class never changes, so cache it.
     */
    private static volatile Map<Class<?>, Field> skeletonFieldCache = new IdentityHashMap<Class<?>, Field>();

    public static boolean canSwitch(AbstractMonster _inst) {
        try {
            return HalfLifePatch.canSwitch(HalfLifePatch.getSkeleton(_inst));
        }
        catch (Exception exception) {
            return false;
        }
    }

    private static boolean canSwitch(Skeleton skeleton) {
        return HalfLifePatch.hasSkin(skeleton, DEFAULT_SKIN) && HalfLifePatch.hasSkin(skeleton, DAMAGED_SKIN);
    }

    private static boolean hasSkin(Skeleton skeleton, String skinName) {
        return skeleton != null && skeleton.getData() != null && skeleton.getData().findSkin(skinName) != null;
    }

    private static Skeleton getSkeleton(AbstractMonster _inst) throws Exception {
        Class<?> monsterClass = _inst.getClass();
        Field skeletonFiled = skeletonFieldCache.get(monsterClass);
        if (skeletonFiled == null) {
            skeletonFiled = SuperclassFinder.getSuperclassField(monsterClass, (String)"skeleton");
            if (skeletonFiled == null) {
                return null;
            }
            skeletonFiled.setAccessible(true);
            synchronized (HalfLifePatch.class) {
                Map<Class<?>, Field> updated = new IdentityHashMap<Class<?>, Field>(skeletonFieldCache);
                updated.put(monsterClass, skeletonFiled);
                skeletonFieldCache = updated;
            }
        }
        return (Skeleton)skeletonFiled.get(_inst);
    }

    @SpirePatch(clz=AbstractMonster.class, method="heal")
    public static class heal {
        @SpirePostfixPatch
        public static void postfix(AbstractMonster _inst, int healAmount) throws Exception {
            if (!MonsterFeminizationMod.activeFeminization) {
                return;
            }
            try {
                Skeleton skeleton = HalfLifePatch.getSkeleton(_inst);
                if (!HalfLifePatch.canSwitch(skeleton)) {
                    return;
                }
                if (_inst.currentHealth * 2 > _inst.maxHealth) {
                    skeleton.setSkin(DEFAULT_SKIN);
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
            try {
                Skeleton skeleton = HalfLifePatch.getSkeleton(_inst);
                if (!HalfLifePatch.canSwitch(skeleton)) {
                    return;
                }
                if (_inst.currentHealth * 2 <= _inst.maxHealth) {
                    skeleton.setSkin(DAMAGED_SKIN);
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }
}
