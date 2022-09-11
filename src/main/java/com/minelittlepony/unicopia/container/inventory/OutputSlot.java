package com.minelittlepony.unicopia.container.inventory;

import com.minelittlepony.unicopia.USounds;
import com.minelittlepony.unicopia.client.sound.BufferedExecutor;
import com.minelittlepony.unicopia.container.*;
import com.minelittlepony.unicopia.entity.player.Pony;
import com.minelittlepony.unicopia.item.UItems;
import com.minelittlepony.unicopia.util.InventoryUtil;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.sound.SoundCategory;

public class OutputSlot extends CraftingResultSlot implements SpellbookSlot {
    private final SpellbookScreenHandler handler;
    private final PlayerEntity player;
    private final SpellbookInventory input;

    private final int ring;

    public OutputSlot(SpellbookScreenHandler spellbookScreenHandler, PlayerEntity player, SpellbookInventory input, Inventory inventory, int index, int[] params) {
        super(player, input, inventory, index, params[0], params[1]);
        handler = spellbookScreenHandler;
        this.player = player;
        this.input = input;
        this.ring = params[2];
    }

    @Override
    public void setStack(ItemStack stack) {
        if (!stack.isEmpty() && !ItemStack.areEqual(stack, getStack())) {
            BufferedExecutor.bufferExecution(player, () -> {
                player.playSound(stack.getItem() == UItems.BOTCHED_GEM ? USounds.GUI_ABILITY_FAIL : USounds.GUI_SPELL_CRAFT_SUCCESS, SoundCategory.MASTER, 1, 0.3F);
            });
        }
        super.setStack(stack);
    }

    @Override
    public int getRing() {
        return ring;
    }

    @Override
    public boolean isEnabled() {
       return handler.canShowSlots.test(SlotType.CRAFTING) && hasStack();
    }

    @Override
    public void onTakeItem(PlayerEntity player, ItemStack stack) {
        Pony pony = Pony.of(player);
        InventoryUtil.stream(input).forEach(s -> {
            pony.getDiscoveries().unlock(s.getItem());
        });
       //gemSlot.setStack(ItemStack.EMPTY);
        super.onTakeItem(player, stack);
    }
}