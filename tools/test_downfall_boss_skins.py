#!/usr/bin/env python3
"""Exercise the optional boss patch without starting Slay the Spire or installing Downfall.

Usage: python3 tools/test_downfall_boss_skins.py --game-root "$STS_ROOT"
Only the game/MTS compile dependencies are needed. Minimal creature stubs avoid game
static initializers and OpenGL; the actual patch and registered skin paths are tested.
"""

import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile
import textwrap


ROOT = Path(__file__).resolve().parents[1]
BOSSES = {
    "ironclad": "charbosses.bosses.Ironclad.CharBossIronclad",
    "silent": "charbosses.bosses.Silent.CharBossSilent",
    "defect": "charbosses.bosses.Defect.CharBossDefect",
    "watcher": "charbosses.bosses.Watcher.CharBossWatcher",
}


def registered_skins():
    skins = {key: [] for key in BOSSES}
    for name in ("feminization/helper/FeminizationHelper.java",
                 "extraFeminization/helper/ExtraFeminizationHelper.java"):
        source = (ROOT / "src/main/java" / name).read_text()
        for line in source.splitlines():
            field = re.search(r"FeminizationUtil\.(\w+)Selections\.add\(", line)
            atlas = re.search(r'\.atlas\("([^"]+)"\)', line)
            skeleton = re.search(r'\.json\("([^"]+)"\)', line)
            if field and atlas and skeleton:
                skins[field[1]].append((atlas[1], skeleton[1]))
    for role, entries in skins.items():
        assert entries, f"No registered skins for {role}"
        for atlas, skeleton in entries:
            atlas_file = ROOT / "src/main/resources" / atlas
            skeleton_file = ROOT / "src/main/resources" / skeleton
            assert atlas_file.is_file(), atlas
            data = json.loads(skeleton_file.read_text())
            assert {"Idle", "Hit"} <= data["animations"].keys(), skeleton
            if role == "watcher":
                assert any(bone["name"] == "eye_anchor" for bone in data["bones"]), skeleton
            # Every atlas page must be packaged beside the atlas.
            for page in re.findall(r"(?:^|\n\s*\n)([^\s\n]+\.png)\s*\n", atlas_file.read_text()):
                assert (atlas_file.parent / page).is_file(), page
    return skins


HARNESS = r'''
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import downfallFeminization.DownfallBossSkinPatches;
import downfallFeminization.DownfallCompatibilityPatches;
import feminization.utils.CharacterSelectionInfo;
import feminization.utils.FeminizationUtil;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;
import javassist.ClassPool;
import javassist.CtClass;

public class BossSkinRegression {
    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    static String base(String role) {
        return "images/characters/" + (role.equals("silent") ? "theSilent" : role) + "/idle/skeleton";
    }
    static String load(AbstractCreature creature, String role) {
        return loadBody(creature, base(role));
    }
    static String loadBody(AbstractCreature creature, String animationBase) {
        return loadPair(creature, animationBase + ".atlas", animationBase + ".json");
    }
    static String loadPair(AbstractCreature creature, String originalAtlas, String originalJson) {
        String[] atlas = {originalAtlas};
        String[] json = {originalJson};
        float[] scale = {2.0f};
        DownfallBossSkinPatches.BossAnimationPatch.prefix(creature, atlas, json, scale);
        if (!atlas[0].equals(originalAtlas)) {
            check(new File("src/main/resources", atlas[0]).isFile(), "missing atlas");
            check(new File("src/main/resources", json[0]).isFile(), "missing json");
            check(scale[0] == 1.0f, "incorrect skin scale");
        } else {
            check(json[0].equals(originalJson), "fallback changed json");
            check(scale[0] == 2.0f, "fallback changed scale");
        }
        return atlas[0];
    }
    public static void main(String[] args) throws Exception {
        Gdx.files = (Files)Proxy.newProxyInstance(Files.class.getClassLoader(), new Class[]{Files.class},
            (proxy, method, values) -> {
                if (method.getName().equals("internal"))
                    return new FileHandle(new File("src/main/resources", (String)values[0]));
                throw new UnsupportedOperationException(method.getName());
            });
        Field random = DownfallBossSkinPatches.class.getDeclaredField("SKIN_RANDOM");
        random.setAccessible(true);
        ((Random)random.get(null)).setSeed(123456L);
        // REGISTRATIONS
        String[] roles = {"ironclad", "silent", "defect", "watcher"};
        String[] classes = {"charbosses.bosses.Ironclad.CharBossIronclad",
            "charbosses.bosses.Silent.CharBossSilent", "charbosses.bosses.Defect.CharBossDefect",
            "charbosses.bosses.Watcher.CharBossWatcher"};
        List<CharacterSelectionInfo>[] lists = new List[]{FeminizationUtil.ironcladSelections,
            FeminizationUtil.silentSelections, FeminizationUtil.defectSelections, FeminizationUtil.watcherSelections};
        for (int i = 0; i < roles.length; i++) {
            List<CharacterSelectionInfo> entries = lists[i];
            Set<String> expected = new HashSet<>();
            for (CharacterSelectionInfo skin : entries) expected.add(skin.atlas);
            entries.add(0, new CharacterSelectionInfo.Builder().name("vanilla").create());
            entries.add(new CharacterSelectionInfo.Builder().atlas("missing.atlas").json("missing.json").create());
            entries.add(null);
            Set<String> seen = new HashSet<>();
            for (int j = 0; j < 128; j++) {
                AbstractCreature boss = (AbstractCreature)Class.forName(classes[i]).getDeclaredConstructor().newInstance();
                String skin = load(boss, roles[i]);
                check(expected.contains(skin), "not a feminized skin for " + roles[i]);
                check(skin.equals(load(boss, roles[i])), "skin rerolled for same boss");
                seen.add(skin);
            }
            check(seen.equals(expected), "some skins never selected: " + roles[i]);
            System.out.println("PASS " + roles[i] + ": " + expected.size() + " skins, stable per boss");
        }
        // Downfall archetypes replace the body again during initialize()/addedPreBattle(), via reflection.
        // These are the real paths used by both the NewAge and Simpler archetypes.
        String[][] variants = {
            {"silent", classes[1], "expansioncontentResources/images/bosses/silent/1/Poison_Silent"},
            {"defect", classes[2], "expansioncontentResources/images/bosses/defect/1/Defect_thief"},
            {"defect", classes[2], "expansioncontentResources/images/bosses/defect/2/clockworkDefect"},
            // The mushroom body is disabled upstream now, but used by older Downfall versions.
            {"ironclad", classes[0], "expansioncontentResources/images/bosses/ironclad/2/Mushroomskeleton"}
        };
        for (String[] variant : variants) {
            AbstractCreature boss = (AbstractCreature)Class.forName(variant[1]).getDeclaredConstructor().newInstance();
            String chosen = load(boss, variant[0]);
            check(chosen.equals(loadBody(boss, variant[2])), "archetype overwrote feminized skin: " + variant[2]);
            check(chosen.equals(loadBody(boss, variant[2])), "archetype rerolled skin: " + variant[2]);
            AbstractCreature fresh = (AbstractCreature)Class.forName(variant[1]).getDeclaredConstructor().newInstance();
            String first = loadBody(fresh, variant[2]);
            check(!first.equals(variant[2] + ".atlas"), "initial variant body was not feminized");
            check(first.equals(load(fresh, variant[0])), "variant to vanilla reload rerolled skin");
            check(loadBody(new AbstractMonster(), variant[2]).equals(variant[2] + ".atlas"), "variant path changed unrelated monster");
            DownfallCompatibilityPatches.activeFeminization = false;
            check(loadBody(boss, variant[2]).equals(variant[2] + ".atlas"), "disabled variant changed");
            DownfallCompatibilityPatches.activeFeminization = true;
            System.out.println("PASS pre-battle variant: " + variant[2]);
        }
        AbstractCreature protectedWatcher = new charbosses.bosses.Watcher.CharBossWatcher();
        load(protectedWatcher, "watcher");
        String eyeBase = "images/characters/watcher/eye_anim/skeleton";
        check(loadBody(protectedWatcher, eyeBase).equals(eyeBase + ".atlas"), "body patch changed eye effects");
        String foreignBase = "anotherModResources/characters/skin";
        check(loadBody(protectedWatcher, foreignBase).equals(foreignBase + ".atlas"), "body patch changed unrelated animation");
        String mismatchedAtlas = variants[0][2] + ".atlas";
        check(loadPair(new charbosses.bosses.Silent.CharBossSilent(), mismatchedAtlas, base("silent") + ".json")
            .equals(mismatchedAtlas), "mismatched animation pair changed");
        AbstractMonster silent = new charbosses.bosses.Silent.CharBossSilent();
        String silentSkin = load(silent, "silent");
        check(silentSkin.equals(loadBody(silent, variants[0][2])), "Silent variant changed cached skin");
        AbstractDungeon.room.monsters.monsters.add(silent);
        check(silentSkin.equals(load(new charbosses.monsters.MirrorImageSilent(), "silent")), "mirror reveals boss");
        check(load(new AbstractMonster(), "ironclad").equals(base("ironclad") + ".atlas"), "unrelated monster changed");
        AbstractCreature player = new charbosses.bosses.Ironclad.CharBossIronclad();
        player.isPlayer = true;
        check(load(player, "ironclad").equals(base("ironclad") + ".atlas"), "player changed");
        DownfallCompatibilityPatches.activeFeminization = false;
        check(load(silent, "silent").equals(base("silent") + ".atlas"), "disabled cached boss changed");
        check(load(new charbosses.bosses.Defect.CharBossDefect(), "defect").equals(base("defect") + ".atlas"), "disabled boss changed");
        DownfallCompatibilityPatches.activeFeminization = true;
        List<CharacterSelectionInfo> backup = new ArrayList<>(FeminizationUtil.defectSelections);
        FeminizationUtil.defectSelections.clear();
        check(load(new charbosses.bosses.Defect.CharBossDefect(), "defect").equals(base("defect") + ".atlas"), "empty registry did not fall back");
        FeminizationUtil.defectSelections.add(new CharacterSelectionInfo.Builder().atlas("missing.atlas").json("missing.json").create());
        check(load(new charbosses.bosses.Defect.CharBossDefect(), "defect").equals(base("defect") + ".atlas"), "missing resources did not fall back");
        FeminizationUtil.defectSelections.clear();
        FeminizationUtil.defectSelections.addAll(backup);
        check(FeminizationUtil.ironcladSelected == 7 && FeminizationUtil.silentSelected == 8
            && FeminizationUtil.defectSelected == 9 && FeminizationUtil.watcherSelected == 10, "player preferences changed");
        System.out.println("PASS mirror, disabled toggle, unrelated creatures, empty registry, player preferences");

        // Apply the real Javassist editor to a synthetic Watcher method, then execute its bytecode.
        ClassPool pool = ClassPool.getDefault();
        CtClass watcher = pool.get("charbosses.bosses.Watcher.CharBossWatcher");
        watcher.getDeclaredMethod("loadEyeAnimation").instrument(DownfallBossSkinPatches.WatcherEyeAnimationPatch.Instrument());
        byte[] bytes = watcher.toBytecode();
        Class<?> patched = new ClassLoader(BossSkinRegression.class.getClassLoader()) {
            Class<?> loadPatched() { return defineClass(watcher.getName(), bytes, 0, bytes.length); }
        }.loadPatched();
        for (CharacterSelectionInfo skin : FeminizationUtil.watcherSelections) {
            if (skin == null || skin.atlas == null || skin.atlas.equals("missing.atlas")) continue;
            Object boss = patched.getDeclaredConstructor().newInstance();
            Field map = DownfallBossSkinPatches.class.getDeclaredField("BOSS_SKINS");
            map.setAccessible(true);
            ((Map)map.get(null)).put(boss, skin);
            patched.getMethod("loadEyeAnimation").invoke(boss);
            String original = "images/characters/watcher/eye_anim/skeleton";
            String expected = skin.json.startsWith("feminizationResources/")
                ? "feminizationResources/images/characters/watcher-2/eye_anim/skeleton" : original;
            check(patched.getField("eyeAtlasPath").get(boss).toString().endsWith(expected + ".atlas"), "wrong eye atlas");
            check(patched.getField("eyeJsonPath").get(boss).toString().endsWith(expected + ".json"), "wrong eye json");
            DownfallCompatibilityPatches.activeFeminization = false;
            patched.getMethod("loadEyeAnimation").invoke(boss);
            check(patched.getField("eyeAtlasPath").get(boss).toString().endsWith(original + ".atlas"), "disabled eye redirect");
            DownfallCompatibilityPatches.activeFeminization = true;
        }
        System.out.println("PASS Watcher eye resource bytecode patch for all skins and disabled toggle");
    }
}
'''


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--game-root", required=True, type=Path)
    args = parser.parse_args()
    game = args.game_root.resolve()
    desktop = next((game / name for name in ("tools/desktop-1.0.jar", "build-deps/desktop.jar",
                   "build-deps/steamapps/common/SlayTheSpire/desktop-1.0.jar") if (game / name).is_file()), None)
    mts = game / "app/src/main/assets/components/mods/ModTheSpire.jar"
    if desktop is None or not mts.is_file():
        parser.error("game root must contain the desktop game jar and ModTheSpire.jar")
    skins = registered_skins()
    stubs = {
        "com.megacrit.cardcrawl.core.AbstractCreature": "public boolean isPlayer;",
        "com.megacrit.cardcrawl.characters.AbstractPlayer": "",
        "com.megacrit.cardcrawl.monsters.AbstractMonster": "",
        "com.megacrit.cardcrawl.monsters.MonsterGroup": "public java.util.ArrayList<AbstractMonster> monsters = new java.util.ArrayList<>();",
        "com.megacrit.cardcrawl.rooms.AbstractRoom": "public com.megacrit.cardcrawl.monsters.MonsterGroup monsters = new com.megacrit.cardcrawl.monsters.MonsterGroup();",
        "com.megacrit.cardcrawl.dungeons.AbstractDungeon": "public static com.megacrit.cardcrawl.rooms.AbstractRoom room = new com.megacrit.cardcrawl.rooms.AbstractRoom(); public static com.megacrit.cardcrawl.rooms.AbstractRoom getCurrRoom() { return room; }",
        "downfallFeminization.DownfallCompatibilityPatches": "public static boolean activeFeminization = true;",
        "feminization.utils.FeminizationUtil": "public static int ironcladSelected=7, silentSelected=8, defectSelected=9, watcherSelected=10; " + " ".join(
            f"public static java.util.ArrayList<CharacterSelectionInfo> {role}Selections = new java.util.ArrayList<>();" for role in BOSSES),
    }
    for class_name in [*BOSSES.values(), "charbosses.monsters.MirrorImageSilent"]:
        stubs[class_name] = ""
    stubs[BOSSES["watcher"]] = '''
        public String eyeAtlasPath, eyeJsonPath;
        public void loadEyeAnimation() {
            eyeAtlasPath = com.badlogic.gdx.Gdx.files.internal("images/characters/watcher/eye_anim/skeleton.atlas").path();
            eyeJsonPath = com.badlogic.gdx.Gdx.files.internal("images/characters/watcher/eye_anim/skeleton.json").path();
        }
    '''
    registrations = "\n".join(
        f'FeminizationUtil.{role}Selections.add(new CharacterSelectionInfo.Builder().atlas("{atlas}").json("{skeleton}").create());'
        for role, entries in skins.items() for atlas, skeleton in entries)
    harness = HARNESS.replace("// REGISTRATIONS", registrations)
    build = ROOT / "build"
    build.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="boss-skin-tests-", dir=build) as folder:
        folder = Path(folder)
        sources = []
        for name, body in stubs.items():
            package, _, simple = name.rpartition(".")
            parent = ""
            if name in [*BOSSES.values(), "charbosses.monsters.MirrorImageSilent"]:
                parent = " extends com.megacrit.cardcrawl.monsters.AbstractMonster"
            elif simple in ("AbstractMonster", "AbstractPlayer"):
                parent = " extends com.megacrit.cardcrawl.core.AbstractCreature"
            path = folder / (name.replace(".", "/") + ".java")
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(f"package {package}; public class {simple}{parent} {{ {textwrap.dedent(body)} }}")
            sources.append(str(path))
        main_file = folder / "BossSkinRegression.java"
        main_file.write_text(harness)
        sources.extend([str(main_file), str(ROOT / "src/main/java/downfallFeminization/DownfallBossSkinPatches.java"),
                        str(ROOT / "src/main/java/feminization/utils/CharacterSelectionInfo.java")])
        classpath = os.pathsep.join(map(str, (mts, desktop)))
        subprocess.run(["javac", "-proc:none", "-cp", classpath, "-d", str(folder), *sources], cwd=ROOT, check=True)
        subprocess.run(["java", "-cp", f"{folder}{os.pathsep}{classpath}", "BossSkinRegression"], cwd=ROOT, check=True)


if __name__ == "__main__":
    main()
