package wheelchair.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import wheelchair.network.PacketStorageAction;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidRegistry;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Storage page — 1:1 from prototype/terminal.html v25 final (CSS/3 = GUI px),
 * with AE2-style adaptive grid rows: rows = (canvasHeight - 202) / 18 clamped
 * to 3..10; everything below the grid (chips, band, inventory) shifts with it.
 *
 * Grid items are rendered by VANILLA's drawScreen slot loop through client-only
 * {@link SlotVirtual} objects (old-tree / AE2 SlotME pattern): same render path
 * as the player inventory, so icon colours always match and the hover
 * highlight lands on the slot rectangle by itself. Count badges are drawn in
 * the FOREGROUND layer (zLevel 300, depth test off, black backing strip) so
 * they always float above the items.
 */
@SideOnly(Side.CLIENT)
public class GuiStoragePage extends GuiContainer implements VirtualSlotHost {
    // ---- palette ----
    private static final int COL_BG = 0xffc6c6c6;
    private static final int COL_BEV_HI = 0xffffffff, COL_BEV_LO = 0xff555555;
    private static final int COL_SLOT = 0xff8b8b8b, COL_SLOT_DK = 0xff373737;
    private static final int COL_SEP = 0xff999999;
    private static final int BTN_OUTLINE = 0xff030303, BTN_TOP_HI = 0xffaaaaaa;
    private static final int BTN_G1 = 0xff6b6b6b, BTN_G2 = 0xff707070, BTN_G3 = 0xff565656;
    private static final int BTN_H1 = 0xffbec7ff, BTN_H2 = 0xff808ac1, BTN_H3 = 0xff5c669d;
    private static final int BTN_ON1 = 0xff6f9e6f, BTN_ON2 = 0xff5a7d5a, BTN_ON3 = 0xff3f5c3f;
    private static final int BTN_ON_HI = 0xffa8d0a8;
    private static final int COL_HOVER_RAIL = 0xff9c9c9c;

    // ---- fixed layout (window px; panel coords + 35/+3) ----
    private static final int FRAME = 3;
    private static final int RAIL_X = 3, RAIL_W = 28, SEP_X = 31, SEP_W = 4;
    private static final int PANEL_X = 35;
    private static final int SEARCH_X = 43, SEARCH_Y = 6, SEARCH_W = 129, SEARCH_H = 12;
    private static final int SORT_X = 175, SORT_Y = 6, SORT_W = 24, SORT_H = 12;   // sort mode
    private static final int SORTD_X = 199, SORTD_W = 12;                           // sort direction, flush against the mode button
    private static final int TYPE_X = 43, NBT_X = 91, LBL_Y = 20, TYPE_W = 45, NBT_W = 60, LBL_H = 12;
    private static final int CAT_LINE_Y = 34;
    private static final int GRID_X = 43, GRID_Y = 39, COLS = 9;
    private static final int SB_X = 203, SB_W = 8, SB_Y = 39, KNOB_W = 4, KNOB_H = 24;
    private static final int CHIP1_X = 98, CHIP2_X = 126, CHIP_W = 24, CHIP_H = 12;
    private static final int MAT_X = 52;                                   // 3x3 matrix, pitch 18
    private static final int ARROW_X = 112;                                // 24 wide, centre = col5
    private static final int RESULT_X = 151;
    private static final int DECON_IN_X = 70;
    private static final int DOUT_X = 142;                                 // decon output 3x3
    private static final int NAVL_X = 112, NAVR_X = 124, NAV_W = 12, NAV_H = 10;
    private static final int COUNT_X = 163;
    private static final int INV_X = 43;
    /** fixed chrome above the grid + below the last inventory row (3-row window = 252);
     *  v26 law: panel content 176 wide (vanilla 8+160+8), grid x8..168, scrollbar x168..176 flush right */
    private static final int FIXED_V = 200;

    private static final String[] SORTS = {"名称", "数量", "类型", "创造"};
    private static final String[] TYPE_NAMES = {"全部", "物品", "其他"};
    private static final String[] NBT_NAMES = {"全部", "无NBT", "有NBT"};
    private static final String[] RAIL_TABS = {"存储", "附魔", "能源", "加速"};
    private static final String[] RAIL_PIN = {"手杖", "设置"};

    // ---- adaptive layout (recomputed for the row count) ----
    private final int rows;
    private final int chipY, bandTop, matY, arrowY, countY, invLineY, invY, hotY, sbH;

    /** AE2-style: grid rows for a canvas height (min 3, max 10) */
    public static int rowsFor(int canvasH) {
        int r = (canvasH - FIXED_V) / 18;
        if (r < 3) r = 3;
        if (r > 10) r = 10;
        return r;
    }

    public GuiStoragePage(wheelchair.inventory.ContainerStoragePage c, int rows) {
        super(c);
        if (rows < 3) rows = 3;
        if (rows > 10) rows = 10;
        this.rows = rows;
        this.sbH = (rows - 1) * 18 + 16;  // grid footprint, NOT rows*18 (last row has no tail gap)
        this.chipY = GRID_Y + sbH + 2;            // band switch chips
        this.bandTop = chipY + 13;                 // band top
        this.matY = bandTop + 3;                   // 3x3 matrix top
        this.arrowY = bandTop + 21;                // arrow / result / decon-input row
        this.countY = bandTop + 43;                // recipe counter
        this.invLineY = bandTop + 59;              // inventory rule
        this.invY = invLineY + 5;
        this.hotY = invY + 58;
        xSize = 214;
        ySize = hotY + 18 + 6;                     // bottom pad 3 + frame 3
    }

    private GuiTextField search;
    private int typeSel, nbtSel;       // 0 all/1 item/2 other ; 0 all/1 no-nbt/2 has-nbt
    private int sortIdx;
    private boolean asc = true;
    private int bandMode;              // 0 craft, 1 decon (placeholder)
    private final List<String> view = new ArrayList<String>();
    private int scroll;
    private boolean draggingKnob;
    private int dragY0, scroll0;

    @Override
    public void initGui() {
        super.initGui();
        installBigCountRenderer(); // one-shot: global vanilla digit pass -> K/M/G
        search = new GuiTextField(this.fontRendererObj,
                guiLeft + SEARCH_X, guiTop + SEARCH_Y, SEARCH_W, SEARCH_H);
        search.setTextColor(0xe0e0e0);
        search.setMaxStringLength(40);
        search.setFocused(true);

        // client-only virtual grid slots (re-init safe: clear stale ones first,
        // AE2 removes stale SlotME the same way)
        for (int i = 0; i < inventorySlots.inventorySlots.size(); i++) {
            if (inventorySlots.inventorySlots.get(i) instanceof SlotVirtual) {
                inventorySlots.inventorySlots.remove(i--);
            }
        }
        for (int i = 0; i < rows * COLS; i++) {
            SlotVirtual sv = new SlotVirtual(this, i,
                    GRID_X + (i % COLS) * 18, GRID_Y + (i / COLS) * 18);
            sv.slotNumber = inventorySlots.inventorySlots.size();
            inventorySlots.inventorySlots.add(sv);
        }
        refresh();
    }

    /** vanilla digit pass replaced ONCE, globally (GuiContainer.itemRender is a
     *  shared static; the HUD hotbar gets the patch from ClientProxy.postInit):
     *  counts beyond 4 characters render as 10K/2147M/2G.
     *  Yields when InfiniStack is installed — its RenderItem mixin owns digits. */
    public static boolean bigCountInstalled;

    private static void installBigCountRenderer() {
        if (bigCountInstalled) return;
        bigCountInstalled = true;
        if (cpw.mods.fml.common.Loader.isModLoaded("infinistack")) return;
        BigCountRenderItem r = new BigCountRenderItem();
        itemRender = r; // container GUIs (non-final static)
        try { // the HUD hotbar renders through GuiIngame.itemRenderer — a
            // protected static FINAL field, so strip the final flag first
            // (Java-8 modifiers hack; this mod is 1.7.10 / Java 8 only anyway)
            java.lang.reflect.Field f;
            try {
                f = net.minecraft.client.gui.GuiIngame.class.getDeclaredField("itemRenderer"); // dev MCP name
            } catch (NoSuchFieldException e) {
                f = net.minecraft.client.gui.GuiIngame.class.getDeclaredField("field_73841_b"); // production SRG name
            }
            f.setAccessible(true);
            java.lang.reflect.Field mods = java.lang.reflect.Field.class.getDeclaredField("modifiers");
            mods.setAccessible(true);
            mods.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
            f.set(null, r);
        } catch (Throwable t) {
            cpw.mods.fml.common.FMLLog.warning("[wheelchair] hotbar count renderer not installed: %s",
                    new Object[]{String.valueOf(t)});
        }
    }

    /** early install hook (ClientProxy.postInit): the HUD renders the hotbar
     *  long before any container GUI opens */
    public static void installBigCountRendererPublic() {
        installBigCountRenderer();
    }

    /** vanilla RenderItem.renderItemOverlayIntoGUI with our short counts */
    private static class BigCountRenderItem extends RenderItem {
        @Override
        public void renderItemOverlayIntoGUI(net.minecraft.client.gui.FontRenderer fr,
                net.minecraft.client.renderer.texture.TextureManager tm, ItemStack is, int x, int y, String text) {
            if (is == null) return;
            if (is.getItem().showDurabilityBar(is)) {
                double health = is.getItem().getDurabilityForDisplay(is);
                int j1 = (int) Math.round(13.0D - health * 13.0D);
                int k = (int) Math.round(255.0D - health * 255.0D);
                GL11.glDisable(GL11.GL_LIGHTING);
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glDisable(GL11.GL_ALPHA_TEST);
                GL11.glDisable(GL11.GL_BLEND);
                Tessellator tessellator = Tessellator.instance;
                int l = 255 - k << 16 | k << 8;
                int i1 = (255 - k) / 4 << 16 | 16128;
                quad(tessellator, x + 2, y + 13, 13, 2, 0);
                quad(tessellator, x + 2, y + 13, 12, 1, i1);
                quad(tessellator, x + 2, y + 13, j1, 1, l);
                GL11.glEnable(GL11.GL_ALPHA_TEST);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glEnable(GL11.GL_LIGHTING);
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            }
            String s = text != null ? text : (is.stackSize > 1 ? fmt(is.stackSize) : null);
            if (s != null) {
                // vanilla digit state: without disabling depth the 3D item art on
                // the HUD hotbar draws OVER the digits (z-fighting by depth)
                GL11.glDisable(GL11.GL_LIGHTING);
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glEnable(GL11.GL_BLEND);
                net.minecraft.client.renderer.OpenGlHelper.glBlendFunc(770, 771, 1, 0);
                fr.drawStringWithShadow(s, x + 19 - fr.getStringWidth(s), y + 9, 0xffffff);
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glEnable(GL11.GL_LIGHTING);
                GL11.glEnable(GL11.GL_DEPTH_TEST);
            }
        }

        /** RenderItem.renderQuad is private — plain tessellator quad instead */
        private static void quad(Tessellator t, int x, int y, int w, int h, int color) {
            t.startDrawingQuads();
            t.setColorOpaque_I(color);
            t.addVertex(x, y, 0.0D);
            t.addVertex(x, y + h, 0.0D);
            t.addVertex(x + w, y + h, 0.0D);
            t.addVertex(x + w, y, 0.0D);
            t.draw();
        }
    }

    // ================= VirtualSlotHost (rendered by vanilla's slot loop) =================

    /** stack shown in visible window cell i (null for resource cells) */
    public ItemStack getVisibleStack(int i) {
        int idx = scroll + i;
        if (idx < 0 || idx >= view.size()) return null;
        String uid = view.get(idx);
        if (StorageMirror.res.containsKey(uid)) return null; // drawn by drawResource
        return StorageMirror.stackOf(uid);
    }

    /** true when visible window cell i holds a resource (label drawn by the background layer) */
    public boolean isResourceCell(int i) {
        int idx = scroll + i;
        return idx >= 0 && idx < view.size() && StorageMirror.res.containsKey(view.get(idx));
    }

    // ================= data view =================

    public void onStorageUpdate() {
        refresh();
    }

    private void refresh() {
        String q = search != null ? search.getText().trim().toLowerCase() : "";
        view.clear();
        if (typeSel != 2) {
            for (java.util.Map.Entry<String, long[]> e : StorageMirror.items.entrySet()) {
                if (!passNbt(e.getKey())) continue;
                if (q.length() > 0 && !matches(e.getKey(), q)) continue;
                view.add(e.getKey());
            }
        }
        if (typeSel == 0 || typeSel == 2) {
            for (String uid : StorageMirror.res.keySet()) {
                if (q.length() > 0 && !matches(uid, q)) continue;
                view.add(uid);
            }
        }
        sortView();
        clampScroll();
    }

    private boolean passNbt(String uid) {
        boolean has = uid.indexOf('~') >= 0 || StorageMirror.nbt.containsKey(uid);
        if (nbtSel == 1) return !has;
        if (nbtSel == 2) return has;
        return true;
    }

    private boolean matches(String uid, String q) {
        if (uid.toLowerCase().contains(q)) return true;
        ItemStack s = StorageMirror.stackOf(uid);
        if (s != null) {
            try {
                String d = s.getDisplayName();
                if (d != null) {
                    if (d.toLowerCase().contains(q)) return true;
                    if (isAsciiLetters(q) && PinyinSearch.matches(d, q)) return true; // 石头 <- shitou/st
                }
            } catch (Throwable ignored) {}
        }
        return false;
    }

    private static boolean isAsciiLetters(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 'a' || c > 'z') return false;
        }
        return s.length() > 0;
    }

    private void sortView() {
        if (sortIdx == 3) return; // 创造 (creative): storage insertion order
        Collections.sort(view, new Comparator<String>() {
            public int compare(String a, String b) {
                int r;
                if (sortIdx == 1) {
                    r = Long.compare(StorageMirror.count(a), StorageMirror.count(b));
                } else if (sortIdx == 2) {
                    r = typeOf(a).compareTo(typeOf(b));
                    if (r == 0) r = nameKey(a).compareTo(nameKey(b));
                } else {
                    r = nameKey(a).compareTo(nameKey(b)); // pinyin order for Chinese names
                }
                return asc ? r : -r;
            }
        });
    }

    /** display name folded to a pinyin sort key (Chinese sorts by pinyin, not codepoint) */
    private static String nameKey(String uid) {
        return PinyinSearch.key(nameOf(uid));
    }

    private static String nameOf(String uid) {
        ItemStack s = StorageMirror.stackOf(uid);
        if (s != null) {
            try {
                String d = s.getDisplayName();
                if (d != null) return d;
            } catch (Throwable ignored) {}
        }
        return uid;
    }

    private static String typeOf(String uid) {
        if (StorageMirror.res.containsKey(uid)) return "0" + uid;
        if (uid.indexOf('~') >= 0 || StorageMirror.nbt.containsKey(uid)) return "2" + nameOf(uid);
        return "1" + nameOf(uid);
    }

    private void clampScroll() {
        int max = Math.max(0, view.size() - rows * COLS);
        if (scroll > max) scroll = max;
        if (scroll < 0) scroll = 0;
    }

    // ================= background layer =================

    @Override
    protected void drawGuiContainerBackgroundLayer(float pt, int mx, int my) {
        relocateBandSlots(); // hide the inactive band's real slots (render + click)
        int x = guiLeft, y = guiTop;
        drawRect(x, y, x + xSize, y + ySize, COL_BG);
        bevel(x, y, xSize, ySize, COL_BEV_HI, COL_BEV_LO, FRAME);
        drawRect(x + RAIL_X, y + FRAME, x + SEP_X, y + ySize - FRAME, COL_SLOT);
        drawRect(x + SEP_X, y + FRAME, x + SEP_X + SEP_W, y + ySize - FRAME, COL_SEP);
        for (int i = 0; i < RAIL_TABS.length; i++) drawRailTab(i, x + RAIL_X + 1, y + 6 + i * 17, true, mx, my);
        int py = ySize - 36; // pinned tabs: pitch 17 (26x15 buttons), 2-GUI gap under the frame
        for (int i = 0; i < RAIL_PIN.length; i++) drawRailTab(i, x + RAIL_X + 1, y + py + i * 17, false, mx, my);

        // header strip 1: search + sort (mode button + direction button)
        search.drawTextBox();
        drawButton(x + SORT_X, y + SORT_Y, SORT_W, SORT_H, SORTS[sortIdx],
                inRect(mx, my, x + SORT_X, y + SORT_Y, SORT_W, SORT_H), false);
        drawButton(x + SORTD_X, y + SORT_Y, SORTD_W, SORT_H, asc ? "\u25b2" : "\u25bc",
                inRect(mx, my, x + SORTD_X, y + SORT_Y, SORTD_W, SORT_H), false);

        // header strip 2: cycling filter labels + rule
        drawButton(x + TYPE_X, y + LBL_Y, TYPE_W, LBL_H, "类型:" + TYPE_NAMES[typeSel],
                inRect(mx, my, x + TYPE_X, y + LBL_Y, TYPE_W, LBL_H), false);
        drawButton(x + NBT_X, y + LBL_Y, NBT_W, LBL_H, "NBT:" + NBT_NAMES[nbtSel],
                inRect(mx, my, x + NBT_X, y + LBL_Y, NBT_W, LBL_H), false);
        drawRect(x + PANEL_X, y + CAT_LINE_Y, x + xSize - FRAME, y + CAT_LINE_Y + 2, COL_SEP);

        // storage grid cells (16x16, pitch 18; items themselves come from
        // vanilla's slot loop via SlotVirtual) + resource labels + scrollbar
        for (int i = 0; i < rows * COLS; i++) {
            slotRect(x + GRID_X + (i % COLS) * 18, y + GRID_Y + (i / COLS) * 18);
        }
        for (int i = 0; i < rows * COLS; i++) {
            int idx = scroll + i;
            if (idx >= view.size()) break;
            String uid = view.get(idx);
            if (StorageMirror.res.containsKey(uid)) {
                drawResource(uid, x + GRID_X + (i % COLS) * 18, y + GRID_Y + (i / COLS) * 18);
            }
        }
        drawScrollbar();

        // band switch chips
        drawButton(x + CHIP1_X, y + chipY, CHIP_W, CHIP_H, "合成",
                inRect(mx, my, x + CHIP1_X, y + chipY, CHIP_W, CHIP_H), bandMode == 0);
        drawButton(x + CHIP2_X, y + chipY, CHIP_W, CHIP_H, "拆解",
                inRect(mx, my, x + CHIP2_X, y + chipY, CHIP_W, CHIP_H), bandMode == 1);

        // band
        drawButton(x + NAVL_X, y + matY, NAV_W, NAV_H, "<",
                inRect(mx, my, x + NAVL_X, y + matY, NAV_W, NAV_H), false);
        drawButton(x + NAVR_X, y + matY, NAV_W, NAV_H, ">",
                inRect(mx, my, x + NAVR_X, y + matY, NAV_W, NAV_H), false);
        if (bandMode == 0) {
            for (int r = 0; r < 3; r++) for (int c = 0; c < 3; c++) slotRect(x + MAT_X + c * 18, y + matY + r * 18);
            slotRect(x + RESULT_X, y + arrowY);
            drawArrow(x + ARROW_X, y + arrowY);
        } else {
            slotRect(x + DECON_IN_X, y + arrowY); // decon input (placeholder)
            for (int r = 0; r < 3; r++) for (int c = 0; c < 3; c++) slotRect(x + DOUT_X + c * 18, y + matY + r * 18);
            drawArrow(x + ARROW_X, y + arrowY);
        }

        // inventory block
        drawRect(x + PANEL_X, y + invLineY, x + xSize - FRAME, y + invLineY + 2, COL_SEP);
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) slotRect(x + INV_X + c * 18, y + invY + r * 18);
        for (int c = 0; c < 9; c++) slotRect(x + INV_X + c * 18, y + hotY);
    }

    /** vanilla-structure button: dark outline + top highlight + 3-band gradient body */
    private void drawButton(int x, int y, int w, int h, String label, boolean hover, boolean on) {
        int c1 = on ? BTN_ON1 : hover ? BTN_H1 : BTN_G1;
        int c2 = on ? BTN_ON2 : hover ? BTN_H2 : BTN_G2;
        int c3 = on ? BTN_ON3 : hover ? BTN_H3 : BTN_G3;
        int third = h / 3;
        if (third < 1) third = 1;
        drawRect(x, y, x + w, y + third, c1);
        drawRect(x, y + third, x + w, y + h - third, c2);
        drawRect(x, y + h - third, x + w, y + h, c3);
        drawRect(x, y, x + w, y + 1, BTN_OUTLINE);
        drawRect(x, y + h - 1, x + w, y + h, BTN_OUTLINE);
        drawRect(x, y, x + 1, y + h, BTN_OUTLINE);
        drawRect(x + w - 1, y, x + w, y + h, BTN_OUTLINE);
        drawRect(x + 1, y + 1, x + w - 1, y + 2, on ? BTN_ON_HI : BTN_TOP_HI);
        int tc = on ? 0xffffff : hover ? 0xffffa0 : 0xe0e0e0;
        int tw = fontRendererObj.getStringWidth(label);
        if (tw > w - 2) {
            while (label.length() > 1 && fontRendererObj.getStringWidth(label) > w - 2) {
                label = label.substring(0, label.length() - 1);
            }
            tw = fontRendererObj.getStringWidth(label);
        }
        fontRendererObj.drawStringWithShadow(label, x + (w - tw) / 2, y + (h - 8) / 2, tc);
    }

    /** park the inactive band's real slots off-screen so vanilla neither renders
     *  them nor hits them with clicks; the player inventory (slots 10..45) NEVER
     *  moves — only the two bands swap (craft = 0..9, decon = 46..55) */
    private void relocateBandSlots() {
        wheelchair.inventory.ContainerStoragePage c =
                (wheelchair.inventory.ContainerStoragePage) inventorySlots;
        boolean craft = bandMode == 0;
        if (craft) {
            for (int r = 0; r < 3; r++) for (int col = 0; col < 3; col++) {
                Slot s = (Slot) c.inventorySlots.get(1 + col + r * 3);
                s.xDisplayPosition = MAT_X + col * 18;
                s.yDisplayPosition = matY + r * 18;
            }
            Slot res = (Slot) c.inventorySlots.get(0);
            res.xDisplayPosition = RESULT_X; res.yDisplayPosition = arrowY;
        } else {
            for (int i = 0; i <= 9; i++) ((Slot) c.inventorySlots.get(i)).xDisplayPosition = -5000;
        }
        if (!craft) {
            Slot in = (Slot) c.inventorySlots.get(wheelchair.inventory.ContainerStoragePage.SLOT_DECON_IN);
            in.xDisplayPosition = DECON_IN_X; in.yDisplayPosition = arrowY;
            for (int r = 0; r < 3; r++) for (int col = 0; col < 3; col++) {
                Slot s = (Slot) c.inventorySlots.get(
                        wheelchair.inventory.ContainerStoragePage.SLOT_DECON_OUT_FIRST + col + r * 3);
                s.xDisplayPosition = DOUT_X + col * 18;
                s.yDisplayPosition = matY + r * 18;
            }
        } else {
            for (int i = wheelchair.inventory.ContainerStoragePage.SLOT_DECON_IN;
                 i <= wheelchair.inventory.ContainerStoragePage.SLOT_DECON_OUT_FIRST + 8; i++) {
                ((Slot) c.inventorySlots.get(i)).xDisplayPosition = -5000;
            }
        }
    }

    private void drawRailTab(int i, int tx, int ty, boolean top, int mx, int my) {
        boolean on = top && i == 0;
        String label = top ? RAIL_TABS[i] : RAIL_PIN[i];
        int col = on ? 0xff5a7d5a : (inRect(mx, my, tx, ty, 26, 15) ? COL_HOVER_RAIL : COL_SLOT);
        drawRect(tx, ty, tx + 26, ty + 15, col);
        if (on) bevel(tx, ty, 26, 15, 0xffa8d0a8, 0xff2e4a2e, 2);
        else bevel(tx, ty, 26, 15, COL_BEV_HI, COL_BEV_LO, 2);
        int w = fontRendererObj.getStringWidth(label);
        fontRendererObj.drawStringWithShadow(label, tx + (26 - w) / 2, ty + 4, on ? 0xeaffea : 0xe0e0e0);
    }

    /** 16x16 cell background, EXACTLY on the slot rectangle (vanilla highlight
     *  and item both draw at the slot origin 16x16): #8b8b8b face, #373737
     *  top/left, #ffffff bottom/right; pitch is 18 so cells leave 2px panel */
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

    /** vanilla furnace arrow 24x16 (furnace.png u176 v14) */
    private static final ResourceLocation FURNACE_TEX =
            new ResourceLocation("textures/gui/container/furnace.png");

    private void drawArrow(int x, int y) {
        this.mc.getTextureManager().bindTexture(FURNACE_TEX);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        this.drawTexturedModalRect(x, y, 176, 14, 24, 16);
    }

    private void drawResource(String uid, int gx, int gy) {
        if (uid.startsWith("fluid:") || uid.startsWith("gas:")) {
            // fluids draw as THEMSELVES (their own still-icon from the block atlas,
            // old-tree drawFluidIcon): count comes from the shared badge pass
            drawFluidIcon(uid, gx, gy);
            return;
        }
        // energy icons, BeyondDimensions style: the WATER still sprite with an
        // energy tint (their EnergyStackKeyRender: tint 0x50F18E on water) —
        // resource-pack safe, no hand-drawn png
        if (uid.equals("rf")) {
            drawTintedStill("water", 0x50F18E, gx, gy);
            return;
        }
        if (uid.equals("eu")) {
            drawTintedStill("water", 0x3FB9FF, gx, gy);
            return;
        }
        // scalar resources: dedicated 16x16 icons (old-tree res_*.png set);
        // name in the top-left, count from the shared badge pass
        drawResIcon(uid, gx, gy);
        String label = uid.indexOf(':') >= 0 ? uid.substring(uid.indexOf(':') + 1) : uid;
        if (label.length() > 5) label = label.substring(0, 5);
        fontRendererObj.drawStringWithShadow(label, gx + 2, gy + 1, 0xffff55);
    }

    /** icon file for a scalar resource uid (res_xp = the experience orb art) */
    private static String resIconFile(String uid) {
        if (uid.equals("xp")) return "res_xp.png";
        if (uid.equals("rf")) return "res_rf.png";
        if (uid.equals("eu")) return "res_eu.png";
        if (uid.equals("mana")) return "res_mana.png";
        if (uid.startsWith("aspect:")) return "res_aspect.png";
        return "res_missing.png";
    }

    private void drawResIcon(String uid, int x, int y) {
        this.mc.getTextureManager().bindTexture(
                new ResourceLocation("wheelchair", "textures/items/" + resIconFile(uid)));
        GL11.glColor4f(1F, 1F, 1F, 1F);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.addVertexWithUV(x,      y + 16, this.zLevel, 0.0, 1.0);
        t.addVertexWithUV(x + 16, y + 16, this.zLevel, 1.0, 1.0);
        t.addVertexWithUV(x + 16, y,      this.zLevel, 1.0, 0.0);
        t.addVertexWithUV(x,      y,      this.zLevel, 0.0, 0.0);
        t.draw();
        GL11.glDisable(GL11.GL_BLEND);
    }

    /** water's still sprite 16x16 with a solid RGB tint (energy icons) */
    private void drawTintedStill(String fluidName, int rgb, int x, int y) {
        net.minecraftforge.fluids.Fluid f = FluidRegistry.getFluid(fluidName);
        net.minecraft.util.IIcon icon = f == null ? null : f.getStillIcon();
        if (icon == null) return;
        this.mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        float r = (rgb >> 16 & 255) / 255.0F;
        float g = (rgb >> 8 & 255) / 255.0F;
        float b = (rgb & 255) / 255.0F;
        GL11.glColor4f(r, g, b, 1.0F);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.addVertexWithUV(x,      y + 16, this.zLevel, icon.getMinU(), icon.getMaxV());
        t.addVertexWithUV(x + 16, y + 16, this.zLevel, icon.getMaxU(), icon.getMaxV());
        t.addVertexWithUV(x + 16, y,      this.zLevel, icon.getMaxU(), icon.getMinV());
        t.addVertexWithUV(x,      y,      this.zLevel, icon.getMinU(), icon.getMinV());
        t.draw();
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1F, 1F, 1F, 1F);
    }

    /** draw a stored fluid/gas as itself: bind the block atlas and blit the
     *  fluid's still-icon 16x16 at screen coords (old-tree pattern, verbatim) */
    private void drawFluidIcon(String uid, int x, int y) {
        net.minecraftforge.fluids.Fluid f = FluidRegistry.getFluid(uid.substring(6));
        net.minecraft.util.IIcon icon = f == null ? null : f.getStillIcon();
        if (icon == null) return;
        this.mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        GL11.glColor4f(1F, 1F, 1F, 1F);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.addVertexWithUV(x,      y + 16, this.zLevel, icon.getMinU(), icon.getMaxV());
        t.addVertexWithUV(x + 16, y + 16, this.zLevel, icon.getMaxU(), icon.getMaxV());
        t.addVertexWithUV(x + 16, y,      this.zLevel, icon.getMaxU(), icon.getMinV());
        t.addVertexWithUV(x,      y,      this.zLevel, icon.getMinU(), icon.getMinV());
        t.draw();
        GL11.glDisable(GL11.GL_BLEND);
    }

    private void drawScrollbar() {
        if (view.size() <= rows * COLS) return; // grid not full: hide the scrollbar entirely
        int x = guiLeft + SB_X, y = guiTop + SB_Y;
        drawRect(x, y, x + SB_W, y + sbH, COL_SLOT);
        bevel(x, y, SB_W, sbH, COL_SLOT_DK, COL_BEV_HI, 2);
        int track = sbH - 4;
        int maxScroll = view.size() - rows * COLS;
        int ky = y + 2 + (track - KNOB_H) * scroll / maxScroll;
        drawRect(x + 2, ky, x + 2 + KNOB_W, ky + KNOB_H, COL_BG);
        bevel(x + 2, ky, KNOB_W, KNOB_H, COL_BEV_HI, COL_BEV_LO, 1);
    }

    /** badge format: full digits up to 4 characters; then K (integers, they
     *  start at 10K), M/G with one decimal below 10x the unit ("1.2M", "2.1G") */
    private static String fmt(long n) {
        if (n >= 1000000000L) return String.format(java.util.Locale.US, "%.1fG", n / 1000000000.0);
        if (n >= 10000000L) return (n / 1000000L) + "M";
        if (n >= 1000000L) return String.format(java.util.Locale.US, "%.1fM", n / 1000000.0);
        if (n >= 10000L) return (n / 1000L) + "K";
        return String.valueOf(n);
    }

    private static String fmtShort(long n) {
        if (n >= 1000000000L) return (n / 1000000000L) + "G";
        if (n >= 1000000L) return (n / 1000000L) + "M";
        if (n >= 1000L) return (n / 1000L) + "K";
        return String.valueOf(n);
    }

    // ================= foreground layer (inside translate(guiLeft, guiTop)) =================

    @Override
    protected void drawGuiContainerForegroundLayer(int mx, int my) {
        // recipe counter: decon mode reads the container's real recipe list
        wheelchair.inventory.ContainerStoragePage c =
                (wheelchair.inventory.ContainerStoragePage) inventorySlots;
        String counter = null;
        wheelchair.inventory.ContainerStoragePage cc =
                (wheelchair.inventory.ContainerStoragePage) inventorySlots;
        if (bandMode == 0) {
            // craft conflict counter, same spot as the decon one (‹ › ↔ arrow gap)
            if (cc.craftMatches.size() > 1) {
                counter = (cc.craftIndex + 1) + "/" + cc.craftMatches.size();
            }
        } else {
            int n = cc.current == null ? 0 : cc.current.size();
            if (n > 1) counter = (cc.deconIndex + 1) + "/" + n; // 1/1 carries no info
        }
        if (counter != null) drawCenteredString(fontRendererObj, counter, 124, matY + 11, 0xff404040);

        // decon variant badges: "2/6" on preview slots offering ore-dict variants
        if (bandMode == 1) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            for (int i = 0; i < 9; i++) {
                int n = c.variantCount(i);
                if (n <= 1) continue;
                Slot s = (Slot) c.inventorySlots.get(
                        wheelchair.inventory.ContainerStoragePage.SLOT_DECON_OUT_FIRST + i);
                if (s.xDisplayPosition < 0) continue;
                String t = c.variantPos(i) + "/" + n;
                GL11.glPushMatrix();
                GL11.glTranslatef(s.xDisplayPosition, s.yDisplayPosition, 0.0F);
                GL11.glScalef(0.75F, 0.75F, 1.0F);
                int w = fontRendererObj.getStringWidth(t);
                fontRendererObj.drawStringWithShadow(t, (int) (16 / 0.75F - w), 9, 0xFFFFFF);
                GL11.glPopMatrix();
            }
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }

        // count badges — old-tree pattern, AE2 AppEngRenderItem digit pass: float
        // above items (zLevel 300, depth off), 0.5-scaled ASCII digits (unicode OFF —
        // the zh_CN font page renders digits thin and blurry), no backing strip
        boolean unicode = fontRendererObj.getUnicodeFlag();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        this.zLevel = 300.0F;
        for (int i = 0; i < rows * COLS; i++) {
            int idx = scroll + i;
            if (idx >= view.size()) break;
            String uid = view.get(idx);
            long n = StorageMirror.count(uid);
            if (n <= 1) continue;
            String badge = fmt(n);
            int col = i % COLS, row = i / COLS;
            // AE2 small-font formula: (x + offset + 16 - strW*scale) / scale, offset -1
            float sc = 0.5F;
            int x = (int) (((float) (GRID_X + col * 18) - 1.0F + 16.0F
                    - this.fontRendererObj.getStringWidth(badge) * sc) / sc);
            int y = (int) (((float) (GRID_Y + row * 18 + 9) - 1.0F + 7.0F * sc) / sc);
            GL11.glPushMatrix();
            GL11.glScalef(sc, sc, 1.0F);
            this.fontRendererObj.setUnicodeFlag(false);
            this.fontRendererObj.drawStringWithShadow(badge, x, y, 0xFFFFFF);
            this.fontRendererObj.setUnicodeFlag(unicode);
            GL11.glPopMatrix();
        }
        this.zLevel = 0.0F;
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        super.drawScreen(mx, my, pt);
    }


    // ================= tooltips =================

    /** grid cells get our own tooltip (items AND resources, stack==null for the latter) */
    @Override
    protected void renderToolTip(ItemStack stack, int x, int y) {
        int idx = gridIndexAt(x, y);
        if (idx >= 0 && idx < view.size()) {
            String uid = view.get(idx);
            List<String> lines = new ArrayList<String>();
            if (StorageMirror.res.containsKey(uid)) {
                lines.add(resourceNameOf(uid));
                Long n = StorageMirror.res.get(uid);
                long v = n == null ? 0L : n.longValue();
                if (uid.startsWith("fluid:") || uid.startsWith("gas:")) {
                    lines.add(fmt(v) + " mB (" + fmt(v / 1000L) + "\u6876)");
                } else {
                    lines.add(fmt(v));
                }
            } else {
                // vanilla tooltip (potion effects / enchants / lore all come along)
                // + the stored count, old-tree pattern
                ItemStack s = StorageMirror.stackOf(uid);
                try {
                    List vanilla = s != null
                            ? s.getTooltip(this.mc.thePlayer, this.mc.gameSettings.advancedItemTooltips)
                            : null;
                    if (vanilla != null) {
                        for (int i = 0; i < vanilla.size() && i < 8; i++) {
                            lines.add(String.valueOf(vanilla.get(i)));
                        }
                    } else {
                        lines.add(uid);
                    }
                } catch (Throwable t) {
                    lines.add(s != null ? s.getDisplayName() : uid);
                }
                lines.add(fmt(StorageMirror.count(uid)));
            }
            this.drawHoveringText(lines, x, y, fontRendererObj);
            return;
        }
        if (stack == null) return; // resource-cell edge: vanilla's renderToolTip NPEs on null
        super.renderToolTip(stack, x, y);
    }

    /** display name for a resource uid: localized fluid/gas name, or a mapped
     *  label for the scalar resources */
    private static String resourceNameOf(String uid) {
        if (uid.startsWith("fluid:") || uid.startsWith("gas:")) {
            net.minecraftforge.fluids.Fluid f = FluidRegistry.getFluid(uid.substring(6));
            return f != null ? f.getLocalizedName() : uid;
        }
        if (uid.equals("xp")) return "\u7ecf\u9a8c\u503c";
        if (uid.equals("rf")) return "RF \u80fd\u91cf";
        if (uid.equals("eu")) return "EU \u80fd\u91cf";
        if (uid.equals("mana")) return "\u9b54\u529b";
        if (uid.startsWith("aspect:")) return "\u6e90\u8d28: " + uid.substring(7);
        return uid;
    }

    // ================= input =================

    private int gridIndexAt(int mx, int my) {
        int x = mx - guiLeft - GRID_X, y = my - guiTop - GRID_Y;
        if (x < 0 || y < 0 || x >= COLS * 18 || y >= rows * 18) return -1;
        int col = x / 18, row = y / 18;
        return scroll + row * COLS + col;
    }

    private boolean inRect(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) {
        // rail tabs: 附魔/能源 open their pages; the storage tab is this page
        for (int i = 0; i < RAIL_TABS.length; i++) {
            if (i != 0 && inRect(mx, my, guiLeft + RAIL_X + 1, guiTop + 6 + i * 17, 26, 15)) {
                net.minecraft.client.gui.ScaledResolution sr =
                        new net.minecraft.client.gui.ScaledResolution(this.mc, this.mc.displayWidth, this.mc.displayHeight);
                wheelchair.WheelchairMod.network.sendToServer(
                        new wheelchair.network.PacketOpenTerminal(i == 1 ? 4 : (i == 2 ? 6 : 7),
                                wheelchair.client.GuiStoragePage.rowsFor(sr.getScaledHeight())));
                return;
            }
        }
        int py = ySize - 36;
        for (int i = 0; i < RAIL_PIN.length; i++) {
            if (inRect(mx, my, guiLeft + RAIL_X + 1, guiTop + py + i * 17, 26, 15)) {
                net.minecraft.client.gui.ScaledResolution sr = new net.minecraft.client.gui.ScaledResolution(
                        this.mc, this.mc.displayWidth, this.mc.displayHeight);
                wheelchair.WheelchairMod.network.sendToServer(
                        new wheelchair.network.PacketOpenTerminal(5,
                                wheelchair.client.GuiStoragePage.rowsFor(sr.getScaledHeight())));
                return; // 设置: none yet
            }
        }
        int sx = guiLeft + SB_X;
        if (inRect(mx, my, sx, guiTop + SB_Y, SB_W, sbH)) {
            if (view.size() > rows * COLS) {
                draggingKnob = true;
                dragY0 = my;
                scroll0 = scroll;
            }
            return;
        }
        if (inRect(mx, my, guiLeft + TYPE_X, guiTop + LBL_Y, TYPE_W, LBL_H)) {
            typeSel = (typeSel + 1) % 3;
            refresh();
            return;
        }
        if (inRect(mx, my, guiLeft + NBT_X, guiTop + LBL_Y, NBT_W, LBL_H)) {
            nbtSel = (nbtSel + 1) % 3;
            refresh();
            return;
        }
        if (inRect(mx, my, guiLeft + SORT_X, guiTop + SORT_Y, SORT_W, SORT_H)) {
            sortIdx = (sortIdx + 1) % SORTS.length;
            refresh();
            return;
        }
        if (inRect(mx, my, guiLeft + SORTD_X, guiTop + SORT_Y, SORTD_W, SORT_H)) {
            asc = !asc;
            refresh();
            return;
        }
        if (inRect(mx, my, guiLeft + CHIP1_X, guiTop + chipY, CHIP_W, CHIP_H)) { bandMode = 0; return; }
        if (inRect(mx, my, guiLeft + CHIP2_X, guiTop + chipY, CHIP_W, CHIP_H)) { bandMode = 1; return; }
        if (inRect(mx, my, guiLeft + NAVL_X, guiTop + matY, NAV_W, NAV_H)) {
            if (bandMode == 1) {
                ((wheelchair.inventory.ContainerStoragePage) inventorySlots).cycle(-1); // local predict
                sendAction(6, "", -1, false);
            } else {
                ((wheelchair.inventory.ContainerStoragePage) inventorySlots).cycleCraft(-1); // local predict
                sendAction(8, "", -1, false);
            }
            return;
        }
        if (inRect(mx, my, guiLeft + NAVR_X, guiTop + matY, NAV_W, NAV_H)) {
            if (bandMode == 1) {
                ((wheelchair.inventory.ContainerStoragePage) inventorySlots).cycle(1);
                sendAction(6, "", 1, false);
            } else {
                ((wheelchair.inventory.ContainerStoragePage) inventorySlots).cycleCraft(1);
                sendAction(8, "", 1, false);
            }
            return;
        }

        // grid cells: ALWAYS intercept (vanilla must never emit a click for a
        // virtual slot — the server container does not know them)
        int idx = gridIndexAt(mx, my);
        if (idx >= 0) {
            // SPACE + left-click = deposit the whole inventory into the network
            if (button == 0 && org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_SPACE)) {
                sendAction(4, "", 0, false);
                return;
            }
            if (idx < view.size()) {
                String uid = view.get(idx);
                if (StorageMirror.res.containsKey(uid)) {
                    // fluid cell: with a container the server fills/pours it; with an
                    // empty hand it pulls an empty one from the inventory/network
                    if (uid.startsWith("fluid:") && (thePlayerHasCursorStack() || button == 0)) {
                        sendAction(3, uid, 0, false);
                    }
                    return;
                }
                boolean shift = isShiftKeyDown();
                boolean ctrl = isCtrlKeyDown();
                if (thePlayerHasCursorStack()) {
                    if (button == 1) {
                        sendAction(5, "", 0, false); // right: pour the container's contents in
                    } else {
                        sendAction(1, "", 0, false); // left: deposit the cursor stack itself
                    }
                } else if (shift) {
                    sendAction(0, uid, -1, true);   // shift: a full stack straight to the inventory
                } else if (button == 0) {
                    sendAction(0, uid, ctrl ? 1 : -1, false); // left = a stack (item's own limit), ctrl+left = one
                } else if (button == 1) {
                    sendAction(0, uid, -2, false); // right = half a stack
                }
            } else if (thePlayerHasCursorStack()) {
                sendAction(button == 1 ? 5 : 1, "", 0, false); // empty cell: pour contents or deposit the stack
            }
            return;
        }
        search.mouseClicked(mx, my, button);
        if (!inRect(mx, my, guiLeft + SEARCH_X, guiTop + SEARCH_Y, SEARCH_W, SEARCH_H)) {
            super.mouseClicked(mx, my, button);
        }
    }

    /** last-line guard: ANY vanilla path (drag release, hotbar swap, ...) that tries
     *  to click a virtual slot is dropped here — the server does not know these slots */
    @Override
    protected void handleMouseClick(Slot slot, int slotId, int mouseButton, int type) {
        if (slot instanceof SlotVirtual) {
            return;
        }
        super.handleMouseClick(slot, slotId, mouseButton, type);
    }

    private boolean thePlayerHasCursorStack() {
        return mc.thePlayer != null && mc.thePlayer.inventory.getItemStack() != null;
    }

    private void sendAction(int type, String uid, int amount, boolean toInv) {
        wheelchair.WheelchairMod.network.sendToServer(new PacketStorageAction(type, uid, amount, toInv));
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d != 0 && view.size() > rows * COLS) {
            scroll += d > 0 ? -COLS : COLS; // one row per notch
            clampScroll();
        }
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long held) {
        if (draggingKnob) {
            int track = sbH - 4;
            int maxScroll = view.size() - rows * COLS;
            scroll = scroll0 + (my - dragY0) * maxScroll / Math.max(1, track - KNOB_H);
            clampScroll();
            return;
        }
        // never let vanilla start/continue a drag (quick-craft) over virtual cells
        if (gridIndexAt(mx, my) >= 0) return;
        super.mouseClickMove(mx, my, button, held);
    }

    @Override
    protected void mouseMovedOrUp(int mx, int my, int which) {
        draggingKnob = false;
        super.mouseMovedOrUp(mx, my, which);
    }

    @Override
    protected void keyTyped(char c, int key) {
        if (search.isFocused()) {
            if (key == 1) { // ESC: leave the field first
                search.setFocused(false);
                return;
            }
            if (key == this.mc.gameSettings.keyBindInventory.getKeyCode()) {
                search.setFocused(false);
                return;
            }
            if (search.textboxKeyTyped(c, key)) {
                refresh();
                return;
            }
        }
        super.keyTyped(c, key);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
