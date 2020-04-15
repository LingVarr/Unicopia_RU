package com.minelittlepony.unicopia.ability;

import java.util.Optional;

import org.lwjgl.glfw.GLFW;

import com.minelittlepony.unicopia.Race;
import com.minelittlepony.unicopia.entity.player.Pony;
import com.minelittlepony.unicopia.util.VecHelper;
import com.minelittlepony.unicopia.util.particles.UParticles;

import net.minecraft.entity.Entity;

public class PegasusCloudInteractionAbility implements Ability<Ability.Numeric> {

    @Override
    public String getKeyName() {
        return "unicopia.power.cloud";
    }

    @Override
    public int getKeyCode() {
        return GLFW.GLFW_KEY_J;
    }

    @Override
    public int getWarmupTime(Pony player) {
        return 10;
    }

    @Override
    public int getCooldownTime(Pony player) {
        return 5;
    }

    @Override
    public boolean canUse(Race playerSpecies) {
        return playerSpecies.canInteractWithClouds();
    }

    @Override
    public Numeric tryActivate(Pony player) {
        return findTarget(player).map(cloud -> {
            Numeric data = new Numeric(player.getOwner().inventory.selectedSlot + 1);
            cloud.handlePegasusInteration(data.type);

            return data;
        }).orElse(null);
    }

    @Override
    public Class<Numeric> getPackageType() {
        return Numeric.class;
    }

    @Override
    public void apply(Pony player, Numeric data) {
        findTarget(player).ifPresent(cloud -> {
            cloud.handlePegasusInteration(data.type);
        });
    }

    protected Optional<ICloudEntity> findTarget(Pony player) {
        if (player.getOwner().hasVehicle() && player.getOwner().getVehicle() instanceof ICloudEntity) {
            return Optional.ofNullable((ICloudEntity)player.getOwner().getVehicle());
        }

        Entity e = VecHelper.getLookedAtEntity(player.getOwner(), 18);

        if (e instanceof ICloudEntity) {
            return Optional.of((ICloudEntity)e);
        }

        return Optional.empty();
    }

    @Override
    public void preApply(Pony player) {
        player.spawnParticles(UParticles.UNICORN_MAGIC, 10);
    }

    @Override
    public void postApply(Pony player) {
        player.spawnParticles(UParticles.RAIN_DROPS, 5);
    }

    public interface ICloudEntity {
        void handlePegasusInteration(int interationType);
    }
}