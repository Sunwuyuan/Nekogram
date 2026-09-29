package tw.nekomimi.nekogram.helpers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.AnimatedEmojiDrawable;

public final class BlockedStickerFilter implements NotificationCenter.NotificationCenterDelegate {
    private final ImageReceiver receiver;
    private final BlockedStickerRevealState revealState = new BlockedStickerRevealState();
    private TLRPC.InputStickerSet stickerSet;
    private BlockedStickerPacksController controller;
    private int account;
    private int observingAccount = -1;
    private boolean attached;
    private long cachedRevision = -1;
    private boolean blocked;

    private BlockedStickerFilter(ImageReceiver receiver) {
        this.receiver = receiver;
    }

    public static BlockedStickerFilter bind(ImageReceiver receiver, BlockedStickerFilter filter, ImageLocation media, ImageLocation image, ImageLocation thumb, Object parent) {
        int account = parent instanceof MessageObject ? ((MessageObject) parent).currentAccount : receiver.getCurrentAccount();
        TLRPC.Document document = media != null ? media.document : null;
        if (document == null && image != null) document = image.document;
        if (document == null && thumb != null) document = thumb.document;
        if (document == null && parent instanceof TLRPC.Document) document = (TLRPC.Document) parent;
        if (document == null && parent instanceof MessageObject) document = ((MessageObject) parent).getDocument();
        if (parent instanceof AnimatedEmojiDrawable) {
            AnimatedEmojiDrawable emoji = (AnimatedEmojiDrawable) parent;
            document = emoji.getDocument();
            if (emoji.getImageReceiver() != null) account = emoji.getImageReceiver().getCurrentAccount();
        }
        TLRPC.InputStickerSet set = MessageObject.getInputStickerSet(document);
        if (set == null && document == null) {
            set = media != null ? media.stickerSet : null;
            if (set == null && image != null) set = image.stickerSet;
            if (set == null && thumb != null) set = thumb.stickerSet;
            TLRPC.StickerSet pack = parent instanceof TLRPC.TL_messages_stickerSet ? ((TLRPC.TL_messages_stickerSet) parent).set :
                    parent instanceof TLRPC.StickerSetCovered ? ((TLRPC.StickerSetCovered) parent).set : null;
            if (set == null && pack != null) {
                set = new TLRPC.TL_inputStickerSetID();
                set.id = pack.id;
            }
        }
        if (set == null) {
            if (filter != null) filter.detach();
            return null;
        }
        if (filter == null) filter = new BlockedStickerFilter(receiver);
        filter.revealState.bind(account, document == null ? 0 : document.id, set.id, set.short_name, parent);
        filter.account = account;
        filter.stickerSet = set;
        filter.controller = BlockedStickerPacksController.getInstance(account);
        filter.cachedRevision = -1;
        filter.updateObserver();
        return filter;
    }

    public boolean shouldHide() {
        long revision = controller.getRevision();
        if (cachedRevision != revision) {
            blocked = controller.isBlocked(stickerSet);
            cachedRevision = revision;
        }
        return blocked && !revealState.isRevealed(revision);
    }

    public boolean reveal() {
        if (!shouldHide()) return false;
        revealState.reveal(controller.getRevision());
        receiver.invalidate();
        return true;
    }

    public void resetReveal() {
        revealState.reset();
        receiver.invalidate();
    }

    public void attach() {
        attached = true;
        updateObserver();
    }

    public void detach() {
        attached = false;
        revealState.reset();
        updateObserver();
    }

    private void updateObserver() {
        int target = attached ? account : -1;
        if (observingAccount == target) return;
        if (observingAccount != -1) {
            NotificationCenter.getInstance(observingAccount).removeObserver(this, NotificationCenter.blockedStickerPacksChanged);
        }
        observingAccount = target;
        if (target != -1) {
            NotificationCenter.getInstance(target).addObserver(this, NotificationCenter.blockedStickerPacksChanged);
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        cachedRevision = -1;
        revealState.reset();
        receiver.invalidate();
    }

    /** Each drawing thread owns its tiny surface; shared image-cache bitmaps are never changed. */
    public static final class Mosaic {
        private static final int SIZE = 8;
        private final Bitmap bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
        private final Canvas canvas = new Canvas(bitmap);
        private final Paint paint = new Paint();
        private final RectF bounds = new RectF();
        private int saveCount;

        public Canvas begin(float x, float y, float width, float height) {
            bitmap.eraseColor(Color.TRANSPARENT);
            bounds.set(x, y, x + width, y + height);
            saveCount = canvas.save();
            canvas.scale(SIZE / width, SIZE / height);
            canvas.translate(-x, -y);
            return canvas;
        }

        public void end(Canvas target) {
            canvas.restoreToCount(saveCount);
            target.drawBitmap(bitmap, null, bounds, paint);
        }
    }
}
