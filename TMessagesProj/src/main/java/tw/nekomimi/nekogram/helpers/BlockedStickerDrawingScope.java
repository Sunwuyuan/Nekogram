package tw.nekomimi.nekogram.helpers;

/** Suppresses account-local display filtering only during an explicit preview or media export. */
public final class BlockedStickerDrawingScope implements AutoCloseable {
    private static final ThreadLocal<Boolean> unfiltered = new ThreadLocal<>();
    private final boolean previous;

    public BlockedStickerDrawingScope() {
        previous = isUnfiltered();
        unfiltered.set(true);
    }

    public static boolean isUnfiltered() {
        return Boolean.TRUE.equals(unfiltered.get());
    }

    @Override
    public void close() {
        if (previous) unfiltered.set(true);
        else unfiltered.remove();
    }
}
