/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.evacipated.cardcrawl.modthespire.lib.SpirePatch
 *  com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch
 *  com.megacrit.cardcrawl.core.Settings
 *  com.megacrit.cardcrawl.monsters.exordium.Cultist
 */
package monsterFeminization.patch.bottom;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.monsters.exordium.Cultist;

public class CultistPatch {

    @SpirePatch(clz=Cultist.class, method="<ctor>", paramtypez={float.class, float.class, boolean.class})
    public static class replace {
        @SpirePostfixPatch
        public static void postfix(Cultist _inst, float x, float y, boolean talk) throws Exception {
            if (!monsterFeminization.MonsterFeminizationMod.activeFeminization) {
                return;
            }
            _inst.hb_y = -10.0f * Settings.yScale;
            _inst.hb.move(_inst.drawX + _inst.hb_x + _inst.animX, _inst.drawY + _inst.hb_y + _inst.hb_h / 2.0f);
            _inst.healthHb.move(_inst.hb.cX, _inst.hb.cY - _inst.hb_h / 2.0f - _inst.healthHb.height / 2.0f);
        }
    }
}
