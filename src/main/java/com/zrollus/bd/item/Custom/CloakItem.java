package com.zrollus.bd.item.Custom;

import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** A dependency-free 1.21.1 cloak that is active while held in the offhand. */
public final class CloakItem extends Item {
    public CloakItem(Settings settings) {
        super(settings.maxCount(1));
    }

    public static AttributeModifiersComponent createAttributeModifiers() {
        return AttributeModifiersComponent.builder()
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED,
                        new EntityAttributeModifier(Identifier.of("bd", "cloak_speed"), 0.1,
                                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                        AttributeModifierSlot.OFFHAND)
                .build();
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack held = user.getStackInHand(hand);
        if (hand == Hand.OFF_HAND) return TypedActionResult.success(held, world.isClient);
        if (world.isClient) return TypedActionResult.success(held, true);

        ItemStack previous = user.getOffHandStack().copy();
        ItemStack equipped = held.copyWithCount(1);
        held.decrement(1);
        user.setStackInHand(Hand.OFF_HAND, equipped);
        if (!previous.isEmpty()) {
            if (held.isEmpty()) user.setStackInHand(hand, previous);
            else user.getInventory().offerOrDrop(previous);
        }
        return TypedActionResult.success(user.getStackInHand(hand), false);
    }
}
