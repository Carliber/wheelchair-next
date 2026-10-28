package wheelchair.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import wheelchair.WheelchairMod;
import wheelchair.inventory.ContainerEnch;
import wheelchair.network.PacketEnchAction;
import wheelchair.network.PacketEnchBatch;
import wheelchair.network.PacketOpenTerminal;
import wheelchair.value.EnchCost;
import wheelchair.value.EnchDesc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enchant page (guiId 4) — 1:1 from prototype/terminal.html v26 (CSS/3 = GUI px,
 * window = panel coords + 35/+3). Window 214x254 (same height as the 3-row
 * storage page). Layout law identical to GuiStoragePage: content x8..168, the
 * scrollbar x168..176 flush against the frame.
 *
 * Rows: book icon + name (yellow ->N when staged) + price + a per-row number
 * box (the only level editor — typing stages, Apply submits the whole table as
 * ONE PacketEnchBatch, Shift = force through exclusions). Red row = conflicts
 * with what the item carries; gold row = already on the item. Top-right shows
 * the stored xp; the 存/取 buttons bank/withdraw player levels (click = 1,
 * Shift = all). The 突破上限 checkbox lifts the client clamp past the game max.
 */
@SideOnly(Side.CLIENT)
public class GuiEnchPage extends GuiContainer {
    // ---- palette (shared with GuiStoragePage) ----
    private static final int COL_BG = 0xffc6c6c6;
    private static final int COL_BEV_HI = 0xffffffff, COL_BEV_LO = 0xff555555;
    private static final int COL_SLOT = 0xff8b8b8b, COL_SLOT_DK = 0xff373737;
    private static final int COL_SEP = 0xff999999;
    private static final int BTN_OUTLINE = 0xff030303, BTN_TOP_HI = 0xffaaaaaa;
    private static final int BTN_G1 = 0xff6b6b6b, BTN_G2 = 0xff707070, BTN_G3 = 0xff565656;
    // v26: hover is a LIGHT GREY ramp, not the blue-violet one (user call)
    private static final int BTN_H1 = 0xffa6a6a6, BTN_H2 = 0xff999999, BTN_H3 = 0xff7d7d7d;
    private static final int COL_HOVER_RAIL = 0xff9c9c9c;
    private static final int COL_TEXT = 0xff404040;
    private static final int COL_XP = 0xff80ff20;   // anvil-cost green (GuiRepair)
    private static final int COL_RED = 0xffff5555;  // vanilla chat red
    private static final int COL_GOLD_TINT = 0x40ffaa00; // have-row 25%
    private static final int COL_EXCL_TINT = 0x47ff5555; // excluded-row 28%
    private static final int COL_ROW_BDR = 0xff9a9a9a;   // prototype .erow 1-GUI border
    private static final int COL_ROW_BASE = 0x24000000;  // prototype row tint rgba(0,0,0,.14)

    // ---- fixed layout (window px) ----
    private static final int FRAME = 3;
    private static final int RAIL_X = 3, SEP_X = 31, SEP_W = 4, PANEL_X = 35;
    private static final int SEARCH_X = 43, SEARCH_Y = 6, SEARCH_W = 129, SEARCH_H = 12;
    private static final int XP_RIGHT = 203;              // right-aligned xp counter edge
    private static final int SLOT_X = 43, SLOT_Y = 20;    // target slot (container slot 0)
    private static final int COST_X = 61, COST_Y = 24;    // bare staged-total number
    private static final int CAP_RIGHT = 203, CAP_Y = 20; // over-max checkbox row
    private static final int LIST_X = 43, LIST_Y = 39, LIST_W = 160;
    private static final int ROW_H = 21, PITCH = 23;
    private static final int LIST_H_3ROW = 100;                 // base viewport (3 rows)
    private static final int SB_X = 203, SB_W = 8, KNOB_W = 4, KNOB_H = 24;
    private static final int APPLY_X = 43, APPLY_W = 25, BTN_H = 12; // y = applyY (rows-derived)
    private static final int DEP_X = 167, WDR_X = 186, XPBTN_W = 17; // 存/取 right-flush
    private static final int LVLBOX_X = 172, LVLBOX_W = 26, LVLBOX_H = 16; // right edge 198: 5 clear of the scrollbar
    private static final int INV_LINE_Y_3ROW = 165, HOT_Y_3ROW = 228, INV_X = 43;

    private static final String[] RAIL_TABS = {"存储", "附魔", "能源"};
    private static final String[] RAIL_PIN = {"手杖", "设置"};
    private static final int ACTIVE_TAB = 1; // 附魔

    private final ContainerEnch container;
    /** adaptive rows (mirror of the storage page law): every extra row grows the
     *  list viewport +18, pushes the inventory block down +18 and the window +18,
     *  so the two pages always open the SAME height on the same canvas */
    private final int rows;
    private final int listH, applyY, invLineY, invY, hotY;
    private final List<String> allNames = new ArrayList<String>();
    private final List<String> filtered = new ArrayList<String>();
    /** staged table: name -> target level (0 = remove); submitted as ONE batch */
    private final Map<String, Integer> staged = new HashMap<String, Integer>();

    private GuiTextField search;
    private GuiTextField[] lvlboxes = new GuiTextField[0];
    private int focusedBox = -1;
    private int listScroll;
    private boolean draggingKnob;
    private int dragY0, scroll0;
    private Item itemFilter;
    private String hoverName;
    /** hit width of the xp-counter tooltip zone, set by the FG layer each frame */
    private int xpTipW;

    public GuiEnchPage(ContainerEnch c, int rows) {
        super(c);
        this.container = c;
        if (rows < 3) rows = 3;
        if (rows > 10) rows = 10;
        this.rows = rows;
        this.listH = LIST_H_3ROW + (rows - 3) * 18;
        this.applyY = LIST_Y + listH + 4;            // right under the list
        this.invLineY = INV_LINE_Y_3ROW + (rows - 3) * 18;
        this.invY = invLineY + 5;
        this.hotY = HOT_Y_3ROW + (rows - 3) * 18;
        xSize = 214;
        ySize = hotY + 18 + 6;                      // same formula as the storage page
    }

    private int visibleRows() {
        return Math.max(1, listH / PITCH); // 4 rows in the 3-row viewport
    }

    private int maxScroll() {
        return Math.max(0, filtered.size() - visibleRows());
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        search = new GuiTextField(this.fontRendererObj,
                guiLeft + SEARCH_X, guiTop + SEARCH_Y, SEARCH_W, SEARCH_H);
        search.setTextColor(0xe0e0e0);
        search.setMaxStringLength(40);
        lvlboxes = new GuiTextField[visibleRows()];
        for (int r = 0; r < visibleRows(); r++) {
            GuiTextField box = new GuiTextField(this.fontRendererObj,
                    guiLeft + LVLBOX_X, guiTop + LIST_Y + r * PITCH + 2, LVLBOX_W, LVLBOX_H);
            box.setEnableBackgroundDrawing(false); // we paint the black bed ourselves
            box.setTextColor(0xffff55);
            box.setMaxStringLength(3);
            box.setVisible(false);
            lvlboxes[r] = box;
        }
        reload();
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    // ---------------- data view ----------------

    /** rebuild the list from the registry + the target-slot item filter */
    private void reload() {
        allNames.clear();
        ItemStack target = container.target.getStackInSlot(0);
        for (int i = 0; i < Enchantment.enchantmentsList.length; i++) {
            Enchantment e = Enchantment.enchantmentsList[i];
            if (e == null) continue;
            // with an item in the slot AND 适配 on: only enchantments whose TYPE
            // fits (conflicts still shown, tinted red); 适配 off or no item: browse all
            if (target != null && target.getItem() != Items.enchanted_book
                    && WheelchairMod.enchAdaptive && !e.canApply(target)) continue;
            allNames.add(e.getName());
        }
        applyFilter();
    }

    private void applyFilter() {
        filtered.clear();
        String q = search != null ? search.getText().trim().toLowerCase() : "";
        for (String n : allNames) {
            if (q.length() == 0 || localName(n).toLowerCase().contains(q)) filtered.add(n);
        }
        if (listScroll > maxScroll()) listScroll = maxScroll();
    }

    private static String localName(String enchName) {
        Enchantment e = EnchCost.byName(enchName);
        return e == null ? enchName : e.getTranslatedName(1).replaceAll(" I$", "");
    }

    // ---------------- staged table (EPlus single-commit model, old tree) ----------------

    /** the level the row shows: staged if set, the item's live level otherwise */
    private int shownLevel(String name) {
        Integer s = staged.get(name);
        if (s != null) return s.intValue();
        Enchantment e = EnchCost.byName(name);
        return e == null ? 0 : container.currentLevel(e);
    }

    /** client clamp for typed levels: game max when 上限 is ON, else the 255 cap */
    private int clampMax(Enchantment e) {
        int max = Math.max(1, e.getMaxLevel());
        return WheelchairMod.enchLimit ? max : EnchCost.MAX_LEVEL_CAP;
    }

    /** record a typed/staged level (0 = remove), dropping no-op entries */
    private void stageLevel(String name, int v) {
        Enchantment e = EnchCost.byName(name);
        if (e == null) return;
        if (v < 0) v = 0;
        if (v > clampMax(e)) v = clampMax(e);
        int cur = container.currentLevel(e);
        if (v == cur) staged.remove(name);
        else staged.put(name, Integer.valueOf(v));
    }

    /** total xp points the staged table would cost (upgrades only; removals free) */
    private long stagedTotal() {
        ItemStack t = container.target.getStackInSlot(0);
        if (t == null) return 0;
        long total = 0;
        for (Map.Entry<String, Integer> en : staged.entrySet()) {
            Enchantment e = EnchCost.byName(en.getKey());
            if (e == null) continue;
            int cur = container.currentLevel(e);
            int lvl = en.getValue().intValue();
            if (lvl > cur) total += EnchCost.xpPoints(e, lvl, cur, t);
        }
        return total;
    }

    /** EPlus-style lock hint: the enchantment conflicts with one already on the item */
    private boolean isExcluded(String name) {
        ItemStack t = container.target.getStackInSlot(0);
        if (t == null) return false;
        Enchantment e = EnchCost.byName(name);
        if (e == null) return false;
        Map ench = EnchantmentHelper.getEnchantments(t);
        for (Object o : ench.entrySet()) {
            Map.Entry en = (Map.Entry) o;
            int id = ((Integer) en.getKey()).intValue();
            if (id == e.effectId || ((Integer) en.getValue()).intValue() <= 0) continue;
            Enchantment other = id >= 0 && id < Enchantment.enchantmentsList.length
                    ? Enchantment.enchantmentsList[id] : null;
            if (other != null && !other.canApplyTogether(e)) return true;
        }
        return false;
    }

    /** price the row shows: against the target item, or book-priced when browsing */
    private long rowPrice(Enchantment e, int lvl, int cur) {
        ItemStack t = container.target.getStackInSlot(0);
        if (t != null) return EnchCost.xpPoints(e, lvl, cur, t);
        return EnchCost.bookPrice(e, lvl);
    }

    // ---------------- lifecycle watch ----------------

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (search != null) search.updateCursorCounter();
        // the player fills the target slot while the GUI is open: refilter
        ItemStack t = container.target.getStackInSlot(0);
        Item now = t == null ? null : t.getItem();
        if (now != itemFilter) {
            itemFilter = now;
            staged.clear(); // the pending table belongs to the previous item
            reload();
        }
        // drop staged entries the server has already applied (batch landed)
        if (!staged.isEmpty()) {
            java.util.Iterator<Map.Entry<String, Integer>> it = staged.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Integer> en = it.next();
                Enchantment e = EnchCost.byName(en.getKey());
                if (e == null || container.currentLevel(e) == en.getValue().intValue()) it.remove();
            }
        }
        // number boxes mirror their row's shown level unless focused
        for (int r = 0; r < lvlboxes.length; r++) {
            int idx = listScroll + r;
            boolean has = idx < filtered.size();
            lvlboxes[r].setVisible(has);
            if (r != focusedBox) {
                lvlboxes[r].setText(has ? String.valueOf(shownLevel(filtered.get(idx))) : "");
                lvlboxes[r].updateCursorCounter();
            }
        }
    }

    // ---------------- background layer ----------------

    @Override
    protected void drawGuiContainerBackgroundLayer(float pt, int mx, int my) {
        int x = guiLeft, y = guiTop;
        drawRect(x, y, x + xSize, y + ySize, COL_BG);
        bevel(x, y, xSize, ySize, COL_BEV_HI, COL_BEV_LO, FRAME);
        drawRect(x + RAIL_X, y + FRAME, x + SEP_X, y + ySize - FRAME, COL_SLOT);
        drawRect(x + SEP_X, y + FRAME, x + SEP_X + SEP_W, y + ySize - FRAME, COL_SEP);
        for (int i = 0; i < RAIL_TABS.length; i++) drawRailTab(i, x + RAIL_X + 1, y + 6 + i * 17, true, mx, my);
        int py = ySize - 36;
        for (int i = 0; i < RAIL_PIN.length; i++) drawRailTab(i, x + RAIL_X + 1, y + py + i * 17, false, mx, my);

        search.drawTextBox();

        // row beds, prototype .erow law: 14% black base on EVERY row, replaced by
        // the gold (have) / red (excl) tint, hover ADDS 50% white; 1-GUI border.
        // Row inner padding = 3 GUI (icon/name shift with it in drawRow)
        for (int r = 0; r < visibleRows(); r++) {
            int idx = listScroll + r;
            if (idx >= filtered.size()) break;
            String name = filtered.get(idx);
            int ry = LIST_Y + r * PITCH;
            int rx = LIST_X, rw = LIST_W;
            drawRect(x + rx, y + ry, x + rx + rw, y + ry + 1, COL_ROW_BDR);
            drawRect(x + rx, y + ry + ROW_H - 1, x + rx + rw, y + ry + ROW_H, COL_ROW_BDR);
            drawRect(x + rx, y + ry, x + rx + 1, y + ry + ROW_H, COL_ROW_BDR);
            drawRect(x + rx + rw - 1, y + ry, x + rx + rw, y + ry + ROW_H, COL_ROW_BDR);
            Enchantment e = EnchCost.byName(name);
            boolean have = e != null && container.currentLevel(e) > 0 && !isExcluded(name);
            int bed = isExcluded(name) ? COL_EXCL_TINT : have ? COL_GOLD_TINT : COL_ROW_BASE;
            drawRect(x + rx + 1, y + ry + 1, x + rx + rw - 1, y + ry + ROW_H - 1, bed);
            if (inRect(mx, my, x + rx, y + ry, rw, ROW_H))
                drawRect(x + rx + 1, y + ry + 1, x + rx + rw - 1, y + ry + ROW_H - 1, 0x80ffffff);
        }

        // target-slot well + inventory wells (items come from vanilla's slot loop)
        slotRect(x + SLOT_X, y + SLOT_Y);
        drawRect(x + PANEL_X, y + invLineY, x + xSize - FRAME, y + invLineY + 2, COL_SEP);
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) slotRect(x + INV_X + c * 18, y + invY + r * 18);
        for (int c = 0; c < 9; c++) slotRect(x + INV_X + c * 18, y + hotY);

        drawScrollbar();
    }

    private void slotRect(int x, int y) {
        // vanilla slot well: 18x18 AROUND the 16x16 item anchor (bed extends
        // OUTWARD by 1, bevel on the outer ring) — no panel gap between slots
        drawRect(x - 1, y - 1, x + 17, y + 17, COL_SLOT);
        drawRect(x - 1, y - 1, x + 17, y, COL_SLOT_DK);
        drawRect(x - 1, y - 1, x, y + 17, COL_SLOT_DK);
        drawRect(x - 1, y + 16, x + 17, y + 17, COL_BEV_HI);
        drawRect(x + 16, y - 1, x + 17, y + 17, COL_BEV_HI);
    }

    private void bevel(int x, int y, int w, int h, int hi, int lo, int t) {
        drawRect(x, y, x + w, y + t, hi);
        drawRect(x, y, x + t, y + h, hi);
        drawRect(x, y + h - t, x + w, y + h, lo);
        drawRect(x + w - t, y, x + w, y + h, lo);
    }

    private void drawRailTab(int i, int tx, int ty, boolean top, int mx, int my) {
        boolean on = top && i == ACTIVE_TAB;
        String label = top ? RAIL_TABS[i] : RAIL_PIN[i];
        int col = on ? 0xff5a7d5a : (inRect(mx, my, tx, ty, 26, 15) ? COL_HOVER_RAIL : COL_SLOT);
        drawRect(tx, ty, tx + 26, ty + 15, col);
        if (on) bevel(tx, ty, 26, 15, 0xffa8d0a8, 0xff2e4a2e, 2);
        else bevel(tx, ty, 26, 15, COL_BEV_HI, COL_BEV_LO, 2);
        int w = fontRendererObj.getStringWidth(label);
        fontRendererObj.drawStringWithShadow(label, tx + (26 - w) / 2, ty + 4, on ? 0xeaffea : 0xe0e0e0);
    }

    private void drawScrollbar() {
        if (filtered.size() <= visibleRows()) return; // list not full: hidden entirely
        int x = guiLeft + SB_X, y = guiTop + LIST_Y;
        drawRect(x, y, x + SB_W, y + listH, COL_SLOT);
        bevel(x, y, SB_W, listH, COL_SLOT_DK, COL_BEV_HI, 2);
        int track = listH - 4;
        int ky = y + 2 + (track - KNOB_H) * listScroll / maxScroll();
        drawRect(x + 2, ky, x + 2 + KNOB_W, ky + KNOB_H, COL_BG);
        bevel(x + 2, ky, KNOB_W, KNOB_H, COL_BEV_HI, COL_BEV_LO, 1);
    }

    // ---------------- foreground layer (translated origin) ----------------

    private static final ItemStack BOOK_ICON = new ItemStack(Items.enchanted_book);

    @Override
    protected void drawGuiContainerForegroundLayer(int mx, int my) {
        hoverName = null;
        // 1. top-right xp counter (anvil-green, right-aligned to the content edge)
        Long xp = wheelchair.client.StorageMirror.res.get("xp");
        String xs = fmt(xp == null ? 0L : xp.longValue()) + "点";
        int xw = fontRendererObj.getStringWidth(xs);
        fontRendererObj.drawString(xs, XP_RIGHT - xw, 8, COL_XP);
        xpTipW = xw + 3; // hover hit width; the TOOLTIP itself is drawn in drawScreen
        // (drawHoveringText needs ABSOLUTE screen coords — calling it here inside
        // the translated FG layer would render the box offset by guiLeft/guiTop)

        // 2. staged cost as a bare number beside the target slot
        fontRendererObj.drawString(String.valueOf(stagedTotal()), COST_X, COST_Y, COL_TEXT);

        // 3. checkboxes on the target-slot row, right-aligned: [适配] [上限]
        int capX = drawCheckBox("上限", WheelchairMod.enchLimit, CAP_RIGHT);
        drawCheckBox("适配", WheelchairMod.enchAdaptive, capX - 8);

        // 4. list rows
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        for (int r = 0; r < visibleRows(); r++) {
            int idx = listScroll + r;
            if (idx >= filtered.size()) break;
            String name = filtered.get(idx);
            if (name.equals(hoverName(mx, my))) hoverName = name;
            drawRow(name, r);
        }
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        // 5. number boxes are drawn in drawScreen (GuiTextField uses ABSOLUTE
        //    screen coords — drawing it inside the translated FG layer would put
        //    the digits at 2x guiLeft, outside the box)

        // 6. apply row: [应用] left | player xp | [存][取] right-flush
        drawButton(APPLY_X, applyY, APPLY_W, BTN_H, "应用",
                inRect(mx, my, guiLeft + APPLY_X, guiTop + applyY, APPLY_W, BTN_H));
        String pinfo = mc.thePlayer.experienceLevel + "级 "
                + fmt(wheelchair.value.XpMath.totalXpOf(mc.thePlayer)) + "点经验";
        fontRendererObj.drawString(pinfo, APPLY_X + APPLY_W + 4, applyY + 2, COL_TEXT);
        drawButton(DEP_X, applyY, XPBTN_W, BTN_H, "存",
                inRect(mx, my, guiLeft + DEP_X, guiTop + applyY, XPBTN_W, BTN_H));
        drawButton(WDR_X, applyY, XPBTN_W, BTN_H, "取",
                inRect(mx, my, guiLeft + WDR_X, guiTop + applyY, XPBTN_W, BTN_H));

        // 7. inventory label
        fontRendererObj.drawString(net.minecraft.util.StatCollector
                .translateToLocal("container.inventory"), INV_X, invY - 11, COL_TEXT);
    }

    private String hoverName(int mx, int my) {
        int x = mx - guiLeft, y = my - guiTop;
        if (x < LIST_X || y < LIST_Y || x >= LIST_X + LIST_W || y >= LIST_Y + visibleRows() * PITCH) return null;
        int idx = listScroll + (y - LIST_Y) / PITCH;
        if (idx < 0 || idx >= filtered.size()) return null;
        return filtered.get(idx);
    }

    private void drawRow(String name, int r) {
        Enchantment e = EnchCost.byName(name);
        if (e == null) return;
        int y = LIST_Y + r * PITCH;
        int cur = container.currentLevel(e);
        int lvl = shownLevel(name);
        boolean pending = staged.containsKey(name);
        // book icon (row inner padding = 3 GUI, prototype .erow padding 0 9px)
        RenderItem.getInstance().renderItemAndEffectIntoGUI(this.fontRendererObj,
                this.mc.getTextureManager(), BOOK_ICON, LIST_X + 3, y + 2);
        // name (+ yellow ->N when staged); prototype flex law: the name only spans
        // what is LEFT of the price column — never overlaps the price digits
        long price = rowPrice(e, lvl, cur);
        String ps = "经验" + price + "点";
        int psW = fontRendererObj.getStringWidth(ps);
        String tag = isExcluded(name) ? "互斥" : null;
        int tagW = tag == null ? 0 : fontRendererObj.getStringWidth(tag) + 3;
        int nameAvail = (LVLBOX_X - 4 - psW - 2) - (LIST_X + 23) - tagW;
        String nm = localName(name) + (pending ? " §e\u2192" + lvl : "");
        nm = fontRendererObj.trimStringToWidth(nm, nameAvail);
        fontRendererObj.drawString(nm, LIST_X + 23, y + 6, pending ? 0xffff55 : 0xffffff);
        if (tag != null)
            fontRendererObj.drawString(tag, LIST_X + 23 + fontRendererObj.getStringWidth(nm) + 3, y + 6, COL_RED);
        // price, right-aligned before the number box
        fontRendererObj.drawString(ps, LVLBOX_X - 2 - psW, y + 6, COL_TEXT);
    }

    /** [chk][gap][label] right-aligned to rightEdge; returns the group's left edge */
    private int drawCheckBox(String label, boolean on, int rightEdge) {
        int lw = fontRendererObj.getStringWidth(label);
        int x = rightEdge - (8 + 2 + 1 + lw);
        drawRect(x, CAP_Y, x + 8, CAP_Y + 8, 0xffa0a0a0);
        drawRect(x + 1, CAP_Y + 1, x + 7, CAP_Y + 7, 0xff000000);
        if (on) drawRect(x + 2, CAP_Y + 2, x + 6, CAP_Y + 6, 0xff7fff7f);
        fontRendererObj.drawString(label, x + 11, CAP_Y, COL_TEXT);
        return x;
    }

    private int checkBoxX(String label, int rightEdge) {
        return rightEdge - (8 + 2 + 1 + fontRendererObj.getStringWidth(label));
    }

    /** vanilla-structure button, v26 hover ramp (grey, not blue) */
    private void drawButton(int x, int y, int w, int h, String label, boolean hover) {
        int c1 = hover ? BTN_H1 : BTN_G1;
        int c2 = hover ? BTN_H2 : BTN_G2;
        int c3 = hover ? BTN_H3 : BTN_G3;
        int third = Math.max(1, h / 3);
        drawRect(x, y, x + w, y + third, c1);
        drawRect(x, y + third, x + w, y + h - third, c2);
        drawRect(x, y + h - third, x + w, y + h, c3);
        drawRect(x, y, x + w, y + 1, BTN_OUTLINE);
        drawRect(x, y + h - 1, x + w, y + h, BTN_OUTLINE);
        drawRect(x, y, x + 1, y + h, BTN_OUTLINE);
        drawRect(x + w - 1, y, x + w, y + h, BTN_OUTLINE);
        drawRect(x + 1, y + 1, x + w - 1, y + 2, BTN_TOP_HI);
        int tw = fontRendererObj.getStringWidth(label);
        fontRendererObj.drawStringWithShadow(label, x + (w - tw) / 2, y + (h - 8) / 2,
                hover ? 0xffffa0 : 0xe0e0e0);
    }

    /** badge format shared with GuiStoragePage (full digits to 4 chars, then K/M/G) */
    private static String fmt(long n) {
        if (n >= 1000000000L) return String.format(java.util.Locale.US, "%.1fG", n / 1000000000.0);
        if (n >= 10000000L) return (n / 1000000L) + "M";
        if (n >= 1000000L) return String.format(java.util.Locale.US, "%.1fM", n / 1000000.0);
        if (n >= 10000L) return (n / 1000L) + "K";
        return String.valueOf(n);
    }

    // ---------------- tooltip ----------------

    private List<String> rowTooltip(String name) {
        List<String> tip = new ArrayList<String>();
        Enchantment e = EnchCost.byName(name);
        if (e != null) tip.add("\u00a7f" + localName(name) + " " + EnchCost.roman(shownLevel(name)));
        String desc = EnchDesc.of(name);
        if (desc != null) tip.add("\u00a7b" + desc);
        if (isExcluded(name)) tip.add("\u00a7c与物品已有附魔互斥 (Shift应用=强制)");
        tip.add("\u00a78数字框=改等级(暂存)  应用按钮=统一结算");
        return tip;
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        super.drawScreen(mx, my, pt);
        // number boxes: SELF-DRAWN text (GuiTextField.drawTextBox is left/top
        // aligned and ugly) — the field only captures keyboard input. Yellow
        // digits centered both ways, blinking caret after them when focused
        for (int r = 0; r < lvlboxes.length; r++) {
            if (!lvlboxes[r].getVisible()) continue;
            GuiTextField box = lvlboxes[r];
            boolean pending = listScroll + r < filtered.size()
                    && staged.containsKey(filtered.get(listScroll + r));
            int bx = box.xPosition, by = box.yPosition;
            drawRect(bx - 1, by - 1, bx + LVLBOX_W + 1, by + LVLBOX_H + 1,
                    pending || r == focusedBox ? 0xffffff55 : 0xffa0a0a0);
            drawRect(bx, by, bx + LVLBOX_W, by + LVLBOX_H, 0xff000000);
            String t = box.getText();
            int tw = fontRendererObj.getStringWidth(t);
            int tx = bx + (LVLBOX_W - tw) / 2, ty = by + (LVLBOX_H - 8) / 2;
            fontRendererObj.drawString(t, tx, ty, 0xffff55);
            if (r == focusedBox && (System.currentTimeMillis() / 500) % 2 == 0)
                drawRect(tx + tw + 1, ty + 1, tx + tw + 2, ty + 9, 0xffffff55);
        }
        // row tooltip (not over a number box / button) — absolute coords, correct layer
        String hn = hoverName(mx, my);
        if (hn != null && !overWidget(mx, my)) {
            this.drawHoveringText(rowTooltip(hn), mx, my, fontRendererObj);
        }
        // xp-counter tooltip (hit box computed in the FG layer, drawn here)
        if (xpTipW > 0 && inRect(mx, my, guiLeft + XP_RIGHT - xpTipW, guiTop + SEARCH_Y, xpTipW, SEARCH_H)) {
            this.drawHoveringText(java.util.Arrays.asList("存储经验点数", "存/取按钮在这里增减"), mx, my, fontRendererObj);
        }
    }

    private boolean overWidget(int mx, int my) {
        for (int r = 0; r < lvlboxes.length; r++) {
            GuiTextField box = lvlboxes[r];
            if (box.getVisible() && mx >= box.xPosition - 2 && my >= box.yPosition - 2
                    && mx < box.xPosition + box.width + 3 && my < box.yPosition + box.height + 3) return true;
        }
        return inRect(mx, my, guiLeft + APPLY_X, guiTop + applyY, APPLY_W, BTN_H)
                || inRect(mx, my, guiLeft + DEP_X, guiTop + applyY, XPBTN_W, BTN_H)
                || inRect(mx, my, guiLeft + WDR_X, guiTop + applyY, XPBTN_W, BTN_H);
    }


    // ---------------- input ----------------

    @Override
    protected void keyTyped(char c, int code) {
        if (focusedBox >= 0 && focusedBox < lvlboxes.length) {
            GuiTextField box = lvlboxes[focusedBox];
            if (code == Keyboard.KEY_RETURN || code == Keyboard.KEY_ESCAPE) {
                if (code == Keyboard.KEY_RETURN) {
                    commitBox(focusedBox);
                    box.setFocused(false);
                    focusedBox = -1;
                    return; // Enter stays on the page
                }
                box.setFocused(false); // ESC falls through: the GUI closes
                focusedBox = -1;
            } else if (box.textboxKeyTyped(c, code)) {
                return; // digits/backspace consumed by the number box
            }
        }
        if (search.isFocused()) {
            if (code == 1) {
                search.setFocused(false);
                return;
            }
            if (code == this.mc.gameSettings.keyBindInventory.getKeyCode()) {
                search.setFocused(false);
                return;
            }
            if (search.textboxKeyTyped(c, code)) {
                applyFilter();
                return;
            }
        }
        super.keyTyped(c, code);
    }

    /** commit a typed level into the staged table */
    private void commitBox(int r) {
        int idx = listScroll + r;
        if (idx < 0 || idx >= filtered.size()) return;
        String text = lvlboxes[r].getText().trim();
        if (text.length() == 0) return;
        int v;
        try {
            v = Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return;
        }
        stageLevel(filtered.get(idx), v);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) {
        // rail pinned: 手杖 opens the staff page
        int py = ySize - 36;
        for (int i = 0; i < RAIL_PIN.length; i++) {
            if (inRect(mx, my, guiLeft + RAIL_X + 1, guiTop + py + i * 17, 26, 15)) {
                net.minecraft.client.gui.ScaledResolution sr = new net.minecraft.client.gui.ScaledResolution(
                        this.mc, this.mc.displayWidth, this.mc.displayHeight);
                wheelchair.WheelchairMod.network.sendToServer(
                        new wheelchair.network.PacketOpenTerminal(5,
                                wheelchair.client.GuiStoragePage.rowsFor(sr.getScaledHeight())));
                return;
            }
        }
        // rail tabs: storage/energy open their pages; the ench tab is this page
        for (int i = 0; i < RAIL_TABS.length; i++) {
            if (i != ACTIVE_TAB && inRect(mx, my, guiLeft + RAIL_X + 1, guiTop + 6 + i * 17, 26, 15)) {
                if (i == 0) {
                    net.minecraft.client.gui.ScaledResolution sr =
                            new net.minecraft.client.gui.ScaledResolution(this.mc, this.mc.displayWidth, this.mc.displayHeight);
                    int rows = wheelchair.client.GuiStoragePage.rowsFor(sr.getScaledHeight());
                    WheelchairMod.network.sendToServer(new PacketOpenTerminal(0, rows));
                } else if (i == 2) {
                    WheelchairMod.network.sendToServer(new PacketOpenTerminal(6, 3));
                }
                return;
            }
        }
        // scrollbar knob grab
        if (button == 0 && inRect(mx, my, guiLeft + SB_X, guiTop + LIST_Y, SB_W, listH)) {
            if (filtered.size() > visibleRows()) {
                draggingKnob = true;
                dragY0 = my;
                scroll0 = listScroll;
            }
            return;
        }
        // level boxes take focus before anything else in their row
        for (int r = 0; r < lvlboxes.length; r++) {
            GuiTextField box = lvlboxes[r];
            if (box.getVisible()
                    && mx >= box.xPosition && mx < box.xPosition + box.width
                    && my >= box.yPosition && my < box.yPosition + box.height) {
                if (focusedBox != r) {
                    if (focusedBox >= 0 && focusedBox < lvlboxes.length) {
                        commitBox(focusedBox);
                        lvlboxes[focusedBox].setFocused(false);
                    }
                    focusedBox = r;
                    box.setFocused(true);
                }
                return;
            }
        }
        if (focusedBox >= 0 && focusedBox < lvlboxes.length) {
            commitBox(focusedBox); // click outside: commit what was typed, blur
            lvlboxes[focusedBox].setFocused(false);
            focusedBox = -1;
        }
        // checkboxes: [适配] [上限] (both default ON)
        int capX2 = checkBoxX("上限", CAP_RIGHT);
        int adpX = checkBoxX("适配", capX2 - 8);
        if (button == 0 && inRect(mx, my, guiLeft + capX2, guiTop + CAP_Y, CAP_RIGHT - capX2, 12)) {
            WheelchairMod.enchLimit = !WheelchairMod.enchLimit;
            return;
        }
        if (button == 0 && inRect(mx, my, guiLeft + adpX, guiTop + CAP_Y, capX2 - 8 - adpX, 12)) {
            WheelchairMod.enchAdaptive = !WheelchairMod.enchAdaptive;
            reload();
            return;
        }
        // xp buttons: click = one level, Shift = all
        if (button == 0 && inRect(mx, my, guiLeft + DEP_X, guiTop + applyY, XPBTN_W, BTN_H)) {
            WheelchairMod.network.sendToServer(new PacketEnchAction(0, isShiftKeyDown() ? 1 : 0));
            return;
        }
        if (button == 0 && inRect(mx, my, guiLeft + WDR_X, guiTop + applyY, XPBTN_W, BTN_H)) {
            WheelchairMod.network.sendToServer(new PacketEnchAction(1, isShiftKeyDown() ? 1 : 0));
            return;
        }
        // apply the staged table (Shift = force through exclusions)
        if (button == 0 && inRect(mx, my, guiLeft + APPLY_X, guiTop + applyY, APPLY_W, BTN_H)) {
            if (!staged.isEmpty()) {
                PacketEnchBatch pkt = new PacketEnchBatch();
                pkt.force = isShiftKeyDown();
                for (Map.Entry<String, Integer> en : staged.entrySet()) {
                    pkt.names.add(en.getKey());
                    pkt.levels.add(en.getValue());
                }
                WheelchairMod.network.sendToServer(pkt);
            }
            return;
        }
        search.mouseClicked(mx, my, button);
        if (!inRect(mx, my, guiLeft + SEARCH_X, guiTop + SEARCH_Y, SEARCH_W, SEARCH_H)) {
            super.mouseClicked(mx, my, button);
        }
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d != 0 && filtered.size() > visibleRows()) {
            listScroll += d > 0 ? -1 : 1; // one row per notch
            if (listScroll > maxScroll()) listScroll = maxScroll();
            if (listScroll < 0) listScroll = 0;
        }
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long held) {
        if (draggingKnob) {
            int track = listH - 4;
            listScroll = scroll0 + (my - dragY0) * maxScroll() / Math.max(1, track - KNOB_H);
            if (listScroll > maxScroll()) listScroll = maxScroll();
            if (listScroll < 0) listScroll = 0;
            return;
        }
        super.mouseClickMove(mx, my, button, held);
    }

    @Override
    protected void mouseMovedOrUp(int mx, int my, int which) {
        draggingKnob = false;
        super.mouseMovedOrUp(mx, my, which);
    }

    private boolean inRect(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
