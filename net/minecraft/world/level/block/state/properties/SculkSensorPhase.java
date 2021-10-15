package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.INamable;

public enum SculkSensorPhase implements INamable {
    INACTIVE("inactive"),
    ACTIVE("active"),
    COOLDOWN("cooldown");

    private final String name;

    private SculkSensorPhase(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
