package com.zrollus.bd.renderer;

import com.zrollus.bd.Entity.PlayerShopkeeperEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public final class PlayerShopkeeperRenderer extends MobEntityRenderer<PlayerShopkeeperEntity, PlayerEntityModel<PlayerShopkeeperEntity>> {
    private final PlayerEntityModel<PlayerShopkeeperEntity> normalModel;
    private final PlayerEntityModel<PlayerShopkeeperEntity> slimModel;

    public PlayerShopkeeperRenderer(EntityRendererFactory.Context context) {
        super(context, new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false), 0.5F);
        this.normalModel = model;
        this.slimModel = new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER_SLIM), true);
    }

    @Override
    public void render(PlayerShopkeeperEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        SkinTextures textures = skin(entity);
        model = textures.model() == SkinTextures.Model.SLIM ? slimModel : normalModel;
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(PlayerShopkeeperEntity entity) {
        return skin(entity).texture();
    }

    private static SkinTextures skin(PlayerShopkeeperEntity entity) {
        return MinecraftClient.getInstance().getSkinProvider().getSkinTextures(entity.getSkinProfile());
    }
}
