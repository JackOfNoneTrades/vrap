package org.fentanylsolutions.vrap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(
    modid = VintageResourcifyApiPlugin.MODID,
    name = VintageResourcifyApiPlugin.NAME,
    version = Tags.VERSION,
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = VintageResourcifyApiPlugin.RESOURCIFY_DEPENDENCIES,
    guiFactory = "org.fentanylsolutions.vrap.gui.VrapGuiFactory",
    customProperties = { @Mod.CustomProperty(k = "license", v = "WTFPL"),
        @Mod.CustomProperty(k = "issueTrackerUrl", v = "https://github.com/JackOfNoneTrades/vrap/issues"),
        @Mod.CustomProperty(k = "iconFile", v = "assets/vrap/logo.png") },
    acceptableRemoteVersions = "*")
public class VintageResourcifyApiPlugin {

    public static final String MODID = "vrap";
    public static final String NAME = "Vintage Resourcify API Plugin";
    public static final String RESOURCIFY_MODID = "resourcify";
    public static final String VINTAGE_RESOURCIFY_MODID = "vintage-resourcify";
    public static final String RESOURCIFY_DEPENDENCIES = "after:" + VINTAGE_RESOURCIFY_MODID
        + ";after:"
        + RESOURCIFY_MODID
        + ";";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        String resourcifyModId = getLoadedResourcifyModId();
        if (resourcifyModId == null) {
            throw new IllegalStateException(
                NAME + " requires Vintage Resourcify under mod id '"
                    + VINTAGE_RESOURCIFY_MODID
                    + "' or '"
                    + RESOURCIFY_MODID
                    + "'.");
        }

        LOG.info("Found Vintage Resourcify using mod id {}", resourcifyModId);
        VrapConfig.load(event.getSuggestedConfigurationFile());
        CurseTokenProvider.registerResourcifyConfigHook();
        CurseTokenProvider.fetchAndApplyAsync("game boot");
    }

    public static boolean isResourcifyLoaded() {
        return getLoadedResourcifyModId() != null;
    }

    private static String getLoadedResourcifyModId() {
        if (Loader.isModLoaded(VINTAGE_RESOURCIFY_MODID)) return VINTAGE_RESOURCIFY_MODID;
        if (Loader.isModLoaded(RESOURCIFY_MODID)) return RESOURCIFY_MODID;
        return null;
    }
}
