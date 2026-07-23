    package com.zrollus.bd.item.Custom;

    import com.zrollus.bd.ModEnchantments;
    import com.zrollus.bd.utils.Nudge;
    import net.minecraft.block.Block;
    import net.minecraft.block.BlockState;
    import net.minecraft.block.Blocks;
    import net.minecraft.block.FluidBlock;
    import net.minecraft.enchantment.EnchantmentHelper;
    import net.minecraft.entity.LivingEntity;
    import net.minecraft.entity.player.PlayerEntity;
    import net.minecraft.item.*;
    import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
    import net.minecraft.particle.ParticleTypes;
    import net.minecraft.registry.RegistryKeys;
    import net.minecraft.registry.tag.BlockTags;
    import net.minecraft.server.world.ServerWorld;
    import net.minecraft.sound.SoundCategory;
    import net.minecraft.sound.SoundEvents;
    import net.minecraft.util.hit.BlockHitResult;
    import net.minecraft.util.math.*;
    import net.minecraft.world.World;

    import java.util.ArrayList;
    import java.util.List;

    public class HammerItem extends MiningToolItem {
        public HammerItem(ToolMaterial material, float attackDamage, float attackSpeed, Settings settings) {
            super(material, BlockTags.PICKAXE_MINEABLE,
                    settings.attributeModifiers(MiningToolItem.createAttributeModifiers(material, attackDamage, attackSpeed)));
        }

        @Override
        public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
            if (world.isClient || !(miner instanceof PlayerEntity player))
                return super.postMine(stack, world, state, pos, miner);

            // 1) grid radius from Hammering I–III
            int level = ModEnchantments.getLevel(
                    stack,
                    miner.getWorld().getRegistryManager(),
                    ModEnchantments.HAMMERING
            );
            int radius = 1 + level;  // 1→3×3, 2→5×5, 3→7×7, 4→9×9

            Vec3i rawOffset = Nudge.getFor(player.getUuid());
            Direction face = getHitFaceFromLook(player);
            BlockPos center;

            if (face.getAxis() == Direction.Axis.Y) {
                // Strip Y when mining up/down
                center = pos.add(rawOffset.getX(), 0, rawOffset.getZ());
            } else {
                center = pos.add(rawOffset);
            }

            // 4) calculate grid
            List<BlockPos> area = calculateDynamicGrid(center, face, player, stack, radius);
            System.out.println("[Hammer] Grid size: " + area.size() + " around " + center);

            // 5) break them all
            breakBlocks(world, area, player, stack, center);

            // 6) now let vanilla break the center block, drop loot, damage tool, etc.
            return super.postMine(stack, world, state, pos, miner);
        }


        public static List<BlockPos> calculateDynamicGrid(BlockPos center,
                                                          Direction face, PlayerEntity player, ItemStack stack, int radius) {
            Vec3i[] plane = getPlaneVectors(face, player);
            Vec3i u = plane[0], v = plane[1];
            List<BlockPos> out = new ArrayList<>();
            for (int i = -radius; i <= radius; i++) {
                for (int j = -radius; j <= radius; j++) {
                    out.add(center.add(u.getX()*i+v.getX()*j,
                            u.getY()*i+v.getY()*j,
                            u.getZ()*i+v.getZ()*j));
                }
            }
            return out;
        }

        public Direction getHitFaceFromLook(PlayerEntity player) {
            float pitch = player.getPitch();
            if      (pitch < -45F) return Direction.UP;
            else if (pitch >  45F) return Direction.DOWN;
            else                   return player.getHorizontalFacing();
        }

        private static void breakBlocks(World world, List<BlockPos> positions,
                                        PlayerEntity player, ItemStack tool, BlockPos center) {
            boolean damaged = false;
            for (BlockPos p : positions) {
                // Skip the center block! Vanilla handles the center block automatically
                // after postMine returns. Breaking it here manually causes double-drops/bugs.
                if (p.equals(center)) continue;

                BlockState s = world.getBlockState(p);

                if (isSuitableFor(tool, s) && !s.isAir()) {
                    if (!world.isClient) {
                        if (!s.getFluidState().isEmpty() && ModEnchantments.getLevel(
                                tool,
                                world.getRegistryManager(),
                                ModEnchantments.FLUIDBREAKER
                        ) > 0) {
                            playFluidBreakEffects((ServerWorld) world, p, s);
                            world.setBlockState(p, Blocks.AIR.getDefaultState(), 3);
                            world.syncWorldEvent(2001, p, Block.getRawIdFromState(s));
                        }
                        else {
                            // 1. Get the drops based on the tool (This handles Silk Touch/Fortune)
                            Block.getDroppedStacks(s, (net.minecraft.server.world.ServerWorld) world, p, null, player, tool)
                                    .forEach(stack -> Block.dropStack(world, p, stack));

                            // 2. Remove the block and trigger game events (like vibration/sculk)
                            world.breakBlock(p, false, player);
                        }
                        int fakeId = p.hashCode() + player.getId();
                        ((ServerWorld)world).getPlayers().forEach(nearbyPlayer -> {
                            nearbyPlayer.networkHandler.sendPacket(new BlockBreakingProgressS2CPacket(fakeId, p, -1));
                        });

                    }
                    damaged = true;
                    world.playSound(null, center, SoundEvents.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.PLAYERS, 0.8f, 0.5f);
                }
            }

            if (damaged) {
                tool.damage(1, player, net.minecraft.entity.EquipmentSlot.MAINHAND);
            }
        }

        public static void playFluidBreakEffects(ServerWorld world, BlockPos pos, BlockState state) {
            boolean lava = state.getFluidState().isIn(net.minecraft.registry.tag.FluidTags.LAVA);
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5;

            world.spawnParticles(lava ? ParticleTypes.FLAME : ParticleTypes.SPLASH,
                    x, y, z, 18, 0.35, 0.35, 0.35, 0.08);
            world.spawnParticles(lava ? ParticleTypes.LARGE_SMOKE : ParticleTypes.BUBBLE,
                    x, y, z, 10, 0.3, 0.3, 0.3, 0.04);
            world.playSound(null, pos,
                    lava ? SoundEvents.ITEM_BUCKET_EMPTY_LAVA : SoundEvents.ITEM_BUCKET_EMPTY,
                    SoundCategory.BLOCKS, 0.8f, 1.15f);
        }

        public static Vec3i[] getPlaneVectors(Direction face, PlayerEntity player) {
            Direction.Axis axis = face.getAxis();
            Vec3i u, v;

            if (axis == Direction.Axis.Y) {
                u = player.getHorizontalFacing().rotateYClockwise().getVector();
                v = player.getHorizontalFacing().getVector();
            } else if (axis == Direction.Axis.X) {
                u = Direction.UP.getVector();
                v = Direction.NORTH.getVector();
            } else {
                u = Direction.UP.getVector();
                v = Direction.EAST.getVector();
            }

            return new Vec3i[]{u, v};
        }

        // Change breakBlocks to be public static so we can call it from our Event handler
        public static void executeHammerGrid(World world, BlockPos pos, PlayerEntity player, ItemStack stack) {
            if (world.isClient) return;

            int level = ModEnchantments.getLevel(stack, world.getRegistryManager(), ModEnchantments.HAMMERING);
            int radius = 1 + level;

            // Use your existing logic to get the face and grid
            // Note: Since we don't have a 'Miner' direction in the Attack Event easily,
            // we use a raycast to find the face.
            BlockHitResult hit = (BlockHitResult) player.raycast(5.0, 0.0f, false);
            Direction face = hit.getSide();

            List<BlockPos> area = calculateDynamicGrid(pos, face, player, stack, radius);

            // Call your existing breakBlocks logic
            // You'll need to make breakBlocks static too!
            breakBlocks(world, area, player, stack, pos);
        }

        @Override
        public float getMiningSpeed(ItemStack stack, BlockState state) {
            if (isSuitableFor(stack, state)) {
                return super.getMiningSpeed(stack, state);
            }
            return 1.0F;
        }
        @Override
        public boolean isEnchantable(ItemStack stack) {
            return true; // allows enchantment via table
        }

        public static boolean isSuitableFor(ItemStack stack, BlockState state) {
            if (state.isIn(BlockTags.PICKAXE_MINEABLE)) return true;

            if (state.isIn(BlockTags.AXE_MINEABLE)) return true;

            if (state.isIn(BlockTags.SHOVEL_MINEABLE)) return true;

            return state.getBlock() instanceof FluidBlock;
        }
    }
