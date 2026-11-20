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
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.monsters.exordium.Hexaghost;
import com.megacrit.cardcrawl.monsters.exordium.HexaghostOrb;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import feminization.utils.ResourceUtil;
import monsterFeminization.utils.FeminizationUtil;

public class HexaghostPatch {
    private static final Field SKELETON_FIELD = findField(Hexaghost.class, "skeleton");
    private static final Field ORBS_FIELD = findField(Hexaghost.class, "orbs");
    private static final Field ORB_X_FIELD = findField(HexaghostOrb.class, "x");
    private static final Field ORB_Y_FIELD = findField(HexaghostOrb.class, "y");
    private static final String[] FIRE_BONE_NAMES = {"guihuo1", "guihuo2", "guihuo3", "guihuo4", "guihuo5", "guihuo6"};
    /**
     * Cache indices rather than Bones: every Bone strongly references its Skeleton, which would
     * keep a WeakHashMap key alive through the value. Indices have no back-reference to the key.
     */
    private static final Map<Skeleton, int[]> FIRE_BONES = Collections.synchronizedMap(new WeakHashMap<Skeleton, int[]>());

    private static int[] getFireBones(Skeleton skeleton) {
        if (skeleton == null) {
            return null;
        }
        int[] bones = FIRE_BONES.get(skeleton);
        if (bones != null) {
            return bones;
        }

        bones = new int[FIRE_BONE_NAMES.length];
        for (int i = 0; i < FIRE_BONE_NAMES.length; ++i) {
            Bone bone = skeleton.findBone(FIRE_BONE_NAMES[i]);
            if (bone == null) {
                // Unexpected skeleton; do not cache a partial result.
                return null;
            }
            bones[i] = skeleton.getBones().indexOf(bone, true);
        }
        FIRE_BONES.put(skeleton, bones);
        return bones;
    }

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
        // Runs immediately before the vanilla body — the same point in the frame the original
        // SpireInsertPatch(rloc=1) targeted, but without hardcoding a bytecode offset that breaks
        // when the game or another mod shifts the method.
        @SpirePrefixPatch
        public static void prefix(Hexaghost _inst) throws Exception {
            if (!monsterFeminization.MonsterFeminizationMod.activeFeminization) {
                return;
            }
            Skeleton skeleton = (Skeleton)SKELETON_FIELD.get(_inst);
            int[] fires = getFireBones(skeleton);
            if (fires == null) {
                return;
            }
            ArrayList orbs = (ArrayList)ORBS_FIELD.get(_inst);
            if (orbs == null || orbs.size() < fires.length) {
                // Multiplayer (Together in Spire) can rebuild a Hexaghost whose orbs list is
                // empty/absent; without this the unconditional orbs.get(i) below throws.
                return;
            }
            for (int i = 0; i < fires.length; ++i) {
                moveOrb(orbs.get(i), skeleton, skeleton.getBones().get(fires[i]), _inst);
            }
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
            if (e != null) {
                e.setTime(e.getEndTime() * MathUtils.random());
            }
            Skeleton skeleton = (Skeleton)SKELETON_FIELD.get(_inst);
            ArrayList orbs = (ArrayList)ORBS_FIELD.get(_inst);
            if (orbs == null) {
                return;
            }
            int[] fires = getFireBones(skeleton);
            if (fires == null) {
                // Fem skeleton's flame bones unavailable. The createOrbs patch above suppresses
                // vanilla orb creation, so never leave the list empty (empty orbs crashes the
                // per-frame update patch in multiplayer). Fall back to vanilla flame offsets.
                addFallbackOrbs(orbs);
                return;
            }
            for (int i = 0; i < fires.length; ++i) {
                Bone fire = skeleton.getBones().get(fires[i]);
                orbs.add(new HexaghostOrb(skeleton.getX() + fire.getWorldX(), skeleton.getY() + fire.getWorldY(), orbs.size()));
            }
        }
    }

    private static void addFallbackOrbs(ArrayList orbs) {
        orbs.add(new HexaghostOrb(-90.0f, 380.0f, orbs.size()));
        orbs.add(new HexaghostOrb(90.0f, 380.0f, orbs.size()));
        orbs.add(new HexaghostOrb(160.0f, 250.0f, orbs.size()));
        orbs.add(new HexaghostOrb(90.0f, 120.0f, orbs.size()));
        orbs.add(new HexaghostOrb(-90.0f, 120.0f, orbs.size()));
        orbs.add(new HexaghostOrb(-160.0f, 250.0f, orbs.size()));
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
