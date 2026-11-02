package wheelchair.mixin;

import net.minecraft.inventory.InventoryLargeChest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * These inventories implement IInventory DIRECTLY (not via InventoryBasic),
 * so MixinInventoryBasic cannot reach them — vanilla 64 limit stays and big
 * stacks break in them. Same overwrite law as InfiniStack (which covers every
 * one of these).
 */
@Mixin(InventoryLargeChest.class)
public abstract class MixinInventoryLargeChest {

    @Overwrite
    public int getInventoryStackLimit() {
        return Integer.MAX_VALUE;
    }
}
