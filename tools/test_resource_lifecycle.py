#!/usr/bin/env python3
"""Headless regression tests for card art, texture ownership and atlas cleanup.

Usage: python3 tools/test_resource_lifecycle.py --game-root ../SlayTheAmethystModded
Uses the real libGDX atlas parser/Spine runtime and the actual Javassist editors.
Only GPU textures and game static initializers are replaced with small test doubles.
"""

import argparse
import os
from pathlib import Path
import subprocess
import tempfile


ROOT = Path(__file__).resolve().parents[1]

TEXTURE_STUB = r'''
    public static final java.util.List<Texture> all = new java.util.ArrayList<>();
    public static String failPath;
    public final String path;
    public int disposals;
    public boolean disposed;
    public enum TextureFilter { Nearest, Linear, MipMap, MipMapNearestNearest,
        MipMapLinearNearest, MipMapNearestLinear, MipMapLinearLinear;
        public boolean isMipMap() { return ordinal() >= 2; }
    }
    public enum TextureWrap { MirroredRepeat, ClampToEdge, Repeat }
    public Texture(String path) {
        if (path.equals(failPath)) throw new RuntimeException("test texture load failure");
        this.path = path;
        all.add(this);
    }
    public Texture(com.badlogic.gdx.files.FileHandle file) { this(file.path()); }
    public Texture(com.badlogic.gdx.files.FileHandle file, com.badlogic.gdx.graphics.Pixmap.Format format,
            boolean mipmaps) { this(file); }
    public void setFilter(TextureFilter a, TextureFilter b) {}
    public void setWrap(TextureWrap a, TextureWrap b) {}
    public int getWidth() { return 2048; }
    public int getHeight() { return 2048; }
    public int getTextureObjectHandle() { return disposals == 0 ? 1 : 0; }
    public void dispose() { disposals++; disposed = true; }
'''

STUBS = {
    "feminization.FeminizationMod": '''
        public static boolean activeOtherModFeminization;
        public static final org.apache.logging.log4j.Logger logger =
            org.apache.logging.log4j.LogManager.getLogger("resource-regression");
    ''',
    "com.badlogic.gdx.graphics.Texture": TEXTURE_STUB,
    "com.megacrit.cardcrawl.helpers.ImageMaster": '''
        public static final java.util.List<String> paths = new java.util.ArrayList<>();
        public static String failPath;
        public static com.badlogic.gdx.graphics.Texture loadImage(String path) {
            paths.add(path);
            return path != null && path.equals(failPath) ? null : new com.badlogic.gdx.graphics.Texture(path);
        }
    ''',
    "com.megacrit.cardcrawl.cards.AbstractCard": '''
        public enum CardColor { RED, GREEN, BLUE, PURPLE, CURSE, COLORLESS, OTHER }
        public CardColor color = CardColor.RED;
        public String assetUrl = "red/attack/strike";
        public com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion portrait, jokePortrait;
    ''',
    "com.megacrit.cardcrawl.cards.red.TestCard": "",
    "otherMod.TestCard": "",
    "com.megacrit.cardcrawl.screens.SingleCardViewPopup": '''
        private com.megacrit.cardcrawl.cards.AbstractCard card;
        public SingleCardViewPopup(com.megacrit.cardcrawl.cards.AbstractCard card) { this.card = card; }
    ''',
    "com.megacrit.cardcrawl.core.AbstractCreature": '''
        protected com.badlogic.gdx.graphics.g2d.TextureAtlas atlas;
        protected Object skeleton, stateData;
        public Object state;
        protected void loadAnimation(String atlasPath, String json, float scale) {
            atlas = feminization.utils.ResourceUtil.loadAtlas(atlasPath);
            skeleton = new Object();
            stateData = new Object();
            state = new Object();
            if (json.equals("fail")) throw new IllegalArgumentException("test JSON failure");
        }
    ''',
    "com.megacrit.cardcrawl.characters.AbstractPlayer": "",
    "com.megacrit.cardcrawl.characters.Watcher": "",
}

HARNESS = r'''
import com.badlogic.gdx.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.screens.SingleCardViewPopup;
import feminization.cardArt.CardArtFeminization;
import feminization.patches.CardArtFeminizationPatch;
import feminization.patches.PlayerResourceOwnershipPatch;
import feminization.utils.ResourceUtil;
import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.*;
import javassist.*;

public class ResourceRegression {
    static final String PREFIX = "feminizethespire/card_overrides/";
    static final String ATLAS = PREFIX + "cards/cards.atlas";
    static final String BODY = "feminizationResources/images/characters/ironclad-2/idle/skeleton.atlas";
    static int probes;
    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    static Field field(Class<?> type, String name) throws Exception {
        Field result = type.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }
    static Object get(Object owner, String name) throws Exception {
        return field(AbstractCreature.class, name).get(owner);
    }
    static void enabled(boolean value) {
        CardArtFeminization.activeIroncladCardFeminization = value;
        CardArtFeminization.activeSilentCardFeminization = value;
        CardArtFeminization.activeDefectCardFeminization = value;
        CardArtFeminization.activeWatcherCardFeminization = value;
        CardArtFeminization.activeCurseCardFeminization = value;
        CardArtFeminization.activeColorlessCardFeminization = value;
    }
    static void collect(WeakReference<?> reference) {
        for (int i = 0; i < 100 && reference.get() != null; i++) {
            System.gc();
        }
        check(reference.get() == null, "ownership registry retains disposed resource");
    }
    static int calls(CtMethod method, String owner, String name) throws Exception {
        final int[] count = {0};
        method.instrument(new javassist.expr.ExprEditor() {
            @Override public void edit(javassist.expr.MethodCall call) {
                if (owner.equals(call.getClassName()) && name.equals(call.getMethodName())) count[0]++;
            }
        });
        return count[0];
    }
    static void patchActualBytecode() throws Exception {
        ClassPool pool = new ClassPool(false);
        pool.appendSystemPath();
        pool.insertClassPath(System.getProperty("game.jar"));
        CtClass popup = pool.get("com.megacrit.cardcrawl.screens.SingleCardViewPopup");
        CtMethod loadPortrait = popup.getDeclaredMethod("loadPortraitImg");
        int portraitLoads = calls(loadPortrait, ImageMaster.class.getName(), "loadImage");
        check(portraitLoads > 0, "actual popup has no image load site");
        loadPortrait.instrument(CardArtFeminizationPatch.SingleCardViewPatch.Instrument());
        check(calls(loadPortrait, CardArtFeminization.class.getName(), "loadSingleCardViewPortrait") == portraitLoads
            && calls(loadPortrait, ImageMaster.class.getName(), "loadImage") == 0, "actual popup load sites not replaced");
        popup.toBytecode();
        CtClass player = pool.get("com.megacrit.cardcrawl.characters.AbstractPlayer");
        CtMethod initialize = player.getDeclaredMethod("initializeClass");
        int playerLoads = calls(initialize, ImageMaster.class.getName(), "loadImage");
        check(playerLoads == 4, "unexpected native player image load sites");
        initialize.instrument(PlayerResourceOwnershipPatch.PlayerImages.Instrument());
        check(calls(initialize, ResourceUtil.class.getName(), "loadPlayerTexture") == playerLoads
            && calls(initialize, ImageMaster.class.getName(), "loadImage") == 0, "native player load sites not replaced");
        player.toBytecode();
        CtClass creature = pool.get("com.megacrit.cardcrawl.core.AbstractCreature");
        creature.getDeclaredMethod("loadAnimation").instrument(PlayerResourceOwnershipPatch.BodyAtlas.Instrument());
        check(calls(creature.getDeclaredMethod("loadAnimation"), ResourceUtil.class.getName(), "loadAtlas") == 1,
            "native body atlas allocation not replaced");
        creature.toBytecode();
        System.out.println("PASS actual popup/player/creature bytecode: all portrait loads, 4 player images, 1 body atlas replaced");
    }
    static void popupBytecode() throws Exception {
        ClassPool pool = new ClassPool(true);
        CtClass popup = pool.makeClass("com.megacrit.cardcrawl.screens.TestPopup");
        popup.setSuperclass(pool.get(SingleCardViewPopup.class.getName()));
        popup.addConstructor(CtNewConstructor.make("public TestPopup(com.megacrit.cardcrawl.cards.AbstractCard card) { super(card); }", popup));
        CtMethod load = CtNewMethod.make("public com.badlogic.gdx.graphics.Texture load(String path) { return com.megacrit.cardcrawl.helpers.ImageMaster.loadImage(path); }", popup);
        popup.addMethod(load);
        load.instrument(CardArtFeminizationPatch.SingleCardViewPatch.Instrument());
        byte[] bytes = popup.toBytecode();
        Class<?> patched = new ClassLoader(ResourceRegression.class.getClassLoader()) {
            Class<?> define() { return defineClass("com.megacrit.cardcrawl.screens.TestPopup", bytes, 0, bytes.length); }
        }.define();
        AbstractCard card = new com.megacrit.cardcrawl.cards.red.TestCard();
        Object instance = patched.getConstructor(AbstractCard.class).newInstance(card);
        Method call = patched.getMethod("load", String.class);
        ImageMaster.paths.clear();
        for (int i = 0; i < 100; i++) {
            Texture texture = (Texture)call.invoke(instance, "images/1024Portraits/red/attack/strike.png");
            check(texture.path.equals(PREFIX + "images/1024Portraits/red/attack/strike.png"), "incorrect large portrait");
            texture.dispose(); // The game's close/openNext owns this one and only allocation.
        }
        check(ImageMaster.paths.size() == 100, "popup double-loads original art");
        for (String path : ImageMaster.paths) check(path.startsWith(PREFIX), "original portrait leaked");
        Texture beta = (Texture)call.invoke(instance, "images/1024PortraitsBeta/red/attack/strike.png");
        check(beta.path.startsWith("images/1024PortraitsBeta/"), "Beta art overridden");
        String override = PREFIX + "images/1024Portraits/red/attack/strike.png";
        ImageMaster.failPath = override;
        Texture fallback = (Texture)call.invoke(instance, "images/1024Portraits/red/attack/strike.png");
        check(fallback.path.equals("images/1024Portraits/red/attack/strike.png"), "decode failure did not fall back");
        ImageMaster.failPath = null;
        enabled(false);
        Texture disabled = (Texture)call.invoke(instance, "images/1024Portraits/red/attack/strike.png");
        check(!disabled.path.startsWith(PREFIX), "disabled portrait replaced");
        enabled(true);
        AbstractCard foreign = new otherMod.TestCard();
        check(!CardArtFeminization.loadSingleCardViewPortrait(new SingleCardViewPopup(foreign),
            "images/1024Portraits/red/attack/strike.png").path.startsWith(PREFIX), "foreign card replaced by default");
        feminization.FeminizationMod.activeOtherModFeminization = true;
        check(CardArtFeminization.loadSingleCardViewPortrait(new SingleCardViewPopup(foreign),
            "images/1024Portraits/red/attack/strike.png").path.startsWith(PREFIX), "opt-in compatibility broken");
        feminization.FeminizationMod.activeOtherModFeminization = false;
        System.out.println("PASS 100 popup open/close cycles: one allocation, Beta, toggles, fallback, mod opt-in");
    }
    static void cards() throws Exception {
        enabled(false);
        int before = Texture.all.size();
        CardArtFeminization.preloadCardAtlases();
        check(Texture.all.size() == before && probes == 0, "disabled preload did work");
        enabled(true);
        FileHandle file = Gdx.files.internal(ATLAS);
        TextureAtlas.TextureAtlasData data = new TextureAtlas.TextureAtlasData(file, file.parent(), false);
        int pages = data.getPages().size;
        for (TextureAtlas.TextureAtlasData.Page page : data.getPages()) {
            check(page.width * page.height * 4 <= 1536 * 1024, "oversized card atlas page: " + page.textureFile);
        }
        String[][] starters = {
            {"red/attack/strike", "red/skill/defend", "red/attack/bash"},
            {"green/attack/strike", "green/skill/defend", "green/attack/neutralize", "green/skill/survivor"},
            {"blue/attack/strike", "blue/skill/defend", "blue/skill/zap", "blue/skill/dualcast"},
            {"purple/attack/strike", "purple/skill/defend", "purple/attack/eruption", "purple/skill/vigilance"}
        };
        for (String[] deck : starters) {
            TextureAtlas.TextureAtlasData.Page starterPage = null;
            for (String name : deck) {
                TextureAtlas.TextureAtlasData.Page found = null;
                for (TextureAtlas.TextureAtlasData.Region region : data.getRegions()) {
                    if (name.equals(region.name)) { found = region.page; break; }
                }
                check(found != null && (starterPage == null || starterPage == found), "split/missing starter: " + name);
                starterPage = found;
            }
            check(starterPage.width * starterPage.height * 4 <= 1024 * 1024, "starter hand exceeds 1 MiB");
        }
        Texture.failPath = data.getPages().get(1).textureFile.path();
        before = Texture.all.size();
        CardArtFeminization.preloadCardAtlases();
        check(Texture.all.size() > before, "failure was not exercised");
        for (int i = before; i < Texture.all.size(); i++) check(Texture.all.get(i).disposals == 1, "partial atlas leaked page");
        int after = Texture.all.size();
        CardArtFeminization.preloadCardAtlases();
        check(Texture.all.size() == after, "failed atlas retried without backoff");
        Texture.failPath = null;
        field(CardArtFeminization.class, "nextAtlasAttempt").setLong(null, 1L);
        CardArtFeminization.preloadCardAtlases();
        check(Texture.all.size() == after + pages, "atlas did not recover after backoff");
        AbstractCard card = new com.megacrit.cardcrawl.cards.red.TestCard();
        TextureAtlas.AtlasRegion original = card.portrait;
        CardArtFeminization.beforeRenderPortrait(card);
        TextureAtlas.AtlasRegion replacement = card.portrait;
        check(replacement != original, "small portrait not replaced");
        CardArtFeminization.beforeRenderPortrait(card);
        CardArtFeminization.afterRenderPortrait(card);
        check(card.portrait == replacement, "nested render restored too early");
        CardArtFeminization.afterRenderPortrait(card);
        check(card.portrait == original, "small portrait not restored");
        int checked = probes;
        for (int i = 0; i < 500; i++) {
            CardArtFeminization.beforeRenderPortrait(card);
            CardArtFeminization.afterRenderPortrait(card);
        }
        check(probes == checked, "render path repeats existence probes");
        card.assetUrl = "purple/skill/pray"; card.color = AbstractCard.CardColor.PURPLE;
        CardArtFeminization.beforeRenderPortrait(card);
        check(card.portrait == original, "missing art no longer keeps original");
        checked = probes;
        for (int i = 0; i < 500; i++) CardArtFeminization.beforeRenderPortrait(card);
        check(probes == checked, "negative resource result not cached");
        enabled(false);
        CardArtFeminization.onSettingsChanged();
        for (int i = after; i < Texture.all.size(); i++) check(Texture.all.get(i).disposals == 1, "disabled atlas not freed exactly once");
        enabled(true);
        CardArtFeminization.onSettingsChanged();
        check(Texture.all.size() == after + 2 * pages, "settings re-enable did not load atlas");
        card.assetUrl = "red/attack/strike"; card.color = AbstractCard.CardColor.RED;
        CardArtFeminization.beforeRenderPortrait(card);
        CardArtFeminization.beforeRenderPortrait(card);
        Texture active = card.portrait.getTexture();
        enabled(false);
        CardArtFeminization.onSettingsChanged();
        check(active.disposals == 0, "settings freed atlas during rendering");
        CardArtFeminization.afterRenderPortrait(card);
        check(active.disposals == 0, "nested render freed atlas too early");
        CardArtFeminization.afterRenderPortrait(card);
        check(card.portrait == original && active.disposals == 1, "deferred atlas release did not restore and free");
        enabled(true);
        System.out.println("PASS cached positive/negative lookups, nested rendering, disabled preload/deferred release, partial failure/retry");
    }
    static void resources() throws Exception {
        Texture shared = new Texture("shared");
        Texture first = ResourceUtil.replaceOwnedTexture(shared, "first");
        check(shared.disposals == 0, "unknown/shared texture disposed");
        Texture second = ResourceUtil.replaceOwnedTexture(first, "second");
        check(first.disposals == 1, "owned texture not released");
        Texture.failPath = "src/main/resources/missing";
        check(ResourceUtil.replaceOwnedTexture(second, "missing") == second && second.disposals == 0,
            "failed texture load damaged old texture");
        Texture.failPath = null;
        Texture nativeImage = ResourceUtil.loadPlayerTexture(new com.megacrit.cardcrawl.characters.Watcher(), "native shoulder");
        ResourceUtil.replaceOwnedTexture(nativeImage, "new shoulder");
        check(nativeImage.disposals == 1, "native instance allocation not tracked");
        second.dispose();
        WeakReference<Texture> weak = new WeakReference<>(second);
        Texture.all.remove(second); second = null;
        collect(weak);
        AbstractCreature creature = new AbstractCreature();
        Method method = AbstractCreature.class.getDeclaredMethod("loadAnimation", String.class, String.class, float.class);
        method.setAccessible(true);
        ResourceUtil.invokeLoadAnimation(creature, method, BODY, "ok", 1f);
        TextureAtlas old = (TextureAtlas)get(creature, "atlas");
        Texture oldTexture = old.getTextures().iterator().next();
        Object[] snapshot = {get(creature, "atlas"), get(creature, "skeleton"), get(creature, "stateData"), get(creature, "state")};
        int before = Texture.all.size();
        try { ResourceUtil.invokeLoadAnimation(creature, method, BODY, "fail", 1f); throw new AssertionError("load must fail"); }
        catch (InvocationTargetException expected) {}
        String[] fields = {"atlas", "skeleton", "stateData", "state"};
        for (int i = 0; i < fields.length; i++) check(get(creature, fields[i]) == snapshot[i], "failed load did not roll back " + fields[i]);
        check(oldTexture.disposals == 0, "failed load disposed usable atlas");
        for (int i = before; i < Texture.all.size(); i++) check(Texture.all.get(i).disposals == 1, "failed JSON leaked atlas");
        ResourceUtil.reloadAnimation(creature, BODY, "ok", 1f);
        check(oldTexture.disposals == 1, "successful swap leaked old atlas");
        for (int i = 0; i < 100; i++) ResourceUtil.reloadAnimation(creature, BODY, "ok", 1f);
        Texture current = ((TextureAtlas)get(creature, "atlas")).getTextures().iterator().next();
        for (int i = before; i < Texture.all.size(); i++) {
            Texture texture = Texture.all.get(i);
            check(texture == current ? texture.disposals == 0 : texture.disposals == 1,
                "repeated body swaps leaked or double-disposed an atlas");
        }
        TextureAtlas foreign = new TextureAtlas();
        Texture foreignTexture = new Texture("foreign atlas");
        foreign.getTextures().add(foreignTexture);
        field(AbstractCreature.class, "atlas").set(creature, foreign);
        ResourceUtil.reloadAnimation(creature, BODY, "ok", 1f);
        check(foreignTexture.disposals == 0, "unowned/shared atlas disposed");
        System.out.println("PASS instance ownership, weak registry, 100 body swaps, failed load rollback, shared resource safety");
    }
    public static void main(String[] args) throws Exception {
        Gdx.files = (Files)Proxy.newProxyInstance(Files.class.getClassLoader(), new Class[]{Files.class},
            (proxy, method, values) -> {
                if (method.getName().equals("internal")) return new FileHandle(new File("src/main/resources", (String)values[0])) {
                    @Override public boolean exists() { probes++; return super.exists(); }
                };
                throw new UnsupportedOperationException(method.getName());
            });
        patchActualBytecode();
        cards();
        popupBytecode();
        resources();
    }
}
'''


def write_stubs(folder, stubs):
    sources = []
    for name, body in stubs.items():
        package, _, simple = name.rpartition(".")
        parent = ""
        if name in ("com.megacrit.cardcrawl.cards.red.TestCard", "otherMod.TestCard"):
            parent = " extends com.megacrit.cardcrawl.cards.AbstractCard"
        elif simple == "AbstractPlayer":
            parent = " extends com.megacrit.cardcrawl.core.AbstractCreature"
        elif simple == "Watcher":
            parent = " extends com.megacrit.cardcrawl.characters.AbstractPlayer"
        path = folder / (name.replace(".", "/") + ".java")
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(f"package {package}; public class {simple}{parent} {{ {body} }}")
        sources.append(str(path))
    return sources


def dependencies(game):
    desktop = next((game / name for name in ("tools/desktop-1.0.jar", "build-deps/desktop.jar",
                   "build-deps/steamapps/common/SlayTheSpire/desktop-1.0.jar") if (game / name).is_file()), None)
    mts = game / "app/src/main/assets/components/mods/ModTheSpire.jar"
    if desktop is None or not mts.is_file():
        raise SystemExit("game root must contain desktop game jar and ModTheSpire.jar")
    return desktop, mts


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--game-root", required=True, type=Path)
    args = parser.parse_args()
    desktop, mts = dependencies(args.game_root.resolve())
    classpath = os.pathsep.join(map(str, (mts, desktop)))
    build = ROOT / "build"
    build.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="resource-tests-", dir=build) as folder:
        folder = Path(folder)
        sources = write_stubs(folder, STUBS)
        harness = folder / "ResourceRegression.java"
        harness.write_text(HARNESS)
        sources.append(str(harness))
        sources.extend(str(ROOT / "src/main/java" / name) for name in (
            "feminization/utils/ResourceUtil.java", "feminization/cardArt/CardArtFeminization.java",
            "feminization/patches/CardArtFeminizationPatch.java", "feminization/patches/PlayerResourceOwnershipPatch.java"))
        subprocess.run(["javac", "-proc:none", "-cp", classpath, "-d", str(folder), *sources], cwd=ROOT, check=True)
        subprocess.run(["java", f"-Dgame.jar={desktop}", "-cp", f"{folder}{os.pathsep}{classpath}",
                        "ResourceRegression"], cwd=ROOT, check=True)


if __name__ == "__main__":
    main()
