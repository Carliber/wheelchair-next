package wheelchair.compat.mixins.storagedrawers;

import com.jaquadro.minecraft.storagedrawers.block.tile.TileEntityFramingTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityFramingTable.class)
@Pseudo
public abstract class MixinTileEntityFramingTable {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
