package com.notnite.gloppers

import net.minecraft.block.entity.HopperBlockEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack

object Gloppers {
    private fun matchesGlob(glob: String, str: String): Boolean {
        val regex = glob
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".")
        return str.matches(regex.toRegex())
    }

    fun canTransfer(to: Inventory, stack: ItemStack): Boolean {
        if (to is HopperBlockEntity) {
            val hopperName = to.name.copyContentOnly().string
            val itemName = stack.registryEntry.key.get().value.path

            if (hopperName.startsWith("!")) {
                val globs = hopperName.substring(1).split(",")
                for (glob in globs) {
                    if (matchesGlob(glob, itemName)) {
                        // Glob matched, transfer
                        return true
                    }
                }

                // No globs matched, so don't transfer
                return false
            }
        }

        // Doesn't have a glob, so transfer
        return true
    }
}
