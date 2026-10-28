package wheelchair.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;

/** empty container for the staff page (guiId 5): pure control page, no slots */
public class ContainerStaff extends Container {
    public ContainerStaff(InventoryPlayer playerInv) {
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }
}
