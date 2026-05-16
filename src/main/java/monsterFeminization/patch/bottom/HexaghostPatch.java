/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  basemod.helpers.SuperclassFinder
 *  com.badlogic.gdx.math.MathUtils
 *  com.esotericsoftware.spine.AnimationState$TrackEntry
 *  com.esotericsoftware.spine.Bone
 *  com.esotericsoftware.spine.Skeleton
 *  com.evacipated.cardcrawl.modthespire.lib.SpireInsertPatch
 *  com.evacipated.cardcrawl.modthespire.lib.SpirePatch
 *  com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch
 *  com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch
 *  com.evacipated.cardcrawl.modthespire.lib.SpireReturn
 *  com.megacrit.cardcrawl.monsters.exordium.Hexaghost
 *  com.megacrit.cardcrawl.monsters.exordium.HexaghostOrb
 */
package monsterFeminization.patch.bottom;

import basemod.helpers.SuperclassFinder;
import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.Bone;
import com.esotericsoftware.spine.Skeleton;
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.monsters.exordium.Hexaghost;
import com.megacrit.cardcrawl.monsters.exordium.HexaghostOrb;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import feminization.utils.ResourceUtil;
import monsterFeminization.utils.FeminizationUtil;

public class HexaghostPatch {
    private static final Field SKELETON_FIELD = findField(Hexaghost.class, "skeleton");
    private static final Field ORBS_FIELD = findField(Hexaghost.class, "orbs");
    private static final Field ORB_X_FIELD = findField(HexaghostOrb.class, "x");
    private static final Field ORB_Y_FIELD = findField(HexaghostOrb.class, "y");

    @SpirePatch(clz=Hexaghost.class, method="createOrbs")
    public static class createOrbs {
        @SpirePrefixPatch
        public static SpireReturn prefix(Hexaghost _inst) throws Exception {
            if (!monsterFeminization.MonsterFeminizationMod.activeFeminization) {
                return SpireReturn.Continue();
            }
            return SpireReturn.Return(null);
        }
    }

    @SpirePatch(clz=Hexaghost.class, method="update")
    public static class update {
        @SpireInsertPatch(rloc=1)
        public static void insert(Hexaghost _inst) throws Exception {
            if (!monsterFeminization.MonsterFeminizationMod.activeFeminization) {
                return;
            }
            Skeleton skeleton = (Skeleton)SKELETON_FIELD.get(_inst);
            Bone fire1 = skeleton.findBone("guihuo1");
            Bone fire2 = skeleton.findBone("guihuo2");
            Bone fire3 = skeleton.findBone("guihuo3");
            Bone fire4 = skeleton.findBone("guihuo4");
            Bone fire5 = skeleton.findBone("guihuo5");
            Bone fire6 = skeleton.findBone("guihuo6");
            ArrayList orbs = (ArrayList)ORBS_FIELD.get(_inst);
            moveOrb(orbs.get(0), skeleton, fire1, _inst);
            moveOrb(orbs.get(1), skeleton, fire2, _inst);
            moveOrb(orbs.get(2), skeleton, fire3, _inst);
            moveOrb(orbs.get(3), skeleton, fire4, _inst);
            moveOrb(orbs.get(4), skeleton, fire5, _inst);
            moveOrb(orbs.get(5), skeleton, fire6, _inst);
        }
    }

    @SpirePatch(clz=Hexaghost.class, method="<ctor>")
    public static class replace {
        @SpirePostfixPatch
        public static void postfix(Hexaghost _inst) throws Exception {
            if (!monsterFeminization.MonsterFeminizationMod.activeFeminization) {
                return;
            }
            Method loadAnimation = SuperclassFinder.getSuperClassMethod(_inst.getClass(), (String)"loadAnimation", (Class[])new Class[]{String.class, String.class, Float.TYPE});
            loadAnimation.setAccessible(true);
            ResourceUtil.invokeLoadAnimation(_inst, loadAnimation, "feminizethespire/monster_overrides/images/monsters/theBottom/boss/ghost/liuhuo.atlas", "feminizethespire/monster_overrides/images/monsters/theBottom/boss/ghost/liuhuo.json", 1.0f);
            AnimationState.TrackEntry e = _inst.state.setAnimation(0, "animation", true);
            e.setTime(e.getEndTime() * MathUtils.random());
            Skeleton skeleton = (Skeleton)SKELETON_FIELD.get(_inst);
            Bone fire1 = skeleton.findBone("guihuo1");
            Bone fire2 = skeleton.findBone("guihuo2");
            Bone fire3 = skeleton.findBone("guihuo3");
            Bone fire4 = skeleton.findBone("guihuo4");
            Bone fire5 = skeleton.findBone("guihuo5");
            Bone fire6 = skeleton.findBone("guihuo6");
            ArrayList orbs = (ArrayList)ORBS_FIELD.get(_inst);
            orbs.add(new HexaghostOrb(skeleton.getX() + fire1.getWorldX(), skeleton.getY() + fire1.getWorldY(), orbs.size()));
            orbs.add(new HexaghostOrb(skeleton.getX() + fire2.getWorldX(), skeleton.getY() + fire2.getWorldY(), orbs.size()));
            orbs.add(new HexaghostOrb(skeleton.getX() + fire3.getWorldX(), skeleton.getY() + fire3.getWorldY(), orbs.size()));
            orbs.add(new HexaghostOrb(skeleton.getX() + fire4.getWorldX(), skeleton.getY() + fire4.getWorldY(), orbs.size()));
            orbs.add(new HexaghostOrb(skeleton.getX() + fire5.getWorldX(), skeleton.getY() + fire5.getWorldY(), orbs.size()));
            orbs.add(new HexaghostOrb(skeleton.getX() + fire6.getWorldX(), skeleton.getY() + fire6.getWorldY(), orbs.size()));
        }
    }

    private static void moveOrb(Object orb, Skeleton skeleton, Bone fire, Hexaghost owner) throws IllegalAccessException {
        ORB_X_FIELD.setFloat(orb, skeleton.getX() + fire.getWorldX() - owner.drawX - owner.animX);
        ORB_Y_FIELD.setFloat(orb, skeleton.getY() + fire.getWorldY() - owner.drawY - owner.animY);
    }

    private static Field findField(Class<?> type, String name) {
        try {
            Field field = SuperclassFinder.getSuperclassField(type, name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
}
