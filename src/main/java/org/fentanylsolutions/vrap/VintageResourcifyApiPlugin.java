package org.fentanylsolutions.vrap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(
    modid = VintageResourcifyApiPlugin.MODID,
    name = VintageResourcifyApiPlugin.NAME,
    version = Tags.VERSION,
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = "required-after:resourcify;",
    guiFactory = "org.fentanylsolutions.vrap.gui.VrapGuiFactory",
    acceptableRemoteVersions = "*")
public class VintageResourcifyApiPlugin {

    public static final String MODID = "vrap";
    public static final String NAME = "Vintage Resourcify API Plugin";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        VrapConfig.load(event.getSuggestedConfigurationFile());
        CurseTokenProvider.registerResourcifyConfigHook();
        CurseTokenProvider.fetchAndApplyAsync("game boot");
    }
}
