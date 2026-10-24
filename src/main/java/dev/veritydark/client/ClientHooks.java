package dev.veritydark.client;

import net.minecraft.client.Minecraft;

/** Client-only entry points called through DistExecutor, so common classes never touch client classes directly. */
public final class ClientHooks {
    public static void openJournal() {
        Minecraft.getInstance().setScreen(new JournalScreen());
    }

    private ClientHooks() {}
}
