/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.badlogic.gdx.graphics.Color
 *  com.badlogic.gdx.graphics.g2d.BitmapFont
 *  com.badlogic.gdx.graphics.g2d.SpriteBatch
 *  com.megacrit.cardcrawl.core.CardCrawlGame
 *  com.megacrit.cardcrawl.core.Settings
 *  com.megacrit.cardcrawl.helpers.FontHelper
 *  com.megacrit.cardcrawl.helpers.Hitbox
 *  com.megacrit.cardcrawl.helpers.ImageMaster
 *  com.megacrit.cardcrawl.helpers.input.InputHelper
 *  com.megacrit.cardcrawl.screens.charSelect.CharacterOption
 */
package feminization.charSelect;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.screens.charSelect.CharacterOption;
import feminization.utils.CharacterSelectionInfo;
import feminization.utils.FeminizationUtil;
import feminization.utils.ResourceUtil;

public class IroncladChangePart {
    private static final Color HOVER_COLOR = new Color(1.0f, 1.0f, 1.0f, 0.5f);
    private float infoX = 200.0f * Settings.scale;
    private float infoY = (float)Settings.HEIGHT / 2.0f;
    private Hitbox lastBox = new Hitbox(80.0f * Settings.scale, 80.0f * Settings.scale);
    private Hitbox nextBox;
    private boolean changeImg = true;

    public IroncladChangePart() {
        this.lastBox.move(this.infoX, this.infoY + 272.0f * Settings.scale);
        this.nextBox = new Hitbox(80.0f * Settings.scale, 80.0f * Settings.scale);
        this.nextBox.move(this.infoX + 250.0f * Settings.scale, this.infoY + 272.0f * Settings.scale);
    }

    public void update(CharacterOption option) {
        this.updateButton(this.lastBox);
        this.updateButton(this.nextBox);
        if (this.lastBox.clicked) {
            this.lastBox.clicked = false;
            --FeminizationUtil.ironcladSelected;
            this.changeSelected(option);
        }
        if (this.nextBox.clicked) {
            this.nextBox.clicked = false;
            ++FeminizationUtil.ironcladSelected;
            this.changeSelected(option);
        }
        if (this.changeImg) {
            CharacterSelectionInfo info = FeminizationUtil.ironcladSelections.get(FeminizationUtil.ironcladSelected);
            if (info.bgCharImg != null) {
                CardCrawlGame.mainMenuScreen.charSelectScreen.bgCharImg = ResourceUtil.replaceTexture(CardCrawlGame.mainMenuScreen.charSelectScreen.bgCharImg, info.bgCharImg);
            }
            this.changeImg = false;
        }
    }

    private void updateButton(Hitbox button) {
        button.update();
        if (button.justHovered) {
            CardCrawlGame.sound.playA("UI_HOVER", -0.3f);
        }
        if (InputHelper.justClickedLeft && button.hovered) {
            CardCrawlGame.sound.playA("UI_CLICK_1", -0.4f);
            button.clickStarted = true;
        }
    }

    private void changeSelected(CharacterOption option) {
        int max = FeminizationUtil.ironcladSelections.size();
        if (FeminizationUtil.ironcladSelected < 0) {
            FeminizationUtil.ironcladSelected += max;
        }
        if (FeminizationUtil.ironcladSelected >= max) {
            FeminizationUtil.ironcladSelected -= max;
        }
        FeminizationUtil.saveSelectedSkins();
        CharacterSelectionInfo info = FeminizationUtil.ironcladSelections.get(FeminizationUtil.ironcladSelected);
        CardCrawlGame.mainMenuScreen.charSelectScreen.bgCharImg = ResourceUtil.replaceTexture(CardCrawlGame.mainMenuScreen.charSelectScreen.bgCharImg, info.bgCharImg != null ? info.bgCharImg : "images/ui/charSelect/ironcladPortrait.jpg");
        option.c.doCharSelectScreenSelectEffect();
    }

    public void disable() {
        this.changeImg = true;
    }

    public void render(SpriteBatch sb) {
        sb.draw(ImageMaster.POPUP_ARROW, this.lastBox.cX - 40.0f, this.lastBox.cY - 40.0f, 20.0f, 20.0f, 80.0f, 80.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 256, 256, false, false);
        if (this.lastBox.hovered) {
            sb.setBlendFunction(770, 1);
            sb.setColor(HOVER_COLOR);
            sb.draw(ImageMaster.POPUP_ARROW, this.lastBox.cX - 40.0f, this.lastBox.cY - 40.0f, 20.0f, 20.0f, 80.0f, 80.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 256, 256, false, false);
            sb.setColor(Color.WHITE);
            sb.setBlendFunction(770, 771);
        }
        FontHelper.renderSmartText((SpriteBatch)sb, (BitmapFont)FontHelper.buttonLabelFont, (String)FeminizationUtil.ironcladSelections.get((int)FeminizationUtil.ironcladSelected).name, (float)(this.infoX + 50.0f * Settings.scale), (float)(this.infoY + 280.0f * Settings.scale), (float)10000.0f, (float)(40.0f * Settings.scale), (Color)Settings.CREAM_COLOR, (float)1.0f);
        sb.draw(ImageMaster.POPUP_ARROW, this.nextBox.cX - 40.0f, this.nextBox.cY - 40.0f, 20.0f, 20.0f, 80.0f, 80.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 256, 256, true, false);
        if (this.nextBox.hovered) {
            sb.setBlendFunction(770, 1);
            sb.setColor(HOVER_COLOR);
            sb.draw(ImageMaster.POPUP_ARROW, this.nextBox.cX - 40.0f, this.nextBox.cY - 40.0f, 20.0f, 20.0f, 80.0f, 80.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 256, 256, true, false);
            sb.setColor(Color.WHITE);
            sb.setBlendFunction(770, 771);
        }
    }
}
