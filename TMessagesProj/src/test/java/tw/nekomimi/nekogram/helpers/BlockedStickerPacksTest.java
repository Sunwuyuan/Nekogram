package tw.nekomimi.nekogram.helpers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Run with Tools/test_blocked_sticker_packs.py; no Android SDK or test dependency required. */
public final class BlockedStickerPacksTest {
    private static int assertions;

    public static void main(String[] args) {
        persistence();
        identities();
        corruptStorage();
        accountIsolationAndLogout();
        revealLifecycle();
        drawingScope();
        System.out.println("Blocked sticker packs: " + assertions + " assertions passed");
    }

    private static void persistence() {
        BlockedStickerPacks packs = new BlockedStickerPacks(null);
        check(packs.getPacks().isEmpty(), "empty on first use");
        check(!packs.block(null), "reject null pack");
        check(!packs.block(new BlockedStickerPacks.Pack(0, "", "")), "reject missing ID");
        String title = "Cats 😺\nSecond line\tand tabs";
        check(packs.block(new BlockedStickerPacks.Pack(Long.MAX_VALUE, "Cats", title)), "block pack");
        long revision = packs.getRevision();
        check(!packs.block(new BlockedStickerPacks.Pack(Long.MAX_VALUE, "Cats", title)), "deduplicate IDs");
        check(packs.getRevision() == revision, "no-op does not reset reveals");
        check(packs.block(new BlockedStickerPacks.Pack(Long.MIN_VALUE, null, null)), "preserve all 64 ID bits");
        BlockedStickerPacks restored = new BlockedStickerPacks(packs.serialize());
        check(restored.contains(Long.MAX_VALUE, null), "restore large ID");
        check(restored.contains(Long.MIN_VALUE, null), "restore signed ID");
        List<BlockedStickerPacks.Pack> list = restored.getPacks();
        check(list.get(1).title.equals(title), "lossless Unicode and multiline title");
        list.clear();
        check(restored.getPacks().size() == 2, "list is a defensive copy");
        Set<String> storage = restored.serialize();
        storage.clear();
        check(restored.getPacks().size() == 2, "serialized set is a defensive copy");
        check(restored.unblock(Long.MAX_VALUE), "unblock");
        check(!new BlockedStickerPacks(restored.serialize()).contains(Long.MAX_VALUE, "Cats"), "persist unblock");
        check(!restored.unblock(Long.MAX_VALUE), "unblock is idempotent");
    }

    private static void identities() {
        BlockedStickerPacks packs = new BlockedStickerPacks(null);
        packs.block(new BlockedStickerPacks.Pack(123, "CaTs", "Cats"));
        check(packs.contains(123, null), "document ID lookup");
        check(packs.contains(123, "RenamedCats"), "renaming cannot bypass an ID block");
        check(packs.contains(0, "cats"), "case insensitive short-name lookup");
        check(packs.contains(0, "CATS"), "uppercase lookup");
        check(!packs.contains(124, "CaTs"), "reused short name cannot override explicit ID");
        check(!packs.contains(0, null), "null is not a pack");
        check(!packs.contains(0, ""), "empty is not a pack");
        packs.unblock(123);
        check(!packs.contains(0, "cats"), "unblock removes aliases");
    }

    private static void corruptStorage() {
        Set<String> values = new HashSet<>(Arrays.asList(null, "", "abc\nCats\nCats", "123", "123\nCats", "0\nCats\nCats", "999999999999999999999999\nCats\nCats", "7\nGood\nGood pack"));
        BlockedStickerPacks packs = new BlockedStickerPacks(values);
        values.clear();
        check(packs.getPacks().size() == 1, "skip malformed records individually");
        check(packs.contains(7, null), "valid data survives malformed neighbours");
        check(packs.serialize().equals(Set.of("7\nGood\nGood pack")), "discard corrupt data when saving");
    }

    private static void accountIsolationAndLogout() {
        BlockedStickerPacks a = new BlockedStickerPacks(null);
        BlockedStickerPacks b = new BlockedStickerPacks(null);
        a.block(new BlockedStickerPacks.Pack(1, "Cats", "Cats"));
        check(!b.contains(1, null), "accounts do not share a block list");
        b.block(new BlockedStickerPacks.Pack(2, "Dogs", "Dogs"));
        check(!a.contains(2, null), "reverse account isolation");
        long revision = a.getRevision();
        a.clear();
        check(a.getPacks().isEmpty() && a.serialize().isEmpty(), "logout clears memory and persistence data");
        check(a.getRevision() > revision, "logout invalidates old reveals");
        check(b.contains(2, null), "logout leaves the other account untouched");
    }

    private static void revealLifecycle() {
        BlockedStickerRevealState state = new BlockedStickerRevealState();
        Object firstMessage = new Object();
        state.bind(0, 10, 20, null, firstMessage);
        check(!state.isRevealed(0), "blocked by default");
        state.reveal(0);
        check(state.isRevealed(0), "tap reveals");
        state.bind(0, 10, 20, null, firstMessage);
        check(state.isRevealed(0), "same binding redraw stays revealed");
        check(!state.isRevealed(1), "block-list changes re-hide");
        state.bind(0, 10, 20, null, new Object());
        check(!state.isRevealed(0), "same sticker in another message is hidden");
        state.reveal(1);
        state.bind(1, 10, 20, null, firstMessage);
        check(!state.isRevealed(1), "account switch resets reveal");
        state.reveal(1);
        state.bind(1, 11, 20, null, firstMessage);
        check(!state.isRevealed(1), "recycled cell with another document is hidden");
        state.reveal(1);
        state.bind(1, 11, 21, null, firstMessage);
        check(!state.isRevealed(1), "changed pack resets reveal");
        state.reveal(1);
        state.bind(1, 11, 21, "Cats", firstMessage);
        check(!state.isRevealed(1), "changed short-name binding resets reveal");
        state.reveal(1);
        state.reset();
        check(!state.isRevealed(1), "detach resets reveal");
        state.reveal(1);
        state.bind(1, 0, 0, null, null);
        check(!state.isRevealed(1), "placeholder clears reveal");
    }

    private static void drawingScope() {
        check(!BlockedStickerDrawingScope.isUnfiltered(), "ordinary rendering remains filtered");
        try (BlockedStickerDrawingScope outer = new BlockedStickerDrawingScope()) {
            check(BlockedStickerDrawingScope.isUnfiltered(), "export bypasses display filtering");
            try (BlockedStickerDrawingScope inner = new BlockedStickerDrawingScope()) {
                check(BlockedStickerDrawingScope.isUnfiltered(), "nested preview remains unfiltered");
            }
            check(BlockedStickerDrawingScope.isUnfiltered(), "nested scope restores its caller");
            boolean[] workerUnfiltered = {true};
            Thread worker = new Thread(() -> workerUnfiltered[0] = BlockedStickerDrawingScope.isUnfiltered());
            worker.start();
            try {
                worker.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
            check(!workerUnfiltered[0], "exports cannot reveal concurrent background rendering");
        }
        check(!BlockedStickerDrawingScope.isUnfiltered(), "filtering resumes after export");
        try {
            try (BlockedStickerDrawingScope ignored = new BlockedStickerDrawingScope()) {
                throw new IllegalStateException("drawing failed");
            }
        } catch (IllegalStateException expected) {
            check(!BlockedStickerDrawingScope.isUnfiltered(), "failed exports restore filtering");
        }
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}
