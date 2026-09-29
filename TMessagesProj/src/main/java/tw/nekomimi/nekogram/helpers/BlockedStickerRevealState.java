package tw.nekomimi.nekogram.helpers;

import java.util.Objects;

/** A reveal belongs to one binding, not to a cached document or an entire pack. */
public final class BlockedStickerRevealState {
    private int account = -1;
    private long documentId;
    private long packId;
    private String shortName;
    private Object parent;
    private long revealedRevision = -1;

    public void bind(int account, long documentId, long packId, String shortName, Object parent) {
        if (this.account != account || this.documentId != documentId || this.packId != packId || !Objects.equals(this.shortName, shortName) || this.parent != parent) {
            reset();
        }
        this.account = account;
        this.documentId = documentId;
        this.packId = packId;
        this.shortName = shortName;
        this.parent = parent;
    }

    public boolean isRevealed(long revision) {
        return revealedRevision == revision;
    }

    public void reveal(long revision) {
        revealedRevision = revision;
    }

    public void reset() {
        revealedRevision = -1;
    }
}
