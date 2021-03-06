package net.minecraft.world.entity.projectile;

import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.RayTrace;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.MovingObjectPositionEntity;
import net.minecraft.world.phys.Vec3D;

public final class ProjectileHelper {
    public static MovingObjectPosition getHitResult(Entity entity, Predicate<Entity> predicate) {
        Vec3D vec3 = entity.getMot();
        World level = entity.level;
        Vec3D vec32 = entity.getPositionVector();
        Vec3D vec33 = vec32.add(vec3);
        MovingObjectPosition hitResult = level.rayTrace(new RayTrace(vec32, vec33, RayTrace.BlockCollisionOption.COLLIDER, RayTrace.FluidCollisionOption.NONE, entity));
        if (hitResult.getType() != MovingObjectPosition.EnumMovingObjectType.MISS) {
            vec33 = hitResult.getPos();
        }

        MovingObjectPosition hitResult2 = getEntityHitResult(level, entity, vec32, vec33, entity.getBoundingBox().expandTowards(entity.getMot()).inflate(1.0D), predicate);
        if (hitResult2 != null) {
            hitResult = hitResult2;
        }

        return hitResult;
    }

    @Nullable
    public static MovingObjectPositionEntity getEntityHitResult(Entity entity, Vec3D min, Vec3D max, AxisAlignedBB box, Predicate<Entity> predicate, double d) {
        World level = entity.level;
        double e = d;
        Entity entity2 = null;
        Vec3D vec3 = null;

        for(Entity entity3 : level.getEntities(entity, box, predicate)) {
            AxisAlignedBB aABB = entity3.getBoundingBox().inflate((double)entity3.getPickRadius());
            Optional<Vec3D> optional = aABB.clip(min, max);
            if (aABB.contains(min)) {
                if (e >= 0.0D) {
                    entity2 = entity3;
                    vec3 = optional.orElse(min);
                    e = 0.0D;
                }
            } else if (optional.isPresent()) {
                Vec3D vec32 = optional.get();
                double f = min.distanceSquared(vec32);
                if (f < e || e == 0.0D) {
                    if (entity3.getRootVehicle() == entity.getRootVehicle()) {
                        if (e == 0.0D) {
                            entity2 = entity3;
                            vec3 = vec32;
                        }
                    } else {
                        entity2 = entity3;
                        vec3 = vec32;
                        e = f;
                    }
                }
            }
        }

        return entity2 == null ? null : new MovingObjectPositionEntity(entity2, vec3);
    }

    @Nullable
    public static MovingObjectPositionEntity getEntityHitResult(World world, Entity entity, Vec3D min, Vec3D max, AxisAlignedBB box, Predicate<Entity> predicate) {
        return getEntityHitResult(world, entity, min, max, box, predicate, 0.3F);
    }

    @Nullable
    public static MovingObjectPositionEntity getEntityHitResult(World world, Entity entity, Vec3D min, Vec3D max, AxisAlignedBB box, Predicate<Entity> predicate, float f) {
        double d = Double.MAX_VALUE;
        Entity entity2 = null;

        for(Entity entity3 : world.getEntities(entity, box, predicate)) {
            AxisAlignedBB aABB = entity3.getBoundingBox().inflate((double)f);
            Optional<Vec3D> optional = aABB.clip(min, max);
            if (optional.isPresent()) {
                double e = min.distanceSquared(optional.get());
                if (e < d) {
                    entity2 = entity3;
                    d = e;
                }
            }
        }

        return entity2 == null ? null : new MovingObjectPositionEntity(entity2);
    }

    public static void rotateTowardsMovement(Entity entity, float delta) {
        Vec3D vec3 = entity.getMot();
        if (vec3.lengthSqr() != 0.0D) {
            double d = vec3.horizontalDistance();
            entity.setYRot((float)(MathHelper.atan2(vec3.z, vec3.x) * (double)(180F / (float)Math.PI)) + 90.0F);
            entity.setXRot((float)(MathHelper.atan2(d, vec3.y) * (double)(180F / (float)Math.PI)) - 90.0F);

            while(entity.getXRot() - entity.xRotO < -180.0F) {
                entity.xRotO -= 360.0F;
            }

            while(entity.getXRot() - entity.xRotO >= 180.0F) {
                entity.xRotO += 360.0F;
            }

            while(entity.getYRot() - entity.yRotO < -180.0F) {
                entity.yRotO -= 360.0F;
            }

            while(entity.getYRot() - entity.yRotO >= 180.0F) {
                entity.yRotO += 360.0F;
            }

            entity.setXRot(MathHelper.lerp(delta, entity.xRotO, entity.getXRot()));
            entity.setYRot(MathHelper.lerp(delta, entity.yRotO, entity.getYRot()));
        }
    }

    public static EnumHand getWeaponHoldingHand(EntityLiving entity, Item item) {
        return entity.getItemInMainHand().is(item) ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND;
    }

    public static EntityArrow getMobArrow(EntityLiving entity, ItemStack stack, float damageModifier) {
        ItemArrow arrowItem = (ItemArrow)(stack.getItem() instanceof ItemArrow ? stack.getItem() : Items.ARROW);
        EntityArrow abstractArrow = arrowItem.createArrow(entity.level, stack, entity);
        abstractArrow.setEnchantmentEffectsFromEntity(entity, damageModifier);
        if (stack.is(Items.TIPPED_ARROW) && abstractArrow instanceof EntityTippedArrow) {
            ((EntityTippedArrow)abstractArrow).setEffectsFromItem(stack);
        }

        return abstractArrow;
    }
}
