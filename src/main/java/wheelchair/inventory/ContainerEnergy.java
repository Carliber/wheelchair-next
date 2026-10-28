package wheelchair.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * Energy page container (guiId 6): read-only mark list + [X], plus the
 * standard player inventory band (so items stay movable while the page is
 * open — same law as the storage page). Geometry mirrors ContainerStoragePage:
 * invY/hotY from the fixed list layout in GuiEnergyPage.
 */
public class ContainerEnergy extends Container {

    public static final int INV_X = 43;
    public static final int INV_LINE_Y = 164, INV_Y = 169, HOT_Y = 227;

    public ContainerEnergy(InventoryPlayer playerInv) {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                addSlotToContainer(new wheelchair.inventory.ContainerStoragePage.SlotBig(playerInv, 9 + c + r * 9, INV_X + c * 18, INV_Y + r * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            addSlotToContainer(new wheelchair.inventory.ContainerStoragePage.SlotBig(playerInv, c, INV_X + c * 18, HOT_Y));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer p, int idx) {
        // vanilla-style: hotbar <-> main inventory swap; no special targets here
        ItemStack merge = null;
        Slot s = (Slot) this.inventorySlots.get(idx);
        if (s != null && s.getHasStack()) {
            ItemStack st = s.getStack();
            merge = st.copy();
            if (idx >= 27) { // hotbar -> main
                if (!this.mergeItemStack(st, 0, 27, false)) return null;
            } else {        // main -> hotbar
                if (!this.mergeItemStack(st, 27, 36, false)) return null;
            }
            if (st.stackSize == 0) s.putStack(null);
            else s.onSlotChanged();
        }
        return merge;
    }
}
