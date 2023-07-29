package dev.tenacity.module.impl.render.targethud;

import dev.tenacity.utils.animations.ContinualAnimation;
import dev.tenacity.utils.font.FontUtil;
import dev.tenacity.utils.render.ColorUtil;
import dev.tenacity.utils.render.RenderUtil;
import dev.tenacity.utils.render.RoundedUtil;
import dev.tenacity.utils.render.StencilUtil;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class VapeTargetHUD extends TargetHUD {

    public VapeTargetHUD() {
        super("Vape");
    }

    private final ContinualAnimation healthAnim = new ContinualAnimation();
    private final ContinualAnimation absAnim = new ContinualAnimation();

    @Override
    public void render(float x, float y, float alpha, EntityLivingBase target) {
        setWidth(110);
        setHeight(40);

        if (target instanceof AbstractClientPlayer) {
            StencilUtil.initStencilToWrite();
            RoundedUtil.drawRound(x + 4, y + 5.5f, 29, 29, 1, Color.BLACK);
            StencilUtil.readStencilBuffer(1);
            RenderUtil.color(-1, alpha);
            renderPlayer2D(x + 3, y + 4.5f, 31f, 31f, (AbstractClientPlayer) target);
            StencilUtil.uninitStencilBuffer();
            GlStateManager.disableBlend();
        } else {
            RoundedUtil.drawRound(x + 4, y + 5.5f, 29, 29, 1, ColorUtil.applyOpacity(new Color(30, 30, 30), alpha));
            FontUtil.tenacityBoldFont32.drawCenteredStringWithShadow("?", x + 18.5f, y + 20 - FontUtil.tenacityBoldFont32.getHeight() / 2f, ColorUtil.applyOpacity(-1, alpha));
        }

        tenacityBoldFont14.drawString(target.getName(), x + 36.5f, y + 5.5f, ColorUtil.applyOpacity(-1, alpha)); // y + 1.2f

        float targetHealth = target.getHealth();
        float targetMaxHealth = target.getMaxHealth();
        float targetAbsorptionAmount = target.getAbsorptionAmount();
        float targetHealthD = targetHealth / Math.max(targetMaxHealth, 1.0f);

        healthAnim.animate(targetHealthD, 18);

        Color color = ColorUtil.blendColors(new float[]{0.0f, 0.5f, 1.0f}, new Color[]{new Color(250, 50, 56), new Color(236, 129, 44), new Color(5, 134, 105)}, healthAnim.getOutput());

        RoundedUtil.drawRound(x + 37f, y + 12.6f + 1.2f, 68, 2f, 1, ColorUtil.applyOpacity(new Color(43, 42, 43), alpha));
        RoundedUtil.drawRound(x + 37f, y + 12.6f + 1.2f, 68f * healthAnim.getOutput(), 2f, 1, ColorUtil.applyOpacity(color, alpha));

        if (targetAbsorptionAmount > 0) {
            float absLength = 68f * Math.min(targetAbsorptionAmount / targetMaxHealth, 1);
            absAnim.animate(absLength, 18);
            RoundedUtil.drawRound(x + 37f + Math.max(68f * healthAnim.getOutput() - absAnim.getOutput(), 0), y + 12.6f + 1.2f, absAnim.getOutput(), 2f, 1, ColorUtil.applyOpacity(new Color(0xFFAA00), alpha));
        }

        String hp = (int) Math.ceil(targetHealth + targetAbsorptionAmount) + " hp";
        tenacityFont14.drawString(hp, x + 105 - tenacityFont14.getStringWidth(hp), y + 5.5f, ColorUtil.applyOpacity(-1, alpha));

        GL11.glPushMatrix();
        GL11.glTranslatef(x + 36.5f, y + 18.5f + 1.2f, 0);
        GL11.glScaled(0.8, 0.8, 0.8);

        if (target instanceof EntityPlayer) {
            ArrayList<ItemStack> arrayList = new ArrayList<>(Arrays.asList(((EntityPlayer) target).inventory.armorInventory));
            if (((EntityPlayer) target).inventory.getCurrentItem() != null)
                arrayList.add(((EntityPlayer) target).inventory.getCurrentItem());

            if (arrayList.size() >= 1) {
                int n = 0;
                Collections.reverse(arrayList);
                for (ItemStack item : arrayList) {
                    RenderHelper.enableGUIStandardItemLighting();
                    mc.getRenderItem().renderItemAndEffectIntoGUI(item, n, 0);
                    RenderHelper.disableStandardItemLighting();
                    n += 17;
                }
            }
        }

        GL11.glScalef(1, 1, 1);
        GL11.glPopMatrix();
    }

    @Override
    public void renderEffects(float x, float y, float alpha, boolean glow) {
        RoundedUtil.drawRound(x + 4, y + 5.5f, 29, 29, 1, ColorUtil.applyOpacity(Color.BLACK, alpha));
        RoundedUtil.drawRound(x + 37f, y + 12.6f + 1.2f, 68, 2f, 1, ColorUtil.applyOpacity(Color.BLACK, alpha));
    }
}
