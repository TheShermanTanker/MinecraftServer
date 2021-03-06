package net.minecraft.world.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.world.IInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipes;

public class SlotResult extends Slot {
    private final InventoryCrafting craftSlots;
    private final EntityHuman player;
    private int removeCount;

    public SlotResult(EntityHuman player, InventoryCrafting input, IInventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
        this.player = player;
        this.craftSlots = input;
    }

    @Override
    public boolean isAllowed(ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack remove(int amount) {
        if (this.hasItem()) {
            this.removeCount += Math.min(amount, this.getItem().getCount());
        }

        return super.remove(amount);
    }

    @Override
    protected void onQuickCraft(ItemStack stack, int amount) {
        this.removeCount += amount;
        this.checkTakeAchievements(stack);
    }

    @Override
    protected void onSwapCraft(int amount) {
        this.removeCount += amount;
    }

    @Override
    protected void checkTakeAchievements(ItemStack stack) {
        if (this.removeCount > 0) {
            stack.onCraftedBy(this.player.level, this.player, this.removeCount);
        }

        if (this.container instanceof RecipeHolder) {
            ((RecipeHolder)this.container).awardUsedRecipes(this.player);
        }

        this.removeCount = 0;
    }

    @Override
    public void onTake(EntityHuman player, ItemStack stack) {
        this.checkTakeAchievements(stack);
        NonNullList<ItemStack> nonNullList = player.level.getCraftingManager().getRemainingItemsFor(Recipes.CRAFTING, this.craftSlots, player.level);

        for(int i = 0; i < nonNullList.size(); ++i) {
            ItemStack itemStack = this.craftSlots.getItem(i);
            ItemStack itemStack2 = nonNullList.get(i);
            if (!itemStack.isEmpty()) {
                this.craftSlots.splitStack(i, 1);
                itemStack = this.craftSlots.getItem(i);
            }

            if (!itemStack2.isEmpty()) {
                if (itemStack.isEmpty()) {
                    this.craftSlots.setItem(i, itemStack2);
                } else if (ItemStack.isSame(itemStack, itemStack2) && ItemStack.equals(itemStack, itemStack2)) {
                    itemStack2.add(itemStack.getCount());
                    this.craftSlots.setItem(i, itemStack2);
                } else if (!this.player.getInventory().pickup(itemStack2)) {
                    this.player.drop(itemStack2, false);
                }
            }
        }

    }
}
