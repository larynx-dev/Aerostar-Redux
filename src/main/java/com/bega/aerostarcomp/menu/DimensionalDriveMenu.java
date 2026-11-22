package com.bega.aerostarcomp.menu;

import com.bega.aerostarcomp.block.entity.DimensionalDriveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class DimensionalDriveMenu extends AbstractContainerMenu {
    private final DimensionalDriveBlockEntity blockEntity;
    public static final int STAR_MAP_X = 23;
    public static final int STAR_MAP_Y = 41;
    public static final int INV_X = 19;
    public static final int INV_Y = 118;
    public static final int HOTBAR_Y = 176;

    public DimensionalDriveMenu(int containerId, Inventory inventory, DimensionalDriveBlockEntity be) {
        super(AeroStarCompMenuTypes.DIMENSIONAL_DRIVE.get(), containerId);
        this.blockEntity = be;
        this.addSlots(inventory, be.getStarMapHandler());
    }

    public DimensionalDriveMenu(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        super(AeroStarCompMenuTypes.DIMENSIONAL_DRIVE.get(), containerId);
        BlockPos pos = buf.readBlockPos();
        // Client side: keep a handle on the synced block entity so the screen can show the
        // selected destination. Slot contents still come through a dummy handler + menu sync.
        this.blockEntity = inventory.player.level().getBlockEntity(pos) instanceof DimensionalDriveBlockEntity be ? be : null;
        this.addSlots(inventory, new ItemStackHandler(1));
    }

    private void addSlots(Inventory inventory, IItemHandler starMap) {
        this.addSlot(new SlotItemHandler(starMap, 0, 23, 41));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, 9 + row * 9 + col, 19 + col * 18, 118 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 19 + col * 18, 176));
        }
    }

    public DimensionalDriveBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    public ItemStack getStarMapStack() {
        return this.slots.get(0).getItem();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int invStart = 1;
            int invEnd = this.slots.size();
            if (index == 0) {
                if (!this.moveItemStackTo(stack, invStart, invEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!DimensionalDriveBlockEntity.isStarMap(stack)) {
                    return ItemStack.EMPTY;
                }

                if (!this.moveItemStackTo(stack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            return result;
        } else {
            return ItemStack.EMPTY;
        }
    }
}
