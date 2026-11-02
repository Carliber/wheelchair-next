package wheelchair.compat.mixins.bibliocraft;

import jds.bibliocraft.tileentities.TileEntityTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityTable.class)
@Pseudo
public abstract class MixinTileEntityTable {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
