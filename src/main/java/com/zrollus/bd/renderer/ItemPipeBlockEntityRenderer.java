package com.zrollus.bd.renderer;

import com.zrollus.bd.Entity.ItemPipeBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class ItemPipeBlockEntityRenderer implements BlockEntityRenderer<ItemPipeBlockEntity> {
    private final ItemRenderer itemRenderer;

    public ItemPipeBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ItemPipeBlockEntity pipe, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider consumers, int light, int overlay) {
        ItemStack stack = pipe.getMovingStack();
        if (stack.isEmpty()) return;
        float progress = pipe.getTravelProgress(tickDelta);
        Vec3d from = vector(pipe.getTravelFrom()).multiply(0.48);
        Vec3d to = vector(pipe.getTravelTo()).multiply(0.48);
        Vec3d offset = from.lerp(to, progress);

        matrices.push();
        matrices.translate(0.5 + offset.x, 0.5 + offset.y, 0.5 + offset.z);
        long time = pipe.getWorld() == null ? 0 : pipe.getWorld().getTime();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((time + tickDelta) * 12.0F));
        matrices.scale(0.28F, 0.28F, 0.28F);
        itemRenderer.renderItem(stack, ModelTransformationMode.FIXED,
                LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV,
                matrices, consumers, pipe.getWorld(), 0);
        matrices.pop();
    }

    private static Vec3d vector(Direction direction) {
        return new Vec3d(direction.getOffsetX(), direction.getOffsetY(), direction.getOffsetZ());
    }
}
