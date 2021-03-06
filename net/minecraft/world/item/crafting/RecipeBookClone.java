package net.minecraft.world.item.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.inventory.InventoryCrafting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemWrittenBook;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;

public class RecipeBookClone extends IRecipeComplex {
    public RecipeBookClone(MinecraftKey id) {
        super(id);
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        int i = 0;
        ItemStack itemStack = ItemStack.EMPTY;

        for(int j = 0; j < inventory.getSize(); ++j) {
            ItemStack itemStack2 = inventory.getItem(j);
            if (!itemStack2.isEmpty()) {
                if (itemStack2.is(Items.WRITTEN_BOOK)) {
                    if (!itemStack.isEmpty()) {
                        return false;
                    }

                    itemStack = itemStack2;
                } else {
                    if (!itemStack2.is(Items.WRITABLE_BOOK)) {
                        return false;
                    }

                    ++i;
                }
            }
        }

        return !itemStack.isEmpty() && itemStack.hasTag() && i > 0;
    }

    @Override
    public ItemStack assemble(InventoryCrafting inventory) {
        int i = 0;
        ItemStack itemStack = ItemStack.EMPTY;

        for(int j = 0; j < inventory.getSize(); ++j) {
            ItemStack itemStack2 = inventory.getItem(j);
            if (!itemStack2.isEmpty()) {
                if (itemStack2.is(Items.WRITTEN_BOOK)) {
                    if (!itemStack.isEmpty()) {
                        return ItemStack.EMPTY;
                    }

                    itemStack = itemStack2;
                } else {
                    if (!itemStack2.is(Items.WRITABLE_BOOK)) {
                        return ItemStack.EMPTY;
                    }

                    ++i;
                }
            }
        }

        if (!itemStack.isEmpty() && itemStack.hasTag() && i >= 1 && ItemWrittenBook.getGeneration(itemStack) < 2) {
            ItemStack itemStack3 = new ItemStack(Items.WRITTEN_BOOK, i);
            NBTTagCompound compoundTag = itemStack.getTag().copy();
            compoundTag.setInt("generation", ItemWrittenBook.getGeneration(itemStack) + 1);
            itemStack3.setTag(compoundTag);
            return itemStack3;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inventory) {
        NonNullList<ItemStack> nonNullList = NonNullList.withSize(inventory.getSize(), ItemStack.EMPTY);

        for(int i = 0; i < nonNullList.size(); ++i) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.getItem().hasCraftingRemainingItem()) {
                nonNullList.set(i, new ItemStack(itemStack.getItem().getCraftingRemainingItem()));
            } else if (itemStack.getItem() instanceof ItemWrittenBook) {
                ItemStack itemStack2 = itemStack.cloneItemStack();
                itemStack2.setCount(1);
                nonNullList.set(i, itemStack2);
                break;
            }
        }

        return nonNullList;
    }

    @Override
    public RecipeSerializer<?> getRecipeSerializer() {
        return RecipeSerializer.BOOK_CLONING;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }
}
