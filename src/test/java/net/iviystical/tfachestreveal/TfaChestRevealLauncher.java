package net.iviystical.tfachestreveal;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public final class TfaChestRevealLauncher
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(TfaChestRevealPlugin.class);
        RuneLite.main(args);
    }
}
