package com.zrollus.bd;

import com.zrollus.bd.Entity.ModEntities;
import com.zrollus.bd.GUI.InvSeeScreen;
import com.zrollus.bd.GUI.InvSeeScreenHandler;
import com.zrollus.bd.GUI.ModScreenHandlers;
import com.zrollus.bd.GUI.PlayerVaultScreen;
import com.zrollus.bd.GUI.ShopkeeperEditorScreenHandler;
import com.zrollus.bd.GUI.ShopkeeperEditorScreen;
import com.zrollus.bd.GUI.ItemPipeScreen;
import com.zrollus.bd.GUI.CollectorScreenHandler;
import com.zrollus.bd.GUI.CollectorScreen;
import com.zrollus.bd.Entity.CollectorBlockEntity;
import com.zrollus.bd.block.ModBlocks;
import com.zrollus.bd.block.custom.BluestoneWireBlock;
import com.zrollus.bd.item.Custom.HammerItem;
import com.zrollus.bd.renderer.DisplayCaseBlockEntityRenderer;
import com.zrollus.bd.renderer.ItemPipeBlockEntityRenderer;
import com.zrollus.bd.renderer.SeatRenderer;
import com.zrollus.bd.renderer.PlayerShopkeeperRenderer;
import com.zrollus.bd.utils.Nudge;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.WitherSkeletonEntityRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
public class BuildersDelightClient implements ClientModInitializer {


    public static KeyBinding nudgeUp;
    public static KeyBinding nudgeDown;
    public static KeyBinding nudgeLeft;
    public static KeyBinding nudgeRight;

    public static Vec3i nudgeOffset = new Vec3i(0, 0, 0);
    public static List<BlockPos> renderArea = new ArrayList<>();
    private static boolean wasMiningFluid;
    private static final Map<net.minecraft.registry.RegistryKey<World>, Set<BlockPos>> COLLECTOR_PREVIEW_PINS =
            new HashMap<>();

    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlers.PLAYER_VAULT, PlayerVaultScreen::new);
        HandledScreens.register(ModScreenHandlers.INVSEE, InvSeeScreen::new);
        HandledScreens.register(ModScreenHandlers.SHOPKEEPER_EDITOR, ShopkeeperEditorScreen::new);
        HandledScreens.register(ModScreenHandlers.ITEM_PIPE, ItemPipeScreen::new);
        HandledScreens.register(ModScreenHandlers.COLLECTOR, CollectorScreen::new);
        ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
            // If the block is powered, return Orange (0xFF8000), else Blue (0x00AEEF)
            if (state.contains(BluestoneWireBlock.POWERED) && state.get(BluestoneWireBlock.POWERED)) {
                return 0xFF8000;
            }
            return 0x00AEEF;
        }, ModBlocks.BLUESTONE_WIRE);

        // Don't forget to register it for the Item in hand too!
        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> 0x00AEEF, ModBlocks.BLUESTONE_WIRE);

        net.minecraft.block.Block[] pipeBlocks = ModBlocks.getItemPipeBlocks();
        ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) ->
                        state.getBlock() instanceof com.zrollus.bd.block.custom.ItemPipeBlock pipe
                                ? pipe.getPipeColor().getEntityColor() : 0xFFFFFF,
                pipeBlocks);
        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
            if (stack.getItem() instanceof net.minecraft.item.BlockItem item
                    && item.getBlock() instanceof com.zrollus.bd.block.custom.ItemPipeBlock pipe) {
                return pipe.getPipeColor().getEntityColor();
            }
            return 0xFFFFFF;
        }, pipeBlocks);

        // Ensure the wire is transparent (Cutout)
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BLUESTONE_WIRE, RenderLayer.getCutout());

        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.STEEL_GRATE, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BRAZIER, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.SOUL_BRAZIER, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.TOGGLE_TORCH, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BOOK_STACK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.AQUARIUM_GLASS, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DISPLAY_CASE, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getTranslucent(), pipeBlocks);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getTranslucent(),
                ModBlocks.EXPERIENCE_COLLECTORS.values().toArray(net.minecraft.block.Block[]::new));
        BlockEntityRendererRegistry.register(ModEntities.DISPLAY_CASE, DisplayCaseBlockEntityRenderer::new);
        BlockEntityRendererRegistry.register(ModEntities.ITEM_PIPE, ItemPipeBlockEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.SEAT, SeatRenderer::new);
        EntityRendererRegistry.register(ModEntities.PLAYER_SHOPKEEPER, PlayerShopkeeperRenderer::new);
        EntityRendererRegistry.register(ModEntities.DARKBOUND, WitherSkeletonEntityRenderer::new);

        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.ConfirmNudgePayload.ID, (payload, context) -> {
            System.out.println("Nudge acknowledged by server.");
        });
        nudgeUp = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.buildersdelight.nudge_up", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_HOME, "category.buildersdelight"));
        nudgeDown = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.buildersdelight.nudge_down", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_END, "category.buildersdelight"));
        nudgeLeft = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.buildersdelight.nudge_left", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_DELETE, "category.buildersdelight"));
        nudgeRight = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.buildersdelight.nudge_right", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_PAGE_DOWN, "category.buildersdelight"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            boolean miningFluid = false;
            if (client.options.attackKey.isPressed()
                    && ModEnchantments.getLevel(client.player.getMainHandStack(), client.world.getRegistryManager(), ModEnchantments.FLUIDBREAKER) > 0
                    && client.crosshairTarget instanceof BlockHitResult fluidHit
                    && !client.world.getBlockState(fluidHit.getBlockPos()).getFluidState().isEmpty()) {
                ClientPlayNetworking.send(new ModNetworking.FluidMiningPayload(fluidHit.getBlockPos(), true));
                miningFluid = true;
                if (client.player.age % 3 == 0) {
                    BlockPos pos = fluidHit.getBlockPos();
                    boolean lava = client.world.getFluidState(pos).isIn(FluidTags.LAVA);
                    for (int i = 0; i < 4; i++) {
                        double x = pos.getX() + client.world.random.nextDouble();
                        double y = pos.getY() + 0.15 + client.world.random.nextDouble() * 0.7;
                        double z = pos.getZ() + client.world.random.nextDouble();
                        client.world.addParticle(lava ? ParticleTypes.FLAME : ParticleTypes.SPLASH,
                                x, y, z, 0.0, 0.025, 0.0);
                    }
                    client.world.addParticle(lava ? ParticleTypes.LARGE_SMOKE : ParticleTypes.BUBBLE,
                            pos.getX() + 0.5, pos.getY() + 0.55, pos.getZ() + 0.5,
                            0.0, 0.02, 0.0);
                }
            }
            if (!miningFluid && wasMiningFluid) {
                ClientPlayNetworking.send(new ModNetworking.FluidMiningPayload(BlockPos.ORIGIN, false));
            }
            wasMiningFluid = miningFluid;

            // Nudge logic
            ItemStack mainHand = client.player.getMainHandStack();
            if (mainHand.getItem() instanceof HammerItem) {
                if (nudgeUp.wasPressed()) {
                    Vec3i dir = getRelativeUpDown(client.player, true);
                    Nudge.offset = clampNudge(Nudge.offset.add(dir));
                    sendNudgeToServer(Nudge.offset);
                }
                if (nudgeDown.wasPressed()) {
                    Vec3i dir = getRelativeUpDown(client.player, false);
                    Nudge.offset = clampNudge(Nudge.offset.add(dir));
                    sendNudgeToServer(Nudge.offset);
                }
                if (nudgeLeft.wasPressed()) {
                    Vec3i left = getRelativeLeftRight(client.player, true);
                    Nudge.offset = clampNudge(Nudge.offset.add(left));
                    sendNudgeToServer(Nudge.offset);
                }
                if (nudgeRight.wasPressed()) {
                    Vec3i right = getRelativeLeftRight(client.player, false);
                    Nudge.offset = clampNudge(Nudge.offset.add(right));
                    sendNudgeToServer(Nudge.offset);
                }
            }
            boolean isHoldingHammer = client.player.getMainHandStack().getItem() instanceof HammerItem;

            // Reset client-side offset when hammer is not held
            if (!isHoldingHammer && !Nudge.offset.equals(Vec3i.ZERO)) {
                Nudge.offset = Vec3i.ZERO;
                sendNudgeToServer(Vec3i.ZERO); // Also tell server to clear
            }
            
            

            // Grid render logic
            ItemStack stack = client.player.getMainHandStack();
            if (!(stack.getItem() instanceof HammerItem)) {
                renderArea.clear();
                return;
            }

            HitResult hit = client.crosshairTarget;
            if (hit instanceof BlockHitResult blockHit) {
                BlockPos target = blockHit.getBlockPos();
                if (!client.world.getBlockState(target).isAir()) { // ✅ Don't render if hitting air
                    Direction face = blockHit.getSide();
                    //istg this shit does nothing?
                    BlockPos offset = ((net.minecraft.util.math.Direction) face).getAxis() == Direction.Axis.Y
                            ? new BlockPos(Nudge.offset.getX(), 0, Nudge.offset.getZ())
                            : new BlockPos(Nudge.offset);

                    BlockPos origin = blockHit.getBlockPos().add(offset);
                    // compute hammering level & radius
                    ItemStack stack2       = client.player.getMainHandStack();
                    int level = ModEnchantments.getLevel(stack, client.world.getRegistryManager(), ModEnchantments.HAMMERING);
                    int      radius      = 1 + level;      // 0 → 3×3, 1 → 5×5, 2 → 7×7, 3 → 9×9

                    renderArea = HammerItem.calculateDynamicGrid(
                            origin,
                            face,
                            client.player,
                            stack2,
                            radius
                    );

                } else {
                    renderArea.clear();
                }
            } else {
                renderArea.clear();
            }
            if (client.player != null) {
                float pitch = client.player.getPitch();
                boolean lookingVerticallyNow = (pitch > 45 || pitch < -45);
                boolean wasVertical = Nudge.offset.getX() != 0 || Nudge.offset.getZ() != 0;

                if (!lookingVerticallyNow && wasVertical) {
                    Vec3i newOffset = new Vec3i(0, Nudge.offset.getY(), 0);
                    if (!newOffset.equals(Nudge.offset)) {
                        Nudge.offset = newOffset;
                        sendNudgeToServer(newOffset);
                    }
                }
            }
        });

        // Box rendering
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            Camera camera = context.camera();
            Vec3d camPos = camera.getPos();
            VertexConsumerProvider.Immediate buffer = (VertexConsumerProvider.Immediate) context.consumers();

            for (BlockPos pos : renderArea) {
                Box box = new Box(pos).offset(-camPos.x, -camPos.y, -camPos.z);

                WorldRenderer.drawBox(
                        context.matrixStack(), buffer.getBuffer(RenderLayer.getLines()),
                        box, 1.0f, 0.0f, 0.31f, 1.0f // folly red (#ff004f)
                );
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && client.world != null) {
                net.minecraft.registry.RegistryKey<World> dimension = client.world.getRegistryKey();
                Set<BlockPos> pins = COLLECTOR_PREVIEW_PINS.computeIfAbsent(dimension, ignored -> new HashSet<>());
                BlockPos openPos = null;
                if (client.player.currentScreenHandler instanceof CollectorScreenHandler collector) {
                    openPos = collector.getCollectorPos().toImmutable();
                    if (collector.previewEnabled()) {
                        pins.add(openPos);
                        renderCollectorPreview(context, buffer, camPos, client, collector.getPreviewBox(),
                                Vec3d.ofCenter(openPos), collector.getCenter(), collector.getShape());
                    } else {
                        pins.remove(openPos);
                    }
                }

                for (BlockPos pinnedPos : List.copyOf(pins)) {
                    if (pinnedPos.equals(openPos)) continue;
                    if (client.player.squaredDistanceTo(Vec3d.ofCenter(pinnedPos)) > 128.0 * 128.0) continue;
                    if (!client.world.isChunkLoaded(pinnedPos.getX() >> 4, pinnedPos.getZ() >> 4)) continue;
                    if (!(client.world.getBlockEntity(pinnedPos) instanceof CollectorBlockEntity collector)
                            || !collector.isPreviewEnabled()) {
                        pins.remove(pinnedPos);
                        continue;
                    }
                    renderCollectorPreview(context, buffer, camPos, client, collector.getRegion().bounds(pinnedPos),
                            Vec3d.ofCenter(pinnedPos), collector.getRegion().center(pinnedPos),
                            collector.getRegion().shape());
                }
            }

            buffer.draw();
        });
    }

    private static void renderCollectorPreview(
            net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext context,
            VertexConsumerProvider.Immediate buffer, Vec3d camera, MinecraftClient client,
            Box worldBox, Vec3d origin, Vec3d center,
            com.zrollus.bd.block.custom.CollectionRegion.Shape shape) {
        boolean loaded = isCollectorPreviewLoaded(client, worldBox);
        float red = loaded ? 0.15F : 1.0F;
        float green = loaded ? 1.0F : 0.15F;
        Box preview = worldBox.offset(-camera.x, -camera.y, -camera.z);
        WorldRenderer.drawBox(context.matrixStack(), buffer.getBuffer(RenderLayer.getLines()),
                preview, red, green, 0.25F, 1.0F);
        drawMarker(context, buffer, camera, origin, 0.16, 1.0F, 0.35F, 0.1F);
        drawMarker(context, buffer, camera, center, 0.23, 0.2F, 0.8F, 1.0F);
        drawOffsetGuide(context, buffer, camera, origin, center);

        if (shape == com.zrollus.bd.block.custom.CollectionRegion.Shape.SPHERE) {
            Vec3d half = new Vec3d(preview.getLengthX() * 0.25,
                    preview.getLengthY() * 0.25, preview.getLengthZ() * 0.25);
            Box inner = new Box(center.subtract(half), center.add(half))
                    .offset(-camera.x, -camera.y, -camera.z);
            WorldRenderer.drawBox(context.matrixStack(), buffer.getBuffer(RenderLayer.getLines()),
                    inner, red, green, 0.8F, 0.75F);
        } else if (shape == com.zrollus.bd.block.custom.CollectionRegion.Shape.CYLINDER) {
            Box axis = new Box(center.x - 0.08, worldBox.minY, center.z - 0.08,
                    center.x + 0.08, worldBox.maxY, center.z + 0.08)
                    .offset(-camera.x, -camera.y, -camera.z);
            WorldRenderer.drawBox(context.matrixStack(), buffer.getBuffer(RenderLayer.getLines()),
                    axis, 0.25F, 0.7F, 1.0F, 0.8F);
        }
    }

    private static boolean isCollectorPreviewLoaded(MinecraftClient client, Box box) {
        if (client.world == null || box.minY < client.world.getBottomY() || box.maxY > client.world.getTopY()) {
            return false;
        }
        int minX = MathHelper.floor(box.minX) >> 4;
        int maxX = MathHelper.floor(box.maxX) >> 4;
        int minZ = MathHelper.floor(box.minZ) >> 4;
        int maxZ = MathHelper.floor(box.maxZ) >> 4;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!client.world.isChunkLoaded(x, z)) return false;
            }
        }
        return true;
    }

    private static void drawMarker(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext context,
                                   VertexConsumerProvider.Immediate buffer, Vec3d camera, Vec3d point,
                                   double radius, float red, float green, float blue) {
        Box marker = new Box(point.x - radius, point.y - radius, point.z - radius,
                point.x + radius, point.y + radius, point.z + radius)
                .offset(-camera.x, -camera.y, -camera.z);
        WorldRenderer.drawBox(context.matrixStack(), buffer.getBuffer(RenderLayer.getLines()),
                marker, red, green, blue, 1.0F);
    }

    private static void drawOffsetGuide(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext context,
                                        VertexConsumerProvider.Immediate buffer, Vec3d camera,
                                        Vec3d start, Vec3d end) {
        double distance = start.distanceTo(end);
        if (distance < 0.1) return;
        int steps = Math.max(2, (int) Math.ceil(distance * 2.0));
        for (int i = 1; i < steps; i++) {
            double progress = i / (double) steps;
            Vec3d point = start.lerp(end, progress);
            drawMarker(context, buffer, camera, point, 0.035, 1.0F, 0.8F, 0.1F);
        }
    }

    private static Vec3i getRelativeUpDown(PlayerEntity player, boolean isUpKey) {
        float pitch = player.getPitch();

        if (pitch > 45 || pitch < -45) {
            // Looking up/down — move forward/backward on XZ plane
            Direction dir = player.getHorizontalFacing();
            if (!isUpKey) dir = dir.getOpposite();
            Vec3i vec = dir.getVector();
            return new Vec3i(vec.getX(), 0, vec.getZ());
        } else {
            // Looking horizontally — move Y
            return new Vec3i(0, isUpKey ? 1 : -1, 0);
        }
    }

    private static void sendNudgeToServer(Vec3i offset) {
        if (MinecraftClient.getInstance().player != null) {
            Direction face = getPlayerFacingDirection(MinecraftClient.getInstance().player);
            if (face.getAxis() == Direction.Axis.Y) {
                // Ignore Y-nudge when looking vertically
                offset = new Vec3i(offset.getX(), 0, offset.getZ());
            }
        }

        ClientPlayNetworking.send(new ModNetworking.UpdateNudgePayload(offset.getX(), offset.getY(), offset.getZ()));
    }

    private static Direction getPlayerFacingDirection(PlayerEntity player) {
        float pitch = player.getPitch();
        if (pitch < -45F) return Direction.UP;
        if (pitch > 45F) return Direction.DOWN;
        return player.getHorizontalFacing();
    }

    private static Vec3i clampNudge(Vec3i offset) {
        int x = MathHelper.clamp(offset.getX(), -3, 3);
        int y = MathHelper.clamp(offset.getY(), -3, 3);
        int z = MathHelper.clamp(offset.getZ(), -3, 3);
        return new Vec3i(x, y, z);
    }

    private Vec3i getRelativeLeftRight(PlayerEntity player, boolean isLeft) {
        float yaw = player.getYaw();
        double radians = Math.toRadians(yaw);

        // Rotate 90° for left, -90° for right
        double angle = radians + (isLeft ? Math.PI / 2 : -Math.PI / 2);

        int dx = (int) Math.round(Math.sin(angle));
        int dz = (int) Math.round(Math.cos(angle));

        return new Vec3i(dx, 0, dz);
    }
}
