/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  basemod.helpers.SuperclassFinder
 *  com.esotericsoftware.spine.Skeleton
 *  com.evacipated.cardcrawl.modthespire.lib.SpirePatch
 *  com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch
 *  com.megacrit.cardcrawl.characters.AnimatedNpc
 *  com.megacrit.cardcrawl.neow.NeowEvent
 */
package npcFeminization.patch;

import basemod.helpers.SuperclassFinder;
import com.esotericsoftware.spine.Skeleton;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.characters.AnimatedNpc;
import com.megacrit.cardcrawl.neow.NeowEvent;
import java.lang.reflect.Field;
import npcFeminization.NpcFeminizationMod;

public class NeowPatch {

    @SpirePatch(clz=NeowEvent.class, method="<ctor>", paramtypez={boolean.class})
    public static class nsfw {
        @SpirePostfixPatch
        public static void postfix(NeowEvent _inst, boolean isDone) throws Exception {
            if (!NpcFeminizationMod.activeFeminization || !NpcFeminizationMod.activeH) {
                return;
            }
            try {
                Field npcField = SuperclassFinder.getSuperclassField(_inst.getClass(), (String)"npc");
                npcField.setAccessible(true);
                AnimatedNpc npc = (AnimatedNpc)npcField.get(_inst);
                Skeleton skeleton = npc.skeleton;
                skeleton.setSkin("nsfw");
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }
}
