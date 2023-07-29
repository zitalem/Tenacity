package dev.tenacity.module.api;

import dev.tenacity.module.Category;
import dev.tenacity.module.Module;
import dev.tenacity.module.settings.impl.BooleanSetting;
import dev.tenacity.module.settings.impl.MultipleBoolSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

public class TargetManager extends Module {

    public static MultipleBoolSetting targetType = new MultipleBoolSetting(
            "Target Type",
            new BooleanSetting("Players", true),
            new BooleanSetting("Mobs", false),
            new BooleanSetting("Animals", false),
            new BooleanSetting("Invisibles", true)
    );

    public static EntityLivingBase target;

    public TargetManager() {
        super("Target", Category.COMBAT, "");
        addSettings(targetType);
    }

    public static boolean checkEntity(Entity entity) {
        if (entity instanceof EntityPlayer && !targetType.isEnabled("Players")) return false;
        if (entity.getClass().getPackage().getName().contains("monster") && !targetType.isEnabled("Mobs")) return false;
        if (entity.getClass().getPackage().getName().contains("passive") && !targetType.isEnabled("Animals")) return false;
        return !entity.isInvisible() || targetType.isEnabled("Invisibles");
    }
}
