package wheelchair.client;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * Client-only virtual slot (old-tree pattern, AE2 SlotME shape): a real Slot
 * object added to the CLIENT container so vanilla GuiContainer renders items,
 * hover highlight and tooltips for free — same render path as the player
 * inventory, so icon colours always match. Clicks are intercepted by the GUI
 * before vanilla can emit a click packet, and the server container never
 * contains these.
 */
public class SlotVirtual extends Slot {
    private static final IInventory DUMMY = new InventoryBasic("wheelchair.virtual", false, 0);

    /** index within the visible window (0..rows*COLS-1) */
    public final int windowIndex;
    /** the page that owns this slot */
    private final VirtualSlotHost gui;

    public SlotVirtual(VirtualSlotHost gui, int windowIndex, int x, int y) {
        super(DUMMY, 0, x, y);
        this.gui = gui;
        this.windowIndex = windowIndex;
    }

    @Override
    public ItemStack getStack() {
        return gui.getVisibleStack(windowIndex);
    }

    @Override
    public boolean getHasStack() {
        // resource cells carry no ItemStack (the GUI draws its own label),
        // but vanilla only draws the hover highlight and calls renderToolTip
        // for slots whose getHasStack() is true.
        return super.getHasStack() || gui.isResourceCell(windowIndex);
    }

    @Override
    public boolean canTakeStack(EntityPlayer player) {
        return false;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return false;
    }

    @Override
    public int getSlotStackLimit() {
        return 64;
    }
}
