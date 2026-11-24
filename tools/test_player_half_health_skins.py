#!/usr/bin/env python3
"""Exercise the player damage/heal patches headlessly with the real Spine runtime.

Usage: python3 tools/test_player_half_health_skins.py --game-root ../SlayTheAmethystModded
"""

import argparse
import os
from pathlib import Path
import subprocess
import tempfile

from test_resource_lifecycle import ROOT, dependencies, write_stubs


HARNESS = r'''
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.SkeletonData;
import com.esotericsoftware.spine.Skin;
import com.megacrit.cardcrawl.characters.*;
import feminization.FeminizationMod;
import feminization.patches.*;
import feminization.utils.CharacterSelectionInfo;
import feminization.utils.FeminizationUtil;
import java.util.List;

public class PlayerHalfHealthRegression {
    interface Hook { void run() throws Exception; }

    static Skeleton skeleton(String... names) {
        SkeletonData data = new SkeletonData();
        for (String name : names) data.getSkins().add(new Skin(name));
        Skeleton skeleton = new Skeleton(data);
        skeleton.setSkin(names[0]);
        return skeleton;
    }

    static void expect(AbstractPlayer player, String name) {
        if (!name.equals(player.skeleton.getSkin().getName())) {
            throw new AssertionError(player.getClass().getSimpleName() + " should keep or switch to " + name
                + ", got " + player.skeleton.getSkin().getName());
        }
    }

    static void exercise(AbstractPlayer player, List<CharacterSelectionInfo> selections, Hook damage, Hook heal)
            throws Exception {
        selections.add(new CharacterSelectionInfo.Builder().canHalfHealth(true).create());
        FeminizationMod.activeH = true;
        player.maxHealth = 80;
        player.currentHealth = 40;
        damage.run(); // No Spine model.
        player.skeleton = skeleton("custom", "default"); // Replacement lacks the damaged skin.
        damage.run();
        expect(player, "custom");
        player.currentHealth = 41;
        heal.run();
        expect(player, "custom"); // Do not reset another mod's skin just because it has a default.

        player.skeleton = skeleton("custom", "damaged"); // Cannot switch back on heal.
        heal.run();
        player.currentHealth = 40;
        damage.run();
        expect(player, "custom");

        player.skeleton = skeleton("default", "damaged");
        FeminizationMod.activeH = false;
        damage.run();
        expect(player, "default");
        FeminizationMod.activeH = true;
        damage.run();
        expect(player, "damaged"); // Exact half health.
        heal.run();
        expect(player, "damaged");
        player.currentHealth = 41;
        heal.run();
        expect(player, "default");
    }

    public static void main(String[] args) throws Exception {
        Ironclad ironclad = new Ironclad();
        exercise(ironclad, FeminizationUtil.ironcladSelections,
            () -> IroncladPatch.damage.postfix(ironclad, null), () -> IroncladPatch.heal.postfix(ironclad, 1));
        TheSilent silent = new TheSilent();
        exercise(silent, FeminizationUtil.silentSelections,
            () -> TheSilentPatch.damage2.postfix(silent, null), () -> TheSilentPatch.heal.postfix(silent, 1));
        Defect defect = new Defect();
        exercise(defect, FeminizationUtil.defectSelections,
            () -> DefectPatch.damage2.postfix(defect, null), () -> DefectPatch.heal.postfix(defect, 1));
        Watcher watcher = new Watcher();
        exercise(watcher, FeminizationUtil.watcherSelections,
            () -> WatcherPatch.damage2.postfix(watcher, null), () -> WatcherPatch.heal.postfix(watcher, 1));
        System.out.println("PASS player half-health skins: missing skin pairs, replaced skeletons, NSFW toggle and HP threshold");
    }
}
'''


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--game-root", required=True, type=Path)
    game = parser.parse_args().game_root.resolve()
    desktop, mts = dependencies(game)
    base = game / "app/src/main/assets/components/mods/BaseMod.jar"
    classpath = os.pathsep.join(map(str, (mts, base, desktop)))
    with tempfile.TemporaryDirectory(prefix="player-half-health-skins-") as folder:
        folder = Path(folder)
        sources = write_stubs(folder, {
            "feminization.FeminizationMod": 'public static final String MODNAME = "Feminization"; public static boolean activeH;',
            "com.megacrit.cardcrawl.core.AbstractCreature": '''
                public int currentHealth, maxHealth;
                public com.esotericsoftware.spine.Skeleton skeleton;
            ''',
            "com.megacrit.cardcrawl.characters.AbstractPlayer": "",
            "com.megacrit.cardcrawl.characters.Watcher": "",
        })
        for name in ("Ironclad", "TheSilent", "Defect"):
            stub = folder / f"com/megacrit/cardcrawl/characters/{name}.java"
            stub.write_text(f"package com.megacrit.cardcrawl.characters; public class {name} extends AbstractPlayer {{}}")
            sources.append(str(stub))
        harness = folder / "PlayerHalfHealthRegression.java"
        harness.write_text(HARNESS)
        sources.extend(str(ROOT / "src/main/java/feminization" / name) for name in (
            "utils/CharacterSelectionInfo.java", "utils/FeminizationUtil.java", "patches/IroncladPatch.java",
            "patches/TheSilentPatch.java", "patches/DefectPatch.java", "patches/WatcherPatch.java",
        ))
        subprocess.run(["javac", "-proc:none", "-cp", classpath, "-d", str(folder), *sources, str(harness)], check=True)
        subprocess.run(["java", "-cp", f"{folder}{os.pathsep}{classpath}", "PlayerHalfHealthRegression"], check=True)


if __name__ == "__main__":
    main()
