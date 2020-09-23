package com.minelittlepony.unicopia.entity.player;

import java.util.Optional;

import javax.annotation.Nullable;

import com.minelittlepony.unicopia.Affinity;
import com.minelittlepony.unicopia.InteractionManager;
import com.minelittlepony.unicopia.Race;
import com.minelittlepony.unicopia.ability.AbilityDispatcher;
import com.minelittlepony.unicopia.ability.magic.AttachableSpell;
import com.minelittlepony.unicopia.ability.magic.Caster;
import com.minelittlepony.unicopia.ability.magic.Spell;
import com.minelittlepony.unicopia.ability.magic.spell.SpellRegistry;
import com.minelittlepony.unicopia.entity.Physics;
import com.minelittlepony.unicopia.entity.PonyContainer;
import com.minelittlepony.unicopia.entity.Equine;
import com.minelittlepony.unicopia.entity.Trap;
import com.minelittlepony.unicopia.item.toxin.Toxicity;
import com.minelittlepony.unicopia.item.toxin.Toxin;
import com.minelittlepony.unicopia.network.Channel;
import com.minelittlepony.unicopia.network.EffectSync;
import com.minelittlepony.unicopia.network.MsgPlayerCapabilities;
import com.minelittlepony.unicopia.network.Transmittable;
import com.minelittlepony.unicopia.util.Copieable;
import com.minelittlepony.common.util.animation.LinearInterpolator;
import com.minelittlepony.common.util.animation.Interpolator;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class Pony implements Caster<PlayerEntity>, Equine<PlayerEntity>, Transmittable, Copieable<Pony> {

    private static final TrackedData<Integer> RACE = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.INTEGER);
    static final TrackedData<Float> ENERGY = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.FLOAT);
    static final TrackedData<Float> EXERTION = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<CompoundTag> EFFECT = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.TAG_COMPOUND);
    private static final TrackedData<CompoundTag> HELD_EFFECT = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.TAG_COMPOUND);

    private final AbilityDispatcher powers = new AbilityDispatcher(this);
    private final PlayerPhysics gravity = new PlayerPhysics(this);
    private final PlayerAttributes attributes = new PlayerAttributes();
    private final PlayerCamera camera = new PlayerCamera(this);
    private final MagicReserves mana;

    private final EffectSync effectDelegate = new EffectSync(this, EFFECT);

    private final Interpolator interpolator = new LinearInterpolator();

    private float nextStepDistance = 1;

    private final PlayerEntity entity;

    private boolean dirty = false;

    private boolean invisible = false;

    public Pony(PlayerEntity player) {
        this.entity = player;
        this.mana = new ManaContainer(this);

        player.getDataTracker().startTracking(RACE, Race.EARTH.ordinal());
        player.getDataTracker().startTracking(EFFECT, new CompoundTag());
        player.getDataTracker().startTracking(HELD_EFFECT, new CompoundTag());
    }

    public static void registerAttributes(DefaultAttributeContainer.Builder builder) {
        builder.add(PlayerAttributes.EXTENDED_REACH_DISTANCE);
    }

    @Override
    public Race getSpecies() {
        if (getOwner() == null) {
            return Race.HUMAN;
        }

        return Race.fromId(getOwner().getDataTracker().get(RACE));
    }

    @Override
    public void setSpecies(Race race) {
        race = race.validate(entity);

        entity.getDataTracker().set(RACE, race.ordinal());

        gravity.updateFlightStat(entity.abilities.flying);
        entity.sendAbilitiesUpdate();
    }

    public MagicReserves getMagicalReserves() {
        return mana;
    }

    @Override
    public boolean isInvisible() {
        return invisible && hasSpell();
    }

    @Override
    public void setInvisible(boolean invisible) {
        this.invisible = invisible;
    }

    @Override
    public Affinity getAffinity() {
        return Affinity.NEUTRAL;
    }

    public void setDirty() {
        dirty = true;
    }

    @Override
    public void sendCapabilities(boolean full) {
        dirty = false;

        if (entity instanceof ServerPlayerEntity) {
            System.out.println("Sending capabilities for player");
            Channel.BROADCAST_CAPABILITIES.send(new MsgPlayerCapabilities(full, this));
        }
    }

    public AbilityDispatcher getAbilities() {
        return powers;
    }

    @Override
    public Physics getPhysics() {
        return gravity;
    }

    public float getExtendedReach() {
        return (float)entity.getAttributeInstance(PlayerAttributes.EXTENDED_REACH_DISTANCE).getValue();
    }

    public Motion getMotion() {
        return gravity;
    }

    public PlayerCamera getCamera() {
        return camera;
    }

    public Interpolator getInterpolator() {
        return interpolator;
    }

    @Override
    public boolean beforeUpdate() {
        if (entity.world.isClient()) {
            if (entity.hasVehicle() && entity.isSneaking()) {

                Entity ridee = entity.getVehicle();

                if (ridee instanceof Trap) {
                    if (((Trap)ridee).attemptDismount(entity)) {
                        entity.stopRiding();
                    } else {
                        entity.setSneaking(false);
                    }
                } else {
                    entity.stopRiding();

                    if (ridee instanceof ServerPlayerEntity) {
                        ((ServerPlayerEntity)ridee).networkHandler.sendPacket(new EntityPassengersSetS2CPacket(ridee));
                    }
                }
            }
        }

        powers.tick();

        return false;
    }

    @Override
    public void tick() {
        gravity.tick();

        if (hasSpell()) {
            AttachableSpell effect = getSpell(AttachableSpell.class, true);

            if (effect != null) {
                if (entity.getEntityWorld().isClient()) {
                    effect.renderOnPerson(this);
                }

                if (!effect.updateOnPerson(this)) {
                    setSpell(null);
                }
            }
        }

        mana.addExertion(-10);
        mana.addEnergy(-1);

        attributes.applyAttributes(this);

        if (dirty) {
            sendCapabilities(true);
        }
    }

    public float onImpact(float distance) {
        if (getSpecies().canFly()) {
            distance = Math.max(0, distance - 5);
        }
        return distance;
    }

    @Override
    public void onJump() {
        if (gravity.isGravityNegative()) {
            Vec3d velocity = entity.getVelocity();
            entity.setVelocity(velocity.x, velocity.y * -1,  velocity.z);
        }
    }

    @Override
    public boolean onProjectileImpact(ProjectileEntity projectile) {
        if (hasSpell()) {
            Spell effect = getSpell();
            if (!effect.isDead() && effect.handleProjectileImpact(projectile)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean subtractEnergyCost(double foodSubtract) {
        if (!entity.abilities.creativeMode) {
            int food = (int)(entity.getHungerManager().getFoodLevel() - foodSubtract);

            if (food < 0) {
                entity.getHungerManager().add(-entity.getHungerManager().getFoodLevel(), 0);
                entity.damage(DamageSource.MAGIC, -food/2);
            } else {
                entity.getHungerManager().add((int)-foodSubtract, 0);
            }
        }

        return entity.getHealth() > 0;
    }

    public boolean stepOnCloud() {
        if (entity.fallDistance > 1 || entity.distanceTraveled > nextStepDistance) {
            nextStepDistance = entity.distanceTraveled + 2;
            entity.fallDistance = 0;

            return true;
        }

        return false;
    }

    public Optional<Text> trySleep(BlockPos pos) {

        if (findAllSpellsInRange(10).filter(p -> p instanceof Pony).map(Pony.class::cast).map(Pony::getSpecies).anyMatch(r -> r.isEnemy(getSpecies()))) {
            return Optional.of(new TranslatableText("block.unicopia.bed.not_safe"));
        }

        return Optional.empty();
    }

    public void onEat(ItemStack stack) {
        if (getSpecies() == Race.CHANGELING) {
            Toxin.POISON.afflict(getOwner(), Toxicity.SAFE, stack);
        }
    }

    @Override
    public void toNBT(CompoundTag compound) {
        compound.putString("playerSpecies", getSpecies().name());

        compound.put("powers", powers.toNBT());
        compound.put("gravity", gravity.toNBT());

        Spell effect = getSpell();

        if (effect != null) {
            compound.put("effect", SpellRegistry.toNBT(effect));
        }
    }

    @Override
    public void fromNBT(CompoundTag compound) {
        setSpecies(Race.fromName(compound.getString("playerSpecies")));

        powers.fromNBT(compound.getCompound("powers"));
        gravity.fromNBT(compound.getCompound("gravity"));

        if (compound.contains("effect")) {
            effectDelegate.set(SpellRegistry.instance().createEffectFromNBT(compound.getCompound("effect")));
        }
    }

    @Override
    public void copyFrom(Pony oldPlayer) {
        setSpell(oldPlayer.getSpell());
        setSpecies(oldPlayer.getSpecies());
        setDirty();
    }

    @Override
    public EffectSync getPrimarySpellSlot() {
        return effectDelegate;
    }

    @Override
    public void setSpell(@Nullable Spell effect) {
        Caster.super.setSpell(effect);
        setDirty();
    }

    @Override
    public void setOwner(PlayerEntity owner) {
    }

    @Override
    public PlayerEntity getOwner() {
        return entity;
    }

    @Override
    public int getCurrentLevel() {
        return 0;
    }

    @Override
    public void setCurrentLevel(int level) {
    }

    public boolean isClientPlayer() {
        return InteractionManager.instance().isClientPlayer(getOwner());
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static Pony of(@Nullable PlayerEntity player) {
        return player == null ? null : ((PonyContainer<Pony>)player).get();
    }

    public static boolean equal(GameProfile one, GameProfile two) {
        return one == two || (one != null && two != null && one.getId().equals(two.getId()));
    }

    public static boolean equal(PlayerEntity one, PlayerEntity two) {
        return one == two || (one != null && two != null && equal(one.getGameProfile(), two.getGameProfile()));
    }
}
