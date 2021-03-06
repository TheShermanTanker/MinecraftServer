package net.minecraft.world.entity.projectile;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Set;
import net.minecraft.core.IRegistry;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionRegistry;
import net.minecraft.world.item.alchemy.PotionUtil;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.World;

public class EntityTippedArrow extends EntityArrow {
    private static final int EXPOSED_POTION_DECAY_TIME = 600;
    private static final int NO_EFFECT_COLOR = -1;
    private static final DataWatcherObject<Integer> ID_EFFECT_COLOR = DataWatcher.defineId(EntityTippedArrow.class, DataWatcherRegistry.INT);
    private static final byte EVENT_POTION_PUFF = 0;
    private PotionRegistry potion = Potions.EMPTY;
    public final Set<MobEffect> effects = Sets.newHashSet();
    private boolean fixedColor;

    public EntityTippedArrow(EntityTypes<? extends EntityTippedArrow> type, World world) {
        super(type, world);
    }

    public EntityTippedArrow(World world, double x, double y, double z) {
        super(EntityTypes.ARROW, x, y, z, world);
    }

    public EntityTippedArrow(World world, EntityLiving owner) {
        super(EntityTypes.ARROW, owner, world);
    }

    public void setEffectsFromItem(ItemStack stack) {
        if (stack.is(Items.TIPPED_ARROW)) {
            this.potion = PotionUtil.getPotion(stack);
            Collection<MobEffect> collection = PotionUtil.getCustomEffects(stack);
            if (!collection.isEmpty()) {
                for(MobEffect mobEffectInstance : collection) {
                    this.effects.add(new MobEffect(mobEffectInstance));
                }
            }

            int i = getCustomColor(stack);
            if (i == -1) {
                this.updateColor();
            } else {
                this.setColor(i);
            }
        } else if (stack.is(Items.ARROW)) {
            this.potion = Potions.EMPTY;
            this.effects.clear();
            this.entityData.set(ID_EFFECT_COLOR, -1);
        }

    }

    public static int getCustomColor(ItemStack stack) {
        NBTTagCompound compoundTag = stack.getTag();
        return compoundTag != null && compoundTag.hasKeyOfType("CustomPotionColor", 99) ? compoundTag.getInt("CustomPotionColor") : -1;
    }

    private void updateColor() {
        this.fixedColor = false;
        if (this.potion == Potions.EMPTY && this.effects.isEmpty()) {
            this.entityData.set(ID_EFFECT_COLOR, -1);
        } else {
            this.entityData.set(ID_EFFECT_COLOR, PotionUtil.getColor(PotionUtil.getAllEffects(this.potion, this.effects)));
        }

    }

    public void addEffect(MobEffect effect) {
        this.effects.add(effect);
        this.getDataWatcher().set(ID_EFFECT_COLOR, PotionUtil.getColor(PotionUtil.getAllEffects(this.potion, this.effects)));
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(ID_EFFECT_COLOR, -1);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level.isClientSide) {
            if (this.inGround) {
                if (this.inGroundTime % 5 == 0) {
                    this.makeParticle(1);
                }
            } else {
                this.makeParticle(2);
            }
        } else if (this.inGround && this.inGroundTime != 0 && !this.effects.isEmpty() && this.inGroundTime >= 600) {
            this.level.broadcastEntityEffect(this, (byte)0);
            this.potion = Potions.EMPTY;
            this.effects.clear();
            this.entityData.set(ID_EFFECT_COLOR, -1);
        }

    }

    private void makeParticle(int amount) {
        int i = this.getColor();
        if (i != -1 && amount > 0) {
            double d = (double)(i >> 16 & 255) / 255.0D;
            double e = (double)(i >> 8 & 255) / 255.0D;
            double f = (double)(i >> 0 & 255) / 255.0D;

            for(int j = 0; j < amount; ++j) {
                this.level.addParticle(Particles.ENTITY_EFFECT, this.getRandomX(0.5D), this.getRandomY(), this.getRandomZ(0.5D), d, e, f);
            }

        }
    }

    public int getColor() {
        return this.entityData.get(ID_EFFECT_COLOR);
    }

    public void setColor(int color) {
        this.fixedColor = true;
        this.entityData.set(ID_EFFECT_COLOR, color);
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        if (this.potion != Potions.EMPTY) {
            nbt.setString("Potion", IRegistry.POTION.getKey(this.potion).toString());
        }

        if (this.fixedColor) {
            nbt.setInt("Color", this.getColor());
        }

        if (!this.effects.isEmpty()) {
            NBTTagList listTag = new NBTTagList();

            for(MobEffect mobEffectInstance : this.effects) {
                listTag.add(mobEffectInstance.save(new NBTTagCompound()));
            }

            nbt.set("CustomPotionEffects", listTag);
        }

    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        if (nbt.hasKeyOfType("Potion", 8)) {
            this.potion = PotionUtil.getPotion(nbt);
        }

        for(MobEffect mobEffectInstance : PotionUtil.getCustomEffects(nbt)) {
            this.addEffect(mobEffectInstance);
        }

        if (nbt.hasKeyOfType("Color", 99)) {
            this.setColor(nbt.getInt("Color"));
        } else {
            this.updateColor();
        }

    }

    @Override
    protected void doPostHurtEffects(EntityLiving target) {
        super.doPostHurtEffects(target);
        Entity entity = this.getEffectSource();

        for(MobEffect mobEffectInstance : this.potion.getEffects()) {
            target.addEffect(new MobEffect(mobEffectInstance.getMobEffect(), Math.max(mobEffectInstance.getDuration() / 8, 1), mobEffectInstance.getAmplifier(), mobEffectInstance.isAmbient(), mobEffectInstance.isShowParticles()), entity);
        }

        if (!this.effects.isEmpty()) {
            for(MobEffect mobEffectInstance2 : this.effects) {
                target.addEffect(mobEffectInstance2, entity);
            }
        }

    }

    @Override
    public ItemStack getItemStack() {
        if (this.effects.isEmpty() && this.potion == Potions.EMPTY) {
            return new ItemStack(Items.ARROW);
        } else {
            ItemStack itemStack = new ItemStack(Items.TIPPED_ARROW);
            PotionUtil.setPotion(itemStack, this.potion);
            PotionUtil.setCustomEffects(itemStack, this.effects);
            if (this.fixedColor) {
                itemStack.getOrCreateTag().setInt("CustomPotionColor", this.getColor());
            }

            return itemStack;
        }
    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 0) {
            int i = this.getColor();
            if (i != -1) {
                double d = (double)(i >> 16 & 255) / 255.0D;
                double e = (double)(i >> 8 & 255) / 255.0D;
                double f = (double)(i >> 0 & 255) / 255.0D;

                for(int j = 0; j < 20; ++j) {
                    this.level.addParticle(Particles.ENTITY_EFFECT, this.getRandomX(0.5D), this.getRandomY(), this.getRandomZ(0.5D), d, e, f);
                }
            }
        } else {
            super.handleEntityEvent(status);
        }

    }
}
