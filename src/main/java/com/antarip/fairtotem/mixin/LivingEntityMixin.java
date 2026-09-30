package com.antarip.fairtotem.mixin;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Unique
    private static final ThreadLocal<ItemStack> fairtotem$restoredOffhand = new ThreadLocal<>();

    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"))
    private void fairtotem$prepareInventoryTotem(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        // Void damage and /kill bypass totem protection
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }

        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player)) {
            return;
        }

        // If player is already holding a totem in either hand, vanilla handles it
        if (player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.TOTEM_OF_UNDYING) ||
            player.getItemInHand(InteractionHand.OFF_HAND).is(Items.TOTEM_OF_UNDYING)) {
            return;
        }

        // Search player inventory for a Totem of Undying
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(Items.TOTEM_OF_UNDYING)) {
                // Consume 1 totem from inventory
                stack.shrink(1);

                // Save current offhand item and temporarily place a totem in offhand
                fairtotem$restoredOffhand.set(player.getItemInHand(InteractionHand.OFF_HAND));
                player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING, 1));
                return;
            }
        }
    }

    @Inject(method = "checkTotemDeathProtection", at = @At("RETURN"))
    private void fairtotem$cleanupInventoryTotem(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        ItemStack oldOffhand = fairtotem$restoredOffhand.get();
        if (oldOffhand != null) {
            fairtotem$restoredOffhand.remove();
            LivingEntity self = (LivingEntity) (Object) this;
            if (self instanceof Player player) {
                // Restore player's original offhand item
                player.setItemInHand(InteractionHand.OFF_HAND, oldOffhand);
            }
        }
    }
}
