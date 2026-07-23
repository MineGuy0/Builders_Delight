package com.zrollus.bd.renderer;


import com.zrollus.bd.Entity.DisplayCaseBlockEntity;
import com.zrollus.bd.block.custom.DisplayCaseBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import org.joml.Quaternionf;


public class DisplayCaseBlockEntityRenderer implements BlockEntityRenderer<DisplayCaseBlockEntity> {

    private final ItemRenderer itemRenderer;

    public DisplayCaseBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(DisplayCaseBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {

        ItemStack stack = entity.getStack();
        if (stack.isEmpty()) return;

        BlockState state = entity.getCachedState();
        Direction facing = state.get(DisplayCaseBlock.FACING);
        BlockFace mount = state.get(DisplayCaseBlock.FACE);

        matrices.push();

        // 1. Move to the EXACT center of the block space
        matrices.translate(0.5, 0.5, 0.5);

        // 2. Handle Orientation
        if (mount == BlockFace.FLOOR) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
            // Rotate the item to face "upright" relative to the block's orientation
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-facing.asRotation()));
        }
        else if (mount == BlockFace.CEILING) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(facing.asRotation()));
        }
        else { // WALL
            // Rotate the entire coordinate system to face the wall direction
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing.asRotation() + 180));
        }

        // 3. The "Stick to Back" Offset
        // 0.5 is the edge of the block. We use 0.4375 to keep it
        // inside the glass but flush against the back.
        matrices.translate(0, 0, 0.4375);

        // 4. Flatten and Scale
        // We scale Z to near-zero so it looks like a flat sprite
        matrices.scale(0.5f, 0.5f, 0.005f);

        this.itemRenderer.renderItem(
                stack,
                ModelTransformationMode.FIXED,
                getLightLevel(entity.getWorld(), entity.getPos()),
                OverlayTexture.DEFAULT_UV,
                matrices,
                vertexConsumers,
                entity.getWorld(),
                0
        );

        matrices.pop();
    }
    private int getLightLevel(World world, BlockPos pos) {
        int bLight = world.getLightLevel(LightType.BLOCK, pos);
        int sLight = world.getLightLevel(LightType.SKY, pos);
        return LightmapTextureManager.pack(bLight, sLight);
    }
}
