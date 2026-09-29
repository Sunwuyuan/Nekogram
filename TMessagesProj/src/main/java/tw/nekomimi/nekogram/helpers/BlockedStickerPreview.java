package tw.nekomimi.nekogram.helpers;

import android.view.View;

import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ContentPreviewViewer;

/** Shared emoji drawables must not be revealed globally when one cell is tapped. */
public final class BlockedStickerPreview {
    private final BlockedStickerRevealState viewed = new BlockedStickerRevealState();

    public boolean onClick(View view, ImageReceiver receiver, TLRPC.Document document) {
        if (receiver == null || document == null) return false;
        int account = receiver.getCurrentAccount();
        BlockedStickerPacksController controller = BlockedStickerPacksController.getInstance(account);
        TLRPC.InputStickerSet set = MessageObject.getInputStickerSet(document);
        viewed.bind(account, document.id, set == null ? 0 : set.id, set == null ? null : set.short_name, view);
        if (!controller.isBlocked(set) || viewed.isRevealed(controller.getRevision())) return false;
        if (ContentPreviewViewer.getInstance().showBlockedStickerPreview(view, document, account)) {
            viewed.reveal(controller.getRevision());
        }
        return true;
    }

    public void reset() {
        viewed.reset();
    }
}
