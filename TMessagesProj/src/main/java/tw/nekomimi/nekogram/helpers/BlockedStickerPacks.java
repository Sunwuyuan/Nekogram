package tw.nekomimi.nekogram.helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Account-local data only; never changes the installed sticker sets. */
public final class BlockedStickerPacks {
    public static final class Pack {
        public final long id;
        public final String shortName;
        public final String title;

        public Pack(long id, String shortName, String title) {
            this.id = id;
            this.shortName = shortName == null ? "" : shortName;
            this.title = title == null ? "" : title;
        }
    }

    private final Map<Long, Pack> packs = new HashMap<>();
    private final Set<String> names = new HashSet<>();
    private volatile long revision;

    public BlockedStickerPacks(Set<String> stored) {
        if (stored != null) {
            for (String value : stored) {
                if (value == null) continue;
                // Short names cannot contain newlines; titles may contain any text.
                String[] fields = value.split("\n", 3);
                if (fields.length != 3) continue;
                try {
                    long id = Long.parseLong(fields[0]);
                    if (id != 0) packs.put(id, new Pack(id, fields[1], fields[2]));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        updateNames();
    }

    public synchronized boolean contains(long id, String shortName) {
        // An explicit ID is authoritative, even if an old short name was reused.
        return id != 0 ? packs.containsKey(id) : shortName != null && !shortName.isEmpty() && names.contains(shortName.toLowerCase(Locale.ROOT));
    }

    public synchronized boolean block(Pack pack) {
        if (pack == null || pack.id == 0 || packs.containsKey(pack.id)) return false;
        packs.put(pack.id, pack);
        changed();
        return true;
    }

    public synchronized boolean unblock(long id) {
        if (packs.remove(id) == null) return false;
        changed();
        return true;
    }

    public synchronized void clear() {
        packs.clear();
        changed();
    }

    public long getRevision() {
        return revision;
    }

    public synchronized List<Pack> getPacks() {
        ArrayList<Pack> result = new ArrayList<>(packs.values());
        result.sort((a, b) -> {
            int order = a.title.compareToIgnoreCase(b.title);
            return order == 0 ? Long.compare(a.id, b.id) : order;
        });
        return result;
    }

    public synchronized Set<String> serialize() {
        Set<String> result = new HashSet<>();
        for (Pack pack : packs.values()) {
            result.add(pack.id + "\n" + pack.shortName + "\n" + pack.title);
        }
        return result;
    }

    private void changed() {
        updateNames();
        revision++;
    }

    private void updateNames() {
        names.clear();
        for (Pack pack : packs.values()) {
            if (!pack.shortName.isEmpty()) names.add(pack.shortName.toLowerCase(Locale.ROOT));
        }
    }
}
