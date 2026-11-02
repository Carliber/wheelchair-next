package wheelchair.compat.mixins.hbm;

import com.hbm.entity.cart.EntityMinecartContainerBase; // hbm\entity\cart\EntityMinecartContainerBase.java
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Overwrite;
import wheelchair.compat.helper.Configurables;

@Mixin(EntityMinecartContainerBase.class)
@Pseudo
public abstract class MixinEntityMinecartContainerBase {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}