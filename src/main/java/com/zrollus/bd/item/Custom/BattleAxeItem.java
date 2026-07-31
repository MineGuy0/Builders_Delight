package com.zrollus.bd.item.Custom;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/** Heavy netherite-tier battle axe with a short-ranged runic sonic attack. */
public final class BattleAxeItem extends MiningToolItem {
    private static final double SONIC_RANGE = 32.0;
    private static final double SONIC_TARGET_MARGIN = 0.65;
    private static final float SONIC_DAMAGE = 40.0F;
    private static final int SONIC_COOLDOWN_TICKS = 60;
    private static final DustParticleEffect RED_BEAM_CORE =
            new DustParticleEffect(new Vector3f(1.0F, 0.06F, 0.06F), 1.35F);
    private static final DustParticleEffect RED_BEAM_GLOW =
            new DustParticleEffect(new Vector3f(1.0F, 0.35F, 0.13F), 0.8F);

    public BattleAxeItem(ToolMaterial material, float attackDamage, float attackSpeed, Settings settings) {
        super(material, BlockTags.AXE_MINEABLE,
                settings.attributeModifiers(MiningToolItem.createAttributeModifiers(
                        material, attackDamage, attackSpeed)));
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) {
            return TypedActionResult.success(stack, true);
        }

        ServerWorld serverWorld = (ServerWorld) world;
        Vec3d start = user.getEyePos();
        Vec3d direction = user.getRotationVec(1.0F).normalize();

        // A block raycast limits the beam, preventing it from damaging through walls.
        Vec3d unobstructedEnd = user.raycast(SONIC_RANGE, 1.0F, false).getPos();
        double unobstructedDistance = Math.min(SONIC_RANGE, start.distanceTo(unobstructedEnd));
        Vec3d maximumEnd = start.add(direction.multiply(unobstructedDistance));

        LivingEntity target = findFirstTarget(serverWorld, user, start, maximumEnd, direction);
        Vec3d beamEnd = target == null
                ? maximumEnd
                : target.getPos().add(0.0, target.getHeight() * 0.55, 0.0);

        spawnSonicBeam(serverWorld, start, beamEnd);
        serverWorld.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 1.5F, 1.15F);

        if (target != null) {
            target.damage(serverWorld.getDamageSources().sonicBoom(user), SONIC_DAMAGE);
            target.addVelocity(direction.x * 2.25, 0.65, direction.z * 2.25);
            target.velocityModified = true;
        }

        user.getItemCooldownManager().set(this, SONIC_COOLDOWN_TICKS);
        stack.damage(1, user, hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        return TypedActionResult.success(stack, false);
    }

    private static LivingEntity findFirstTarget(ServerWorld world, PlayerEntity user, Vec3d start,
                                                 Vec3d end, Vec3d direction) {
        double beamLength = start.distanceTo(end);
        Box searchArea = user.getBoundingBox()
                .stretch(direction.multiply(beamLength))
                .expand(SONIC_TARGET_MARGIN);

        LivingEntity closest = null;
        double closestDistanceSquared = beamLength * beamLength;
        for (LivingEntity candidate : world.getEntitiesByClass(
                LivingEntity.class,
                searchArea,
                entity -> entity != user && entity.isAlive() && !entity.isSpectator())) {
            Optional<Vec3d> intersection = candidate.getBoundingBox()
                    .expand(SONIC_TARGET_MARGIN)
                    .raycast(start, end);
            if (intersection.isEmpty()) {
                continue;
            }

            double distanceSquared = start.squaredDistanceTo(intersection.get());
            if (distanceSquared < closestDistanceSquared) {
                closestDistanceSquared = distanceSquared;
                closest = candidate;
            }
        }
        return closest;
    }

    private static void spawnSonicBeam(ServerWorld world, Vec3d start, Vec3d end) {
        Vec3d delta = end.subtract(start);
        double length = delta.length();
        if (length < 0.01) {
            return;
        }

        Vec3d stepDirection = delta.normalize();
        for (double distance = 0.5; distance <= length; distance += 0.35) {
            Vec3d point = start.add(stepDirection.multiply(distance));
            world.spawnParticles(RED_BEAM_CORE,
                    point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
            world.spawnParticles(RED_BEAM_GLOW,
                    point.x, point.y, point.z, 1, 0.08, 0.08, 0.08, 0.005);
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("tooltip.bd.battle_axe.sonic").formatted(Formatting.AQUA));
        tooltip.add(Text.translatable("tooltip.bd.battle_axe.sonic_details").formatted(Formatting.DARK_GRAY));
        super.appendTooltip(stack, context, tooltip, type);
    }
}
