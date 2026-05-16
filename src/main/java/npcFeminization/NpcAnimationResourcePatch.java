package npcFeminization;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

@SpirePatch(clz = com.megacrit.cardcrawl.characters.AnimatedNpc.class, method = SpirePatch.CONSTRUCTOR, paramtypez = {float.class, float.class, String.class, String.class, String.class})
public class NpcAnimationResourcePatch {
    private static final String OVERRIDE_PREFIX = "feminizethespire/npc_overrides/";

    public static ExprEditor Instrument() {
        return new ExprEditor() {
            @Override
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getClassName().equals("com.megacrit.cardcrawl.characters.AnimatedNpc") && m.getMethodName().equals("loadAnimation")) {
                    m.replace("{ $proceed(npcFeminization.NpcAnimationResourcePatch.redirectPath($1), npcFeminization.NpcAnimationResourcePatch.redirectPath($2), $3); }");
                }
            }
        };
    }

    public static String redirectPath(String path) {
        if (!NpcFeminizationMod.activeFeminization || !isOverriddenNpcResource(path)) {
            return path;
        }
        return OVERRIDE_PREFIX + path;
    }

    private static boolean isOverriddenNpcResource(String path) {
        return "images/npcs/neow/skeleton.atlas".equals(path)
                || "images/npcs/neow/skeleton.json".equals(path)
                || "images/npcs/merchant/skeleton.atlas".equals(path)
                || "images/npcs/merchant/skeleton.json".equals(path);
    }
}
