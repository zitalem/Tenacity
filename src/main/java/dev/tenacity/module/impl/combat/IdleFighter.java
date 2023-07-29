package dev.tenacity.module.impl.combat;

import dev.tenacity.Tenacity;
import dev.tenacity.commands.impl.FriendCommand;
import dev.tenacity.event.impl.player.AttackEvent;
import dev.tenacity.event.impl.player.MotionEvent;
import dev.tenacity.module.Category;
import dev.tenacity.module.Module;
import dev.tenacity.module.api.TargetManager;
import dev.tenacity.module.settings.impl.BooleanSetting;
import dev.tenacity.module.settings.impl.MultipleBoolSetting;
import dev.tenacity.module.settings.impl.NumberSetting;
import dev.tenacity.utils.misc.MathUtils;
import dev.tenacity.utils.player.RotationUtils;
import dev.tenacity.utils.time.TimerUtil;
import dev.tenacity.viamcp.utils.AttackOrder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class IdleFighter extends Module {

    private final List<EntityLivingBase> targets = new ArrayList<>();

    private final NumberSetting minCPS = new NumberSetting("Min CPS", 10, 20, 1, 1);
    private final NumberSetting maxCPS = new NumberSetting("Max CPS", 10, 20, 1, 1);

    private final NumberSetting reach = new NumberSetting("Reach", 4, 6, 3, 0.1);

    private final TimerUtil attackTimer = new TimerUtil();

    public IdleFighter() {
        super("IdleFighter", Category.COMBAT, "Automatically finds the nearest player and attempts to kill them");
        this.addSettings(minCPS, maxCPS, reach);
    }

    @Override
    public void onMotionEvent(MotionEvent event) {
        if (minCPS.getValue() > maxCPS.getValue()) {
            minCPS.setValue(minCPS.getValue() - 1);
        }

        mc.gameSettings.keyBindForward.pressed = TargetManager.target != null && !(mc.thePlayer.getDistanceToEntity(TargetManager.target) <= reach.getValue());
        mc.gameSettings.keyBindJump.pressed = mc.thePlayer.isCollidedHorizontally || mc.thePlayer.isInWater();

        if (event.isPre()) {
            sortTargets();
            if (!targets.isEmpty()) {
                TargetManager.target = targets.get(0);
                final float[] rotations = RotationUtils.getRotations(TargetManager.target.posX, TargetManager.target.posY, TargetManager.target.posZ);
                mc.thePlayer.rotationYaw = rotations[0];
                mc.thePlayer.rotationPitch = rotations[1];

                if (mc.thePlayer.getDistanceToEntity(TargetManager.target) <= reach.getValue() && attackTimer.hasTimeElapsed(1000 / MathUtils.getRandomInRange(minCPS.getValue(), maxCPS.getValue()))) {
                    AttackEvent attackEvent = new AttackEvent(TargetManager.target);
                    Tenacity.INSTANCE.getEventProtocol().handleEvent(attackEvent);

                    if (!attackEvent.isCancelled()) {
                        AttackOrder.sendFixedAttack(mc.thePlayer, TargetManager.target);
                    }
                    attackTimer.reset();
                }
            } else {
                TargetManager.target = null;
            }
        }
    }

    @Override
    public void onDisable() {
        TargetManager.target = null;
        mc.gameSettings.keyBindForward.pressed = false;
        targets.clear();
        super.onDisable();
    }

    private void sortTargets() {
        targets.clear();
        for (Entity entity : mc.theWorld.getLoadedEntityList()) {
            if (entity instanceof EntityLivingBase) {
                EntityLivingBase entityLivingBase = (EntityLivingBase) entity;
                if (TargetManager.checkEntity(entity) && mc.thePlayer != entityLivingBase && !FriendCommand.isFriend(entityLivingBase.getName())) {
                    targets.add(entityLivingBase);
                }
            }
        }
        targets.sort(Comparator.comparingDouble(mc.thePlayer::getDistanceToEntity));
    }

}
