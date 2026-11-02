package wheelchair.compat.mixins.enderio;

import crazypants.enderio.machine.vacuum.TileVacuumChest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileVacuumChest.class)
@Pseudo
public abstract class MixinTileVacuumChest {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
