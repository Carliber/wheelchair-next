package wheelchair.compat.mixins.manametal;

import project.studio.manametalmod.tileentity.TileEntityManaFurnace; // tileentity\TileEntityManaFurnace.java
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

import org.spongepowered.asm.mixin.Overwrite;
import wheelchair.compat.helper.Configurables;


@Mixin(TileEntityManaFurnace.class)
@Pseudo
public abstract class MixinTileEntityManaFurnace {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}