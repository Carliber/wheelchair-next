package wheelchair.compat.mixins.enderio;

import crazypants.enderio.machine.enchanter.TileEnchanter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEnchanter.class)
@Pseudo
public abstract class MixinTileEnchanter {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
