package com.lindedaoqwq.linclient;

import com.lindedaoqwq.linclient.config.KeyBindings;
import com.lindedaoqwq.linclient.config.ModConfig;
import com.lindedaoqwq.linclient.event.ClientEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * LinClient - a practical, open-source, NON-CHEATING Minecraft client mod.
 *
 * Scope (client side only, no server-side behaviour changes, no hidden server info):
 *   - HUD information display (self / entities / input / environment / items / other / combat / boss)
 *   - Client-side performance & visual toggles
 *   - Fully configurable via Forge config + a runtime HUD editor
 */
@Mod(LinClient.MOD_ID)
public class LinClient {
    public static final String MOD_ID = "linclient";
    public static final String MOD_NAME = "LinClient";
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    public LinClient() {
        // Register the client config (written to config/linclient-client.toml)
        FMLJavaModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.CLIENT, ModConfig.SPEC);

        if (FMLEnvironment.dist.isClient()) {
            IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
            modBus.addListener(KeyBindings::register);
            // All rendering / input / tick handling lives in ClientEvents (client only).
            MinecraftForge.EVENT_BUS.register(new ClientEvents());
            LOGGER.info("LinClient initialized (client).");
        }
    }
}
