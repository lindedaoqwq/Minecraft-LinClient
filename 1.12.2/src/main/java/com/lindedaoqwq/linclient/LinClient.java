package com.lindedaoqwq.linclient;

import com.lindedaoqwq.linclient.config.KeyBindings;
import com.lindedaoqwq.linclient.config.ModConfig;
import com.lindedaoqwq.linclient.event.ClientEvents;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * LinClient - a practical, open-source, NON-CHEATING Minecraft client mod (Forge 1.12.2).
 *
 * Scope (client side only, no server-side behaviour changes, no hidden server info):
 *   - HUD information display (self / entities / input / environment / items / other / combat)
 *   - Client-side visual & performance toggles
 *   - An in-game ClickGUI opened with Right Shift, plus a standalone Home screen (F8)
 */
@Mod(modid = LinClient.MOD_ID, name = LinClient.MOD_NAME, version = LinClient.VERSION,
        clientSideOnly = true, acceptedMinecraftVersions = "[1.12.2]")
public class LinClient {
    public static final String MOD_ID = "linclient";
    public static final String MOD_NAME = "LinClient";
    public static final String VERSION = "1.0.0";
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ModConfig.init(new Configuration(event.getSuggestedConfigurationFile()));
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        KeyBindings.register();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new ClientEvents());
        LOGGER.info("LinClient initialized (client, 1.12.2).");
    }
}
