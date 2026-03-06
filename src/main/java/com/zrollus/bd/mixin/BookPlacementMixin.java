package com.zrollus.bd.mixin;

import com.zrollus.bd.block.ModBlocks;
import com.zrollus.bd.block.custom.BookStackBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class BookPlacementMixin {
    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    private void placeBookStack(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        ItemStack stack = context.getStack();

        // Check if the item is a vanilla Book
        if (stack.isOf(Items.BOOK)) {
            // Check if there is already a book stack there (to allow the block's own 'onUse' to handle stacking)
            if (context.getWorld().getBlockState(context.getBlockPos()).isOf(ModBlocks.BOOK_STACK)) {
                return; // Let the Block handle adding to the stack
            }

            // Logic to place the block on a new surface
            ActionResult result = context.getWorld().setBlockState(context.getBlockPos().offset(context.getSide()),
                    ModBlocks.BOOK_STACK.getDefaultState(), 3) ? ActionResult.SUCCESS : ActionResult.PASS;

            if (result == ActionResult.SUCCESS) {
                if (context.getPlayer() != null && !context.getPlayer().getAbilities().creativeMode) {
                    stack.decrement(1);
                }
                cir.setReturnValue(ActionResult.SUCCESS);
            }
        }
    }
}