package net.minecraft.world.entity.ai.attributes;

import net.minecraft.util.MathHelper;

public class AttributeRanged extends AttributeBase {
    private final double minValue;
    public final double maxValue;

    public AttributeRanged(String translationKey, double fallback, double min, double max) {
        super(translationKey, fallback);
        this.minValue = min;
        this.maxValue = max;
        if (min > max) {
            throw new IllegalArgumentException("Minimum value cannot be bigger than maximum value!");
        } else if (fallback < min) {
            throw new IllegalArgumentException("Default value cannot be lower than minimum value!");
        } else if (fallback > max) {
            throw new IllegalArgumentException("Default value cannot be bigger than maximum value!");
        }
    }

    public double getMinValue() {
        return this.minValue;
    }

    public double getMaxValue() {
        return this.maxValue;
    }

    @Override
    public double sanitizeValue(double value) {
        return MathHelper.clamp(value, this.minValue, this.maxValue);
    }
}
