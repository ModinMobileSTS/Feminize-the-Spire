package feminization.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;

/** Record ownership at allocation, never guess it from a texture's filename or current field. */
public class PlayerResourceOwnershipPatch {
    @SpirePatch2(clz = AbstractPlayer.class, method = "initializeClass")
    public static class PlayerImages {
        public static ExprEditor Instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if ("com.megacrit.cardcrawl.helpers.ImageMaster".equals(call.getClassName())
                            && "loadImage".equals(call.getMethodName())
                            && "(Ljava/lang/String;)Lcom/badlogic/gdx/graphics/Texture;".equals(call.getSignature())) {
                        call.replace("{ $_ = feminization.utils.ResourceUtil.loadPlayerTexture(this, $1); }");
                    }
                }
            };
        }
    }

    @SpirePatch2(clz = AbstractCreature.class, method = "loadAnimation",
            paramtypez = {String.class, String.class, float.class})
    public static class BodyAtlas {
        public static ExprEditor Instrument() {
            return new ExprEditor() {
                @Override
                public void edit(NewExpr expression) throws CannotCompileException {
                    if ("com.badlogic.gdx.graphics.g2d.TextureAtlas".equals(expression.getClassName())
                            && "(Lcom/badlogic/gdx/files/FileHandle;)V".equals(expression.getSignature())) {
                        expression.replace("{ $_ = feminization.utils.ResourceUtil.loadAtlas($1); }");
                    }
                }
            };
        }
    }
}
