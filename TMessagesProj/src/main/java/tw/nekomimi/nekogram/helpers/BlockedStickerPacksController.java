package tw.nekomimi.nekogram.helpers;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BaseController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import java.util.Collections;
import java.util.List;

public final class BlockedStickerPacksController extends BaseController {
    private static final String PREFERENCE_KEY = "tailgram_blocked_sticker_packs_v1";
    private static final BlockedStickerPacksController[] instances = new BlockedStickerPacksController[UserConfig.MAX_ACCOUNT_COUNT];

    public static synchronized BlockedStickerPacksController getInstance(int account) {
        if (instances[account] == null) {
            instances[account] = new BlockedStickerPacksController(account);
        }
        return instances[account];
    }

    private final BlockedStickerPacks packs;

    private BlockedStickerPacksController(int account) {
        super(account);
        packs = new BlockedStickerPacks(getUserConfig().getPreferences().getStringSet(PREFERENCE_KEY, Collections.emptySet()));
    }

    public boolean isBlocked(TLRPC.InputStickerSet set) {
        if (set instanceof TLRPC.TL_inputStickerSetID) {
            return packs.contains(set.id, null);
        } else if (set instanceof TLRPC.TL_inputStickerSetShortName) {
            TLRPC.TL_messages_stickerSet cached = getMediaDataController().getStickerSetByName(set.short_name);
            return packs.contains(cached != null && cached.set != null ? cached.set.id : 0, set.short_name);
        }
        return false;
    }

    public boolean isBlocked(TLRPC.StickerSet set) {
        return set != null && packs.contains(set.id, set.short_name);
    }

    public void toggle(TLRPC.StickerSet set) {
        if (set == null || set.id == 0) return;
        boolean changed = isBlocked(set) ? packs.unblock(set.id) : packs.block(new BlockedStickerPacks.Pack(set.id, set.short_name, set.title));
        if (changed) save();
    }

    public void unblock(long id) {
        if (packs.unblock(id)) save();
    }

    public List<BlockedStickerPacks.Pack> getPacks() {
        return packs.getPacks();
    }

    public long getRevision() {
        return packs.getRevision();
    }

    public void clear() {
        packs.clear();
        getUserConfig().getPreferences().edit().remove(PREFERENCE_KEY).apply();
        notifyChanged();
    }

    private void save() {
        getUserConfig().getPreferences().edit().putStringSet(PREFERENCE_KEY, packs.serialize()).apply();
        notifyChanged();
    }

    private void notifyChanged() {
        AndroidUtilities.runOnUIThread(() -> getNotificationCenter().postNotificationName(NotificationCenter.blockedStickerPacksChanged));
    }
}
