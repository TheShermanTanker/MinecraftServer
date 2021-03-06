package net.minecraft.world.inventory;

import com.mojang.datafixers.util.Pair;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.IInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;

public class Slot {
    public final int slot;
    public final IInventory container;
    public int index;
    public final int x;
    public final int y;

    public Slot(IInventory inventory, int index, int x, int y) {
        this.container = inventory;
        this.slot = index;
        this.x = x;
        this.y = y;
    }

    public void onQuickCraft(ItemStack newItem, ItemStack original) {
        int i = original.getCount() - newItem.getCount();
        if (i > 0) {
            this.onQuickCraft(original, i);
        }

    }

    protected void onQuickCraft(ItemStack stack, int amount) {
    }

    protected void onSwapCraft(int amount) {
    }

    protected void checkTakeAchievements(ItemStack stack) {
    }

    public void onTake(EntityHuman player, ItemStack stack) {
        this.setChanged();
    }

    public boolean isAllowed(ItemStack stack) {
        return true;
    }

    public ItemStack getItem() {
        return this.container.getItem(this.slot);
    }

    public boolean hasItem() {
        return !this.getItem().isEmpty();
    }

    public void set(ItemStack stack) {
        this.container.setItem(this.slot, stack);
        this.setChanged();
    }

    public void setChanged() {
        this.container.update();
    }

    public int getMaxStackSize() {
        return this.container.getMaxStackSize();
    }

    public int getMaxStackSize(ItemStack stack) {
        return Math.min(this.getMaxStackSize(), stack.getMaxStackSize());
    }

    @Nullable
    public Pair<MinecraftKey, MinecraftKey> getNoItemIcon() {
        return null;
    }

    public ItemStack remove(int amount) {
        return this.container.splitStack(this.slot, amount);
    }

    public boolean isAllowed(EntityHuman playerEntity) {
        return true;
    }

    public boolean isActive() {
        return true;
    }

    public Optional<ItemStack> tryRemove(int min, int max, EntityHuman player) {
        if (!this.isAllowed(player)) {
            return Optional.empty();
        } else if (!this.allowModification(player) && max < this.getItem().getCount()) {
            return Optional.empty();
        } else {
            min = Math.min(min, max);
            ItemStack itemStack = this.remove(min);
            if (itemStack.isEmpty()) {
                return Optional.empty();
            } else {
                if (this.getItem().isEmpty()) {
                    this.set(ItemStack.EMPTY);
                }

                return Optional.of(itemStack);
            }
        }
    }

    public ItemStack safeTake(int min, int max, EntityHuman player) {
        Optional<ItemStack> optional = this.tryRemove(min, max, player);
        optional.ifPresent((stack) -> {
            this.onTake(player, stack);
        });
        return optional.orElse(ItemStack.EMPTY);
    }

    public ItemStack safeInsert(ItemStack stack) {
        return this.safeInsert(stack, stack.getCount());
    }

    public ItemStack safeInsert(ItemStack stack, int count) {
        if (!stack.isEmpty() && this.isAllowed(stack)) {
            ItemStack itemStack = this.getItem();
            int i = Math.min(Math.min(count, stack.getCount()), this.getMaxStackSize(stack) - itemStack.getCount());
            if (itemStack.isEmpty()) {
                this.set(stack.cloneAndSubtract(i));
            } else if (ItemStack.isSameItemSameTags(itemStack, stack)) {
                stack.subtract(i);
                itemStack.add(i);
                this.set(itemStack);
            }

            return stack;
        } else {
            return stack;
        }
    }

    public boolean allowModification(EntityHuman player) {
        return this.isAllowed(player) && this.isAllowed(this.getItem());
    }

    public int getContainerSlot() {
        return this.slot;
    }
}
