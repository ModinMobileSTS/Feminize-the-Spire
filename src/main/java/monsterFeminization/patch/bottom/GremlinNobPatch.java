/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.evacipated.cardcrawl.modthespire.lib.SpirePatch
 *  com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch
 *  com.megacrit.cardcrawl.core.Settings
 *  com.megacrit.cardcrawl.monsters.exordium.GremlinNob
 *  javassist.CannotCompileException
 *  javassist.expr.ExprEditor
 *  javassist.expr.MethodCall
 */
package monsterFeminization.patch.bottom;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.monsters.exordium.GremlinNob;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class GremlinNobPatch {

    @SpirePatch(clz=GremlinNob.class, method="<ctor>", paramtypez={float.class, float.class, boolean.class})
    public static class replace {
        @SpirePostfixPatch
        public static void postfix(GremlinNob _inst, float x, float y, boolean setVuln) throws Exception {
            if (!monsterFeminization.MonsterFeminizationMod.activeFeminization) {
                return;
            }
            _inst.drawX -= 80.0f * Settings.xScale;
            _inst.hb_x += 80.0f * Settings.xScale;
            _inst.hb.move(_inst.drawX + _inst.hb_x + _inst.animX, _inst.drawY + _inst.hb_y + _inst.hb_h / 2.0f);
            _inst.healthHb.move(_inst.hb.cX, _inst.hb.cY - _inst.hb_h / 2.0f - _inst.healthHb.height / 2.0f);
        }
    }

    @SpirePatch(clz=GremlinNob.class, method="<ctor>", paramtypez={float.class, float.class, boolean.class})
    public static class Resize {
        public static ExprEditor Instrument() {
            return new ExprEditor(){

                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getClassName().equals(GremlinNob.class.getName()) && m.getMethodName().equals("loadAnimation")) {
                        m.replace("{ if (monsterFeminization.MonsterFeminizationMod.activeFeminization) { $proceed($1, $2, 1.2f); } else { $proceed($$); } }");
                    }
                }
            };
        }
    }
}
