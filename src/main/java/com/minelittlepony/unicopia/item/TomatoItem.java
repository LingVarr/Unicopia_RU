package com.minelittlepony.unicopia.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class TomatoItem extends Item {

    public TomatoItem(int hunger, float saturation) {
        super(new Settings()
                .group(ItemGroup.FOOD)
                .food(new FoodComponent.Builder()
                        .hunger(hunger)
                        .saturationModifier(saturation)
                        .build())
        );
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity entity) {
        stack = super.finishUsing(stack, world, entity);
        entity.removePotionEffect(StatusEffects.NAUSEA);
        return stack;
    }

}