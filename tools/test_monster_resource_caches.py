#!/usr/bin/env python3
"""Test real Spine bone/gremlin caches headlessly, without Downfall installed.

Requires a compiled mod: ./gradlew classes
Usage: python3 tools/test_monster_resource_caches.py --game-root ../SlayTheAmethystModded
"""

import argparse
import os
from pathlib import Path
import subprocess
import tempfile

from test_resource_lifecycle import TEXTURE_STUB, dependencies, write_stubs


ROOT = Path(__file__).resolve().parents[1]

HARNESS = r'''
import com.badlogic.gdx.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.esotericsoftware.spine.*;
import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.*;
import javassist.*;

public class MonsterResourceRegression {
    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); return field;
    }
    static Skeleton skeleton() {
        SkeletonData data = new SkeletonData();
        data.getBones().add(new BoneData(0, "root", null));
        for (int i = 1; i <= 6; i++) data.getBones().add(new BoneData(i, "guihuo" + i, data.getBones().first()));
        return new Skeleton(data);
    }
    static WeakReference<Skeleton> populateBoneCache(Method method) throws Exception {
        Skeleton skeleton = skeleton();
        int[] indices = (int[])method.invoke(null, skeleton);
        check(indices.length == 6, "wrong number of flame bones");
        for (int i = 0; i < indices.length; i++) check(skeleton.getBones().get(indices[i]).getData().getName().equals("guihuo" + (i + 1)), "wrong cached bone index");
        check(indices == method.invoke(null, skeleton), "bone lookup not cached");
        return new WeakReference<>(skeleton);
    }
    static void bones() throws Exception {
        Class<?> type = Class.forName("monsterFeminization.patch.bottom.HexaghostPatch");
        Method method = type.getDeclaredMethod("getFireBones", Skeleton.class); method.setAccessible(true);
        check(method.invoke(null, new Object[]{null}) == null, "null skeleton not safe");
        check(method.invoke(null, new Skeleton(new SkeletonData())) == null, "missing bones not safe");
        WeakReference<Skeleton> weak = populateBoneCache(method);
        Map<?, ?> cache = (Map<?, ?>)field(type, "FIRE_BONES").get(null);
        // Weak-reference clearing and WeakHashMap's reference-queue drain need not be simultaneous.
        for (int i = 0; i < 100 && (weak.get() != null || !cache.isEmpty()); i++) System.gc();
        check(weak.get() == null && cache.isEmpty(), "bone cache retains old Skeleton through its value");
        System.out.println("PASS real Spine flame-bone indices, cached lookup, null/missing bones, old skeleton GC");
    }
    static void gremlins() throws Exception {
        Class<?> type = Class.forName("downfallFeminization.DownfallCompatibilityPatches");
        field(type, "gremlinFeminizedSelected").setBoolean(null, true);
        Method clear = type.getDeclaredMethod("clearGremlinSkeletonCaches"); clear.setAccessible(true);
        Map<String, Object> nativeSkeletons = gremlin.GremlinMod.gremlinSkeletonMap;
        Map<String, Object> nativeStates = gremlin.GremlinMod.gremlinAnimationStateMap;
        Skeleton original = skeleton();
        AnimationState originalState = new AnimationState(new AnimationStateData(original.getData()));
        nativeSkeletons.put("angry", original); nativeStates.put("angry", originalState);
        Object foreign = new Object(); nativeSkeletons.put("foreign", foreign); nativeStates.put("foreign", foreign);
        Method prefix = Class.forName(type.getName() + "$GremlinSkeletonPatch").getMethod("prefix", String.class);
        Map<?, ?> assets = (Map<?, ?>)field(type, "GREMLIN_ASSETS").get(null);
        for (int i = 0; i < 20; i++) {
            int before = Texture.all.size();
            prefix.invoke(null, "angry");
            check(nativeSkeletons.get("angry") != original && assets.size() == 1, "gremlin skin not installed");
            prefix.invoke(null, "angry");
            check(Texture.all.size() == before + 1, "gremlin cache reloaded texture");
            clear.invoke(null);
            check(nativeSkeletons.get("angry") == original && nativeStates.get("angry") == originalState, "native cache was not restored");
            check(nativeSkeletons.get("foreign") == foreign && nativeStates.get("foreign") == foreign, "foreign cache was cleared");
            check(assets.isEmpty() && Texture.all.get(before).disposals == 1, "gremlin cache leaked atlas");
        }
        prefix.invoke(null, "angry");
        nativeSkeletons.put("angry", foreign); nativeStates.put("angry", foreign);
        clear.invoke(null);
        check(nativeSkeletons.get("angry") == foreign && nativeStates.get("angry") == foreign, "third-party replacement overwritten");
        nativeSkeletons.remove("angry"); nativeStates.remove("angry");
        prefix.invoke(null, "angry");
        clear.invoke(null);
        check(!nativeSkeletons.containsKey("angry") && !nativeStates.containsKey("angry"), "empty native cache not restored");
        field(type, "activeFeminization").setBoolean(null, false);
        int before = Texture.all.size(); prefix.invoke(null, "angry");
        check(Texture.all.size() == before && assets.isEmpty(), "disabled Downfall patch loaded textures");
        System.out.println("PASS 20 Gremlin skin cycles: reuse, atlas disposal, native/foreign caches preserved, disabled no-op");

        // Exercise the editor on a synthetic optional class without importing Downfall types.
        ClassPool pool = new ClassPool(true);
        CtClass target = pool.makeClass("gremlin.characters.TestGremlin");
        target.setSuperclass(pool.get("com.megacrit.cardcrawl.characters.AbstractPlayer"));
        target.addConstructor(CtNewConstructor.defaultConstructor(target));
        target.addMethod(CtNewMethod.make("public String swapBody(String atlas, String json) { this.loadAnimation(atlas, json, 1.0f); return atlas; }", target));
        javassist.expr.ExprEditor editor = (javassist.expr.ExprEditor)Class.forName(type.getName() + "$GremlinCharacterSwapBodyPatch").getMethod("Instrument").invoke(null);
        target.getDeclaredMethod("swapBody").instrument(editor);
        byte[] bytes = target.toBytecode();
        Class<?> patched = new ClassLoader(MonsterResourceRegression.class.getClassLoader()) {
            Class<?> define() { return defineClass("gremlin.characters.TestGremlin", bytes, 0, bytes.length); }
        }.define();
        Object instance = patched.getConstructor().newInstance();
        Method swap = patched.getMethod("swapBody", String.class, String.class);
        String body = "feminizethespire/downfall_overrides/gremlinResources/images/char/angry/skeleton.atlas";
        before = Texture.all.size();
        for (int i = 0; i < 20; i++) {
            // Resource cleanup stays active on the native path when the art override is disabled.
            field(type, "activeFeminization").setBoolean(null, i % 2 == 0);
            check(body.equals(swap.invoke(instance, body, "ok")), "native swap return changed");
        }
        check(Texture.all.size() == before + 20, "swapBody editor lost or repeated a load");
        for (int i = before; i < Texture.all.size(); i++) {
            check(Texture.all.get(i).disposals == (i == Texture.all.size() - 1 ? 0 : 1), "native swapBody leaked or double-disposed atlas");
        }
        Object oldAtlas = field(com.megacrit.cardcrawl.core.AbstractCreature.class, "atlas").get(instance);
        try { swap.invoke(instance, body, "fail"); throw new AssertionError("swap must fail"); }
        catch (InvocationTargetException expected) {}
        check(field(com.megacrit.cardcrawl.core.AbstractCreature.class, "atlas").get(instance) == oldAtlas,
            "native swapBody failure did not restore atlas");
        check(Texture.all.get(Texture.all.size() - 1).disposals == 1, "native swapBody failure leaked temporary atlas");
        System.out.println("PASS optional Gremlin swapBody editor: 20 executed swaps, enabled/disabled cleanup, failed load rollback");
    }
    public static void main(String[] args) throws Exception {
        Gdx.files = (Files)Proxy.newProxyInstance(Files.class.getClassLoader(), new Class[]{Files.class},
            (proxy, method, values) -> {
                if (method.getName().equals("internal")) return new FileHandle(new File("src/main/resources", (String)values[0]));
                throw new UnsupportedOperationException(method.getName());
            });
        bones();
        gremlins();
    }
}
'''


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--game-root", required=True, type=Path)
    args = parser.parse_args()
    desktop, mts = dependencies(args.game_root.resolve())
    base = args.game_root.resolve() / "app/src/main/assets/components/mods/BaseMod.jar"
    compiled = ROOT / "build/classes/java/main"
    classpath = os.pathsep.join(map(str, (compiled, mts, base, desktop)))
    stubs = {
        "com.badlogic.gdx.graphics.Texture": TEXTURE_STUB,
        "com.megacrit.cardcrawl.core.AbstractCreature": '''
            protected com.badlogic.gdx.graphics.g2d.TextureAtlas atlas;
            protected com.esotericsoftware.spine.Skeleton skeleton;
            protected com.esotericsoftware.spine.AnimationStateData stateData;
            public com.esotericsoftware.spine.AnimationState state;
            protected void loadAnimation(String atlasPath, String json, float scale) {
                atlas = feminization.utils.ResourceUtil.loadAtlas(atlasPath);
                if (json.equals("fail")) throw new IllegalArgumentException("test JSON failure");
            }
        ''',
        "com.megacrit.cardcrawl.characters.AbstractPlayer": "",
        "com.megacrit.cardcrawl.monsters.exordium.Hexaghost": "private java.util.ArrayList orbs;",
        "com.megacrit.cardcrawl.monsters.exordium.HexaghostOrb": "private float x, y;",
        "com.megacrit.cardcrawl.core.Settings": "public static float scale = 1.0f;",
        "gremlin.GremlinMod": '''
            public static java.util.Map<String, Object> gremlinSkeletonMap = new java.util.HashMap<>();
            public static java.util.Map<String, Object> gremlinAnimationStateMap = new java.util.HashMap<>();
        ''',
        "com.evacipated.cardcrawl.modthespire.lib.SpireConfig": '''
            public SpireConfig(String a, String b) {}
            public boolean has(String key) { return true; }
            public boolean getBool(String key) { return false; }
            public void setBool(String key, boolean value) {}
            public void save() {}
        ''',
    }
    with tempfile.TemporaryDirectory(prefix="monster-cache-tests-", dir=ROOT / "build") as folder:
        folder = Path(folder)
        sources = write_stubs(folder, stubs)
        # The Hexaghost class must inherit skeleton; production superclass lookup is tested unchanged.
        path = folder / "com/megacrit/cardcrawl/monsters/exordium/Hexaghost.java"
        path.write_text("package com.megacrit.cardcrawl.monsters.exordium; public class Hexaghost extends com.megacrit.cardcrawl.core.AbstractCreature { private java.util.ArrayList orbs; }")
        harness = folder / "MonsterResourceRegression.java"
        harness.write_text(HARNESS)
        sources.append(str(harness))
        subprocess.run(["javac", "-proc:none", "-cp", classpath, "-d", str(folder), *sources], cwd=ROOT, check=True)
        subprocess.run(["java", "-cp", f"{folder}{os.pathsep}{classpath}", "MonsterResourceRegression"], cwd=ROOT, check=True)


if __name__ == "__main__":
    main()
