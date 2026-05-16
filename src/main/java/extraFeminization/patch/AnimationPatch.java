/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.badlogic.gdx.Gdx
 *  com.esotericsoftware.spine.AnimationState$TrackEntry
 *  com.evacipated.cardcrawl.modthespire.lib.SpireInsertPatch
 *  com.evacipated.cardcrawl.modthespire.lib.SpirePatch
 *  com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch
 *  com.megacrit.cardcrawl.cards.AbstractCard
 *  com.megacrit.cardcrawl.cards.DamageInfo
 *  com.megacrit.cardcrawl.characters.AbstractPlayer
 *  com.megacrit.cardcrawl.characters.Ironclad
 *  com.megacrit.cardcrawl.characters.Watcher
 *  com.megacrit.cardcrawl.monsters.AbstractMonster
 */
package extraFeminization.patch;

import com.badlogic.gdx.Gdx;
import com.esotericsoftware.spine.AnimationState;
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.characters.Ironclad;
import com.megacrit.cardcrawl.characters.Watcher;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import feminization.utils.FeminizationUtil;

public class AnimationPatch {
    public static float animTimer = 0.0f;
    public static float defaultAnimTimer = 2.0f;

    public static void changeAnim(AbstractPlayer p, String anim) {
        if (animTimer > 0.0f) {
            return;
        }
        animTimer = defaultAnimTimer;
        AnimationState.TrackEntry e = p.state.setAnimation(0, anim, false);
        p.state.addAnimation(0, "Idle", true, 0.0f);
    }

    @SpirePatch(clz=AbstractPlayer.class, method="update")
    public static class update {
        @SpirePostfixPatch
        public static void postfix(AbstractPlayer _inst) {
            if (animTimer > 0.0f) {
                animTimer -= Gdx.graphics.getDeltaTime();
            }
        }
    }

    @SpirePatch(clz=Ironclad.class, method="damage")
    public static class damage {
        @SpireInsertPatch(rloc=1)
        public static void postfix(Ironclad _inst, DamageInfo info) {
            if (animTimer <= 0.0f) {
                animTimer = defaultAnimTimer;
            }
        }
    }

    @SpirePatch(clz=AbstractPlayer.class, method="useCard")
    public static class useCard {
        @SpirePostfixPatch
        public static void postfix(AbstractPlayer _inst, AbstractCard c, AbstractMonster monster, int energyOnUse) {
            if (_inst instanceof Ironclad && FeminizationUtil.ironcladId.equals("ironclad_lily")) {
                block0 : switch (c.type) {
                    case ATTACK: {
                        switch (c.cost) {
                            case 0: 
                            case 1: {
                                AnimationPatch.changeAnim(_inst, "attack1");
                                break block0;
                            }
                            case 2: {
                                AnimationPatch.changeAnim(_inst, "attack_rush");
                                break block0;
                            }
                        }
                        AnimationPatch.changeAnim(_inst, "attack123");
                        break;
                    }
                    case SKILL: {
                        AnimationPatch.changeAnim(_inst, "attack_ranged");
                        break;
                    }
                    case POWER: {
                        AnimationPatch.changeAnim(_inst, "buff");
                    }
                }
            } else if (_inst instanceof Watcher && FeminizationUtil.watcherId.equals("watcher_succubus_nun")) {
                block9 : switch (c.type) {
                    case ATTACK: {
                        switch (c.cost) {
                            case 0: 
                            case 1: {
                                AnimationPatch.changeAnim(_inst, "attack_ranged");
                                break block9;
                            }
                            case 2: {
                                AnimationPatch.changeAnim(_inst, "attack_artillery");
                                break block9;
                            }
                        }
                        AnimationPatch.changeAnim(_inst, "attack_point");
                        break;
                    }
                    case SKILL: {
                        AnimationPatch.changeAnim(_inst, "attack_bandage");
                    }
                }
            }
        }
    }
}

