package dev.veritydark.client;

import dev.veritydark.Journal;

/** Plain data holder for the synced journal pages (safe on any side). */
public final class ClientJournal {
    public static long mask = Journal.defaultMask();

    public static boolean has(Journal.Entry entry) {
        return (mask & entry.bit()) != 0L;
    }

    private ClientJournal() {}
}
