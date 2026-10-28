package wheelchair.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import wheelchair.recipe.RecipeIndex;
import wheelchair.recipe.RecipeIndex.DeconRecipe;

/**
 * Storage page container: vanilla-semantics 3x3 crafting band + a DECONSTRUCTION
 * band (old-tree ContainerDecon semantics, ported verbatim) + player inventory.
 * The storage grid is NOT slots — clicks become PacketStorageAction.
 *
 * Adaptive rows (AE2-style): everything below the grid shifts with the row
 * count. bandTop = 54 + rows*18 (window y); matrix = bandTop+3, result row =
 * bandTop+21, inventory rule = bandTop+59, inv = +5, hotbar = inv+60.
 *
 * Slot order: 0 craft result, 1..9 craft matrix, 10..45 player inventory,
 * 46 decon input, 47..55 decon preview 3x3. The client appends SlotVirtual
 * grid slots AFTER these (never referenced here).
 *
 * Decon (old-tree hard guarantees, kept):
 *  - preview slots isItemValid=false: every vanilla PLACE path is blocked
 *  - takeIngredient aggregates from the RECIPE, never from the preview stacks
 *  - batch clamped to MAX_BATCH (4096)
 */
public class ContainerStoragePage extends Container {
    /** InfiniStack protection: max products deconstructed per shift-click */
    public static final int MAX_BATCH = Integer.MAX_VALUE;

    public static final int SLOT_CRAFT_RESULT = 0;
    public static final int SLOT_CRAFT_FIRST = 1;               // 1..9
    public static final int SLOT_INV_FIRST = 10;                // 10..45
    public static final int SLOT_DECON_IN = 46;
    public static final int SLOT_DECON_OUT_FIRST = 47;          // 47..55

    public final InventoryCrafting craftMatrix = new InventoryCrafting(this, 3, 3);
    /** InventoryCrafting so windowItems sync (putStack) fires onCraftMatrixChanged,
     *  which is what re-lays the decon preview on the client */
    public final InventoryCrafting deconInput = new InventoryCrafting(this, 1, 1);
    public final IInventory deconOut = new InventoryBasic("wheelchair.decon_out", false, 9);
    private final InventoryCraftResult craftResult;
    private final InventoryPlayer playerInv;

    /** recipes for the current decon input (null = none) */
    public List<DeconRecipe> current;
    public int deconIndex;
    /** ALL recipes matching the current craft matrix (conflict cycling) + cursor */
    public final java.util.List<net.minecraft.item.ItemStack> craftMatches = new java.util.ArrayList<net.minecraft.item.ItemStack>();
    public int craftIndex;
    /** per-preview-slot ore-dictionary variant selection (right-click cycles) */
    public final int[] variantSel = new int[9];

    public ContainerStoragePage(InventoryPlayer playerInv, int rows) {
        this.playerInv = playerInv;
        this.craftResult = new InventoryCraftResult();
        if (rows < 3) rows = 3;
        if (rows > 10) rows = 10;
        int bandTop = 54 + (rows - 1) * 18 + 16; // v26: grid footprint (rows-1)*18+16, same law as the GUI
        int matY = bandTop + 3;
        int arrowY = bandTop + 21;
        int invLineY = bandTop + 59;
        int invY = invLineY + 5;
        int hotY = invY + 58;

        addSlotToContainer(new SlotCrafting(playerInv.player, craftMatrix, craftResult, 0, 151, arrowY));
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                addSlotToContainer(new SlotBig(craftMatrix, c + r * 3, 52 + c * 18, matY + r * 18));
            }
        }
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                addSlotToContainer(new SlotBig(playerInv, 9 + c + r * 9, 43 + c * 18, invY + r * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            addSlotToContainer(new SlotBig(playerInv, c, 43 + c * 18, hotY));
        }
        addSlotToContainer(new SlotBig(deconInput, 0, 70, arrowY));
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                final int previewIdx = r * 3 + c;
                addSlotToContainer(new Slot(deconOut, previewIdx, 142 + c * 18, matY + r * 18) {
                    @Override
                    public boolean isItemValid(ItemStack stack) {
                        // preview-only slot: blocking PLACE structurally kills the
                        // inject-then-amplify dupe vector (old-tree hard guarantee)
                        return false;
                    }

                    @Override
                    public void onPickupFromSlot(EntityPlayer player, ItemStack stack) {
                        super.onPickupFromSlot(player, stack);
                        if (!player.worldObj.isRemote && stack != null
                                && current != null && !current.isEmpty()) {
                            takeIngredient(player, previewIdx, false); // normal click = deconstruct ONE
                        }
                    }
                });
            }
        }
    }

    @Override
    public void onCraftMatrixChanged(net.minecraft.inventory.IInventory inv) {
        if (inv == deconInput) {
            relayoutDecon();
            return;
        }
        if (this.craftResult != null && this.craftMatrix != null) {
            // collect EVERY matching recipe (vanilla findMatchingRecipe returns only
            // the first registered — the ‹ › cycle switches between them)
            craftMatches.clear();
            craftIndex = 0;
            java.util.List rl = net.minecraft.item.crafting.CraftingManager
                    .getInstance().getRecipeList();
            for (Object o : rl) {
                net.minecraft.item.crafting.IRecipe r = (net.minecraft.item.crafting.IRecipe) o;
                net.minecraft.item.ItemStack res;
                try {
                    if (!r.matches(this.craftMatrix, this.playerInv.player.worldObj)) continue;
                    res = r.getCraftingResult(this.craftMatrix);
                } catch (Throwable t) {
                    continue; // hostile modded recipes must not break the page
                }
                if (res == null) continue;
                boolean dup = false;
                for (net.minecraft.item.ItemStack m : craftMatches) {
                    if (net.minecraft.item.ItemStack.areItemStacksEqual(m, res)) { dup = true; break; }
                }
                if (!dup) craftMatches.add(res);
            }
            this.craftResult.setInventorySlotContents(0,
                    craftMatches.isEmpty() ? null : craftMatches.get(0));
        }
    }

    /** ‹ › in craft mode: cycle the matched-recipe cursor (server + client mirror) */
    public void cycleCraft(int d) {
        if (craftMatches.isEmpty()) return;
        craftIndex = ((craftIndex + d) % craftMatches.size() + craftMatches.size()) % craftMatches.size();
        this.craftResult.setInventorySlotContents(0, craftMatches.get(craftIndex));
    }

    /** slot whose stack limit is int-max (Item.maxStackSize is patched to
     *  Integer.MAX_VALUE, but Slot.getSlotStackLimit() would still delegate to
     *  the inventory's hard-coded 64 — vanilla placement and mergeItemStack both
     *  take min(slotLimit, itemMax)) */
    public static class SlotBig extends Slot {
        public SlotBig(IInventory inv, int idx, int x, int y) { super(inv, idx, x, y); }

        @Override
        public int getSlotStackLimit() { return Integer.MAX_VALUE; }
    }

    // ---------------- deconstruction band ----------------

    /** re-run the recipe lookup for the current input and lay the preview grid */
    public void relayoutDecon() {
        current = RecipeIndex.getRecipes(deconInput.getStackInSlot(0));
        if (current == null || current.isEmpty()) {
            current = null;
            deconIndex = 0;
            clearDeconPreview();
            return;
        }
        if (deconIndex >= current.size()) deconIndex = 0;
        DeconRecipe r = current.get(deconIndex);
        clearDeconPreview();
        for (int row = 0; row < r.height; row++) {
            for (int col = 0; col < r.width; col++) {
                int slot = row * r.width + col;
                if (slot >= r.ingredients.length) break;
                deconOut.setInventorySlotContents(row * 3 + col,
                        ingredientFor(r, slot, row * 3 + col));
            }
        }
    }

    /** the ingredient shown in preview slot matrixIdx (ore-variant aware copy) */
    private ItemStack ingredientFor(DeconRecipe r, int slotIdx, int matrixIdx) {
        ItemStack ing = r.ingredients[slotIdx];
        if (ing == null) return null;
        List<ItemStack> vs = r.variants != null && slotIdx < r.variants.length ? r.variants[slotIdx] : null;
        if (vs == null || vs.isEmpty()) return ing.copy();
        int sel = Math.floorMod(variantSel[matrixIdx], vs.size());
        return vs.get(sel).copy();
    }

    private void clearDeconPreview() {
        for (int i = 0; i < 9; i++) deconOut.setInventorySlotContents(i, null);
    }

    /** how many ore-dictionary variants preview slot matrixIdx offers (0 = fixed) */
    public int variantCount(int matrixIdx) {
        int slotIdx = ingredientIndexForMatrix(matrixIdx);
        if (slotIdx < 0 || current == null || current.isEmpty()) return 0;
        DeconRecipe r = current.get(deconIndex);
        if (r.variants == null || slotIdx >= r.variants.length) return 0;
        List<ItemStack> vs = r.variants[slotIdx];
        return vs == null ? 0 : vs.size();
    }

    /** 1-based position of the currently selected variant (for "2/6" badges) */
    public int variantPos(int matrixIdx) {
        int n = variantCount(matrixIdx);
        if (n == 0) return 0;
        return Math.floorMod(variantSel[matrixIdx], n) + 1;
    }

    private int ingredientIndexForMatrix(int matrixIdx) {
        if (current == null || current.isEmpty()) return -1;
        DeconRecipe r = current.get(deconIndex);
        for (int row = 0; row < r.height; row++) {
            for (int col = 0; col < r.width; col++) {
                if (row * 3 + col == matrixIdx) return row * r.width + col;
            }
        }
        return -1;
    }

    /** right-click a preview slot: cycle through every registered material variant.
     *  Runs on BOTH sides (vanilla predicts locally, contents re-sync from server) */
    public void cycleVariant(int matrixIdx, int delta) {
        if (variantCount(matrixIdx) == 0) return;
        variantSel[matrixIdx] = Math.floorMod(variantSel[matrixIdx] + delta, variantCount(matrixIdx));
        int slotIdx = ingredientIndexForMatrix(matrixIdx);
        if (slotIdx < 0 || current == null || current.isEmpty()) return;
        DeconRecipe r = current.get(deconIndex);
        if (slotIdx >= r.ingredients.length) return;
        deconOut.setInventorySlotContents(matrixIdx, ingredientFor(r, slotIdx, matrixIdx));
    }

    /** ‹ › recipe cycling (both sides) */
    public void cycle(int delta) {
        if (current == null || current.isEmpty()) return;
        deconIndex = Math.floorMod(deconIndex + delta, current.size());
        relayoutDecon();
    }

    /**
     * server-side: player took ingredients from preview slotIndex.
     * deconstructAll=false: consume 1 product; true: consume the whole input stack.
     * Aggregated from the RECIPE (+variantSel), never from the live preview: the
     * preview is display state, so nothing can amplify it (old-tree defense).
     */
    private void takeIngredient(EntityPlayer player, int slotIndex, boolean deconstructAll) {
        if (current == null || current.isEmpty()) return;
        DeconRecipe r = current.get(deconIndex);
        ItemStack inStack = deconInput.getStackInSlot(0);
        if (inStack == null || r.result.stackSize <= 0 || inStack.stackSize < r.result.stackSize) return;

        int times = 1;
        if (deconstructAll) {
            long t = ((long) inStack.stackSize) / r.result.stackSize;
            if (t > MAX_BATCH) t = MAX_BATCH;
            times = (int) t;
        }

        LinkedHashMap<String, Object[]> totals = new LinkedHashMap<String, Object[]>();
        for (int row = 0; row < r.height; row++) {
            for (int col = 0; col < r.width; col++) {
                int slotIdx = row * r.width + col;
                if (slotIdx >= r.ingredients.length) break;
                int matrixIdx = row * 3 + col;
                ItemStack ing = ingredientFor(r, slotIdx, matrixIdx); // variant-aware copy
                if (ing == null) continue;
                String key = ing.getItem() + "@" + ing.getItemDamage();
                Object[] e = totals.get(key);
                if (e == null) {
                    e = new Object[]{ing, (long) ing.stackSize * times};
                    totals.put(key, e);
                } else {
                    e[1] = (Long) e[1] + (long) ing.stackSize * times;
                }
            }
        }

        for (Object[] e : totals.values()) {
            ItemStack proto = (ItemStack) e[0];
            long count = (Long) e[1];
            if (count <= 0) continue;
            ItemStack give = proto.copy();
            give.stackSize = (int) Math.min((long) Integer.MAX_VALUE, count);
            if (!giveToPlayer(player, give)) {
                player.dropPlayerItemWithRandomChoice(give, false); // inventory full
            }
        }

        long consume = (long) r.result.stackSize * (long) times;
        deconInput.decrStackSize(0, (int) Math.min((long) inStack.stackSize, consume));
        relayoutDecon();
    }

    /** move a stack into the player inv (hotbar last, like vanilla) */
    private boolean giveToPlayer(EntityPlayer player, ItemStack stack) {
        if (stack.stackSize <= 0) return true;
        if (mergeItemStack(stack, SLOT_INV_FIRST, 46, true)) return stack.stackSize <= 0;
        if (mergeItemStack(stack, 46 - 9, 46, false)) return stack.stackSize <= 0;
        return false;
    }

    // ---------------- clicks ----------------

    /**
     * decon preview slots: STRICT whitelist (old-tree).
     * mode 0 + left  = take once (vanilla pickup -> onPickupFromSlot -> takeIngredient)
     * mode 0 + right = cycle the ore-dict variant (runs on both sides)
     * mode 1 (shift) = batch-deconstruct ALL (transferStackInSlot -> takeIngredient all)
     * every other mode is dropped: they bypass onPickupFromSlot and would either
     * free-take the preview stacks or inject items that takeIngredient multiplies.
     */
    @Override
    public ItemStack slotClick(int slotId, int clickedButton, int mode, EntityPlayer p) {
        if (slotId < 0 || slotId >= inventorySlots.size()) return null; // client-only virtual slots
        if (slotId >= SLOT_DECON_OUT_FIRST && slotId <= SLOT_DECON_OUT_FIRST + 8) {
            if (mode == 0 && clickedButton == 0) {
                // one click = one full deconstruction: input -1 product, ALL materials
                // straight to the inventory (nothing on the cursor)
                if (!p.worldObj.isRemote) {
                    takeIngredient(p, slotId - SLOT_DECON_OUT_FIRST, false);
                }
                return null;
            }
            if (mode == 1 && clickedButton == 0) {
                return super.slotClick(slotId, clickedButton, mode, p);
            }
            if (mode == 0 && clickedButton == 1) {
                cycleVariant(slotId - SLOT_DECON_OUT_FIRST, 1);
                return null;
            }
            return null;
        }
        return super.slotClick(slotId, clickedButton, mode, p);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        for (int i = 0; i < 9; i++) {
            ItemStack s = craftMatrix.getStackInSlotOnClosing(i);
            if (s != null) {
                player.dropPlayerItemWithRandomChoice(s, true);
            }
        }
        ItemStack s = deconInput.getStackInSlotOnClosing(0);
        if (s != null) {
            player.dropPlayerItemWithRandomChoice(s, true);
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotId) {
        Slot slot = (Slot) inventorySlots.get(slotId);
        if (slot == null || !slot.getHasStack()) return null;
        ItemStack stack = slot.getStack();
        ItemStack copy = stack.copy();

        // decon preview: shift = deconstruct the WHOLE input (old-tree path)
        if (slotId >= SLOT_DECON_OUT_FIRST && slotId <= SLOT_DECON_OUT_FIRST + 8) {
            if (current != null && !current.isEmpty()) {
                takeIngredient(player, slotId - SLOT_DECON_OUT_FIRST, true);
            }
            return null;
        }
        // decon input: shift = move back to the inventory
        if (slotId == SLOT_DECON_IN) {
            if (!this.mergeItemStack(stack, SLOT_INV_FIRST, 46, true)) return null;
        } else if (slotId == SLOT_CRAFT_RESULT) {
            // ONE transfer call crafts EVERYTHING: batches = min(matrix stack sizes),
            // the whole result batch merges at once. With int-max stacking the vanilla
            // per-craft retrySlotClick recursion would run billions of levels deep
            // (StackOverflow crash), so the loop lives INSIDE this single call.
            if (player.worldObj.isRemote) return null;
            ItemStack result = slot.getStack().copy();
            int maxBatches = Integer.MAX_VALUE;
            for (int i = 0; i < craftMatrix.getSizeInventory(); i++) {
                ItemStack m = craftMatrix.getStackInSlot(i);
                if (m != null) maxBatches = Math.min(maxBatches, m.stackSize);
            }
            if (maxBatches <= 0) return null;
            long want = (long) result.stackSize * maxBatches;
            long placed = 0;
            while (placed < want) {
                ItemStack give = result.copy();
                give.stackSize = (int) Math.min(Integer.MAX_VALUE, want - placed);
                int beforeSize = give.stackSize;
                boolean ok = mergeItemStack(give, SLOT_INV_FIRST, 46, true);
                placed += beforeSize - give.stackSize;
                if (!ok || give.stackSize > 0) break; // inventory full: stop merging
            }
            if (placed <= 0) return null;
            int batches = (int) (placed / result.stackSize); // only WHOLE recipes count
            for (int i = 0; i < craftMatrix.getSizeInventory(); i++) {
                ItemStack m = craftMatrix.getStackInSlot(i);
                if (m != null) craftMatrix.decrStackSize(i, batches);
            }
            slot.putStack(null);
            onCraftMatrixChanged(craftMatrix);
            return result;
        } else if (slotId >= SLOT_CRAFT_FIRST && slotId <= 9) {
            if (!this.mergeItemStack(stack, SLOT_INV_FIRST, 46, true)) return null;
        } else {
            if (!player.worldObj.isRemote) {
                wheelchair.value.StorageData st = wheelchair.value.StorageData.get(player.worldObj);
                String uid = wheelchair.value.StorageData.uidOf(stack);
                String err = st.deposit(stack);
                if (err == null) {
                    slot.putStack(null);
                    wheelchair.network.StorageNet.broadcast((net.minecraft.entity.player.EntityPlayerMP) player, st, uid);
                    return copy;
                }
                player.addChatMessage(new net.minecraft.util.ChatComponentText(err));
            }
            return null;
        }
        if (stack.stackSize == 0) {
            slot.putStack(null);
        } else {
            slot.onSlotChanged();
        }
        return copy;
    }
}
