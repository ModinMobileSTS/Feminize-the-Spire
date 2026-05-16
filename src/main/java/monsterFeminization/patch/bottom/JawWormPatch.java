/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  basemod.helpers.SuperclassFinder
 *  com.esotericsoftware.spine.AnimationStateData
 *  com.evacipated.cardcrawl.modthespire.lib.SpirePatch
 *  com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch
 *  com.megacrit.cardcrawl.core.Settings
 *  com.megacrit.cardcrawl.monsters.exordium.JawWorm
 *  javassist.CannotCompileException
 *  javassist.expr.ExprEditor
 *  javassist.expr.MethodCall
 */
package monsterFeminization.patch.bottom;

import basemod.helpers.SuperclassFinder;
import com.esotericsoftware.spine.AnimationStateData;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.monsters.exordium.JawWorm;
import java.lang.reflect.Field;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class JawWormPatch {

    @SpirePatch(clz=JawWorm.class, method="<ctor>", paramtypez={float.class, float.class, boolean.class})
    public static class replace {
        @SpirePostfixPatch
        public static void postfix(JawWorm _inst, float x, float y, boolean hard) throws Exception {
            if (!monsterFeminization.MonsterFeminizationMod.activeFeminization) {
                return;
            }
            _inst.drawX -= 20.0f * Settings.xScale;
            _inst.hb_x += 20.0f * Settings.xScale;
            _inst.hb_h += 60.0f * Settings.yScale;
            _inst.hb.resize(_inst.hb_w, _inst.hb_h);
            _inst.hb.move(_inst.drawX + _inst.hb_x + _inst.animX, _inst.drawY + _inst.hb_y + _inst.hb_h / 2.0f);
            _inst.healthHb.move(_inst.hb.cX, _inst.hb.cY - _inst.hb_h / 2.0f - _inst.healthHb.height / 2.0f);
            Field stateDataField = SuperclassFinder.getSuperclassField(_inst.getClass(), (String)"stateData");
            stateDataField.setAccessible(true);
            AnimationStateData stateData = (AnimationStateData)stateDataField.get(_inst);
            stateData.setMix("idle", "chomp", 0.2f);
            stateData.setMix("chomp", "idle", 0.2f);
            stateData.setMix("idle", "tailslam", 0.2f);
            stateData.setMix("tailslam", "idle", 0.2f);
        }
    }

    @SpirePatch(clz=JawWorm.class, method="<ctor>", paramtypez={float.class, float.class, boolean.class})
    public static class Resize {
        public static ExprEditor Instrument() {
            return new ExprEditor(){

                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getClassName().equals(JawWorm.class.getName()) && m.getMethodName().equals("loadAnimation")) {
                        m.replace("{ if (monsterFeminization.MonsterFeminizationMod.activeFeminization) { $proceed($1, $2, 1.35f); } else { $proceed($$); } }");
                    }
                }
            };
        }
    }
}
