package com.minelittlepony.unicopia.entity;

import com.minelittlepony.unicopia.Race;
import com.minelittlepony.unicopia.ducks.IItemEntity;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ActionResult;

public class ItemEntityCapabilities implements RaceContainer<ItemEntity>, Owned<ItemEntity> {
    private static final TrackedData<Integer> ITEM_RACE = DataTracker.registerData(ItemEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private final ItemEntity owner;
    private Race serverRace;

    public ItemEntityCapabilities(ItemEntity owner) {
        this.owner = owner;
        owner.getDataTracker().startTracking(ITEM_RACE, Race.HUMAN.ordinal());
    }

    @Override
    public void onUpdate() {
    }

    @Override
    public boolean beforeUpdate() {

        if (!owner.world.isClient) {
            Race race = getSpecies();
            if (race != serverRace) {
                serverRace = race;
                setSpecies(Race.HUMAN);
                setSpecies(race);
            }
        }

        ItemStack stack = owner.getStack();

        if (!stack.isEmpty() && stack.getItem() instanceof TickableItem) {
            return ((TickableItem)stack.getItem()).onGroundTick((IItemEntity)owner) == ActionResult.SUCCESS;
        }

        return false;
    }

    @Override
    public Race getSpecies() {
        return Race.fromId(getOwner().getDataTracker().get(ITEM_RACE));
    }

    @Override
    public void setSpecies(Race race) {
        getOwner().getDataTracker().set(ITEM_RACE, race.ordinal());
    }

    @Override
    public void toNBT(CompoundTag compound) {
        compound.putString("owner_species", getSpecies().name());
    }


    @Override
    public void fromNBT(CompoundTag compound) {
        setSpecies(Race.fromName(compound.getString("owner_species")));
    }

    @Override
    public void setOwner(ItemEntity owner) {

    }

    @Override
    public void onDimensionalTravel(int destinationDimension) {

    }

    @Override
    public ItemEntity getOwner() {
        return owner;
    }

    public interface TickableItem {
        ActionResult onGroundTick(IItemEntity entity);
    }
}
