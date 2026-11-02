package wheelchair.mixin;

import net.minecraft.tileentity.TileEntityChest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * These inventories implement IInventory DIRECTLY (not via InventoryBasic),
 * so MixinInventoryBasic cannot reach them — vanilla 64 limit stays and big
 * stacks break in them. Same overwrite law as InfiniStack (which covers every
 * one of these).
 */
@Mixin(TileEntityChest.class)
public abstract class MixinTileEntityChest {

    @Overwrite
    public int getInventoryStackLimit() {
        return Integer.MAX_VALUE;
    }
}
