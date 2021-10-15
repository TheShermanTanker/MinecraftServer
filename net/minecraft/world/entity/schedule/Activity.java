package net.minecraft.world.entity.schedule;

import net.minecraft.core.IRegistry;

public class Activity {
    public static final Activity CORE = register("core");
    public static final Activity IDLE = register("idle");
    public static final Activity WORK = register("work");
    public static final Activity PLAY = register("play");
    public static final Activity REST = register("rest");
    public static final Activity MEET = register("meet");
    public static final Activity PANIC = register("panic");
    public static final Activity RAID = register("raid");
    public static final Activity PRE_RAID = register("pre_raid");
    public static final Activity HIDE = register("hide");
    public static final Activity FIGHT = register("fight");
    public static final Activity CELEBRATE = register("celebrate");
    public static final Activity ADMIRE_ITEM = register("admire_item");
    public static final Activity AVOID = register("avoid");
    public static final Activity RIDE = register("ride");
    public static final Activity PLAY_DEAD = register("play_dead");
    public static final Activity LONG_JUMP = register("long_jump");
    public static final Activity RAM = register("ram");
    private final String name;
    private final int hashCode;

    private Activity(String id) {
        this.name = id;
        this.hashCode = id.hashCode();
    }

    public String getName() {
        return this.name;
    }

    private static Activity register(String id) {
        return IRegistry.register(IRegistry.ACTIVITY, id, new Activity(id));
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object != null && this.getClass() == object.getClass()) {
            Activity activity = (Activity)object;
            return this.name.equals(activity.name);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    @Override
    public String toString() {
        return this.getName();
    }
}
