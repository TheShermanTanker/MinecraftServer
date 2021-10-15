package net.minecraft.world.effect;

public class InstantMobEffect extends MobEffectList {
    public InstantMobEffect(MobEffectInfo type, int color) {
        super(type, color);
    }

    @Override
    public boolean isInstant() {
        return true;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration >= 1;
    }
}
