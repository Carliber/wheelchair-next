package wheelchair.compat.mixins.bibliocraft;

import jds.bibliocraft.tileentities.TileEntityFurniturePaneler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityFurniturePaneler.class)
@Pseudo
public abstract class MixinTileEntityFurniturePaneler {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
