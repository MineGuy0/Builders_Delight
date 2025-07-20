package com.zrollus.bd;

import com.zrollus.bd.block.ModBlocks;
import com.zrollus.bd.item.Custom.HammerItem;
import com.zrollus.bd.utils.Nudge;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;

import java.util.ArrayList;
import java.util.List;
public class BuildersDelightClient implements ClientModInitializer {


    public static KeyBinding nudgeUp;
    public static KeyBinding nudgeDown;
    public static KeyBinding nudgeLeft;
    public static KeyBinding nudgeRight;

    public static Vec3i nudgeOffset = new Vec3i(0, 0, 0);
    public static List<BlockPos> renderArea = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.STEEL_GRATE, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.TOGGLE_TORCH, RenderLayer.getCutout());

        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.CONFIRM_NUDGE_PACKET, (client, handler, buf, responseSender) -> {
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
                    int      level       = EnchantmentHelper.getLevel(ModEnchantments.HAMMERING, stack);
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

            buffer.draw();
        });
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

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(offset.getX());
        buf.writeInt(offset.getY());
        buf.writeInt(offset.getZ());
        ClientPlayNetworking.send(ModNetworking.UPDATE_NUDGE_PACKET, buf);
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
