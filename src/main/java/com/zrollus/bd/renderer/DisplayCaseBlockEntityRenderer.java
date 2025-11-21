package com.zrollus.bd.renderer;


import com.zrollus.bd.Entity.DisplayCaseBlockEntity;
import com.zrollus.bd.block.custom.DisplayCaseBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.WallMountLocation;
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

        if (!stack.isEmpty()) {
            ItemRenderer itemRenderer = MinecraftClient.getInstance().getItemRenderer();

            matrices.push();
            float offset = 0.4375f; // same as item frame distance
            // Position on top of block
            matrices.translate(0.5, 0.1, 0.5);

            Direction facing = entity.getCachedState().get(DisplayCaseBlock.FACING);

            WallMountLocation mount = entity.getCachedState().get(DisplayCaseBlock.WALL);

            // --- FLOOR ---
            if (mount == WallMountLocation.FLOOR) {
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90)); // item flat
            }

            // --- CEILING ---
            else if (mount == WallMountLocation.CEILING) {
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90));
                matrices.translate(0, 0, 0.8);
            }

            // --- WALL (each cardinal direction) ---
            else if (mount == WallMountLocation.WALL) {
                switch (facing) {
                    case NORTH -> {
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
                        matrices.translate(0, 0.4, -offset);
                    }
                    case SOUTH -> {
                        matrices.translate(0, 0.4, -offset);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-180));
                    }
                    case WEST -> {
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));
                        matrices.translate(0, 0.4, offset);
                    }
                    case EAST -> {
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90));
                        matrices.translate(0, 0.4, offset);
                    }
                }
            }

            // Size
            matrices.scale(0.5f, 0.5f, 0.5f);


            itemRenderer.renderItem(
                    stack,
                    ModelTransformationMode.FIXED,
                    getLightLevel(entity.getWorld(),
                            entity.getPos()),
                    OverlayTexture.DEFAULT_UV,
                    matrices,
                    vertexConsumers,
                    entity.getWorld(),
                    1
            );

            matrices.pop();
        }
    }
    private int getLightLevel(World world, BlockPos pos) {
        int bLight = world.getLightLevel(LightType.BLOCK, pos);
        int sLight = world.getLightLevel(LightType.SKY, pos);
        return LightmapTextureManager.pack(bLight, sLight);
    }
}