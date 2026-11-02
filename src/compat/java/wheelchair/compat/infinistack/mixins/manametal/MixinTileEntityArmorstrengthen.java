package wheelchair.compat.mixins.manametal;

import project.studio.manametalmod.items.armor.TileEntityArmorstrengthen; // items\armor\TileEntityArmorstrengthen.java
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

import org.spongepowered.asm.mixin.Overwrite;
import wheelchair.compat.helper.Configurables;


@Mixin(TileEntityArmorstrengthen.class)
@Pseudo
public abstract class MixinTileEntityArmorstrengthen {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}