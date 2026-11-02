package wheelchair.compat.mixins.muya;

import wheelchair.compat.helper.Configurables;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Overwrite;
import tw.pearki.mcmod.muya.tileentity.machine.TileEntityRedstoneChuckLoader;

@Mixin(TileEntityRedstoneChuckLoader.class)
@Pseudo
public abstract class MixinTileEntityRedstoneChunkLoader {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
