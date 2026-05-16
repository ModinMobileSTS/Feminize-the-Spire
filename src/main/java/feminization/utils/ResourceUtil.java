package feminization.utils;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public final class ResourceUtil {
    private static final Map<String, Texture> TEXTURE_CACHE = new HashMap<String, Texture>();
    private static final Set<Texture> CACHED_TEXTURES = Collections.newSetFromMap(new IdentityHashMap<Texture, Boolean>());
    private static final Set<Texture> DISPOSED_TEXTURES = Collections.newSetFromMap(new IdentityHashMap<Texture, Boolean>());
    private static final Set<TextureAtlas> DISPOSED_ATLASES = Collections.newSetFromMap(new IdentityHashMap<TextureAtlas, Boolean>());
    private static final Field ATLAS_FIELD = findField(AbstractCreature.class, "atlas");

    private ResourceUtil() {
    }

    public static synchronized Texture getTexture(String path) {
        Texture texture = TEXTURE_CACHE.get(path);
        if (texture == null) {
            texture = ImageMaster.loadImage(path);
            TEXTURE_CACHE.put(path, texture);
            if (texture != null) {
                CACHED_TEXTURES.add(texture);
            }
        }
        return texture;
    }

    public static Texture replaceTexture(Texture current, String path) {
        Texture replacement = getTexture(path);
        if (replacement != current) {
            disposeTextureIfUncached(current);
        }
        return replacement;
    }

    public static void invokeLoadAnimation(AbstractCreature creature, Method loadAnimation, String atlasUrl, String skeletonUrl, float scale) throws Exception {
        TextureAtlas oldAtlas = getAtlas(creature);
        loadAnimation.invoke(creature, atlasUrl, skeletonUrl, Float.valueOf(scale));
        TextureAtlas newAtlas = getAtlas(creature);
        if (newAtlas != oldAtlas) {
            disposeAtlas(oldAtlas);
        }
    }

    public static TextureAtlas getAtlas(AbstractCreature creature) throws IllegalAccessException {
        return ATLAS_FIELD == null ? null : (TextureAtlas) ATLAS_FIELD.get(creature);
    }

    public static void disposeAtlas(TextureAtlas atlas) {
        if (atlas == null || DISPOSED_ATLASES.contains(atlas)) {
            return;
        }
        DISPOSED_ATLASES.add(atlas);
        try {
            atlas.dispose();
        } catch (Exception ignored) {
        }
    }

    private static void disposeTextureIfUncached(Texture texture) {
        if (texture == null || CACHED_TEXTURES.contains(texture) || DISPOSED_TEXTURES.contains(texture)) {
            return;
        }
        DISPOSED_TEXTURES.add(texture);
        try {
            texture.dispose();
        } catch (Exception ignored) {
        }
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
