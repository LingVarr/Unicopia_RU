package com.minelittlepony.unicopia.entity.player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import com.minelittlepony.unicopia.ability.magic.Affine;
import com.minelittlepony.unicopia.util.NbtSerialisable;
import com.minelittlepony.unicopia.util.Tickable;

import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class PlayerCharmTracker implements Tickable, NbtSerialisable {

    private final Pony pony;

    private final ItemTracker armour = new ItemTracker();

    PlayerCharmTracker(Pony pony) {
        this.pony = pony;
    }

    @Override
    public void tick() {
        armour.update(pony.getMaster().getInventory().armor.stream());
    }

    public ItemTracker getArmour() {
        return armour;
    }

    @Override
    public void toNBT(NbtCompound compound) {
        compound.put("armour", armour.toNBT());
    }

    @Override
    public void fromNBT(NbtCompound compound) {
        armour.fromNBT(compound.getCompound("armour"));
    }

    public class ItemTracker implements NbtSerialisable {
        private final Map<Charm, Integer> items = new HashMap<>();

        public void update(Stream<ItemStack> stacks) {
            Set<Charm> found = new HashSet<>();
            stacks.forEach(stack -> {
                if (stack.getItem() instanceof Charm) {
                    items.compute((Charm)stack.getItem(), (item, prev) -> prev == null ? 1 : prev + 1);
                    found.add((Charm)stack.getItem());
                }
            });
            items.entrySet().removeIf(e -> {
                if (!found.contains(e.getKey())) {
                    e.getKey().onRemoved(pony, e.getValue());
                    return true;
                }
                return false;
            });
        }

        public int getTicks(Charm charm) {
            return items.getOrDefault(charm.asItem(), 0);
        }

        public boolean contains(Charm charm) {
            return getTicks(charm) > 0;
        }

        @Override
        public void toNBT(NbtCompound compound) {
            items.forEach((charm, count) -> {
                compound.putInt(Registry.ITEM.getId(charm.asItem()).toString(), count);
            });
        }

        @Override
        public void fromNBT(NbtCompound compound) {
            items.clear();
            compound.getKeys().stream().map(Identifier::tryParse)
                .filter(Objects::nonNull)
                .map(id -> Map.entry(Registry.ITEM.get(id), compound.getInt(id.toString())))
                .filter(i -> i.getKey() instanceof Charm && i.getValue() > 0)
                .forEach(item -> items.put((Charm)item.getKey(), item.getValue()));
        }
    }

    public interface Charm extends Affine, ItemConvertible {
        void onRemoved(Pony pony, int timeWorn);
    }
}