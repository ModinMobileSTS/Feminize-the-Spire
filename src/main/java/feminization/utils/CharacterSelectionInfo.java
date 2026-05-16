/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.esotericsoftware.spine.AnimationStateData
 *  com.megacrit.cardcrawl.characters.AbstractPlayer
 */
package feminization.utils;

import com.esotericsoftware.spine.AnimationStateData;
import com.megacrit.cardcrawl.characters.AbstractPlayer;

public class CharacterSelectionInfo {
    public String id = "";
    public String name;
    public String bgCharImg;
    public String bgCharImgNsfw;
    public String shoulderImg;
    public String shoulder2Img;
    public String corpseImg;
    public String atlas;
    public String json;
    public boolean canHalfHealth;
    public CharacterRunnable customAnim = null;

    public static class CharacterRunnable
    implements Runnable {
        public AbstractPlayer character;
        public AnimationStateData stateData;

        @Override
        public void run() {
        }
    }

    public static class Builder {
        CharacterSelectionInfo info = new CharacterSelectionInfo();

        public CharacterSelectionInfo create() {
            return this.info;
        }

        public Builder id(String id) {
            this.info.id = id;
            return this;
        }

        public Builder name(String name) {
            this.info.name = name;
            return this;
        }

        public Builder bgCharImg(String path) {
            this.info.bgCharImg = path;
            return this;
        }

        public Builder bgCharImgNsfw(String path) {
            this.info.bgCharImgNsfw = path;
            return this;
        }

        public Builder shoulderImg(String path) {
            this.info.shoulderImg = path;
            return this;
        }

        public Builder shoulder2Img(String path) {
            this.info.shoulder2Img = path;
            return this;
        }

        public Builder corpseImg(String path) {
            this.info.corpseImg = path;
            return this;
        }

        public Builder atlas(String path) {
            this.info.atlas = path;
            return this;
        }

        public Builder json(String path) {
            this.info.json = path;
            return this;
        }

        public Builder canHalfHealth(boolean can) {
            this.info.canHalfHealth = can;
            return this;
        }

        public Builder customAnim(CharacterRunnable anim) {
            this.info.customAnim = anim;
            return this;
        }
    }
}

