package net.minecraft.world.food;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;

public class FoodMetaData {
    public int foodLevel = 20;
    public float saturationLevel;
    public float exhaustionLevel;
    private int tickTimer;
    private int lastFoodLevel = 20;

    public FoodMetaData() {
        this.saturationLevel = 5.0F;
    }

    public void eat(int food, float saturationModifier) {
        this.foodLevel = Math.min(food + this.foodLevel, 20);
        this.saturationLevel = Math.min(this.saturationLevel + (float)food * saturationModifier * 2.0F, (float)this.foodLevel);
    }

    public void eat(Item item, ItemStack stack) {
        if (item.isFood()) {
            FoodInfo foodProperties = item.getFoodInfo();
            this.eat(foodProperties.getNutrition(), foodProperties.getSaturationModifier());
        }

    }

    public void tick(EntityHuman player) {
        EnumDifficulty difficulty = player.level.getDifficulty();
        this.lastFoodLevel = this.foodLevel;
        if (this.exhaustionLevel > 4.0F) {
            this.exhaustionLevel -= 4.0F;
            if (this.saturationLevel > 0.0F) {
                this.saturationLevel = Math.max(this.saturationLevel - 1.0F, 0.0F);
            } else if (difficulty != EnumDifficulty.PEACEFUL) {
                this.foodLevel = Math.max(this.foodLevel - 1, 0);
            }
        }

        boolean bl = player.level.getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION);
        if (bl && this.saturationLevel > 0.0F && player.isHurt() && this.foodLevel >= 20) {
            ++this.tickTimer;
            if (this.tickTimer >= 10) {
                float f = Math.min(this.saturationLevel, 6.0F);
                player.heal(f / 6.0F);
                this.addExhaustion(f);
                this.tickTimer = 0;
            }
        } else if (bl && this.foodLevel >= 18 && player.isHurt()) {
            ++this.tickTimer;
            if (this.tickTimer >= 80) {
                player.heal(1.0F);
                this.addExhaustion(6.0F);
                this.tickTimer = 0;
            }
        } else if (this.foodLevel <= 0) {
            ++this.tickTimer;
            if (this.tickTimer >= 80) {
                if (player.getHealth() > 10.0F || difficulty == EnumDifficulty.HARD || player.getHealth() > 1.0F && difficulty == EnumDifficulty.NORMAL) {
                    player.damageEntity(DamageSource.STARVE, 1.0F);
                }

                this.tickTimer = 0;
            }
        } else {
            this.tickTimer = 0;
        }

    }

    public void readAdditionalSaveData(NBTTagCompound nbt) {
        if (nbt.hasKeyOfType("foodLevel", 99)) {
            this.foodLevel = nbt.getInt("foodLevel");
            this.tickTimer = nbt.getInt("foodTickTimer");
            this.saturationLevel = nbt.getFloat("foodSaturationLevel");
            this.exhaustionLevel = nbt.getFloat("foodExhaustionLevel");
        }

    }

    public void addAdditionalSaveData(NBTTagCompound nbt) {
        nbt.setInt("foodLevel", this.foodLevel);
        nbt.setInt("foodTickTimer", this.tickTimer);
        nbt.setFloat("foodSaturationLevel", this.saturationLevel);
        nbt.setFloat("foodExhaustionLevel", this.exhaustionLevel);
    }

    public int getFoodLevel() {
        return this.foodLevel;
    }

    public int getLastFoodLevel() {
        return this.lastFoodLevel;
    }

    public boolean needsFood() {
        return this.foodLevel < 20;
    }

    public void addExhaustion(float exhaustion) {
        this.exhaustionLevel = Math.min(this.exhaustionLevel + exhaustion, 40.0F);
    }

    public float getExhaustionLevel() {
        return this.exhaustionLevel;
    }

    public float getSaturationLevel() {
        return this.saturationLevel;
    }

    public void setFoodLevel(int foodLevel) {
        this.foodLevel = foodLevel;
    }

    public void setSaturation(float saturationLevel) {
        this.saturationLevel = saturationLevel;
    }

    public void setExhaustion(float exhaustion) {
        this.exhaustionLevel = exhaustion;
    }
}
