package wheelchair.mixin;

import net.minecraft.inventory.InventoryCrafting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * These inventories implement IInventory DIRECTLY (not via InventoryBasic),
 * so MixinInventoryBasic cannot reach them — vanilla 64 limit stays and big
 * stacks break in them. Same overwrite law as InfiniStack (which covers every
 * one of these).
 */
@Mixin(InventoryCrafting.class)
public abstract class MixinInventoryCrafting {

    @Overwrite
    public int getInventoryStackLimit() {
        return Integer.MAX_VALUE;
    }
}
