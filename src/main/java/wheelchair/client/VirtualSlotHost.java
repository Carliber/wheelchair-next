package wheelchair.client;

import net.minecraft.item.ItemStack;

/**
 * What a {@link SlotVirtual} needs from whichever GUI owns it (old-tree
 * pattern, AE2 SlotME/InternalSlotME shape).
 */
public interface VirtualSlotHost {
    /** stack shown in visible window cell i (null for resource cells, which draw themselves) */
    ItemStack getVisibleStack(int i);

    /** true when visible window cell i holds a resource (drawn without an ItemStack) */
    boolean isResourceCell(int i);
}
