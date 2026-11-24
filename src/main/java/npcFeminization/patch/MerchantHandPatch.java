package npcFeminization.patch;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.shop.ShopScreen;
import feminization.utils.ResourceUtil;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import npcFeminization.MerchantFeminizationCompatibility;
import npcFeminization.NpcFeminizationMod;

@SpirePatch(clz = ShopScreen.class, method = "render", paramtypez = {SpriteBatch.class})
public class MerchantHandPatch {
    private static final String HAND_TEXTURE_PATH = "feminizethespire/npc_overrides/images/npcs/merchantHand.png";

    public static ExprEditor Instrument() {
        return new ExprEditor() {
            @Override
            public void edit(FieldAccess field) throws CannotCompileException {
                if (field.isReader() && field.getClassName().equals(ShopScreen.class.getName())
                        && field.getFieldName().equals("handImg")) {
                    field.replace("{ $_ = npcFeminization.patch.MerchantHandPatch.getHandTexture($proceed()); }");
                }
            }
        };
    }

    public static Texture getHandTexture(Texture original) {
        // Merchant Feminization already sets handImg to the player's selected texture.
        if (!NpcFeminizationMod.activeFeminization || MerchantFeminizationCompatibility.isActive()) {
            return original;
        }
        return ResourceUtil.replaceTexture(original, HAND_TEXTURE_PATH);
    }
}
