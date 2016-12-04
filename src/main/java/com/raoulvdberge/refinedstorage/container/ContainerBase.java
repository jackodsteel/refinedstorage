package com.raoulvdberge.refinedstorage.container;

import com.raoulvdberge.refinedstorage.apiimpl.API;
import com.raoulvdberge.refinedstorage.container.slot.*;
import com.raoulvdberge.refinedstorage.tile.TileBase;
import com.raoulvdberge.refinedstorage.tile.grid.WirelessGrid;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;

public abstract class ContainerBase extends Container {
    private TileBase tile;
    private EntityPlayer player;

    public ContainerBase(TileBase tile, EntityPlayer player) {
        this.tile = tile;
        this.player = player;
    }

    public EntityPlayer getPlayer() {
        return player;
    }

    public TileBase getTile() {
        return tile;
    }

    // @todo Forge issue #3498
    @Override
    protected boolean mergeItemStack(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
        boolean flag = false;
        int i = startIndex;

        if (reverseDirection) {
            i = endIndex - 1;
        }

        if (stack.isStackable()) {
            while (!stack.isEmpty()) {
                if (reverseDirection) {
                    if (i < startIndex) {
                        break;
                    }
                } else if (i >= endIndex) {
                    break;
                }

                Slot slot = (Slot) this.inventorySlots.get(i);
                ItemStack itemstack = slot.getStack();

                if (!itemstack.isEmpty() && itemstack.getItem() == stack.getItem() && (!stack.getHasSubtypes() || stack.getMetadata() == itemstack.getMetadata()) && ItemStack.areItemStackTagsEqual(stack, itemstack)) {
                    int j = itemstack.getCount() + stack.getCount();

                    if (j <= slot.getSlotStackLimit()) {
                        stack.setCount(0);
                        itemstack.setCount(j);
                        slot.onSlotChanged();
                        flag = true;
                    } else if (itemstack.getCount() < slot.getSlotStackLimit()) {
                        stack.shrink(slot.getSlotStackLimit() - itemstack.getCount());
                        itemstack.setCount(slot.getSlotStackLimit());
                        slot.onSlotChanged();
                        flag = true;
                    }
                }

                if (reverseDirection) {
                    --i;
                } else {
                    ++i;
                }
            }
        }

        if (!stack.isEmpty()) {
            if (reverseDirection) {
                i = endIndex - 1;
            } else {
                i = startIndex;
            }

            while (true) {
                if (reverseDirection) {
                    if (i < startIndex) {
                        break;
                    }
                } else if (i >= endIndex) {
                    break;
                }

                Slot slot1 = (Slot) this.inventorySlots.get(i);
                ItemStack itemstack1 = slot1.getStack();

                if (itemstack1.isEmpty() && slot1.isItemValid(stack)) {
                    if (stack.getCount() > slot1.getSlotStackLimit()) {
                        slot1.putStack(stack.splitStack(slot1.getSlotStackLimit()));
                    } else {
                        slot1.putStack(stack.splitStack(stack.getCount()));
                    }

                    slot1.onSlotChanged();
                    flag = true;
                    break;
                }

                if (reverseDirection) {
                    --i;
                } else {
                    ++i;
                }
            }
        }

        return flag;
    }

    protected void addPlayerInventory(int xInventory, int yInventory) {
        int id = 0;

        for (int i = 0; i < 9; i++) {
            int x = xInventory + i * 18;
            int y = yInventory + 4 + (3 * 18);

            if (i == player.inventory.currentItem && (ContainerBase.this instanceof ContainerGridFilter || (ContainerBase.this instanceof ContainerGrid && ((ContainerGrid) ContainerBase.this).getGrid() instanceof WirelessGrid))) {
                addSlotToContainer(new SlotDisabled(player.inventory, id, x, y));
            } else {
                addSlotToContainer(new Slot(player.inventory, id, x, y));
            }

            id++;
        }

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                addSlotToContainer(new Slot(player.inventory, id, xInventory + x * 18, yInventory + y * 18));

                id++;
            }
        }
    }

    @Override
    public ItemStack slotClick(int id, int dragType, ClickType clickType, EntityPlayer player) {
        Slot slot = id >= 0 ? getSlot(id) : null;

        if (slot instanceof SlotFilter) {
            if (((SlotFilter) slot).allowsSize()) {
                if (clickType == ClickType.QUICK_MOVE) {
                    slot.putStack(ItemStack.EMPTY);
                } else if (!player.inventory.getItemStack().isEmpty()) {
                    int amount = player.inventory.getItemStack().getCount();

                    slot.putStack(ItemHandlerHelper.copyStackWithSize(player.inventory.getItemStack(), amount));
                } else if (slot.getHasStack()) {
                    int amount = slot.getStack().getCount();

                    if (dragType == 0) {
                        amount = Math.max(1, amount - 1);
                    } else if (dragType == 1) {
                        amount = Math.min(64, amount + 1);
                    }

                    slot.getStack().setCount(amount);
                }
            } else if (player.inventory.getItemStack().isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else if (slot.isItemValid(player.inventory.getItemStack())) {
                slot.putStack(player.inventory.getItemStack().copy());
            }

            return player.inventory.getItemStack();
        } else if (slot instanceof SlotFilterLegacy) {
            if (player.inventory.getItemStack().isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else if (slot.isItemValid(player.inventory.getItemStack())) {
                slot.putStack(player.inventory.getItemStack().copy());
            }

            return player.inventory.getItemStack();
        } else if (slot instanceof SlotDisabled) {
            return ItemStack.EMPTY;
        }

        return super.slotClick(id, dragType, clickType, player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    protected ItemStack mergeItemStackToSpecimen(ItemStack stack, int begin, int end) {
        for (int i = begin; i < end; ++i) {
            if (API.instance().getComparer().isEqualNoQuantity(getStackFromSlot(getSlot(i)), stack)) {
                return ItemStack.EMPTY;
            }
        }

        for (int i = begin; i < end; ++i) {
            Slot slot = getSlot(i);

            if (getStackFromSlot(slot).isEmpty() && slot.isItemValid(stack)) {
                slot.putStack(ItemHandlerHelper.copyStackWithSize(stack, 1));
                slot.onSlotChanged();

                return ItemStack.EMPTY;
            }
        }

        return ItemStack.EMPTY;
    }

    @Nonnull
    private ItemStack getStackFromSlot(Slot slot) {
        ItemStack stackInSlot = slot.getStack();

        if (stackInSlot.isEmpty()) {
            if (slot instanceof SlotFilterFluid) {
                stackInSlot = ((SlotFilterFluid) slot).getRealStack();
            } else if (slot instanceof SlotFilterType) {
                stackInSlot = ((SlotFilterType) slot).getRealStack();
            }
        }

        return stackInSlot;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }
}
