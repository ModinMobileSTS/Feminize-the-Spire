#!/usr/bin/env python3
"""Test Downfall skin registration, saved choices and Spine assets without entering the game.

Requires a compiled mod: ./gradlew classes
Usage: python3 tools/test_downfall_player_skins.py --game-root ../SlayTheAmethystModded
"""

import argparse
import os
from pathlib import Path
import subprocess
import tempfile

from test_resource_lifecycle import ROOT, TEXTURE_STUB, dependencies, write_stubs


HARNESS = r'''
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.esotericsoftware.spine.*;
import downfallFeminization.DownfallCompatibilityPatches;
import downfallFeminization.DownfallCompatibilityPatches.DefaultSkinPatch;
import feminization.utils.ResourceUtil;
import java.io.File;
import java.lang.reflect.Proxy;
import reskinContent.patches.CharacterSelectScreenPatches;
import reskinContent.patches.CharacterSelectScreenPatches.Character;
import reskinContent.patches.CharacterSelectScreenPatches.Skin;
import reskinContent.reskinContent;

public class DownfallPlayerSkinRegression {
    static final String PREFIX = "feminizethespire/downfall_overrides/";

    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    static void loadSettings() {
        DefaultSkinPatch.prefix();
        reskinContent.loadSettings();
        DefaultSkinPatch.postfix();
    }

    static void checkBody(Character character, String path, String animationName) {
        Skin skin = character.skins[character.reskinCount];
        check(skin != null && (PREFIX + path + ".atlas").equals(skin.atlasURL), "wrong battle skin");
        check((PREFIX + path + ".json").equals(skin.jsonURL), "battle atlas and JSON are not paired");
        check(skin.portraitStatic_IMG != null && skin.portraitAtlasPath == null && skin.portraitAnimationType == 0
            && skin.forcePortraitAnimationType, "embedded portrait is not static");

        TextureAtlas atlas = ResourceUtil.loadAtlas(skin.atlasURL);
        try {
            SkeletonData data = new SkeletonJson(atlas).readSkeletonData(Gdx.files.internal(skin.jsonURL));
            Skeleton skeleton = new Skeleton(data);
            AnimationState state = new AnimationState(new AnimationStateData(data));
            state.setAnimation(0, animationName, true);
            state.update(0.1f);
            state.apply(skeleton);
            skeleton.updateWorldTransform();
        } finally {
            ResourceUtil.disposeAtlas(atlas);
        }
    }

    public static void main(String[] args) {
        Gdx.files = (Files)Proxy.newProxyInstance(Files.class.getClassLoader(), new Class[]{Files.class},
            (proxy, method, values) -> {
                if (method.getName().equals("internal")) return new FileHandle(new File("src/main/resources", (String)values[0]));
                throw new UnsupportedOperationException(method.getName());
            });
        Character nativeCharacter = CharacterSelectScreenPatches.characters[0];
        Character slime = CharacterSelectScreenPatches.characters[1];
        Character hexa = CharacterSelectScreenPatches.characters[2];
        Skin nativeSkin = slime.skins[0];
        DownfallCompatibilityPatches.activeFeminization = false;
        loadSettings();
        check(slime.skins.length == 1 && hexa.skins.length == 1 && reskinContent.saves == 0, "disabled patch changed skin settings");

        // Restart with a saved index that doesn't exist in Downfall's initial skin array.
        DownfallCompatibilityPatches.activeFeminization = true;
        reskinContent.indices = new int[]{1, 1, 1};
        loadSettings();
        check(slime.skins.length == 2 && hexa.skins.length == 2, "embedded skins not registered");
        check(slime.skins[0] == nativeSkin && slime.reskinCount == 1 && hexa.reskinCount == 1 && nativeCharacter.reskinCount == 1,
            "load clamped a valid saved choice or changed native skin order");
        check(slime.reskinUnlock && hexa.reskinUnlock && nativeCharacter.reskinUnlock, "skin controls left locked");
        checkBody(slime, "slimeboundResources/SlimeboundImages/char/skeleton", "idle");
        checkBody(hexa, "downfallFeminizationResources/images/hexaghost/liuhuo", "animation");

        reskinContent.unlock = true;
        loadSettings(); // A clean scan could otherwise cache the unlock result forever.
        Skin embedded = hexa.skins[1];
        reskinContent.unlock = false;
        reskinContent.indices = new int[]{0, 0, 1};
        loadSettings();
        check(slime.reskinCount == 0 && hexa.reskinCount == 1 && nativeCharacter.reskinCount == 0,
            "reload did not respect new save-slot choices");
        check(slime.reskinUnlock && hexa.reskinUnlock && nativeCharacter.reskinUnlock, "reloaded unlocks were not restored");
        check(hexa.skins.length == 2 && hexa.skins[1] == embedded, "reloading duplicated the embedded skin");
        System.out.println("PASS Downfall saved player skins, native choices, reload unlocks and actual combat models");
    }
}
'''


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--game-root", required=True, type=Path)
    args = parser.parse_args()
    game = args.game_root.resolve()
    desktop, mts = dependencies(game)
    base = game / "app/src/main/assets/components/mods/BaseMod.jar"
    classpath = os.pathsep.join(map(str, (ROOT / "build/classes/java/main", mts, base, desktop)))
    stubs = {
        "com.badlogic.gdx.graphics.Texture": TEXTURE_STUB,
        "com.megacrit.cardcrawl.core.AbstractCreature": '''
            protected void loadAnimation(String atlas, String json, float scale) {}
        ''',
        "reskinContent.patches.CharacterSelectScreenPatches": '''
            public static class Skin {
                public String NAME, DESCRIPTION, atlasURL, jsonURL, portraitAtlasPath;
                public float renderscale;
                public int portraitAnimationType;
                public boolean forcePortraitAnimationType;
                public com.badlogic.gdx.graphics.Texture portraitStatic_IMG, portraitAnimation_IMG;
            }
            public static class Character {
                public boolean reskinUnlock;
                public int reskinCount;
                public Skin[] skins;
                Character(Skin... skins) { this.skins = skins; }
            }
            public static Character[] characters = {
                new Character(new Skin(), new Skin()), new Character(new Skin()), new Character(new Skin())
            };
        ''',
    }
    with tempfile.TemporaryDirectory(prefix="downfall-player-skins-", dir=ROOT / "build") as folder:
        folder = Path(folder)
        sources = write_stubs(folder, stubs)
        for name in ("Slimebound.Slaifu", "Hexaghost.HexaghostOriginal"):
            package, _, simple = ("reskinContent.skinCharacter.skins." + name).rpartition(".")
            path = folder / package.replace(".", "/") / (simple + ".java")
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(f"package {package}; public class {simple} extends reskinContent.patches.CharacterSelectScreenPatches.Skin {{}}")
            sources.append(str(path))
        settings = folder / "reskinContent/reskinContent.java"
        settings.write_text('''
            package reskinContent;
            import reskinContent.patches.CharacterSelectScreenPatches;
            import reskinContent.patches.CharacterSelectScreenPatches.Character;

            public class reskinContent {
                public static int[] indices = {0, 0, 0};
                public static boolean unlock;
                public static int saves;
                public static void loadSettings() {
                    Character[] characters = CharacterSelectScreenPatches.characters;
                    for (int i = 0; i < characters.length; i++) {
                        Character character = characters[i];
                        character.reskinUnlock = unlock;
                        character.reskinCount = indices[i];
                        // No saved portrait animation-type key for the embedded skin on first load.
                        if (i > 0 && character.skins.length > 1 && !character.skins[1].forcePortraitAnimationType) {
                            throw new NumberFormatException("missing embedded portrait animation key");
                        }
                        if (character.reskinCount >= character.skins.length) character.reskinCount = 0;
                    }
                }
                public static void saveSettings() { saves++; }
            }
        ''')
        sources.append(str(settings))
        harness = folder / "DownfallPlayerSkinRegression.java"
        harness.write_text(HARNESS)
        subprocess.run(["javac", "-proc:none", "-cp", classpath, "-d", str(folder), *sources, str(harness)], cwd=ROOT, check=True)
        subprocess.run(["java", "-cp", f"{folder}{os.pathsep}{classpath}", "DownfallPlayerSkinRegression"], cwd=ROOT, check=True)


if __name__ == "__main__":
    main()
