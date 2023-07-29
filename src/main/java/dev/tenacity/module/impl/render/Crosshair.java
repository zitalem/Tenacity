package dev.tenacity.module.impl.render;

import dev.tenacity.module.Category;
import dev.tenacity.module.Module;
import dev.tenacity.module.settings.impl.BooleanSetting;
import dev.tenacity.module.settings.impl.NumberSetting;

public final class Crosshair extends Module {

    private final BooleanSetting dynamic = new BooleanSetting("Dynamic",false);
    private final NumberSetting gap = new NumberSetting("Crosshair Gap",5,15,0.25,0.25);
    private final NumberSetting width = new NumberSetting("Crosshair Width",5,10,1,0.25);
    private final NumberSetting size = new NumberSetting("Crosshair Size/Length",5,15,1,0.25);
    public Crosshair() {
        super("Crosshair", Category.RENDER, "Personalizes your own crosshair.");
    }
}
