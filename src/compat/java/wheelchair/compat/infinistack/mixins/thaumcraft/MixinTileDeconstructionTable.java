package wheelchair.compat.mixins.thaumcraft;

import thaumcraft.common.tiles.TileDeconstructionTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileDeconstructionTable.class)
@Pseudo
public abstract class MixinTileDeconstructionTable {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
