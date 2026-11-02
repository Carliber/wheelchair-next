package wheelchair.compat.mixins.avaritia;

import fox.spiteful.avaritia.tile.TileEntityAutoDireCrafting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Overwrite;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityAutoDireCrafting.class)
@Pseudo
public abstract class MixinTileEntityAutoDireCrafting {

    @Overwrite(remap = false)
    public int func_70297_j_(){
        return Configurables.maxStackSize;
    }
}
