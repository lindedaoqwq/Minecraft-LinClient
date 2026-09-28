package com.lindedaoqwq.linclient.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import java.util.Map;

/**
 * Core mod entry point. Registers the engine-level bytecode transformer that patches
 * Minecraft's own hot paths (lightmap updates, hurt camera, particle spawning).
 */
@IFMLLoadingPlugin.Name("LinClientCore")
@IFMLLoadingPlugin.MCVersion("1.8.9")
@IFMLLoadingPlugin.TransformerExclusions({"com.lindedaoqwq.linclient.core."})
public class LinClientCorePlugin implements IFMLLoadingPlugin {
    @Override
    public String[] getASMTransformerClass() {
        return new String[]{"com.lindedaoqwq.linclient.core.LinClientTransformer"};
    }
    @Override
    public String getModContainerClass() { return null; }
    @Override
    public String getSetupClass() { return null; }
    @Override
    public void injectData(Map<String, Object> data) { }
    @Override
    public String getAccessTransformerClass() { return null; }
}
