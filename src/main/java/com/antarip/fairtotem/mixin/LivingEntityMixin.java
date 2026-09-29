package com.antarip.fairtotem.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
    private void fairtotem$checkInventoryForTotem(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;

        // Only apply to players — vanilla mobs don't have inventories
        if (!(self instanceof Player player)) {
            return;
        }

        // First check hands (vanilla priority) — if they already have one,
        // let vanilla handle it naturally
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);

        if (mainHand.is(Items.TOTEM_OF_UNDYING) || offHand.is(Items.TOTEM_OF_UNDYING)) {
            return; // Let vanilla handle — totem is in hand
        }

        // Totem is NOT in either hand — search inventory
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(Items.TOTEM_OF_UNDYING)) {
                // Found one! Copy it (for the effect), then consume
                ItemStack totemCopy = stack.copy();
                stack.shrink(1);

                // Trigger the vanilla totem-use logic: set health to 1,
                // clear effects, apply regeneration/absorption/fire resistance
                player.setHealth(1.0F);
                player.removeAllEffects();
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.REGENERATION, 900, 1));
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.ABSORPTION, 100, 1));
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 800, 0));

                // Play the totem animation & sound
                player.level().broadcastEntityEvent(player, (byte) 35);

                cir.setReturnValue(true);
                return;
            }
        }

        // No totem found anywhere — let vanilla return false
    }
}
