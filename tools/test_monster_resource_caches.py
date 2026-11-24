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
import zipfile

from test_resource_lifecycle import TEXTURE_STUB, dependencies, write_stubs


ROOT = Path(__file__).resolve().parents[1]

HARNESS = r'''
import com.badlogic.gdx.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.esotericsoftware.spine.*;
import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.*;
import javassist.*;
import downfallFeminization.DownfallCompatibilityPatches;
import feminization.utils.ResourceUtil;

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
    static SkeletonData loadGremlin(File base) {
        return new SkeletonJson(ResourceUtil.loadAtlas(new FileHandle(base + ".atlas")))
            .readSkeletonData(new FileHandle(base + ".json"));
    }
    static AnimationState animationState(SkeletonData data) {
        AnimationState state = new AnimationState(new AnimationStateData(data));
        state.setAnimation(0, "animation", true).setTime(0.125f);
        return state;
    }
    static void expectStandbyCrash(Method render, Object orb) throws Exception {
        try {
            render.invoke(orb, (Object)null);
            throw new AssertionError("unrepaired standby did not reproduce the crash");
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            check(cause instanceof IndexOutOfBoundsException && cause.getMessage().contains("40 >= 40"), "wrong exception: " + cause);
            check(cause.getStackTrace()[1].getClassName().endsWith("Animation$RotateTimeline"), "wrong Spine crash site");
        }
    }
    static void standby(File gameResources) throws Exception {
        // Run the actual instrumented render call using the field layout and call sequence from Downfall.
        ClassPool pool = new ClassPool(true);
        CtClass target = pool.makeClass("gremlin.orbs.GremlinStandby");
        target.addField(CtField.make("public com.esotericsoftware.spine.Skeleton skeleton;", target));
        target.addField(CtField.make("public com.esotericsoftware.spine.AnimationState state;", target));
        target.addField(CtField.make("public String animationName = \"animation\";", target));
        target.addField(CtField.make("public String assetFolder;", target));
        target.addField(CtField.make("public int rendered;", target));
        target.addMethod(CtNewMethod.make("public void render(com.badlogic.gdx.graphics.g2d.SpriteBatch sb) { this.state.update(0.016f); this.state.apply(this.skeleton); this.skeleton.updateWorldTransform(); this.rendered++; }", target));
        target.getDeclaredMethod("render").instrument(DownfallCompatibilityPatches.GremlinStandbyRenderPatch.Instrument());
        byte[] bytes = target.toBytecode();
        Class<?> patched = new ClassLoader(MonsterResourceRegression.class.getClassLoader()) {
            Class<?> define() { return defineClass("gremlin.orbs.GremlinStandby", bytes, 0, bytes.length); }
        }.define();
        Method render = patched.getMethod("render", SpriteBatch.class);
        Field skeletonField = patched.getField("skeleton"), stateField = patched.getField("state");
        for (String[] paths : new String[][]{{"sneak", "thiefGremlin"}, {"wizard", "wizardGremlin"}}) {
            String folder = paths[0];
            SkeletonData original = loadGremlin(new File(gameResources, "images/monsters/theBottom/" + paths[1] + "/skeleton"));
            SkeletonData feminized = loadGremlin(new File("src/main/resources/feminizethespire/downfall_overrides/gremlinResources/images/char/" + folder + "/skeleton"));
            Skeleton skeleton = new Skeleton(original);
            check(skeleton.getBones().size == 40, "test requires the original 40-bone skeleton");
            Object orb = patched.getConstructor().newInstance();
            patched.getField("assetFolder").set(orb, folder);
            skeletonField.set(orb, skeleton);
            for (boolean foreignTrackOnly : new boolean[]{false, true}) {
                AnimationState shared = animationState(foreignTrackOnly ? original : feminized);
                if (foreignTrackOnly) {
                    shared.getCurrent(0).setAnimation(feminized.findAnimation("animation"));
                }
                shared.setTimeScale(0.75f);
                AnimationState.TrackEntry oldTrack = shared.getCurrent(0);
                stateField.set(orb, shared);
                gremlin.GremlinMod.gremlinAnimationStateMap.put(folder, shared);
                DownfallCompatibilityPatches.activeFeminization = false;
                expectStandbyCrash(render, orb);
                DownfallCompatibilityPatches.activeFeminization = true;
                int before = patched.getField("rendered").getInt(orb);
                render.invoke(orb, (Object)null);
                AnimationState repaired = (AnimationState)stateField.get(orb);
                check(repaired != shared && repaired.getData().getSkeletonData() == original, "orb did not get matching state");
                check(repaired.getCurrent(0).getAnimation() == original.findAnimation("animation"), "orb kept foreign timeline");
                check(repaired.getCurrent(0).getLoop() && repaired.getCurrent(0).getTime() == oldTrack.getTime()
                    && repaired.getTimeScale() == shared.getTimeScale(), "playback was not preserved");
                check(skeletonField.get(orb) == skeleton, "orb skin was replaced");
                check(gremlin.GremlinMod.gremlinAnimationStateMap.get(folder) == shared && shared.getCurrent(0) == oldTrack,
                    "shared state was modified");
                render.invoke(orb, (Object)null);
                check(stateField.get(orb) == repaired && patched.getField("rendered").getInt(orb) == before + 2,
                    "normal render was skipped or rebuilt the state");
            }
            // A matching modded skin must keep its original state.
            AnimationState correct = animationState(feminized);
            skeletonField.set(orb, new Skeleton(feminized));
            stateField.set(orb, correct);
            render.invoke(orb, (Object)null);
            check(stateField.get(orb) == correct, "matching feminized state was replaced");
        }
        System.out.println("PASS Gremlin standby render repairs 40 >= 40 for sneak/wizard, without replacing skins or shared states");
    }
    public static void main(String[] args) throws Exception {
        Gdx.files = (Files)Proxy.newProxyInstance(Files.class.getClassLoader(), new Class[]{Files.class},
            (proxy, method, values) -> {
                if (method.getName().equals("internal")) return new FileHandle(new File("src/main/resources", (String)values[0]));
                throw new UnsupportedOperationException(method.getName());
            });
        bones();
        gremlins();
        standby(new File(args[0]));
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
        with zipfile.ZipFile(desktop) as jar:
            for gremlin in ("thiefGremlin", "wizardGremlin"):
                for ext in ("atlas", "json"):
                    # Downfall's originals use the same 40-bone rigs. Textures are stubbed.
                    jar.extract(f"images/monsters/theBottom/{gremlin}/skeleton.{ext}", folder)
        sources = write_stubs(folder, stubs)
        # The Hexaghost class must inherit skeleton; production superclass lookup is tested unchanged.
        path = folder / "com/megacrit/cardcrawl/monsters/exordium/Hexaghost.java"
        path.write_text("package com.megacrit.cardcrawl.monsters.exordium; public class Hexaghost extends com.megacrit.cardcrawl.core.AbstractCreature { private java.util.ArrayList orbs; }")
        harness = folder / "MonsterResourceRegression.java"
        harness.write_text(HARNESS)
        sources.append(str(harness))
        subprocess.run(["javac", "-proc:none", "-cp", classpath, "-d", str(folder), *sources], cwd=ROOT, check=True)
        subprocess.run(["java", "-cp", f"{folder}{os.pathsep}{classpath}", "MonsterResourceRegression", str(folder)], cwd=ROOT, check=True)


if __name__ == "__main__":
    main()
