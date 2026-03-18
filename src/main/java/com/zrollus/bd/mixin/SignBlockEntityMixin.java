package com.zrollus.bd.mixin;

import com.zrollus.bd.Lib.DynamicIdMapper;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(SignBlockEntity.class)
public abstract class SignBlockEntityMixin extends BlockEntity {

    @Unique
    private boolean isProcessingShop = false;

    public SignBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "setText", at = @At("HEAD"), cancellable = true)
    private void onSetText(SignText text, boolean front, CallbackInfoReturnable<Boolean> cir) {
        if (isProcessingShop || this.world == null || this.world.isClient) return;

        String line1 = text.getMessage(0, false).getString().trim();
        String line4 = text.getMessage(3, false).getString().trim();

        if (line4.startsWith("#") && line4.length() > 1) {
            try {
                int id = Integer.parseInt(line4.substring(1));
                var server = this.world.getServer();
                if (server == null) return;

                String fullName = com.zrollus.bd.Lib.DynamicIdMapper.getServerState(server).getName(id);
                if (fullName != null) {
                    isProcessingShop = true;

                    // 1. Handle Admin Permissions
                    boolean isAdminShop = line1.equalsIgnoreCase("Admin");
                    PlayerEntity player = this.world.getClosestPlayer(this.getPos().getX(), this.getPos().getY(), this.getPos().getZ(), 8.0, false);

                    if (isAdminShop && (player == null || !player.hasPermissionLevel(2))) {
                        if (player != null) player.sendMessage(Text.literal("§cYou need OP to create Admin shops!"), false);
                        isProcessingShop = false;
                        cir.setReturnValue(false);
                        return;
                    }

                    // 2. Prepare the NEW SignText
                    SignText newText = text;

                    // Format Admin Line
                    if (isAdminShop) {
                        newText = newText.withMessage(0, Text.literal("Admin").formatted(Formatting.DARK_RED, Formatting.BOLD));
                    }

                    // Format Item Line (Line 4)
                    String prettyName = formatName(fullName);
                    newText = newText.withMessage(3, Text.literal(prettyName).formatted(Formatting.GREEN));

                    // 3. PERSISTENCE FIX: Save to NBT directly before returning
                    // Using 'this' refers to the SignBlockEntity because of the Mixin target
                    SignBlockEntity sign = (SignBlockEntity)(Object)this;

                    // We use the SignBlockEntity's internal NBT handling
                    NbtCompound nbt = sign.createNbt();
                    nbt.putString("ShopItemRaw", fullName);
                    sign.readNbt(nbt); // This forces the "ShopItemRaw" into the live object
                    sign.markDirty();  // Tells Minecraft to save this to disk

                    // 4. Apply the text and finish
                    sign.setText(newText, front);

                    isProcessingShop = false;
                    cir.setReturnValue(true);
                    cir.cancel();
                }
            } catch (Exception e) {
                isProcessingShop = false;
                e.printStackTrace();
            }
        }
    }

    @Unique
    private String formatName(String fullName) {
        String path = fullName.contains(":") ? fullName.split(":")[1] : fullName;
        String[] words = path.split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty() && sb.length() + w.length() < 15) {
                sb.append(Character.toUpperCase(w.charAt(0)))
                        .append(w.substring(1))
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }
}