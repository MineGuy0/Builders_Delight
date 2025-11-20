package com.zrollus.bd.renderer;


import com.zrollus.bd.Entity.DisplayCaseBlockEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class DisplayCaseBlockEntityRenderer implements BlockEntityRenderer<DisplayCaseBlockEntity> {

    private static final Identifier ITEMFRAME_TEX =
            new Identifier("minecraft", "textures/entity/item_frame/item_frame.png");

    private final ItemRenderer itemRenderer;

    public DisplayCaseBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        this.itemRenderer = ctx.getItemRenderer();
    }

    @Override
    public void render(DisplayCaseBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {

        if (entity.getStack().isEmpty()) return;

        matrices.push();

        // Center & scale
        matrices.translate(0.5, 0.25, 0.5);
        matrices.scale(0.75f, 0.75f, 0.75f);

        // ----------------------------
        // 1) Draw item-frame background
        // ----------------------------
        matrices.push();
        matrices.translate(0, 0, -0.01); // Slight offset so it renders behind the item

        VertexConsumer vc =
                vertexConsumers.getBuffer(RenderLayer.getEntityCutout(ITEMFRAME_TEX));

        MatrixStack.Entry entry = matrices.peek();
        var m = entry.getPositionMatrix();
        var n = entry.getNormalMatrix();

        // Render a flat quad 16×16 like vanilla GUI items
        quad(vc, m, n, -0.5f, -0.5f, 0.5f, 0.5f, light);

        matrices.pop();

        // ----------------------------
        // 2) Render the item normally
        // ----------------------------
        itemRenderer.renderItem(
                entity.getStack(),
                ModelTransformationMode.GUI,
                false,
                matrices,
                vertexConsumers,
                light,
                overlay,
                itemRenderer.getModel(entity.getStack(), entity.getWorld(), null, 0)
        );

        matrices.pop();
    }

    private void quad(VertexConsumer vc, Matrix4f m, Matrix3f n,
                      float x1, float y1, float x2, float y2, int light) {
        vc.vertex(m, x1, y2, 0)
                .color(255, 255, 255, 255)
                .texture(0, 1)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(n, 0, 0, 1)
                .next();

        vc.vertex(m, x2, y2, 0)
                .color(255, 255, 255, 255)
                .texture(1, 1)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(n, 0, 0, 1)
                .next();

        vc.vertex(m, x2, y1, 0)
                .color(255, 255, 255, 255)
                .texture(1, 0)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(n, 0, 0, 1)
                .next();

        vc.vertex(m, x2, y1, 0)
                .color(255, 255, 255, 255)
                .texture(1, 0)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(n, 0, 0, 1)
                .next();

        vc.vertex(m, x1, y1, 0)
                .color(255, 255, 255, 255)
                .texture(0, 0)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(n, 0, 0, 1)
                .next();

        vc.vertex(m, x1, y2, 0)
                .color(255, 255, 255, 255)
                .texture(0, 1)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(n, 0, 0, 1)
                .next();

    }

}
