/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.badlogic.gdx.graphics.g2d.SpriteBatch
 *  com.evacipated.cardcrawl.modthespire.lib.SpirePatch
 *  com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch
 *  com.evacipated.cardcrawl.modthespire.lib.SpireReturn
 *  com.megacrit.cardcrawl.monsters.exordium.HexaghostBody
 */
package monsterFeminization.patch.bottom;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.monsters.exordium.HexaghostBody;

public class HexaghostBodyPatch {

    @SpirePatch(clz=HexaghostBody.class, method="render")
    public static class render {
        @SpirePrefixPatch
        public static SpireReturn prefix(HexaghostBody _inst, SpriteBatch sb) throws Exception {
            if (!monsterFeminization.MonsterFeminizationMod.activeFeminization) {
                return SpireReturn.Continue();
            }
            return SpireReturn.Return(null);
        }
    }

    @SpirePatch(clz=HexaghostBody.class, method="update")
    public static class update {
        @SpirePrefixPatch
        public static SpireReturn prefix(HexaghostBody _inst) throws Exception {
            if (!monsterFeminization.MonsterFeminizationMod.activeFeminization) {
                return SpireReturn.Continue();
            }
            return SpireReturn.Return(null);
        }
    }
}
