package wheelchair.compat.mixins.gtnhcoremod;

import com.dreammaster.modbabychest.TileEntityBabyChest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityBabyChest.class)
@Pseudo
public abstract class MixinTileEntityBabyChest {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
