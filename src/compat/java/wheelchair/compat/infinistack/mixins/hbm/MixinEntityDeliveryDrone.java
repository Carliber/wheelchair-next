package wheelchair.compat.mixins.hbm;

import com.hbm.entity.item.EntityDeliveryDrone; // hbm\entity\item\EntityDeliveryDrone.java
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Overwrite;
import wheelchair.compat.helper.Configurables;

@Mixin(EntityDeliveryDrone.class)
@Pseudo
public abstract class MixinEntityDeliveryDrone {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}