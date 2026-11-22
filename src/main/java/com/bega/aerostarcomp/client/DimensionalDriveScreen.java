package com.bega.aerostarcomp.client;

import com.bega.aerostarcomp.block.entity.DimensionalDriveBlockEntity;
import com.bega.aerostarcomp.menu.DimensionalDriveMenu;
import com.bega.aerostarcomp.network.DriveSettingsPacket;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class DimensionalDriveScreen extends AbstractContainerScreen<DimensionalDriveMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("aerostarcomp", "textures/gui/dimensional_drive.png");
    private static final ResourceLocation VANILLA_INV = ResourceLocation.withDefaultNamespace("textures/gui/container/inventory.png");
    private static final int PANEL_W = 200;
    private static final int PANEL_H = 102;
    private static final int ARM_X = 160;
    private static final int ARM_Y = 76;
    private static final int ARM_W = 40;
    private static final int ARM_H = 20;
    private static Component feedback = null;
    private static long feedbackUntil = 0L;

    public static void setFeedback(Component message) {
        feedback = message;
        feedbackUntil = System.currentTimeMillis() + 4500L;
    }

    public DimensionalDriveScreen(DimensionalDriveMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 200;
        this.imageHeight = 196;
        this.inventoryLabelX = 19;
        this.inventoryLabelY = 106;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        g.blit(TEXTURE, x, y, 0.0F, 0.0F, 200, 102, 256, 256);
        int pTop = y + 106;
        int pLeft = x + 13;
        int pRight = x + 187;
        int pBottom = y + this.imageHeight;
        g.fill(pLeft - 1, pTop - 1, pRight + 1, pBottom, -13158601);
        g.fill(pLeft, pTop, pRight, pBottom, -3750202);
        int gridX = x + 19 - 1;
        g.blit(VANILLA_INV, gridX, y + 118 - 1, 7.0F, 83.0F, 162, 54, 256, 256);
        g.blit(VANILLA_INV, gridX, y + 176 - 1, 7.0F, 141.0F, 162, 18, 256, 256);
        if (this.isOverArm((double)mouseX, (double)mouseY)) {
            g.fill(x + 165, y + 76, x + 186, y + 96, 687865855);
        }
    }

    private boolean isOverArm(double mouseX, double mouseY) {
        int x = this.leftPos + 160;
        int y = this.topPos + 76;
        return mouseX >= (double)x && mouseX < (double)(x + 40) && mouseY >= (double)y && mouseY < (double)(y + 20);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.isOverArm(mouseX, mouseY)) {
            PacketDistributor.sendToServer(DriveSettingsPacket.arm());
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F);
            }

            return true;
        } else {
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g, mx, my, pt);
        super.render(g, mx, my, pt);
        int x = this.leftPos;
        int y = this.topPos;
        ItemStack starMap = this.getMenu().getStarMapStack();
        String planet = DimensionalDriveBlockEntity.planetFromStarMap(starMap);
        Component label = planet == null
            ? Component.translatable("gui.aerostarcomp.no_destination").withStyle(ChatFormatting.GRAY)
            : Component.translatable("gui.aerostarcomp.destination", DimensionalDriveBlockEntity.planetName(planet)).withStyle(ChatFormatting.GREEN);
        g.drawString(this.font, label, x + 46, y + 45, 16777215, true);
        if (feedback != null && System.currentTimeMillis() < feedbackUntil) {
            List<FormattedCharSequence> lines = this.font.split(feedback, 144);
            int needed = lines.size() * 10;
            int avail = 20;
            float scale = needed > avail ? (float)avail / (float)needed : 1.0F;
            g.pose().pushPose();
            g.pose().translate((double)(x + 8), (double)(y + 76), 0.0);
            g.pose().scale(scale, scale, 1.0F);
            int ly = 0;

            for (FormattedCharSequence line : lines) {
                g.drawString(this.font, line, 0, ly, 16777215, true);
                ly += 10;
            }

            g.pose().popPose();
        }

        this.renderTooltip(g, mx, my);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }
}
