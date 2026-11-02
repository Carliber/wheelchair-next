package wheelchair.compat.mixins.bloodarsenal;

import com.arc.bloodarsenal.common.tileentity.TilePortableAltar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TilePortableAltar.class)
@Pseudo
public abstract class MixinTilePortableAltar {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
