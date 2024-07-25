package com.notnite.gloppers.mixin;

import net.minecraft.block.entity.Hopper;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {
    @Unique
    private static int dirtySlotState = 0;

    @Shadow
    private DefaultedList<ItemStack> inventory;

    @Unique
    private static boolean canTransfer(Inventory to, ItemStack stack) {
        try {
            if (to instanceof HopperBlockEntity) {
                var hopperName = ((HopperBlockEntity) to).getName().copyContentOnly().getString();
                var itemRegistryEntry = stack.getRegistryEntry().getKey();
                if (itemRegistryEntry.isEmpty()) return false;
                var itemName = itemRegistryEntry.get().getValue().getPath();

                if (hopperName.startsWith("!")) {
                    var globs = hopperName.substring(1).split(",");
                    for (var glob : globs) {
                        var strippedGlob = glob.replaceAll("[^a-zA-Z0-9_*?]", "");
                        var regex = strippedGlob.replace(".", "\\.").replace("*", ".*").replace("?", ".");
                        if (itemName.matches(regex)) return true;
                    }

                    // No globs matched, so don't transfer
                    return false;
                }
            }
        } catch (Exception e) {
            // ignored
        }
        
        // Doesn't have a glob (or exception), so transfer
        return true;
    }

    // We can't decrement the item stack before it gets passed to transfer, because we may not be allowed to transfer,
    // eating an item. We know removeStack will get called immediately before transfer, so while it's messy, we can use
    // a static variable to keep track of what slot we're removing from.
    @Redirect(
        method = "insert",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/HopperBlockEntity;removeStack(II)Lnet/minecraft/item/ItemStack;")
    )
    private static ItemStack insert$gloppersRemoveStack(HopperBlockEntity instance, int slot, int amount) {
        dirtySlotState = slot;
        return instance.getStack(slot);
    }

    // This works by mixing into the call of HopperBlockEntity::transfer and returning a stubbed item stack if it's not
    // allowed to transfer. I wanted to instead insert a continue statement into the for loop, but I couldn't figure out
    // a good way to do that.
    @Redirect(
        method = "insert",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/HopperBlockEntity;transfer(Lnet/minecraft/inventory/Inventory;Lnet/minecraft/inventory/Inventory;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/math/Direction;)Lnet/minecraft/item/ItemStack;")
    )
    private static ItemStack insert$gloppersTransfer(
        Inventory from, Inventory to, ItemStack stack, Direction side
    ) {
        if (!canTransfer(to, stack)) {
            // The return value of this is only used to check if it's empty, and if so, returns that it succeeded.
            // We can just return the item stack we were given, as we didn't remove from it.
            return stack;
        }

        // Make sure to remove the item here, because we didn't in the actual call.
        return HopperBlockEntity.transfer(from, to, from.removeStack(dirtySlotState, 1), side);
    }

    // This handles the case where a hopper extracts from a hopper above it (such as two anvils facing forward, stacked
    // on top of one another).
    @Inject(
        method = "extract(Lnet/minecraft/block/entity/Hopper;Lnet/minecraft/inventory/Inventory;ILnet/minecraft/util/math/Direction;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void extract(Hopper hopper, Inventory inventory, int slot, Direction
        side, CallbackInfoReturnable<Boolean> cir) {
        var item = inventory.getStack(slot);
        if (!canTransfer(hopper, item)) cir.setReturnValue(false);
    }

    // This handles the case where an item entity is dropped onto the hopper from above.
    @Inject(
        method = "extract(Lnet/minecraft/inventory/Inventory;Lnet/minecraft/entity/ItemEntity;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void extract(Inventory inventory, ItemEntity itemEntity, CallbackInfoReturnable<Boolean> cir) {
        if (!canTransfer(inventory, itemEntity.getStack())) cir.setReturnValue(false);
    }
}
