/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  basemod.helpers.SuperclassFinder
 *  com.esotericsoftware.spine.Skeleton
 *  com.evacipated.cardcrawl.modthespire.lib.SpirePatch
 *  com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch
 *  com.megacrit.cardcrawl.characters.AnimatedNpc
 *  com.megacrit.cardcrawl.rooms.ShopRoom
 *  com.megacrit.cardcrawl.shop.Merchant
 */
package npcFeminization.patch;

import basemod.helpers.SuperclassFinder;
import com.esotericsoftware.spine.Skeleton;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.characters.AnimatedNpc;
import com.megacrit.cardcrawl.rooms.ShopRoom;
import com.megacrit.cardcrawl.shop.Merchant;
import java.lang.reflect.Field;
import npcFeminization.NpcFeminizationMod;

public class MerchantPatch {

    @SpirePatch(clz=ShopRoom.class, method="onPlayerEntry")
    public static class nsfw {
        @SpirePostfixPatch
        public static void postfix(ShopRoom _inst) throws Exception {
            if (!NpcFeminizationMod.activeFeminization || !NpcFeminizationMod.activeH) {
                return;
            }
            try {
                Merchant merchant = _inst.merchant;
                Field npcField = SuperclassFinder.getSuperclassField(merchant.getClass(), (String)"anim");
                npcField.setAccessible(true);
                AnimatedNpc npc = (AnimatedNpc)npcField.get(merchant);
                Skeleton skeleton = npc.skeleton;
                skeleton.setSkin("nsfw");
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }
}
