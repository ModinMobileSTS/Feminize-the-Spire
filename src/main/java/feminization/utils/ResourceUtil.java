package feminization.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class ResourceUtil {
    private static final Map<String, Texture> TEXTURE_CACHE = new HashMap<String, Texture>();
    // Weak keys: AbstractPlayer.dispose also releases these textures, without calling this helper.
    // Only independently allocated resources enter these sets; unknown/shared resources stay intact.
    private static final Set<Texture> OWNED_TEXTURES = Collections.newSetFromMap(new WeakHashMap<Texture, Boolean>());
    private static final Set<TextureAtlas> OWNED_ATLASES = Collections.newSetFromMap(new WeakHashMap<TextureAtlas, Boolean>());
    private static final Field[] ANIMATION_FIELDS = {
            findAnimationField("atlas"), findAnimationField("skeleton"),
            findAnimationField("stateData"), findAnimationField("state")
    };
    private static final Method LOAD_ANIMATION = findLoadAnimation();

    private ResourceUtil() {
    }

    public static synchronized Texture getTexture(String path) {
        Texture texture = TEXTURE_CACHE.get(path);
        if (texture == null) {
            texture = ImageMaster.loadImage(path);
            if (texture != null) {
                TEXTURE_CACHE.put(path, texture);
            }
        }
        return texture;
    }

    public static Texture replaceTexture(Texture current, String path) {
        Texture replacement = getTexture(path);
        return replacement == null ? current : replacement;
    }

    public static Texture replaceOwnedTexture(Texture current, String path) {
        Texture replacement = loadOwnedTexture(path);
        if (replacement == null) {
            return current;
        }
        if (current != null && current != replacement && OWNED_TEXTURES.remove(current)) {
            try {
                current.dispose();
            } catch (Exception ignored) {
            }
        }
        return replacement;
    }

    public static void invokeLoadAnimation(AbstractCreature creature, Method loadAnimation, String atlasUrl, String skeletonUrl, float scale) throws Exception {
        Object[] previous = new Object[ANIMATION_FIELDS.length];
        for (int i = 0; i < ANIMATION_FIELDS.length; i++) {
            if (ANIMATION_FIELDS[i] != null) {
                previous[i] = ANIMATION_FIELDS[i].get(creature);
            }
        }
        try {
            loadAnimation.invoke(creature, atlasUrl, skeletonUrl, Float.valueOf(scale));
        } catch (Exception exception) {
            // loadAnimation assigns atlas before parsing JSON. Restore the complete old state and
            // release the partially installed atlas, not the still usable previous one.
            disposeReplacedAtlas(creature, previous[0]);
            for (int i = 0; i < ANIMATION_FIELDS.length; i++) {
                if (ANIMATION_FIELDS[i] != null) {
                    ANIMATION_FIELDS[i].set(creature, previous[i]);
                }
            }
            throw exception;
        }
        if (ANIMATION_FIELDS[0] != null && ANIMATION_FIELDS[0].get(creature) != previous[0]) {
            disposeOwnedAtlas(previous[0]);
        }
    }

    public static void reloadAnimation(AbstractCreature creature, String atlasUrl, String skeletonUrl, float scale) throws Exception {
        invokeLoadAnimation(creature, LOAD_ANIMATION, atlasUrl, skeletonUrl, scale);
    }

    private static Method findLoadAnimation() {
        try {
            Method method = AbstractCreature.class.getDeclaredMethod("loadAnimation", String.class, String.class, float.class);
            method.setAccessible(true);
            return method;
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Like ImageMaster.loadImage, but allocation/ownership is explicit, never a shared cache. */
    public static Texture loadOwnedTexture(String path) {
        if (path == null) {
            return null;
        }
        try {
            Texture texture = new Texture(Gdx.files.internal(path));
            OWNED_TEXTURES.add(texture);
            try {
                texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                return texture;
            } catch (RuntimeException exception) {
                OWNED_TEXTURES.remove(texture);
                texture.dispose();
                throw exception;
            }
        } catch (Exception exception) {
            feminization.FeminizationMod.logger.warn("Unable to load player texture: " + path, exception);
            return null;
        }
    }

    /** Tracks native per-player allocations without claiming images provided by other mods. */
    public static Texture loadPlayerTexture(Object player, String path) {
        String name = player.getClass().getName();
        if (name.equals("com.megacrit.cardcrawl.characters.Ironclad")
                || name.equals("com.megacrit.cardcrawl.characters.TheSilent")
                || name.equals("com.megacrit.cardcrawl.characters.Defect")
                || name.equals("com.megacrit.cardcrawl.characters.Watcher")) {
            return loadOwnedTexture(path);
        }
        return ImageMaster.loadImage(path);
    }

    /** Load pages explicitly so a failure halfway through a multi-page atlas cannot leak them. */
    public static TextureAtlas loadAtlas(String path) {
        return loadAtlas(Gdx.files.internal(path));
    }

    public static TextureAtlas loadAtlas(com.badlogic.gdx.files.FileHandle file) {
        TextureAtlas.TextureAtlasData data = new TextureAtlas.TextureAtlasData(file, file.parent(), false);
        try {
            for (TextureAtlas.TextureAtlasData.Page page : data.getPages()) {
                page.texture = new Texture(page.textureFile, page.format, page.useMipMaps);
            }
            TextureAtlas atlas = new TextureAtlas(data);
            OWNED_ATLASES.add(atlas);
            return atlas;
        } catch (RuntimeException exception) {
            for (TextureAtlas.TextureAtlasData.Page page : data.getPages()) {
                if (page.texture != null) {
                    page.texture.dispose();
                }
            }
            throw exception;
        }
    }

    private static void disposeReplacedAtlas(AbstractCreature creature, Object previous) throws IllegalAccessException {
        if (ANIMATION_FIELDS[0] != null) {
            Object current = ANIMATION_FIELDS[0].get(creature);
            if (current != previous) {
                disposeOwnedAtlas(current);
            }
        }
    }

    private static void disposeOwnedAtlas(Object atlas) {
        if (atlas instanceof TextureAtlas && OWNED_ATLASES.contains(atlas)) {
            disposeAtlas((TextureAtlas)atlas);
        }
    }

    private static Field findAnimationField(String name) {
        try {
            Field field = AbstractCreature.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static void disposeAtlas(TextureAtlas atlas) {
        if (atlas == null) {
            return;
        }
        try {
            OWNED_ATLASES.remove(atlas);
            atlas.dispose();
        } catch (Exception ignored) {
        }
    }
}
