package wheelchair.mixin;

import net.minecraft.tileentity.TileEntityDispenser;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * These inventories implement IInventory DIRECTLY (not via InventoryBasic),
 * so MixinInventoryBasic cannot reach them — vanilla 64 limit stays and big
 * stacks break in them. Same overwrite law as InfiniStack (which covers every
 * one of these).
 */
@Mixin(TileEntityDispenser.class)
public abstract class MixinTileEntityDispenser {

    @Overwrite
    public int getInventoryStackLimit() {
        return Integer.MAX_VALUE;
    }
}
