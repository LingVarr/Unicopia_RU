package com.minelittlepony.unicopia.ability.magic.spell;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.minelittlepony.unicopia.ability.magic.Caster;
import com.minelittlepony.unicopia.ability.magic.spell.effect.SpellType;
import com.minelittlepony.unicopia.ability.magic.spell.trait.SpellTraits;
import com.minelittlepony.unicopia.item.GemstoneItem;
import com.minelittlepony.unicopia.item.UItems;
import com.minelittlepony.unicopia.projectile.MagicProjectileEntity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

public class ThrowableSpell extends AbstractDelegatingSpell {

    private Spell spell;

    public ThrowableSpell(SpellType<?> type, SpellTraits traits) {
        super(type, traits);
    }

    public ThrowableSpell setSpell(Spell spell) {
        this.spell = spell;
        return this;
    }

    @Override
    protected Collection<Spell> getDelegates() {
        return List.of(spell);
    }

    /**
     * Projects this spell.
     *
     * Returns the resulting projectile entity for customization (or null if on the client).
     */
    public Optional<MagicProjectileEntity> throwProjectile(Caster<?> caster) {
        World world = caster.getWorld();

        LivingEntity entity = caster.getMaster();

        world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT, SoundCategory.NEUTRAL, 0.7F, 0.4F / (world.random.nextFloat() * 0.4F + 0.8F));

        if (!caster.isClient()) {
            MagicProjectileEntity projectile = new MagicProjectileEntity(world, entity);

            projectile.setItem(GemstoneItem.enchanted(UItems.GEMSTONE.getDefaultStack(), spell.getType()));
            projectile.setSpell(this);
            projectile.setProperties(entity, entity.getPitch(), entity.getYaw(), 0, 1.5F, 1);
            projectile.setHydrophobic();
            configureProjectile(projectile, caster);
            world.spawnEntity(projectile);

            return Optional.of(projectile);
        }

        return Optional.empty();
    }

    @Override
    public ThrowableSpell toThrowable() {
        return this;
    }
}