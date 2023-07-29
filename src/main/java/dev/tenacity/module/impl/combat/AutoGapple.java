package dev.tenacity.module.impl.combat;

import dev.tenacity.event.impl.player.UpdateEvent;
import dev.tenacity.module.Category;
import dev.tenacity.module.Module;
import dev.tenacity.module.settings.impl.NumberSetting;
import dev.tenacity.utils.player.InventoryUtils;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;

public class AutoGapple extends Module {
    private final NumberSetting minHealHP = new NumberSetting("Heal HP",12,20,1,0.5);
    int slot;
    private int oldSlot = -1;
    private int eatingTicks = 0;

    public AutoGapple() {
        super("AutoGApple", Category.COMBAT, "auto eats golden apples");
        this.addSettings(minHealHP);
    }

    @Override
    public void onUpdateEvent(UpdateEvent e){
        if (eatingTicks == 0 && mc.thePlayer.getHealth() < minHealHP.getValue()){
            slot = getAppleFromInventory();
            if (slot != -1){
                slot -= 36;
                oldSlot = mc.thePlayer.inventory.currentItem;
                mc.thePlayer.inventory.currentItem = slot;
                mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getCurrentItem()));
                mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(slot + 1 >= 9 ? 0 : slot + 1));  // switch to next slot
                mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(slot)); // switch back to apple
                eatingTicks = 40; // Eating in Minecraft takes 32 ticks, we add some extra time to be safe
            }
        } else if (eatingTicks > 0) {
            eatingTicks--;
            if (eatingTicks == 0 && oldSlot != -1) {
                mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(oldSlot));
            }
        }
    }

    private int getAppleFromInventory() {
        for (int i = 36; i < 45; i++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack == null) continue;

            Item item = stack.getItem();
            if (item == null || InventoryUtils.isItemEmpty(item)) continue;

            if (item != Items.golden_apple) continue;

            return i;
        }

        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack == null) continue;

            Item item = stack.getItem();
            if (item == null || InventoryUtils.isItemEmpty(item)) continue;

            if (item != Items.golden_apple) continue;

            mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, i, slot ,2, mc.thePlayer);
        }

        return -1;
    }
}