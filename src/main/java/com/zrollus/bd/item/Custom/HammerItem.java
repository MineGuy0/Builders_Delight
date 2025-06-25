    package com.zrollus.bd.item.Custom;

    import com.zrollus.bd.ModEnchantments;
    import com.zrollus.bd.utils.Nudge;
    import net.minecraft.block.BlockState;
    import net.minecraft.block.FluidBlock;
    import net.minecraft.enchantment.EnchantmentHelper;
    import net.minecraft.entity.LivingEntity;
    import net.minecraft.entity.player.PlayerEntity;
    import net.minecraft.item.ItemStack;
    import net.minecraft.item.Items;
    import net.minecraft.item.MiningToolItem;
    import net.minecraft.item.ToolMaterial;
    import net.minecraft.registry.tag.BlockTags;
    import net.minecraft.sound.SoundCategory;
    import net.minecraft.util.math.*;
    import net.minecraft.world.World;

    import java.util.ArrayList;
    import java.util.List;

    public class HammerItem extends MiningToolItem {
        public HammerItem(ToolMaterial material, float attackDamage, float attackSpeed, Settings settings) {
            super(attackDamage, attackSpeed, material, BlockTags.PICKAXE_MINEABLE, settings);
        }

        @Override
        public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
            if (world.isClient || !(miner instanceof PlayerEntity player))
                return super.postMine(stack, world, state, pos, miner);

            // 1) grid radius from Hammering I–III
            int level  = EnchantmentHelper.getLevel(ModEnchantments.HAMMERING, stack);
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

        private Direction getHitFaceFromLook(PlayerEntity player) {
            float pitch = player.getPitch();
            if      (pitch < -45F) return Direction.UP;
            else if (pitch >  45F) return Direction.DOWN;
            else                   return player.getHorizontalFacing();
        }

        private void breakBlocks(World world, List<BlockPos> positions,
                                 PlayerEntity player, ItemStack tool, BlockPos center) {
            boolean damaged = false;
            for (BlockPos p : positions) {
                BlockState s = world.getBlockState(p);
                ItemStack hammerItemStack = player.getMainHandStack();
                if (tool.isSuitableFor(s) && !s.isAir()) {
                    if (p.equals(center)) {
                        world.playSound(null, p, s.getSoundGroup().getBreakSound(),
                                SoundCategory.BLOCKS, 1f, 1f);
                    }
                    if (!world.isClient) {
                        world.breakBlock(p, true, player);
                    }
                    damaged = true;
                }
            }
            if (damaged) {
                tool.damage(1, player,
                        pl -> pl.sendToolBreakStatus(pl.getActiveHand()));
            }
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

        @Override
        public float getMiningSpeedMultiplier(ItemStack stack, BlockState state) {
            if (isSuitableFor(stack, state)) {
                return this.miningSpeed; // or optionally boost per-enchant type
            }
            return 1.0F;
        }
        @Override
        public boolean isEnchantable(ItemStack stack) {
            return true; // allows enchantment via table
        }

        public boolean isSuitableFor(ItemStack stack, BlockState state) {
            if (Items.DIAMOND_PICKAXE.isSuitableFor(state)) return true;

            if (EnchantmentHelper.getLevel(ModEnchantments.AXING, stack) > 0 &&
                    state.isIn(BlockTags.AXE_MINEABLE)) return true;

            if (EnchantmentHelper.getLevel(ModEnchantments.SHOVELING, stack) > 0 &&
                    state.isIn(BlockTags.SHOVEL_MINEABLE)) return true;

            return state.getBlock() instanceof FluidBlock;
        }
    }
