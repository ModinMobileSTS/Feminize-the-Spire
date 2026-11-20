package feminization.patches;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.screens.SingleCardViewPopup;
import feminization.cardArt.CardArtFeminization;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class CardArtFeminizationPatch {
    private CardArtFeminizationPatch() {
    }

    @SpirePatch2(clz = AbstractCard.class, method = "renderPortrait", paramtypez = {SpriteBatch.class})
    public static class NormalPortraitPatch {
        @SpirePrefixPatch
        public static void prefix(AbstractCard __instance, SpriteBatch sb) {
            CardArtFeminization.beforeRenderPortrait(__instance);
        }

        @SpirePostfixPatch
        public static void postfix(AbstractCard __instance, SpriteBatch sb) {
            CardArtFeminization.afterRenderPortrait(__instance);
        }
    }

    // Beta/joke art was removed from the mod in 1.1.2; renderJokePortrait is intentionally left
    // vanilla so playtester-art users simply see the original joke portraits.

    @SpirePatch2(clz = SingleCardViewPopup.class, method = "loadPortraitImg")
    public static class SingleCardViewPatch {
        public static ExprEditor Instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if ("com.megacrit.cardcrawl.helpers.ImageMaster".equals(call.getClassName())
                            && "loadImage".equals(call.getMethodName())
                            && "(Ljava/lang/String;)Lcom/badlogic/gdx/graphics/Texture;".equals(call.getSignature())) {
                        call.replace("{ $_ = feminization.cardArt.CardArtFeminization.loadSingleCardViewPortrait(this, $1); }");
                    }
                }
            };
        }
    }
}
