package tw.nekomimi.nekogram.settings;

import android.text.TextUtils;
import android.view.View;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.StickersAlert;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;
import java.util.List;

import tw.nekomimi.nekogram.helpers.BlockedStickerPacks;
import tw.nekomimi.nekogram.helpers.BlockedStickerPacksController;

public class NekoBlockedStickerPacksActivity extends BaseNekoSettingsActivity implements NotificationCenter.NotificationCenterDelegate {
    public NekoBlockedStickerPacksActivity() {
        this(UserConfig.selectedAccount);
    }

    public NekoBlockedStickerPacksActivity(int account) {
        setCurrentAccount(account);
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        getNotificationCenter().addObserver(this, NotificationCenter.blockedStickerPacksChanged);
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        getNotificationCenter().removeObserver(this, NotificationCenter.blockedStickerPacksChanged);
        super.onFragmentDestroy();
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asShadow(LocaleController.getString(R.string.BlockedStickerPacksInfo)));
        List<BlockedStickerPacks.Pack> packs = BlockedStickerPacksController.getInstance(currentAccount).getPacks();
        if (packs.isEmpty()) {
            items.add(UItem.asShadow(LocaleController.getString(R.string.BlockedStickerPacksEmpty)));
            return;
        }
        int id = 1;
        for (BlockedStickerPacks.Pack pack : packs) {
            String title = !TextUtils.isEmpty(pack.title) ? pack.title : !TextUtils.isEmpty(pack.shortName) ? pack.shortName : Long.toString(pack.id);
            UItem item = UItem.asButton(id++, R.drawable.msg_sticker, title, LocaleController.getString(R.string.UnblockStickerPack));
            item.object = pack;
            items.add(item);
        }
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        if (!(item.object instanceof BlockedStickerPacks.Pack)) return;
        BlockedStickerPacks.Pack pack = (BlockedStickerPacks.Pack) item.object;
        TLRPC.TL_messages_stickerSet cached = getMediaDataController().getStickerSetById(pack.id);
        ItemOptions.makeOptions(this, view)
                .add(R.drawable.msg_cancel, LocaleController.getString(R.string.UnblockStickerPack), () -> BlockedStickerPacksController.getInstance(currentAccount).unblock(pack.id))
                .addIf(cached != null, R.drawable.msg_sticker, LocaleController.getString(R.string.ViewStickerPack), () -> {
                    showDialog(new StickersAlert(getParentActivity(), this, null, cached, null, false));
                })
                .show();
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (listView != null) listView.adapter.update(true);
    }

    @Override
    protected String getActionBarTitle() {
        return LocaleController.getString(R.string.BlockedStickerPacks);
    }

    @Override
    protected String getKey() {
        return "blockedstickers";
    }
}
