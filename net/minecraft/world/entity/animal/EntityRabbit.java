package net.minecraft.world.entity.animal;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.control.ControllerJump;
import net.minecraft.world.entity.ai.control.ControllerMove;
import net.minecraft.world.entity.ai.goal.PathfinderGoalAvoidTarget;
import net.minecraft.world.entity.ai.goal.PathfinderGoalBreed;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFloat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalGotoTarget;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalMeleeAttack;
import net.minecraft.world.entity.ai.goal.PathfinderGoalPanic;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStrollLand;
import net.minecraft.world.entity.ai.goal.PathfinderGoalTempt;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalHurtByTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.monster.EntityMonster;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockCarrots;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.pathfinder.PathEntity;
import net.minecraft.world.phys.Vec3D;

public class EntityRabbit extends EntityAnimal {
    public static final double STROLL_SPEED_MOD = 0.6D;
    public static final double BREED_SPEED_MOD = 0.8D;
    public static final double FOLLOW_SPEED_MOD = 1.0D;
    public static final double FLEE_SPEED_MOD = 2.2D;
    public static final double ATTACK_SPEED_MOD = 1.4D;
    private static final DataWatcherObject<Integer> DATA_TYPE_ID = DataWatcher.defineId(EntityRabbit.class, DataWatcherRegistry.INT);
    public static final int TYPE_BROWN = 0;
    public static final int TYPE_WHITE = 1;
    public static final int TYPE_BLACK = 2;
    public static final int TYPE_WHITE_SPLOTCHED = 3;
    public static final int TYPE_GOLD = 4;
    public static final int TYPE_SALT = 5;
    public static final int TYPE_EVIL = 99;
    private static final MinecraftKey KILLER_BUNNY = new MinecraftKey("killer_bunny");
    public static final int EVIL_ATTACK_POWER = 8;
    public static final int EVIL_ARMOR_VALUE = 8;
    private static final int MORE_CARROTS_DELAY = 40;
    private int jumpTicks;
    private int jumpDuration;
    private boolean wasOnGround;
    private int jumpDelayTicks;
    int moreCarrotTicks;

    public EntityRabbit(EntityTypes<? extends EntityRabbit> type, World world) {
        super(type, world);
        this.jumpControl = new EntityRabbit.ControllerJumpRabbit(this);
        this.moveControl = new EntityRabbit.ControllerMoveRabbit(this);
        this.setSpeedModifier(0.0D);
    }

    @Override
    public void initPathfinder() {
        this.goalSelector.addGoal(1, new PathfinderGoalFloat(this));
        this.goalSelector.addGoal(1, new EntityRabbit.PathfinderGoalRabbitPanic(this, 2.2D));
        this.goalSelector.addGoal(2, new PathfinderGoalBreed(this, 0.8D));
        this.goalSelector.addGoal(3, new PathfinderGoalTempt(this, 1.0D, RecipeItemStack.of(Items.CARROT, Items.GOLDEN_CARROT, Blocks.DANDELION), false));
        this.goalSelector.addGoal(4, new EntityRabbit.PathfinderGoalRabbitAvoidTarget<>(this, EntityHuman.class, 8.0F, 2.2D, 2.2D));
        this.goalSelector.addGoal(4, new EntityRabbit.PathfinderGoalRabbitAvoidTarget<>(this, EntityWolf.class, 10.0F, 2.2D, 2.2D));
        this.goalSelector.addGoal(4, new EntityRabbit.PathfinderGoalRabbitAvoidTarget<>(this, EntityMonster.class, 4.0F, 2.2D, 2.2D));
        this.goalSelector.addGoal(5, new EntityRabbit.PathfinderGoalEatCarrots(this));
        this.goalSelector.addGoal(6, new PathfinderGoalRandomStrollLand(this, 0.6D));
        this.goalSelector.addGoal(11, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 10.0F));
    }

    @Override
    protected float getJumpPower() {
        if (!this.horizontalCollision && (!this.moveControl.hasWanted() || !(this.moveControl.getWantedY() > this.locY() + 0.5D))) {
            PathEntity path = this.navigation.getPath();
            if (path != null && !path.isDone()) {
                Vec3D vec3 = path.getNextEntityPos(this);
                if (vec3.y > this.locY() + 0.5D) {
                    return 0.5F;
                }
            }

            return this.moveControl.getSpeedModifier() <= 0.6D ? 0.2F : 0.3F;
        } else {
            return 0.5F;
        }
    }

    @Override
    protected void jump() {
        super.jump();
        double d = this.moveControl.getSpeedModifier();
        if (d > 0.0D) {
            double e = this.getMot().horizontalDistanceSqr();
            if (e < 0.01D) {
                this.moveRelative(0.1F, new Vec3D(0.0D, 0.0D, 1.0D));
            }
        }

        if (!this.level.isClientSide) {
            this.level.broadcastEntityEffect(this, (byte)1);
        }

    }

    public float getJumpCompletion(float delta) {
        return this.jumpDuration == 0 ? 0.0F : ((float)this.jumpTicks + delta) / (float)this.jumpDuration;
    }

    public void setSpeedModifier(double speed) {
        this.getNavigation().setSpeedModifier(speed);
        this.moveControl.setWantedPosition(this.moveControl.getWantedX(), this.moveControl.getWantedY(), this.moveControl.getWantedZ(), speed);
    }

    @Override
    public void setJumping(boolean jumping) {
        super.setJumping(jumping);
        if (jumping) {
            this.playSound(this.getSoundJump(), this.getSoundVolume(), ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F) * 0.8F);
        }

    }

    public void startJumping() {
        this.setJumping(true);
        this.jumpDuration = 10;
        this.jumpTicks = 0;
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_TYPE_ID, 0);
    }

    @Override
    public void mobTick() {
        if (this.jumpDelayTicks > 0) {
            --this.jumpDelayTicks;
        }

        if (this.moreCarrotTicks > 0) {
            this.moreCarrotTicks -= this.random.nextInt(3);
            if (this.moreCarrotTicks < 0) {
                this.moreCarrotTicks = 0;
            }
        }

        if (this.onGround) {
            if (!this.wasOnGround) {
                this.setJumping(false);
                this.checkLandingDelay();
            }

            if (this.getRabbitType() == 99 && this.jumpDelayTicks == 0) {
                EntityLiving livingEntity = this.getGoalTarget();
                if (livingEntity != null && this.distanceToSqr(livingEntity) < 16.0D) {
                    this.facePoint(livingEntity.locX(), livingEntity.locZ());
                    this.moveControl.setWantedPosition(livingEntity.locX(), livingEntity.locY(), livingEntity.locZ(), this.moveControl.getSpeedModifier());
                    this.startJumping();
                    this.wasOnGround = true;
                }
            }

            EntityRabbit.ControllerJumpRabbit rabbitJumpControl = (EntityRabbit.ControllerJumpRabbit)this.jumpControl;
            if (!rabbitJumpControl.wantJump()) {
                if (this.moveControl.hasWanted() && this.jumpDelayTicks == 0) {
                    PathEntity path = this.navigation.getPath();
                    Vec3D vec3 = new Vec3D(this.moveControl.getWantedX(), this.moveControl.getWantedY(), this.moveControl.getWantedZ());
                    if (path != null && !path.isDone()) {
                        vec3 = path.getNextEntityPos(this);
                    }

                    this.facePoint(vec3.x, vec3.z);
                    this.startJumping();
                }
            } else if (!rabbitJumpControl.canJump()) {
                this.enableJumpControl();
            }
        }

        this.wasOnGround = this.onGround;
    }

    @Override
    public boolean canSpawnSprintParticle() {
        return false;
    }

    private void facePoint(double x, double z) {
        this.setYRot((float)(MathHelper.atan2(z - this.locZ(), x - this.locX()) * (double)(180F / (float)Math.PI)) - 90.0F);
    }

    private void enableJumpControl() {
        ((EntityRabbit.ControllerJumpRabbit)this.jumpControl).setCanJump(true);
    }

    private void disableJumpControl() {
        ((EntityRabbit.ControllerJumpRabbit)this.jumpControl).setCanJump(false);
    }

    private void setLandingDelay() {
        if (this.moveControl.getSpeedModifier() < 2.2D) {
            this.jumpDelayTicks = 10;
        } else {
            this.jumpDelayTicks = 1;
        }

    }

    private void checkLandingDelay() {
        this.setLandingDelay();
        this.disableJumpControl();
    }

    @Override
    public void movementTick() {
        super.movementTick();
        if (this.jumpTicks != this.jumpDuration) {
            ++this.jumpTicks;
        } else if (this.jumpDuration != 0) {
            this.jumpTicks = 0;
            this.jumpDuration = 0;
            this.setJumping(false);
        }

    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityInsentient.createMobAttributes().add(GenericAttributes.MAX_HEALTH, 3.0D).add(GenericAttributes.MOVEMENT_SPEED, (double)0.3F);
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setInt("RabbitType", this.getRabbitType());
        nbt.setInt("MoreCarrotTicks", this.moreCarrotTicks);
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.setRabbitType(nbt.getInt("RabbitType"));
        this.moreCarrotTicks = nbt.getInt("MoreCarrotTicks");
    }

    protected SoundEffect getSoundJump() {
        return SoundEffects.RABBIT_JUMP;
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.RABBIT_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.RABBIT_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.RABBIT_DEATH;
    }

    @Override
    public boolean attackEntity(Entity target) {
        if (this.getRabbitType() == 99) {
            this.playSound(SoundEffects.RABBIT_ATTACK, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            return target.damageEntity(DamageSource.mobAttack(this), 8.0F);
        } else {
            return target.damageEntity(DamageSource.mobAttack(this), 3.0F);
        }
    }

    @Override
    public SoundCategory getSoundCategory() {
        return this.getRabbitType() == 99 ? SoundCategory.HOSTILE : SoundCategory.NEUTRAL;
    }

    private static boolean isTemptingItem(ItemStack stack) {
        return stack.is(Items.CARROT) || stack.is(Items.GOLDEN_CARROT) || stack.is(Blocks.DANDELION.getItem());
    }

    @Override
    public EntityRabbit getBreedOffspring(WorldServer serverLevel, EntityAgeable ageableMob) {
        EntityRabbit rabbit = EntityTypes.RABBIT.create(serverLevel);
        int i = this.getRandomRabbitType(serverLevel);
        if (this.random.nextInt(20) != 0) {
            if (ageableMob instanceof EntityRabbit && this.random.nextBoolean()) {
                i = ((EntityRabbit)ageableMob).getRabbitType();
            } else {
                i = this.getRabbitType();
            }
        }

        rabbit.setRabbitType(i);
        return rabbit;
    }

    @Override
    public boolean isBreedItem(ItemStack stack) {
        return isTemptingItem(stack);
    }

    public int getRabbitType() {
        return this.entityData.get(DATA_TYPE_ID);
    }

    public void setRabbitType(int rabbitType) {
        if (rabbitType == 99) {
            this.getAttributeInstance(GenericAttributes.ARMOR).setValue(8.0D);
            this.goalSelector.addGoal(4, new EntityRabbit.PathfinderGoalKillerRabbitMeleeAttack(this));
            this.targetSelector.addGoal(1, (new PathfinderGoalHurtByTarget(this)).setAlertOthers());
            this.targetSelector.addGoal(2, new PathfinderGoalNearestAttackableTarget<>(this, EntityHuman.class, true));
            this.targetSelector.addGoal(2, new PathfinderGoalNearestAttackableTarget<>(this, EntityWolf.class, true));
            if (!this.hasCustomName()) {
                this.setCustomName(new ChatMessage(SystemUtils.makeDescriptionId("entity", KILLER_BUNNY)));
            }
        }

        this.entityData.set(DATA_TYPE_ID, rabbitType);
    }

    @Nullable
    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        int i = this.getRandomRabbitType(world);
        if (entityData instanceof EntityRabbit.GroupDataRabbit) {
            i = ((EntityRabbit.GroupDataRabbit)entityData).rabbitType;
        } else {
            entityData = new EntityRabbit.GroupDataRabbit(i);
        }

        this.setRabbitType(i);
        return super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
    }

    private int getRandomRabbitType(GeneratorAccess world) {
        BiomeBase biome = world.getBiome(this.getChunkCoordinates());
        int i = this.random.nextInt(100);
        if (biome.getPrecipitation() == BiomeBase.Precipitation.SNOW) {
            return i < 80 ? 1 : 3;
        } else if (biome.getBiomeCategory() == BiomeBase.Geography.DESERT) {
            return 4;
        } else {
            return i < 50 ? 0 : (i < 90 ? 5 : 2);
        }
    }

    public static boolean checkRabbitSpawnRules(EntityTypes<EntityRabbit> entity, GeneratorAccess world, EnumMobSpawn spawnReason, BlockPosition pos, Random random) {
        IBlockData blockState = world.getType(pos.below());
        return (blockState.is(Blocks.GRASS_BLOCK) || blockState.is(Blocks.SNOW) || blockState.is(Blocks.SAND)) && world.getLightLevel(pos, 0) > 8;
    }

    boolean wantsMoreFood() {
        return this.moreCarrotTicks == 0;
    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 1) {
            this.spawnSprintParticle();
            this.jumpDuration = 10;
            this.jumpTicks = 0;
        } else {
            super.handleEntityEvent(status);
        }

    }

    @Override
    public Vec3D getLeashOffset() {
        return new Vec3D(0.0D, (double)(0.6F * this.getHeadHeight()), (double)(this.getWidth() * 0.4F));
    }

    public class ControllerJumpRabbit extends ControllerJump {
        private final EntityRabbit rabbit;
        private boolean canJump;

        public ControllerJumpRabbit(EntityRabbit rabbit) {
            super(rabbit);
            this.rabbit = rabbit;
        }

        public boolean wantJump() {
            return this.jump;
        }

        public boolean canJump() {
            return this.canJump;
        }

        public void setCanJump(boolean bl) {
            this.canJump = bl;
        }

        @Override
        public void tick() {
            if (this.jump) {
                this.rabbit.startJumping();
                this.jump = false;
            }

        }
    }

    static class ControllerMoveRabbit extends ControllerMove {
        private final EntityRabbit rabbit;
        private double nextJumpSpeed;

        public ControllerMoveRabbit(EntityRabbit owner) {
            super(owner);
            this.rabbit = owner;
        }

        @Override
        public void tick() {
            if (this.rabbit.onGround && !this.rabbit.jumping && !((EntityRabbit.ControllerJumpRabbit)this.rabbit.jumpControl).wantJump()) {
                this.rabbit.setSpeedModifier(0.0D);
            } else if (this.hasWanted()) {
                this.rabbit.setSpeedModifier(this.nextJumpSpeed);
            }

            super.tick();
        }

        @Override
        public void setWantedPosition(double x, double y, double z, double speed) {
            if (this.rabbit.isInWater()) {
                speed = 1.5D;
            }

            super.setWantedPosition(x, y, z, speed);
            if (speed > 0.0D) {
                this.nextJumpSpeed = speed;
            }

        }
    }

    public static class GroupDataRabbit extends EntityAgeable.GroupDataAgeable {
        public final int rabbitType;

        public GroupDataRabbit(int type) {
            super(1.0F);
            this.rabbitType = type;
        }
    }

    static class PathfinderGoalEatCarrots extends PathfinderGoalGotoTarget {
        private final EntityRabbit rabbit;
        private boolean wantsToRaid;
        private boolean canRaid;

        public PathfinderGoalEatCarrots(EntityRabbit rabbit) {
            super(rabbit, (double)0.7F, 16);
            this.rabbit = rabbit;
        }

        @Override
        public boolean canUse() {
            if (this.nextStartTick <= 0) {
                if (!this.rabbit.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                    return false;
                }

                this.canRaid = false;
                this.wantsToRaid = this.rabbit.wantsMoreFood();
                this.wantsToRaid = true;
            }

            return super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return this.canRaid && super.canContinueToUse();
        }

        @Override
        public void tick() {
            super.tick();
            this.rabbit.getControllerLook().setLookAt((double)this.blockPos.getX() + 0.5D, (double)(this.blockPos.getY() + 1), (double)this.blockPos.getZ() + 0.5D, 10.0F, (float)this.rabbit.getMaxHeadXRot());
            if (this.isReachedTarget()) {
                World level = this.rabbit.level;
                BlockPosition blockPos = this.blockPos.above();
                IBlockData blockState = level.getType(blockPos);
                Block block = blockState.getBlock();
                if (this.canRaid && block instanceof BlockCarrots) {
                    int i = blockState.get(BlockCarrots.AGE);
                    if (i == 0) {
                        level.setTypeAndData(blockPos, Blocks.AIR.getBlockData(), 2);
                        level.destroyBlock(blockPos, true, this.rabbit);
                    } else {
                        level.setTypeAndData(blockPos, blockState.set(BlockCarrots.AGE, Integer.valueOf(i - 1)), 2);
                        level.triggerEffect(2001, blockPos, Block.getCombinedId(blockState));
                    }

                    this.rabbit.moreCarrotTicks = 40;
                }

                this.canRaid = false;
                this.nextStartTick = 10;
            }

        }

        @Override
        protected boolean isValidTarget(IWorldReader world, BlockPosition pos) {
            IBlockData blockState = world.getType(pos);
            if (blockState.is(Blocks.FARMLAND) && this.wantsToRaid && !this.canRaid) {
                blockState = world.getType(pos.above());
                if (blockState.getBlock() instanceof BlockCarrots && ((BlockCarrots)blockState.getBlock()).isRipe(blockState)) {
                    this.canRaid = true;
                    return true;
                }
            }

            return false;
        }
    }

    static class PathfinderGoalKillerRabbitMeleeAttack extends PathfinderGoalMeleeAttack {
        public PathfinderGoalKillerRabbitMeleeAttack(EntityRabbit rabbit) {
            super(rabbit, 1.4D, true);
        }

        @Override
        protected double getAttackReachSqr(EntityLiving entity) {
            return (double)(4.0F + entity.getWidth());
        }
    }

    static class PathfinderGoalRabbitAvoidTarget<T extends EntityLiving> extends PathfinderGoalAvoidTarget<T> {
        private final EntityRabbit rabbit;

        public PathfinderGoalRabbitAvoidTarget(EntityRabbit rabbit, Class<T> fleeFromType, float distance, double slowSpeed, double fastSpeed) {
            super(rabbit, fleeFromType, distance, slowSpeed, fastSpeed);
            this.rabbit = rabbit;
        }

        @Override
        public boolean canUse() {
            return this.rabbit.getRabbitType() != 99 && super.canUse();
        }
    }

    static class PathfinderGoalRabbitPanic extends PathfinderGoalPanic {
        private final EntityRabbit rabbit;

        public PathfinderGoalRabbitPanic(EntityRabbit rabbit, double speed) {
            super(rabbit, speed);
            this.rabbit = rabbit;
        }

        @Override
        public void tick() {
            super.tick();
            this.rabbit.setSpeedModifier(this.speedModifier);
        }
    }
}
