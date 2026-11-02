package wheelchair.compat.mixins.botania;

import vazkii.botania.common.block.tile.TileSimpleInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileSimpleInventory.class)
@Pseudo
public abstract class MixinTileSimpleInventory {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
