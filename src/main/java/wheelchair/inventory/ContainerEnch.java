package wheelchair.inventory;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * Container for the enchant page (guiId 4, prototype v26 layout): ONE target
 * slot at panel x8/y17 (window 43/20) + the player inventory block at the exact
 * 3-row storage-page coordinates (rows 170/188/206, hotbar 230). The list is
 * not slot-based — GuiEnchPage draws it and PacketEnchBatch applies it.
 */
public class ContainerEnch extends Container {
    public final IInventory target = new InventoryBasic("wheelchair.enchtarget", false, 1);
    /** inventory block origin (rows-derived, same law as the storage page) */
    public final int invY, hotY;

    public ContainerEnch(InventoryPlayer playerInv, int rows) {
        if (rows < 3) rows = 3;
        if (rows > 10) rows = 10;
        this.invY = 170 + (rows - 3) * 18;
        this.hotY = 228 + (rows - 3) * 18;
        // slot 0: the item to enchant (plain enchantables AND already-enchanted
        // items — upgrading is the point of the page; books included)
        addSlotToContainer(new Slot(target, 0, 43, 20) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                if (stack == null) return false;
                if (stack.getItem() == Items.enchanted_book) return true;
                // NOTE: vanilla isItemEnchantable() needs getItemStackLimit()==1,
                // which our big-stacking patch breaks for EVERY item (intmax) —
                // accept anything damageable (the real "enchantable" class) or enchanted
                return stack.isItemEnchanted() || stack.getItem().isDamageable();
            }
            @Override
            public int getSlotStackLimit() { return 1; }
        });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlotToContainer(new Slot(playerInv, col + row * 9 + 9, 43 + col * 18, invY + row * 18));
        for (int col = 0; col < 9; col++)
            addSlotToContainer(new Slot(playerInv, col, 43 + col * 18, hotY));
    }

    /** current level of `ench` on the target item (0 = absent) */
    public int currentLevel(Enchantment ench) {
        ItemStack s = target.getStackInSlot(0);
        if (s == null || ench == null) return 0;
        java.util.Map map = EnchantmentHelper.getEnchantments(s);
        Object v = map.get(Integer.valueOf(ench.effectId));
        return v instanceof Integer ? ((Integer) v).intValue() : 0;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotId) {
        Slot slot = (Slot) inventorySlots.get(slotId);
        if (slot == null || !slot.getHasStack()) return null;
        ItemStack stack = slot.getStack();
        if (slotId != 0) {
            // player inventory -> target slot (if enchantable)
            Slot t = (Slot) inventorySlots.get(0);
            if (!t.getHasStack() && t.isItemValid(stack)) {
                ItemStack one = stack.copy();
                one.stackSize = 1;
                t.putStack(one);
                slot.decrStackSize(1);
            }
            return null;
        }
        // target -> player inventory
        ItemStack copy = stack.copy();
        slot.putStack(null);
        if (!mergeItemStack(copy, 1, 37, true)) {
            player.dropPlayerItemWithRandomChoice(copy, false);
        }
        return null;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        // never eat the item: hand it back on close
        ItemStack s = target.getStackInSlot(0);
        if (s != null) {
            target.setInventorySlotContents(0, null);
            if (!player.inventory.addItemStackToInventory(s)) {
                player.dropPlayerItemWithRandomChoice(s, false);
            }
        }
    }
}
