package wheelchair.compat.mixins.mapletree;

import ecru.MapleTree.entity.ecru_EntityMomijiInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(ecru_EntityMomijiInventory.class)
@Pseudo
public abstract class MixinEntityMomijiInventory {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
