/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.evacipated.cardcrawl.modthespire.lib.SpirePatch
 *  com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch
 *  com.megacrit.cardcrawl.core.Settings
 *  com.megacrit.cardcrawl.monsters.city.Healer
 */
package monsterFeminization.patch.bottom;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.monsters.city.Healer;

public class HealerPatch {

    @SpirePatch(clz=Healer.class, method="<ctor>")
    public static class replace {
        @SpirePostfixPatch
        public static void postfix(Healer _inst, float x, float y) throws Exception {
            if (!monsterFeminization.MonsterFeminizationMod.activeFeminization) {
                return;
            }
            _inst.hb_h += 40.0f * Settings.yScale;
            _inst.hb.resize(_inst.hb_w, _inst.hb_h);
        }
    }
}
