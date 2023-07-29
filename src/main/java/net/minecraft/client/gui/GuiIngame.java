package net.minecraft.client.gui;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import dev.tenacity.Tenacity;
import dev.tenacity.event.impl.network.PacketReceiveEvent;
import dev.tenacity.event.impl.render.PreRenderEvent;
import dev.tenacity.intent.api.account.IntentAccount;
import dev.tenacity.module.impl.render.*;
import dev.tenacity.utils.animations.ContinualAnimation;
import dev.tenacity.utils.player.ChatUtil;
import dev.tenacity.utils.render.*;
import dev.tenacity.event.impl.render.Render2DEvent;
import dev.tenacity.utils.Utils;
import dev.tenacity.utils.font.AbstractFontRenderer;
import dev.tenacity.utils.time.TimerUtil;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.boss.BossStatus;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.src.Config;
import net.minecraft.util.*;
import net.minecraft.world.border.WorldBorder;
import net.optifine.CustomColors;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class GuiIngame extends Gui implements Utils {
    private static final ResourceLocation vignetteTexPath = new ResourceLocation("textures/misc/vignette.png");
    private static final ResourceLocation widgetsTexPath = new ResourceLocation("textures/gui/widgets.png");
    private static final ResourceLocation pumpkinBlurTexPath = new ResourceLocation("textures/misc/pumpkinblur.png");
    private final Random rand = new Random();
    private final Minecraft mc;
    private final RenderItem itemRenderer;

    /**
     * ChatGUI instance that retains all previous chat data
     */
    private final GuiNewChat persistantChatGUI;
    private int updateCounter;

    /**
     * The string specifying which record music is playing
     */
    private String recordPlaying = "";

    /**
     * How many ticks the record playing message will be displayed
     */
    private int recordPlayingUpFor;
    private boolean recordIsPlaying;

    /**
     * Previous frame vignette brightness (slowly changes by 1% each frame)
     */
    public float prevVignetteBrightness = 1.0F;

    /**
     * Remaining ticks the item highlight should be visible
     */
    private int remainingHighlightTicks;

    /**
     * The ItemStack that is currently being highlighted
     */
    private ItemStack highlightingItemStack;
    private final GuiOverlayDebug overlayDebug;

    /**
     * The spectator GUI for this in-game GUI instance
     */
    private final GuiSpectator spectatorGui;
    private final GuiPlayerTabOverlay overlayPlayerList;

    /**
     * A timer for the current title and subtitle displayed
     */
    private int titlesTimer;

    /**
     * The current title displayed
     */
    private String displayedTitle = "";

    /**
     * The current sub-title displayed
     */
    private String displayedSubTitle = "";

    /**
     * The time that the title take to fade in
     */
    private int titleFadeIn;

    /**
     * The time that the title is display
     */
    private int titleDisplayTime;

    /**
     * The time that the title take to fade out
     */
    private int titleFadeOut;
    private int playerHealth = 0;
    private int lastPlayerHealth = 0;

    /**
     * The last recorded system time
     */
    private long lastSystemTime = 0L;

    /**
     * Used with updateCounter to make the heart bar flash
     */
    private long healthUpdateCounter = 0L;

    public GuiIngame(Minecraft mcIn) {
        this.mc = mcIn;
        this.itemRenderer = mcIn.getRenderItem();
        this.overlayDebug = new GuiOverlayDebug(mcIn);
        this.spectatorGui = new GuiSpectator(mcIn);
        this.persistantChatGUI = new GuiNewChat(mcIn);
        this.overlayPlayerList = new GuiPlayerTabOverlay(mcIn, this);
        this.setDefaultTitlesTimes();
    }

    /**
     * Set the differents times for the titles to their default values
     */
    public void setDefaultTitlesTimes() {
        this.titleFadeIn = 10;
        this.titleDisplayTime = 70;
        this.titleFadeOut = 20;
    }

    public void renderGameOverlay(float partialTicks) {
        ScaledResolution scaledresolution = new ScaledResolution(this.mc);
        int i = scaledresolution.getScaledWidth();
        int j = scaledresolution.getScaledHeight();
        this.mc.entityRenderer.setupOverlayRendering();
        GLUtil.startBlend();

        if (Config.isVignetteEnabled()) {
            this.renderVignette(this.mc.thePlayer.getBrightness(partialTicks), scaledresolution);
        } else {
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        }

        ItemStack itemstack = this.mc.thePlayer.inventory.armorItemInSlot(3);

        if (this.mc.gameSettings.thirdPersonView == 0 && itemstack != null && itemstack.getItem() == Item.getItemFromBlock(Blocks.pumpkin)) {
            this.renderPumpkinOverlay(scaledresolution);
        }

        if (!this.mc.thePlayer.isPotionActive(Potion.confusion)) {
            float f = this.mc.thePlayer.prevTimeInPortal + (this.mc.thePlayer.timeInPortal - this.mc.thePlayer.prevTimeInPortal) * partialTicks;

            if (f > 0.0F) {
                this.renderPortal(f, scaledresolution);
            }
        }

        Tenacity.INSTANCE.getEventProtocol().handleEvent(new PreRenderEvent());


        PostProcessing postProcessing = (PostProcessing) Tenacity.INSTANCE.getModuleCollection().get(PostProcessing.class);
        postProcessing.blurScreen();


        Tenacity.INSTANCE.getEventProtocol().handleEvent(new Render2DEvent(scaledresolution.getScaledWidth(), scaledresolution.getScaledHeight()));
        NotificationsMod notif = Tenacity.INSTANCE.getModuleCollection().getModule(NotificationsMod.class);
        if (notif.isEnabled()) {
            notif.render();
        }


        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        this.mc.getTextureManager().bindTexture(icons);

        if (this.showCrosshair()) {
            GLUtil.startBlend();
            GlStateManager.tryBlendFuncSeparate(775, 769, 1, 0);
            RenderUtil.setAlphaLimit(0);
            this.drawTexturedModalRect(i / 2 - 7, j / 2 - 7, 0, 0, 16, 16);
        }


        if (this.mc.playerController.isSpectator()) {
            this.spectatorGui.renderTooltip(scaledresolution, partialTicks);
        } else {
            this.renderTooltip(scaledresolution, partialTicks);
        }


        this.mc.getTextureManager().bindTexture(icons);

        this.mc.mcProfiler.startSection("bossHealth");
        this.renderBossHealth();
        this.mc.mcProfiler.endSection();


        GlStateManager.pushMatrix();
        GlStateManager.translate(0, -15 * GuiChat.openingAnimation.getOutput().floatValue(), 0);
        if (this.mc.playerController.shouldDrawHUD()) {
            RenderUtil.resetColor();
            GlStateManager.enableAlpha();
            this.renderPlayerStats(scaledresolution);
        }
        GlStateManager.popMatrix();


        if (this.mc.thePlayer.getSleepTimer() > 0) {
            this.mc.mcProfiler.startSection("sleep");
            GlStateManager.disableDepth();
            GlStateManager.disableAlpha();
            int j1 = this.mc.thePlayer.getSleepTimer();
            float f1 = (float) j1 / 100.0F;

            if (f1 > 1.0F) {
                f1 = 1.0F - (float) (j1 - 100) / 10.0F;
            }

            int k = (int) (220.0F * f1) << 24 | 1052704;
            drawRect(0, 0, i, j, k);
            GlStateManager.enableAlpha();
            GlStateManager.enableDepth();
            this.mc.mcProfiler.endSection();
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int k1 = i / 2 - 91;

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, -15 * GuiChat.openingAnimation.getOutput().floatValue(), 0);
        if (this.mc.thePlayer.isRidingHorse()) {
            this.renderHorseJumpBar(scaledresolution, k1);
        } else if (this.mc.playerController.gameIsSurvivalOrAdventure()) {
            this.renderExpBar(scaledresolution, k1);
        }
        GlStateManager.popMatrix();

        RenderUtil.resetColor();
        if (this.mc.gameSettings.heldItemTooltips && !this.mc.playerController.isSpectator()) {
            this.renderSelectedItem(scaledresolution);
        } else if (this.mc.thePlayer.isSpectator()) {
            this.spectatorGui.renderSelectedItem(scaledresolution);
        }

        if (this.mc.isDemo()) {
            this.renderDemo(scaledresolution);
        }

        if (this.mc.gameSettings.showDebugInfo) {
            this.overlayDebug.renderDebugInfo(scaledresolution);
        }

        if (this.recordPlayingUpFor > 0) {
            this.mc.mcProfiler.startSection("overlayMessage");
            float f2 = (float) this.recordPlayingUpFor - partialTicks;
            int l1 = (int) ((f2 * 255.0F) / 20.0F);

            if (l1 > 255) {
                l1 = 255;
            }

            if (recordPlayingUpFor >= 52) {
                l1 = (int) (255.0F * ((60 - recordPlayingUpFor) / 8.0F));
            }

            if (l1 > 8) {
                GlStateManager.pushMatrix();
                GlStateManager.translate((float) (i / 2), (float) (j - 68), 0.0F);
                GLUtil.startBlend();
                int l = 16777215;

                if (this.recordIsPlaying) {
                    l = MathHelper.hsvToRGB(f2 / 50.0F, 0.7F, 0.6F) & 16777215;
                }

                this.getFontRenderer().drawStringWithShadow(this.recordPlaying, -this.getFontRenderer().getStringWidth(this.recordPlaying) / 2, (float) (-4 - 15 * GuiChat.openingAnimation.getOutput().floatValue()), l + (l1 << 24 & -16777216));
                GLUtil.endBlend();
                GlStateManager.popMatrix();
            }

            this.mc.mcProfiler.endSection();
        }

        if (this.titlesTimer > 0) {
            this.mc.mcProfiler.startSection("titleAndSubtitle");
            float f3 = (float) this.titlesTimer - partialTicks;
            int i2 = 255;

            if (this.titlesTimer > this.titleFadeOut + this.titleDisplayTime) {
                float f4 = (float) (this.titleFadeIn + this.titleDisplayTime + this.titleFadeOut) - f3;
                i2 = (int) (f4 * 255.0F / (float) this.titleFadeIn);
            }

            if (this.titlesTimer <= this.titleFadeOut) {
                i2 = (int) (f3 * 255.0F / (float) this.titleFadeOut);
            }

            i2 = MathHelper.clamp_int(i2, 0, 255);
            if (i2 > 8) {
                GlStateManager.pushMatrix();
                GlStateManager.translate((float) (i / 2), (float) (j / 2), 0.0F);
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                GlStateManager.pushMatrix();
                GlStateManager.scale(4.0F, 4.0F, 4.0F);
                int j2 = i2 << 24 & -16777216;
                this.getFontRenderer().drawString(this.displayedTitle, (float) (-this.getFontRenderer().getStringWidth(this.displayedTitle) / 2), -10.0F, 16777215 | j2, true);
                GlStateManager.popMatrix();
                GlStateManager.pushMatrix();
                GlStateManager.scale(2.0F, 2.0F, 2.0F);
                this.getFontRenderer().drawString(this.displayedSubTitle, (float) (-this.getFontRenderer().getStringWidth(this.displayedSubTitle) / 2), 5.0F, 16777215 | j2, true);
                GlStateManager.popMatrix();
                GlStateManager.disableBlend();
                GlStateManager.popMatrix();
            }

            this.mc.mcProfiler.endSection();
        }

        Scoreboard scoreboard = this.mc.theWorld.getScoreboard();
        ScoreObjective scoreobjective = null;
        ScorePlayerTeam scoreplayerteam = scoreboard.getPlayersTeam(this.mc.thePlayer.getName());

        if (scoreplayerteam != null) {
            int i1 = scoreplayerteam.getChatFormat().getColorIndex();

            if (i1 >= 0) {
                scoreobjective = scoreboard.getObjectiveInDisplaySlot(3 + i1);
            }
        }

        ScoreObjective scoreobjective1 = scoreobjective != null ? scoreobjective : scoreboard.getObjectiveInDisplaySlot(1);

        if (scoreobjective1 != null && Tenacity.INSTANCE.isEnabled(ScoreboardMod.class)) {
            this.renderScoreboard(scoreobjective1, scaledresolution);
        }

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableAlpha();
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, (float) (j - 48), 0.0F);
        this.mc.mcProfiler.startSection("chat");
        this.persistantChatGUI.drawChat(this.updateCounter);
        this.mc.mcProfiler.endSection();
        GlStateManager.popMatrix();
        scoreobjective1 = scoreboard.getObjectiveInDisplaySlot(0);

        if (this.mc.gameSettings.keyBindPlayerList.isKeyDown() && (!this.mc.isIntegratedServerRunning() || this.mc.thePlayer.sendQueue.getPlayerInfoMap().size() > 1 || scoreobjective1 != null)) {
            this.overlayPlayerList.updatePlayerList(true);
            this.overlayPlayerList.renderPlayerlist(i, scoreboard, scoreobjective1);
        } else {
            this.overlayPlayerList.updatePlayerList(false);
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableLighting();
        GlStateManager.enableAlpha();
    }


    protected void renderTooltip(ScaledResolution sr, float partialTicks) {
        if (this.mc.getRenderViewEntity() instanceof EntityPlayer) {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.mc.getTextureManager().bindTexture(widgetsTexPath);
            EntityPlayer entityplayer = (EntityPlayer) this.mc.getRenderViewEntity();
            int i = sr.getScaledWidth() / 2;

            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            drawTexturedModalRect(i - 91, sr.getScaledHeight() - (22 + 15 * GuiChat.openingAnimation.getOutput().floatValue()), 0, 0, 182, 22);
            drawTexturedModalRect(i - 91 - 1 + entityplayer.inventory.currentItem * 20, (float) (sr.getScaledHeight() - (22 + 15 * GuiChat.openingAnimation.getOutput().floatValue())), 0, 22, 24, 22);

            GlStateManager.enableRescaleNormal();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            RenderHelper.enableGUIStandardItemLighting();

            for (int j = 0; j < 9; ++j) {
                int k = sr.getScaledWidth() / 2 - 90 + j * 20 + 2;
                int l = (int) (sr.getScaledHeight() - (19 + 15 * GuiChat.openingAnimation.getOutput().floatValue()));
                this.renderHotbarItem(j, k, l, partialTicks, entityplayer);
            }
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
            GlStateManager.disableBlend();


        }
    }

    private final ContinualAnimation expAnim = new ContinualAnimation();
    private final ContinualAnimation jumpAnim = new ContinualAnimation();

    public void renderHorseJumpBar(ScaledResolution scaledRes, int x) {
        this.mc.mcProfiler.startSection("jumpBar");

        float f = mc.thePlayer.getHorseJumpPower();
        int k = scaledRes.getScaledHeight() - 32 + 3;
        jumpAnim.animate(f, 18);
        Color color = ColorUtil.blendColors(new float[]{0.0f, 0.5f, 1.0f}, new Color[]{new Color(5, 134, 105), new Color(236, 129, 44), new Color(250, 50, 56)}, jumpAnim.getOutput());
        RoundedUtil.drawRound(x, k, 182, 5, 2, new Color(43, 42, 43));
        RoundedUtil.drawRound(x, k, jumpAnim.getOutput() * 182, 5, 2, color);

        this.mc.mcProfiler.endSection();
    }

    public void renderExpBar(ScaledResolution scaledRes, int x) {
        this.mc.mcProfiler.startSection("expBar");

        if (mc.thePlayer.xpBarCap() > 0) {
            int l = scaledRes.getScaledHeight() - 32 + 3;

            if (mc.thePlayer.experienceLevel > 0) {
                String str = "EXP " + mc.thePlayer.experienceLevel;
                float length = tenacityBoldFont14.getStringWidth(str);
                expAnim.animate(mc.thePlayer.experience, 18);
                RoundedUtil.drawRound(x + length + 2, l, 182 - length - 2, 5, 2, new Color(43, 42, 43));
                RoundedUtil.drawRound(x + length + 2, l, expAnim.getOutput() * (182 - length - 2), 5, 2, new Color(0, 168, 107));
                tenacityBoldFont14.drawString(str, x, l - tenacityBoldFont14.getHeight() / 2f + 2.5f, -1);
            } else {
                RoundedUtil.drawRound(x, l, 182, 5, 2, new Color(43, 42, 43));
                RoundedUtil.drawRound(x, l, mc.thePlayer.experience * 182, 5, 2, new Color(0, 168, 107));
            }
        }

        this.mc.mcProfiler.endSection();
    }

    public void renderSelectedItem(ScaledResolution scaledRes) {
        this.mc.mcProfiler.startSection("selectedItemName");

        if (this.remainingHighlightTicks > 0 && this.highlightingItemStack != null) {
            String s = this.highlightingItemStack.getDisplayName();

            if (this.highlightingItemStack.hasDisplayName()) {
                s = EnumChatFormatting.ITALIC + s;
            }

            float i = (scaledRes.getScaledWidth() - getFontRenderer().getStringWidth(s)) / 2;
            int j = scaledRes.getScaledHeight() - 59;

            if (!this.mc.playerController.shouldDrawHUD()) {
                j += 14;
            }

            int k = (int) ((float) this.remainingHighlightTicks * 255 / 10.0F);

            if (k > 0) {
                getFontRenderer().drawStringWithShadow(s, i, j - 15 * GuiChat.openingAnimation.getOutput().floatValue(), ColorUtil.applyOpacity(Color.WHITE, k / 255f));
            }
        }

        this.mc.mcProfiler.endSection();
    }

    public void renderDemo(ScaledResolution scaledRes) {
        this.mc.mcProfiler.startSection("demo");
        String s = "";

        if (this.mc.theWorld.getTotalWorldTime() >= 120500L) {
            s = I18n.format("demo.demoExpired");
        } else {
            s = I18n.format("demo.remainingTime", StringUtils.ticksToElapsedTime((int) (120500L - this.mc.theWorld.getTotalWorldTime())));
        }

        float i = this.getFontRenderer().getStringWidth(s);
        this.getFontRenderer().drawStringWithShadow(s, (float) (scaledRes.getScaledWidth() - i - 10), 5.0F, 16777215);
        this.mc.mcProfiler.endSection();
    }

    protected boolean showCrosshair() {
        if (this.mc.gameSettings.showDebugInfo && !this.mc.thePlayer.hasReducedDebug() && !this.mc.gameSettings.reducedDebugInfo) {
            return false;
        } else if (this.mc.playerController.isSpectator()) {
            if (this.mc.pointedEntity != null) {
                return true;
            } else {
                if (this.mc.objectMouseOver != null && this.mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    return this.mc.theWorld.getTileEntity(mc.objectMouseOver.getBlockPos()) instanceof IInventory;
                }
                return false;
            }
        } else {
            return true;
        }
    }


    public void renderScoreboardBlur(ScaledResolution scaledRes) {
        Scoreboard scoreboardOBJ = this.mc.theWorld.getScoreboard();
        ScoreObjective scoreobjective = null;
        ScorePlayerTeam scoreplayerteamObj = scoreboardOBJ.getPlayersTeam(this.mc.thePlayer.getName());

        if (scoreplayerteamObj != null) {
            int i1 = scoreplayerteamObj.getChatFormat().getColorIndex();

            if (i1 >= 0) {
                scoreobjective = scoreboardOBJ.getObjectiveInDisplaySlot(3 + i1);
            }
        }
        ScoreObjective objective = scoreobjective != null ? scoreobjective : scoreboardOBJ.getObjectiveInDisplaySlot(1);
        if (objective != null && Tenacity.INSTANCE.isEnabled(ScoreboardMod.class)) {

            Scoreboard scoreboard = objective.getScoreboard();
            Collection<Score> collection = scoreboard.getSortedScores(objective);
            List<Score> list = Lists.newArrayList(Iterables.filter(collection, p_apply_1_ -> p_apply_1_.getPlayerName() != null && !p_apply_1_.getPlayerName().startsWith("#")));

            if (list.size() > 15) {
                collection = Lists.newArrayList(Iterables.skip(list, collection.size() - 15));
            } else {
                collection = list;
            }

            float i = this.getScoreboardFontRenderer().getStringWidth(objective.getDisplayName());

            for (Score score : collection) {
                ScorePlayerTeam scoreplayerteam = scoreboard.getPlayersTeam(score.getPlayerName());
                String s = ScorePlayerTeam.formatPlayerName(scoreplayerteam, score.getPlayerName()) + ": " + EnumChatFormatting.RED + score.getScorePoints();
                i = Math.max(i, this.getScoreboardFontRenderer().getStringWidth(s));
            }

            int i1 = collection.size() * this.getScoreboardFontRenderer().getHeight();
            int j1 = scaledRes.getScaledHeight() / 2 + i1 / 3 + ScoreboardMod.yOffset.getValue().intValue();
            int k1 = 3;
            float l1 = scaledRes.getScaledWidth() - i - k1;
            int j = 0;

            for (Score score1 : collection) {
                ++j;
                ScorePlayerTeam scoreplayerteam1 = scoreboard.getPlayersTeam(score1.getPlayerName());
                String s1 = ScorePlayerTeam.formatPlayerName(scoreplayerteam1, score1.getPlayerName());
                int k = j1 - j * this.getScoreboardFontRenderer().getHeight();
                int l = scaledRes.getScaledWidth() - k1 + 2;
                drawRect(l1 - 2, k, l, k + this.getScoreboardFontRenderer().getHeight(), Color.BLACK.getRGB());

                // Line text
                //   this.getFontRenderer().drawString(s1, l1, k, -1, ScoreboardMod.textShadow.isEnabled());


                if (j == collection.size()) {
                    String s3 = objective.getDisplayName();
                    drawRect(l1 - 2, k - this.getScoreboardFontRenderer().getHeight() - 1, l, k - 1, Color.BLACK.getRGB());
                    drawRect(l1 - 2, k - 1, l, k, Color.BLACK.getRGB());
                    // this.getFontRenderer().drawString(s3, l1 + i / 2.0F - this.getFontRenderer().getStringWidth(s3) / 2.0F, k - this.getFontRenderer().getHeight(), -1, ScoreboardMod.textShadow.isEnabled());
                }
            }
        }
    }

    public void renderScoreboard(ScoreObjective objective, ScaledResolution scaledRes) {
        Scoreboard scoreboard = objective.getScoreboard();
        Collection<Score> collection = scoreboard.getSortedScores(objective);
        List<Score> list = Lists.newArrayList(Iterables.filter(collection, p_apply_1_ -> p_apply_1_.getPlayerName() != null && !p_apply_1_.getPlayerName().startsWith("#")));

        if (list.size() > 15) {
            collection = Lists.newArrayList(Iterables.skip(list, collection.size() - 15));
        } else {
            collection = list;
        }

        float i = this.getScoreboardFontRenderer().getStringWidth(objective.getDisplayName());

        for (Score score : collection) {
            ScorePlayerTeam scoreplayerteam = scoreboard.getPlayersTeam(score.getPlayerName());
            String s = ScorePlayerTeam.formatPlayerName(scoreplayerteam, score.getPlayerName()) + ": " + EnumChatFormatting.RED + score.getScorePoints();
            i = Math.max(i, this.getScoreboardFontRenderer().getStringWidth(s));
        }

        int i1 = collection.size() * this.getScoreboardFontRenderer().getHeight();
        int j1 = scaledRes.getScaledHeight() / 2 + i1 / 3 + ScoreboardMod.yOffset.getValue().intValue();
        float l1 = scaledRes.getScaledWidth() - i - 3;
        int j = 0;

        Color color = ColorUtil.applyOpacity(Color.BLACK, 75 / 255f);
        for (Score score1 : collection) {
            ++j;
            ScorePlayerTeam scoreplayerteam1 = scoreboard.getPlayersTeam(score1.getPlayerName());
            String s1 = ScorePlayerTeam.formatPlayerName(scoreplayerteam1, score1.getPlayerName());
            int k = j1 - j * this.getScoreboardFontRenderer().getHeight();
            int l = scaledRes.getScaledWidth() - 3 + 2;
            GLUtil.startBlend();
            drawRect(l1 - 2, k, l, k + this.getScoreboardFontRenderer().getHeight(), color.getRGB());

            // Line text
            this.getScoreboardFontRenderer().drawString(s1, l1, k, -1, ScoreboardMod.textShadow.isEnabled());

            // Line number
            if (ScoreboardMod.redNumbers.isEnabled()) {
                String s2 = EnumChatFormatting.RED + "" + score1.getScorePoints();
                this.getScoreboardFontRenderer().drawString(s2, l - this.getScoreboardFontRenderer().getStringWidth(s2), k, -1, ScoreboardMod.textShadow.isEnabled());
            }

            if (j == collection.size()) {
                String s3 = objective.getDisplayName();
                drawRect(l1 - 2, k - this.getScoreboardFontRenderer().getHeight() - 1, l, k - 1, color.getRGB());
                GLUtil.startBlend();
                drawRect(l1 - 2, k - 1, l, k, color.getRGB());
                this.getScoreboardFontRenderer().drawString(s3, l1 + i / 2.0F - this.getScoreboardFontRenderer().getStringWidth(s3) / 2.0F, k - this.getScoreboardFontRenderer().getHeight(), -1, ScoreboardMod.textShadow.isEnabled());
            }
        }
    }

    private final ContinualAnimation healthAnim = new ContinualAnimation();
    private final ContinualAnimation foodAnim = new ContinualAnimation();
    private final ContinualAnimation saturationAnim = new ContinualAnimation();
    private final ContinualAnimation armorAnim = new ContinualAnimation();
    private final ContinualAnimation airAnim = new ContinualAnimation();

    private void renderPlayerStats(ScaledResolution scaledRes) {
        if (this.mc.getRenderViewEntity() instanceof EntityPlayer) {
            EntityPlayer entityplayer = (EntityPlayer) this.mc.getRenderViewEntity();
            int i = MathHelper.ceiling_float_int(entityplayer.getHealth());

            if (i < this.playerHealth && entityplayer.hurtResistantTime > 0) {
                this.lastSystemTime = Minecraft.getSystemTime();
                this.healthUpdateCounter = this.updateCounter + 20;
            } else if (i > this.playerHealth && entityplayer.hurtResistantTime > 0) {
                this.lastSystemTime = Minecraft.getSystemTime();
                this.healthUpdateCounter = this.updateCounter + 10;
            }

            if (Minecraft.getSystemTime() - this.lastSystemTime > 1000L) {
                this.playerHealth = i;
                this.lastPlayerHealth = i;
                this.lastSystemTime = Minecraft.getSystemTime();
            }

            this.playerHealth = i;
            this.rand.setSeed(this.updateCounter * 312871L);
            int halfWidth = scaledRes.getScaledWidth() / 2;
            int k1 = scaledRes.getScaledHeight() - 40;
            int j2 = k1 - 12;

            this.mc.mcProfiler.startSection("armor");

            if (entityplayer.getTotalArmorValue() > 0) {
                armorAnim.animate(entityplayer.getTotalArmorValue() / 20f, 18);
                RoundedUtil.drawRound(halfWidth - 91, j2, 85, 8, 2, new Color(43, 42, 43));
                RoundedUtil.drawRound(halfWidth - 91, j2, 85 * armorAnim.getOutput(), 8, 2, new Color(0, 168, 107));
                tenacityBoldFont14.drawString("Armor " + entityplayer.getTotalArmorValue() + " | 20", halfWidth - 91 + 2, j2 + 4 - tenacityBoldFont14.getHeight() / 2f, -1);
            }

            this.mc.mcProfiler.endStartSection("health");

            healthAnim.animate(Math.min((entityplayer.getHealth() + entityplayer.getAbsorptionAmount()) / entityplayer.getMaxHealth(), 1), 18);
            RoundedUtil.drawRound(halfWidth - 91, k1, 85, 8, 2, new Color(43, 42, 43));
            RoundedUtil.drawRound(halfWidth - 91, k1, 85 * healthAnim.getOutput(), 8, 2, new Color(250, 50, 56));
            tenacityBoldFont14.drawString("HP " + Math.ceil(entityplayer.getHealth() + entityplayer.getAbsorptionAmount()) + " | " + entityplayer.getMaxHealth(), halfWidth - 91 + 2, k1 + 4 - tenacityBoldFont14.getHeight() / 2f, -1);

            this.mc.mcProfiler.endStartSection("food");

            foodAnim.animate(entityplayer.getFoodStats().getFoodLevel() / 20f, 18);
            saturationAnim.animate(entityplayer.getFoodStats().getSaturationLevel() / 20f, 18); // 饱食度是在服务器发血量包后更新的
            RoundedUtil.drawRound(halfWidth + 6, k1, 85, 8, 2, new Color(43, 42, 43));
            RoundedUtil.drawRound(halfWidth + 6, k1, 85 * foodAnim.getOutput(), 8, 2, new Color(0xFFAA00));
            RoundedUtil.drawRound(halfWidth + 6, k1, 85 * saturationAnim.getOutput(), 8, 2, new Color(255, 113, 0));
            tenacityBoldFont14.drawString("Food " + entityplayer.getFoodStats().getFoodLevel() + " | 20", halfWidth + 6 + 2, k1 + 4 - tenacityBoldFont14.getHeight() / 2f, -1);

            this.mc.mcProfiler.endStartSection("air");

            if (entityplayer.isInsideOfMaterial(Material.water)) {
                int l6 = this.mc.thePlayer.getAir();
                int k7 = MathHelper.ceiling_double_int((double) (l6 - 2) * 10.0D / 300.0D);
                airAnim.animate(k7 / 10f, 18);
                RoundedUtil.drawRound(halfWidth + 6, j2, 85, 8, 2, new Color(43, 42, 43));
                RoundedUtil.drawRound(halfWidth + 6, j2, 85 * airAnim.getOutput(), 8, 2, new Color(28, 167, 222));
                tenacityBoldFont14.drawString("Drown in " + k7 + "s", halfWidth + 6 + 2, j2 + 4 - tenacityBoldFont14.getHeight() / 2f, -1);
            }

            this.mc.mcProfiler.endSection();
        }
    }

    /**
     * Render GUI Effects
     */
    public void renderEffects() {
        GlStateManager.pushMatrix();
        GlStateManager.translate(0, -15 * GuiChat.openingAnimation.getOutput().floatValue(), 0);

        ScaledResolution scaledRes = new ScaledResolution(mc);
        float halfWidth = scaledRes.getScaledWidth() / 2f;
        int k1 = scaledRes.getScaledHeight() - 40;
        int j2 = k1 - 12;
        float x = halfWidth - 91;
        float k = scaledRes.getScaledHeight() - 32 + 3;

        if (mc.playerController.gameIsSurvivalOrAdventure()) {
            if (mc.thePlayer.getTotalArmorValue() > 0) RoundedUtil.drawRound(halfWidth - 91, j2, 85, 8, 2, Color.BLACK);
            RoundedUtil.drawRound(halfWidth - 91, k1, 85, 8, 2, Color.BLACK);
            RoundedUtil.drawRound(halfWidth + 6, k1, 85, 8, 2, Color.BLACK);
            if (mc.thePlayer.isInsideOfMaterial(Material.water))
                RoundedUtil.drawRound(halfWidth + 6, j2, 85, 8, 2, Color.BLACK);
            if (mc.thePlayer.xpBarCap() > 0) {
                if (mc.thePlayer.experienceLevel > 0) {
                    String str = "EXP " + mc.thePlayer.experienceLevel;
                    float length = tenacityBoldFont14.getStringWidth(str);
                    RoundedUtil.drawRound(x + length + 2, k, 182 - length - 2, 5, 2, Color.BLACK);
                } else {
                    RoundedUtil.drawRound(x, k, 182, 5, 2, Color.BLACK);
                }
            }
        }
        if (mc.thePlayer.isRidingHorse()) RoundedUtil.drawRound(x, k, 182, 5, 2, Color.BLACK);

        GlStateManager.popMatrix();
    }

    /**
     * Renders dragon's (boss) health on the HUD
     */
    private void renderBossHealth() {
        if (BossStatus.bossName != null && BossStatus.statusBarTime > 0) {
            --BossStatus.statusBarTime;
            IFontRenderer fontrenderer = this.mc.fontRendererObj;
            ScaledResolution scaledresolution = new ScaledResolution(this.mc);
            int scaledWidth = scaledresolution.getScaledWidth();
            int width = 182;
            int k = scaledresolution.getScaledWidth() / 2 - 182 / 2;
            int l = (int) (BossStatus.healthScale * (float) (width + 1));
            int i1 = 12;
            this.drawTexturedModalRect(k, i1, 0, 74, width, 5);
            this.drawTexturedModalRect(k, i1, 0, 74, width, 5);

            if (l > 0) {
                this.drawTexturedModalRect(k, i1, 0, 79, l, 5);
            }

            String s = BossStatus.bossName;
            this.getFontRenderer().drawStringWithShadow(s, scaledWidth / 2f - this.getFontRenderer().getStringWidth(s) / 2, 2, 16777215);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.mc.getTextureManager().bindTexture(icons);
        }
    }

    private void renderPumpkinOverlay(ScaledResolution scaledRes) {
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableAlpha();
        this.mc.getTextureManager().bindTexture(pumpkinBlurTexPath);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(0.0D, (double) scaledRes.getScaledHeight(), -90.0D).tex(0.0D, 1.0D).endVertex();
        worldrenderer.pos((double) scaledRes.getScaledWidth(), (double) scaledRes.getScaledHeight(), -90.0D).tex(1.0D, 1.0D).endVertex();
        worldrenderer.pos((double) scaledRes.getScaledWidth(), 0.0D, -90.0D).tex(1.0D, 0.0D).endVertex();
        worldrenderer.pos(0.0D, 0.0D, -90.0D).tex(0.0D, 0.0D).endVertex();
        tessellator.draw();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Renders a Vignette arount the entire screen that changes with light level.
     *
     * @param lightLevel The current brightness
     * @param scaledRes  The current resolution of the game
     */
    private void renderVignette(float lightLevel, ScaledResolution scaledRes) {
        if (!Config.isVignetteEnabled()) {
            GlStateManager.enableDepth();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        } else {
            lightLevel = 1.0F - lightLevel;
            lightLevel = MathHelper.clamp_float(lightLevel, 0.0F, 1.0F);
            WorldBorder worldborder = this.mc.theWorld.getWorldBorder();
            float f = (float) worldborder.getClosestDistance(this.mc.thePlayer);
            double d0 = Math.min(worldborder.getResizeSpeed() * (double) worldborder.getWarningTime() * 1000.0D, Math.abs(worldborder.getTargetSize() - worldborder.getDiameter()));
            double d1 = Math.max((double) worldborder.getWarningDistance(), d0);

            if ((double) f < d1) {
                f = 1.0F - (float) ((double) f / d1);
            } else {
                f = 0.0F;
            }

            this.prevVignetteBrightness = (float) ((double) this.prevVignetteBrightness + (double) (lightLevel - this.prevVignetteBrightness) * 0.01D);
            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
            GlStateManager.tryBlendFuncSeparate(0, 769, 1, 0);

            if (f > 0.0F) {
                GlStateManager.color(0.0F, f, f, 1.0F);
            } else {
                GlStateManager.color(this.prevVignetteBrightness, this.prevVignetteBrightness, this.prevVignetteBrightness, 1.0F);
            }

            this.mc.getTextureManager().bindTexture(vignetteTexPath);
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldrenderer = tessellator.getWorldRenderer();
            worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
            worldrenderer.pos(0.0D, (double) scaledRes.getScaledHeight(), -90.0D).tex(0.0D, 1.0D).endVertex();
            worldrenderer.pos((double) scaledRes.getScaledWidth(), (double) scaledRes.getScaledHeight(), -90.0D).tex(1.0D, 1.0D).endVertex();
            worldrenderer.pos((double) scaledRes.getScaledWidth(), 0.0D, -90.0D).tex(1.0D, 0.0D).endVertex();
            worldrenderer.pos(0.0D, 0.0D, -90.0D).tex(0.0D, 0.0D).endVertex();
            tessellator.draw();
            GlStateManager.depthMask(true);
            GlStateManager.enableDepth();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        }
    }

    private void renderPortal(float timeInPortal, ScaledResolution scaledRes) {
        if (timeInPortal < 1.0F) {
            timeInPortal = timeInPortal * timeInPortal;
            timeInPortal = timeInPortal * timeInPortal;
            timeInPortal = timeInPortal * 0.8F + 0.2F;
        }

        GlStateManager.disableAlpha();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1.0F, 1.0F, 1.0F, timeInPortal);
        this.mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        TextureAtlasSprite textureatlassprite = this.mc.getBlockRendererDispatcher().getBlockModelShapes().getTexture(Blocks.portal.getDefaultState());
        float f = textureatlassprite.getMinU();
        float f1 = textureatlassprite.getMinV();
        float f2 = textureatlassprite.getMaxU();
        float f3 = textureatlassprite.getMaxV();
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(0.0D, (double) scaledRes.getScaledHeight(), -90.0D).tex((double) f, (double) f3).endVertex();
        worldrenderer.pos((double) scaledRes.getScaledWidth(), (double) scaledRes.getScaledHeight(), -90.0D).tex((double) f2, (double) f3).endVertex();
        worldrenderer.pos((double) scaledRes.getScaledWidth(), 0.0D, -90.0D).tex((double) f2, (double) f1).endVertex();
        worldrenderer.pos(0.0D, 0.0D, -90.0D).tex((double) f, (double) f1).endVertex();
        tessellator.draw();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderHotbarItem(int index, int xPos, int yPos, float partialTicks, EntityPlayer player) {
        ItemStack itemstack = player.inventory.mainInventory[index];

        if (itemstack != null) {
            float f = (float) itemstack.animationsToGo - partialTicks;

            if (f > 0.0F) {
                GlStateManager.pushMatrix();
                float f1 = 1.0F + f / 5.0F;
                GlStateManager.translate((float) (xPos + 8), (float) (yPos + 12), 0.0F);
                GlStateManager.scale(1.0F / f1, (f1 + 1.0F) / 2.0F, 1.0F);
                GlStateManager.translate((float) (-(xPos + 8)), (float) (-(yPos + 12)), 0.0F);
            }

            this.itemRenderer.renderItemAndEffectIntoGUI(itemstack, xPos, yPos);

            if (f > 0.0F) {
                GlStateManager.popMatrix();
            }

            this.itemRenderer.renderItemOverlays(this.mc.fontRendererObj, itemstack, xPos, yPos);
        }
    }

    /**
     * The update tick for the ingame UI
     */
    public void updateTick() {
        if (this.recordPlayingUpFor > 0) {
            --this.recordPlayingUpFor;
        }

        if (this.titlesTimer > 0) {
            --this.titlesTimer;

            if (this.titlesTimer <= 0) {
                this.displayedTitle = "";
                this.displayedSubTitle = "";
            }
        }

        ++this.updateCounter;

        if (this.mc.thePlayer != null) {
            ItemStack itemstack = this.mc.thePlayer.inventory.getCurrentItem();

            if (itemstack == null) {
                this.remainingHighlightTicks = 0;
            } else if (this.highlightingItemStack != null && itemstack.getItem() == this.highlightingItemStack.getItem() && ItemStack.areItemStackTagsEqual(itemstack, this.highlightingItemStack) && (itemstack.isItemStackDamageable() || itemstack.getMetadata() == this.highlightingItemStack.getMetadata())) {
                if (this.remainingHighlightTicks > 0) {
                    --this.remainingHighlightTicks;
                }
            } else {
                this.remainingHighlightTicks = 40;
            }

            this.highlightingItemStack = itemstack;
        }
    }

    public void setRecordPlayingMessage(String recordName) {
        this.setRecordPlaying(I18n.format("record.nowPlaying", new Object[]{recordName}), true);
    }

    public void setRecordPlaying(String message, boolean isPlaying) {
        this.recordPlaying = message;
        this.recordPlayingUpFor = 60;
        this.recordIsPlaying = isPlaying;
    }

    public void displayTitle(String title, String subTitle, int timeFadeIn, int displayTime, int timeFadeOut) {
        if (title == null && subTitle == null && timeFadeIn < 0 && displayTime < 0 && timeFadeOut < 0) {
            this.displayedTitle = "";
            this.displayedSubTitle = "";
            this.titlesTimer = 0;
        } else if (title != null) {
            this.displayedTitle = title;
            this.titlesTimer = this.titleFadeIn + this.titleDisplayTime + this.titleFadeOut;
        } else if (subTitle != null) {
            this.displayedSubTitle = subTitle;
        } else {
            if (timeFadeIn >= 0) {
                this.titleFadeIn = timeFadeIn;
            }

            if (displayTime >= 0) {
                this.titleDisplayTime = displayTime;
            }

            if (timeFadeOut >= 0) {
                this.titleFadeOut = timeFadeOut;
            }

            if (this.titlesTimer > 0) {
                this.titlesTimer = this.titleFadeIn + this.titleDisplayTime + this.titleFadeOut;
            }
        }
    }

    public void setRecordPlaying(IChatComponent component, boolean isPlaying) {
        this.setRecordPlaying(component.getUnformattedText(), isPlaying);
    }

    /**
     * returns a pointer to the persistant Chat GUI, containing all previous chat messages and such
     */
    public GuiNewChat getChatGUI() {
        return this.persistantChatGUI;
    }

    public int getUpdateCounter() {
        return this.updateCounter;
    }

    public AbstractFontRenderer getFontRenderer() {
        return this.mc.fontRendererObj;
    }

    public AbstractFontRenderer getScoreboardFontRenderer() {
        return ScoreboardMod.customFont.isEnabled() ? tenacityFont20 : this.mc.fontRendererObj;
    }

    public GuiSpectator getSpectatorGui() {
        return this.spectatorGui;
    }

    public GuiPlayerTabOverlay getTabList() {
        return this.overlayPlayerList;
    }

    /**
     * Reset the GuiPlayerTabOverlay's message header and footer
     */
    public void resetPlayersOverlayFooterHeader() {
        this.overlayPlayerList.resetFooterHeader();
    }
}
