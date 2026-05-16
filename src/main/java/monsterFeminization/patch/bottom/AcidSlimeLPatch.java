/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.evacipated.cardcrawl.modthespire.lib.SpirePatch
 *  com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch
 *  com.megacrit.cardcrawl.core.Settings
 *  com.megacrit.cardcrawl.monsters.exordium.AcidSlime_L
 */
package monsterFeminization.patch.bottom;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.monsters.exordium.AcidSlime_L;

public class AcidSlimeLPatch {

    @SpirePatch(clz=AcidSlime_L.class, method="<ctor>", paramtypez={float.class, float.class, int.class, int.class})
    public static class replace {
        @SpirePostfixPatch
        public static void postfix(AcidSlime_L _inst, float x, float y, int poisonAmount, int newHealth) throws Exception {
            if (!monsterFeminization.MonsterFeminizationMod.activeFeminization) {
                return;
            }
            _inst.hb_w -= 80.0f * Settings.xScale;
            _inst.hb_h += 160.0f * Settings.yScale;
            _inst.hb.resize(_inst.hb_w, _inst.hb_h);
        }
    }
}
