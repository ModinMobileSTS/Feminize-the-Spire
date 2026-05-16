/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.evacipated.cardcrawl.modthespire.lib.SpirePatch
 *  com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch
 *  com.megacrit.cardcrawl.core.Settings
 *  com.megacrit.cardcrawl.monsters.exordium.GremlinWizard
 */
package monsterFeminization.patch.bottom;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.monsters.exordium.GremlinWizard;

public class GremlinWizardPatch {

    @SpirePatch(clz=GremlinWizard.class, method="<ctor>")
    public static class replace {
        @SpirePostfixPatch
        public static void postfix(GremlinWizard _inst, float x, float y) throws Exception {
            if (!monsterFeminization.MonsterFeminizationMod.activeFeminization) {
                return;
            }
            _inst.drawX += 40.0f * Settings.xScale;
            _inst.hb_x -= 40.0f * Settings.xScale;
            _inst.hb.move(_inst.drawX + _inst.hb_x + _inst.animX, _inst.drawY + _inst.hb_y + _inst.hb_h / 2.0f);
            _inst.healthHb.move(_inst.hb.cX, _inst.hb.cY - _inst.hb_h / 2.0f - _inst.healthHb.height / 2.0f);
            _inst.hb_h += 80.0f * Settings.yScale;
            _inst.hb.resize(_inst.hb_w, _inst.hb_h);
        }
    }
}
