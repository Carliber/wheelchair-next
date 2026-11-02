package wheelchair.compat.mixins.witchinggadgets;

import witchinggadgets.common.blocks.tiles.TileEntityCuttingTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityCuttingTable.class)
@Pseudo
public abstract class MixinTileEntityCuttingTable {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
