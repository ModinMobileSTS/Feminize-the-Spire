#!/usr/bin/env python3
"""Test the merchant hand patch without starting the game or requiring OpenGL.

Usage: python3 tools/test_merchant_hand.py --game-root "$STS_ROOT" [--jar path/to/mod.jar]
"""

import argparse
import os
from pathlib import Path
import struct
import subprocess
import tempfile
import zipfile
from test_resource_lifecycle import TEXTURE_STUB


ROOT = Path(__file__).resolve().parents[1]
ORIGINAL = "images/npcs/merchantHand.png"
OVERRIDE = "feminizethespire/npc_overrides/" + ORIGINAL

HARNESS = r'''
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import javassist.*;
import javassist.expr.*;
import npcFeminization.NpcFeminizationMod;
import npcFeminization.patch.MerchantHandPatch;

public class MerchantHandRegression {
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static int helperCalls(CtMethod method) throws Exception {
        final int[] count = {0};
        method.instrument(new ExprEditor() {
            public void edit(MethodCall call) {
                if (call.getClassName().equals(MerchantHandPatch.class.getName())
                        && call.getMethodName().equals("getHandTexture")) count[0]++;
            }
        });
        return count[0];
    }

    public static void main(String[] args) throws Exception {
        Texture original = new Texture("vanilla");
        NpcFeminizationMod.activeFeminization = false;
        check(MerchantHandPatch.getHandTexture(original) == original, "disabled hand changed");
        check(ImageMaster.loads == 0, "disabled patch loaded a texture");

        NpcFeminizationMod.activeFeminization = true;
        ImageMaster.fail = true;
        check(MerchantHandPatch.getHandTexture(original) == original, "missing texture did not fall back");
        ImageMaster.fail = false;
        Texture replacement = MerchantHandPatch.getHandTexture(original);
        check(replacement != original, "enabled hand was not replaced");
        check(replacement.path.equals("feminizethespire/npc_overrides/images/npcs/merchantHand.png"), "wrong resource path");
        check(MerchantHandPatch.getHandTexture(original) == replacement, "texture was not cached");
        NpcFeminizationMod.activeH = true;
        check(MerchantHandPatch.getHandTexture(original) == replacement, "NSFW setting changed hand");
        NpcFeminizationMod.activeH = false;
        check(MerchantHandPatch.getHandTexture(original) == replacement, "SFW hand missing");
        NpcFeminizationMod.activeFeminization = false;
        check(MerchantHandPatch.getHandTexture(original) == original, "toggle did not restore vanilla");
        check(ImageMaster.loads == 2, "texture reloaded instead of being cached");
        check(!original.disposed && !replacement.disposed, "shared texture was disposed");
        System.out.println("PASS toggle, SFW/NSFW independence, cache, missing-resource fallback, texture ownership");

        // Validate the annotation and apply the real editor to the actual game bytecode.
        SpirePatch patch = MerchantHandPatch.class.getAnnotation(SpirePatch.class);
        check(patch.clz().getName().equals("com.megacrit.cardcrawl.shop.ShopScreen"), "wrong patch class");
        check(patch.method().equals("render") && patch.paramtypez().length == 1
            && patch.paramtypez()[0] == SpriteBatch.class, "wrong patch signature");
        ClassPool gamePool = new ClassPool(true);
        CtClass actual = gamePool.get(patch.clz().getName());
        CtMethod render = actual.getDeclaredMethod("render");
        check(helperCalls(render) == 0, "game method was already patched");
        render.instrument(MerchantHandPatch.Instrument());
        check(helperCalls(render) == 1, "game hand read was not patched exactly once");
        actual.toBytecode();
        System.out.println("PASS actual ShopScreen.render bytecode and patch signature");

        // Execute the editor on a tiny ShopScreen to avoid the game's static initializers/OpenGL.
        ClassPool pool = new ClassPool(true);
        CtClass screen = pool.makeClass("com.megacrit.cardcrawl.shop.ShopScreen");
        screen.addField(CtField.make("private static com.badlogic.gdx.graphics.Texture handImg;", screen));
        screen.addField(CtField.make("private static com.badlogic.gdx.graphics.Texture rugImg;", screen));
        CtMethod setter = CtNewMethod.make("public static void setImages(com.badlogic.gdx.graphics.Texture hand, "
            + "com.badlogic.gdx.graphics.Texture rug) { handImg = hand; rugImg = rug; }", screen);
        screen.addMethod(setter);
        CtMethod syntheticRender = CtNewMethod.make("public static com.badlogic.gdx.graphics.Texture[] render() { "
            + "return new com.badlogic.gdx.graphics.Texture[]{handImg, rugImg, merchantHandTest.ForeignHand.handImg}; }", screen);
        screen.addMethod(syntheticRender);
        setter.instrument(MerchantHandPatch.Instrument());
        syntheticRender.instrument(MerchantHandPatch.Instrument());
        check(helperCalls(setter) == 0, "field write was patched");
        check(helperCalls(syntheticRender) == 1, "unrelated field or class was patched");
        final byte[] bytes = screen.toBytecode();
        Class<?> patched = new ClassLoader(MerchantHandRegression.class.getClassLoader()) {
            Class<?> loadPatched() { return defineClass("com.megacrit.cardcrawl.shop.ShopScreen", bytes, 0, bytes.length); }
        }.loadPatched();
        Texture rug = new Texture("rug");
        Texture foreign = new Texture("foreign hand");
        merchantHandTest.ForeignHand.handImg = foreign;
        patched.getMethod("setImages", Texture.class, Texture.class).invoke(null, original, rug);
        for (boolean enabled : new boolean[]{true, false, true, false}) {
            NpcFeminizationMod.activeFeminization = enabled;
            Texture[] drawn = (Texture[])patched.getMethod("render").invoke(null);
            check(drawn[0] == (enabled ? replacement : original), "render did not follow toggle");
            check(drawn[1] == rug && drawn[2] == foreign, "unrelated texture changed");
        }
        check(ImageMaster.loads == 2, "render reloaded texture");
        check(!original.disposed && !replacement.disposed, "render disposed shared textures");
        System.out.println("PASS patched rendering, repeated live toggles, unrelated fields, original static preserved");
    }
}
'''


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--game-root", required=True, type=Path)
    parser.add_argument("--jar", type=Path)
    args = parser.parse_args()
    game = args.game_root.resolve()
    desktop = next((game / name for name in (
        "tools/desktop-1.0.jar", "build-deps/desktop.jar",
        "build-deps/steamapps/common/SlayTheSpire/desktop-1.0.jar"
    ) if (game / name).is_file()), None)
    mts = game / "app/src/main/assets/components/mods/ModTheSpire.jar"
    if desktop is None or not mts.is_file():
        parser.error("game root must contain the desktop game jar and ModTheSpire.jar")

    source = (ROOT / "src/main/resources" / ORIGINAL).read_bytes()
    override = (ROOT / "src/main/resources" / OVERRIDE).read_bytes()
    assert source == override, "hand texture differs from upstream resource"
    with zipfile.ZipFile(desktop) as jar:
        vanilla = jar.read(ORIGINAL)
    assert struct.unpack(">II", override[16:24]) == struct.unpack(">II", vanilla[16:24]), "hand dimensions changed"
    print("PASS upstream resource preserved and vanilla dimensions matched", flush=True)
    if args.jar:
        with zipfile.ZipFile(args.jar) as jar:
            assert jar.read(OVERRIDE) == override, "hand override missing or corrupted in jar"
            assert ORIGINAL not in jar.namelist(), "unconditional vanilla-path override packaged"
            assert "npcFeminization/patch/MerchantHandPatch.class" in jar.namelist(), "patch missing in jar"
        print("PASS jar contains patch and namespaced hand texture only", flush=True)

    stubs = {
        "npcFeminization.NpcFeminizationMod": "public static boolean activeFeminization = true, activeH;",
        "com.badlogic.gdx.graphics.Texture": TEXTURE_STUB,
        "feminization.FeminizationMod": "public static final org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger(\"merchant-test\");",
        "com.megacrit.cardcrawl.core.AbstractCreature": "",
        "com.megacrit.cardcrawl.helpers.ImageMaster": """
            public static int loads;
            public static boolean fail;
            public static com.badlogic.gdx.graphics.Texture loadImage(String path) {
                loads++;
                return fail ? null : new com.badlogic.gdx.graphics.Texture(path);
            }
        """,
        "merchantHandTest.ForeignHand": "public static com.badlogic.gdx.graphics.Texture handImg;",
    }
    build = ROOT / "build"
    build.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="merchant-hand-tests-", dir=build) as folder:
        folder = Path(folder)
        sources = []
        for name, body in stubs.items():
            package, _, simple = name.rpartition(".")
            path = folder / (name.replace(".", "/") + ".java")
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(f"package {package}; public class {simple} {{ {body} }}")
            sources.append(str(path))
        main_file = folder / "MerchantHandRegression.java"
        main_file.write_text(HARNESS)
        sources.extend([str(main_file), str(ROOT / "src/main/java/npcFeminization/patch/MerchantHandPatch.java"),
                        str(ROOT / "src/main/java/feminization/utils/ResourceUtil.java")])
        classpath = os.pathsep.join(map(str, (mts, desktop)))
        subprocess.run(["javac", "-proc:none", "-cp", classpath, "-d", str(folder), *sources], cwd=ROOT, check=True)
        subprocess.run(["java", "-cp", f"{folder}{os.pathsep}{classpath}", "MerchantHandRegression"], cwd=ROOT, check=True)


if __name__ == "__main__":
    main()
