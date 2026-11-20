package feminization.cardArt;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.cards.AbstractCard;
import feminization.FeminizationMod;
import feminization.utils.ResourceUtil;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.screens.SingleCardViewPopup;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public final class CardArtFeminization {
    private static final String VANILLA_CARD_PACKAGE_PREFIX = "com.megacrit.cardcrawl.cards.";
    private static final String OVERRIDE_PREFIX = "feminizethespire/card_overrides/";
    private static final String CARD_ATLAS_PATH = OVERRIDE_PREFIX + "cards/cards.atlas";
    private static final String PORTRAIT_PREFIX = OVERRIDE_PREFIX + "images/1024Portraits/";
    private static final Map<AbstractCard, PortraitState> RENDER_STATES = Collections.synchronizedMap(new IdentityHashMap<AbstractCard, PortraitState>());
    private static final Field SINGLE_VIEW_CARD_FIELD = findOptionalField(SingleCardViewPopup.class, "card");
    /**
     * Cache of {@code card class -> is vanilla}. {@link Class#getPackage()} allocates on every call and
     * consults a synchronized ClassLoader map, which is far too costly for a per-card per-frame path.
     * Lock-free: reads hit an immutable snapshot; a miss builds a new snapshot and swaps it in.
     */
    private static volatile Map<Class<?>, Boolean> vanillaCardClassCache = new IdentityHashMap<Class<?>, Boolean>();
    // Both hits and misses are stable for the session. Never probe the filesystem per card/frame.
    private static final Map<String, Boolean> PORTRAIT_EXISTS = new HashMap<String, Boolean>();
    private static TextureAtlas cardAtlas;
    private static final long ATLAS_RETRY_DELAY_NANOS = 30_000_000_000L;
    private static long nextAtlasAttempt;
    private static boolean atlasFailureLogged;
    /**
     * Flattened {@code assetUrl -> region} lookups. {@link TextureAtlas#findRegion(String)} is a linear
     * scan with a {@code String.equals} per entry (375+ regions), executed for every rendered card every
     * frame. Building the map once turns that into a single hash lookup.
     */
    private static Map<String, TextureAtlas.AtlasRegion> cardAtlasRegions;

    public static boolean activeIroncladCardFeminization = true;
    public static boolean activeSilentCardFeminization = true;
    public static boolean activeDefectCardFeminization = true;
    public static boolean activeWatcherCardFeminization = true;
    public static boolean activeCurseCardFeminization = true;
    public static boolean activeColorlessCardFeminization = true;

    private CardArtFeminization() {
    }

    public static void preloadCardAtlases() {
        // Called once from post-initialize. Loading the multi-megabyte card atlas lazily meant the
        // decode + GPU upload landed on the frame where the first feminized card was ever drawn
        // (first deck view / card reward), producing a visible hitch mid-session.
        if (isAnyCardArtEnabled()) {
            getAtlasRegions();
        }
    }

    public static void onSettingsChanged() {
        nextAtlasAttempt = 0L;
        if (!isAnyCardArtEnabled()) {
            releaseDisabledAtlas();
        } else {
            preloadCardAtlases();
        }
    }

    private static void releaseDisabledAtlas() {
        if (cardAtlas != null && !isAnyCardArtEnabled() && RENDER_STATES.isEmpty()) {
            ResourceUtil.disposeAtlas(cardAtlas);
            cardAtlas = null;
            cardAtlasRegions = null;
        }
    }

    private static boolean isAnyCardArtEnabled() {
        return activeIroncladCardFeminization || activeSilentCardFeminization
                || activeDefectCardFeminization || activeWatcherCardFeminization
                || activeCurseCardFeminization || activeColorlessCardFeminization;
    }

    public static void beforeRenderPortrait(AbstractCard card) {
        if (card == null || !isEnabled(card) || !isEligibleCard(card)) {
            return;
        }

        TextureAtlas.AtlasRegion replacement = findAtlasRegion(card);
        if (replacement == null) {
            return;
        }

        synchronized (RENDER_STATES) {
            PortraitState state = RENDER_STATES.get(card);
            if (state != null) {
                state.depth++;
                return;
            }
            RENDER_STATES.put(card, new PortraitState(card.portrait, card.jokePortrait));
        }

        card.portrait = replacement;
    }

    public static void afterRenderPortrait(AbstractCard card) {
        if (card == null) {
            return;
        }

        PortraitState state;
        synchronized (RENDER_STATES) {
            state = RENDER_STATES.get(card);
            if (state != null && --state.depth <= 0) {
                RENDER_STATES.remove(card);
            }
        }
        if (state == null || state.depth > 0) {
            return;
        }

        card.portrait = state.portrait;
        card.jokePortrait = state.jokePortrait;
        releaseDisabledAtlas();
    }

    /** Called only at the normal portrait load site; Beta paths never enter this branch. */
    public static Texture loadSingleCardViewPortrait(SingleCardViewPopup popup, String originalPath) {
        if (popup == null || SINGLE_VIEW_CARD_FIELD == null
                || originalPath == null || !originalPath.startsWith("images/1024Portraits/")) {
            return ImageMaster.loadImage(originalPath);
        }

        try {
            Object value = SINGLE_VIEW_CARD_FIELD.get(popup);
            if (value instanceof AbstractCard) {
                AbstractCard card = (AbstractCard)value;
                String path = isEnabled(card) ? getSingleCardViewPortraitPath(card) : null;
                if (path != null && originalPath.equals("images/1024Portraits/" + card.assetUrl + ".png")) {
                    Texture texture = ImageMaster.loadImage(path);
                    if (texture != null) {
                        return texture;
                    }
                }
            }
        } catch (Exception exception) {
            FeminizationMod.logger.warn("Unable to load feminized card portrait; using original art.", exception);
        }
        return ImageMaster.loadImage(originalPath);
    }

    private static TextureAtlas.AtlasRegion findAtlasRegion(AbstractCard card) {
        if (!isSupportedAssetUrl(card.assetUrl) || isBetaPlaceholder(card.assetUrl)) {
            return null;
        }

        // Atlas entries are not sufficient evidence that this card has a usable replacement.
        // Require the matching portrait asset as well, so missing art keeps the card's original image.
        if (!hasPortrait(card.assetUrl)) {
            return null;
        }

        Map<String, TextureAtlas.AtlasRegion> regions = getAtlasRegions();
        return regions == null ? null : regions.get(card.assetUrl);
    }

    private static Map<String, TextureAtlas.AtlasRegion> getAtlasRegions() {
        if (cardAtlasRegions == null && isAnyCardArtEnabled()
                && (nextAtlasAttempt == 0L || System.nanoTime() - nextAtlasAttempt >= 0L)) {
            try {
                if (!exists(CARD_ATLAS_PATH)) {
                    throw new IOException("Missing card atlas: " + CARD_ATLAS_PATH);
                }
                TextureAtlas loaded = ResourceUtil.loadAtlas(CARD_ATLAS_PATH);
                try {
                    cardAtlasRegions = indexAtlas(loaded);
                    cardAtlas = loaded;
                } catch (RuntimeException exception) {
                    ResourceUtil.disposeAtlas(loaded);
                    throw exception;
                }
                nextAtlasAttempt = 0L;
                atlasFailureLogged = false;
            } catch (Exception exception) {
                nextAtlasAttempt = System.nanoTime() + ATLAS_RETRY_DELAY_NANOS;
                if (!atlasFailureLogged) {
                    atlasFailureLogged = true;
                    FeminizationMod.logger.warn("Unable to load feminized card atlas; using original art. Will retry in 30 seconds or when settings change.", exception);
                }
            }
        }
        return cardAtlasRegions;
    }

    /**
     * Flattens an atlas into a hash map keyed by region name. Later duplicates are ignored so the result
     * matches {@link TextureAtlas#findRegion(String)}, which returns the first match in insertion order.
     */
    private static Map<String, TextureAtlas.AtlasRegion> indexAtlas(TextureAtlas atlas) {
        if (atlas == null) {
            return null;
        }

        Map<String, TextureAtlas.AtlasRegion> regions = new HashMap<String, TextureAtlas.AtlasRegion>();
        for (TextureAtlas.AtlasRegion region : atlas.getRegions()) {
            if (region == null || region.name == null) {
                continue;
            }
            if (!regions.containsKey(region.name)) {
                regions.put(region.name, region);
            }
        }
        return regions;
    }

    private static String getSingleCardViewPortraitPath(AbstractCard card) {
        if (card == null || !isEligibleCard(card) || !isSupportedAssetUrl(card.assetUrl)
                || isBetaPlaceholder(card.assetUrl)) {
            return null;
        }

        String path = PORTRAIT_PREFIX + card.assetUrl + ".png";
        return hasPortrait(card.assetUrl) ? path : null;
    }

    private static boolean hasPortrait(String assetUrl) {
        Boolean cached = PORTRAIT_EXISTS.get(assetUrl);
        if (cached == null) {
            cached = Boolean.valueOf(exists(PORTRAIT_PREFIX + assetUrl + ".png"));
            PORTRAIT_EXISTS.put(assetUrl, cached);
        }
        return cached.booleanValue();
    }

    private static boolean isEnabled(AbstractCard card) {
        return card != null && isEnabled(card.color);
    }

    private static boolean isEnabled(AbstractCard.CardColor color) {
        if (color == null) {
            return false;
        }

        switch (color) {
            case RED:
                return activeIroncladCardFeminization;
            case GREEN:
                return activeSilentCardFeminization;
            case BLUE:
                return activeDefectCardFeminization;
            case PURPLE:
                return activeWatcherCardFeminization;
            case CURSE:
                return activeCurseCardFeminization;
            case COLORLESS:
                return activeColorlessCardFeminization;
            default:
                return false;
        }
    }

    private static boolean isSupportedAssetUrl(String assetUrl) {
        if (assetUrl == null || assetUrl.length() == 0 || assetUrl.indexOf("..") >= 0) {
            return false;
        }

        return assetUrl.startsWith("red/")
                || assetUrl.startsWith("green/")
                || assetUrl.startsWith("blue/")
                || assetUrl.startsWith("purple/")
                || assetUrl.startsWith("colorless/")
                || assetUrl.startsWith("curse/")
                || assetUrl.startsWith("status/");
    }

    /**
     * Some cards from other mods use the vanilla Beta art path as a placeholder when no portrait
     * was supplied. Never resolve that placeholder through our feminized atlas; retaining the card's
     * original portrait is preferable to replacing it with a Beta image.
     */
    private static boolean isBetaPlaceholder(String assetUrl) {
        return assetUrl != null && (assetUrl.equals("colorless/skill/beta")
                || assetUrl.equals("status/beta"));
    }

    private static boolean isVanillaCard(AbstractCard card) {
        if (card == null) {
            return false;
        }

        Class<?> cardClass = card.getClass();
        Boolean cached = vanillaCardClassCache.get(cardClass);
        if (cached != null) {
            return cached.booleanValue();
        }

        Package cardPackage = cardClass.getPackage();
        boolean vanilla = cardPackage != null && cardPackage.getName().startsWith(VANILLA_CARD_PACKAGE_PREFIX);
        synchronized (CardArtFeminization.class) {
            Map<Class<?>, Boolean> updated = new IdentityHashMap<Class<?>, Boolean>(vanillaCardClassCache);
            updated.put(cardClass, Boolean.valueOf(vanilla));
            vanillaCardClassCache = updated;
        }
        return vanilla;
    }

    private static boolean isEligibleCard(AbstractCard card) {
        return isVanillaCard(card) || FeminizationMod.activeOtherModFeminization;
    }

    private static boolean exists(String path) {
        try {
            return Gdx.files.internal(path).exists();
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Field findOptionalField(Class<?> type, String name) {
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

    private static class PortraitState {
        private final TextureAtlas.AtlasRegion portrait;
        private final TextureAtlas.AtlasRegion jokePortrait;
        private int depth;

        private PortraitState(TextureAtlas.AtlasRegion portrait, TextureAtlas.AtlasRegion jokePortrait) {
            this.portrait = portrait;
            this.jokePortrait = jokePortrait;
            this.depth = 1;
        }
    }
}
