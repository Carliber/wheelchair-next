package wheelchair.value;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraftforge.oredict.OreDictionary;
import wheelchair.recipe.RecipeIndex;
import wheelchair.recipe.RecipeIndex.DeconRecipe;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Equivalent-Exchange style pricing:
 * 1. seed table for source materials
 * 2. fixed-point min-propagation over crafting + furnace (+IC2 machine) edges
 * 3. keyword heuristics, then floor price 1 - nothing is unpriced.
 */
public class PriceRegistry {
    /** key: "itemregistryname" (damage-agnostic) */
    private static final Map<String, Long> prices = new HashMap<String, Long>();
    private static final java.util.Set<String> seeded = new java.util.HashSet<String>();
    private static final Map<String, Item> keyIndex = new HashMap<String, Item>();
    private static final Map<String, String> displays = new HashMap<String, String>();
    private static boolean built = false;

    public static long getPrice(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return 0;
        Long p = prices.get(key(stack));
        long v = p == null ? 1L : p.longValue();
        return Math.max(1L, v * Math.max(1, stack.stackSize));
    }

    public static long getUnitPrice(Item item) {
        Long p = prices.get(key(item));
        return p == null ? 1L : Math.max(1L, p.longValue());
    }

    public static boolean isPriced(Item item) {
        return prices.containsKey(key(item));
    }

    public static Map<String, Long> view() { return prices; }

    public static String key(ItemStack s) { return key(s.getItem()); }

    public static String key(Item i) {
        String n = null;
        try { n = (String) Item.itemRegistry.getNameForObject(i); } catch (Throwable ignored) {}
        if (n == null) {
            GameRegistry.UniqueIdentifier u = GameRegistry.findUniqueIdentifierFor(i);
            n = u == null ? null : u.modId + ":" + u.name;
        }
        return n == null ? String.valueOf(System.identityHashCode(i)) : n;
    }

    public static void build() {
        built = false;
        prices.clear();
        seeded.clear();   // rebuilds must not keep stale seed memberships
        keyIndex.clear();
        displays.clear();

        // ---- 1. seeds ----
        seed("cobblestone", 1); seed("dirt", 1); seed("sand", 1); seed("gravel", 1);
        seed("netherrack", 1); seed("snowball", 1); seed("glass", 1);
        seed("log", 32); seed("wood", 32);
        seed("coal", 128); seed("charcoal", 32);
        seed("redstone", 64); seed("lapis", 864);
        seed("diamond", 8192); seed("emerald", 16384); seed("quartz", 256);
        seed("obsidian", 64); seed("enderpearl", 1024); seed("blaze_rod", 1536);
        seed("gunpowder", 192); seed("glowstone_dust", 96); seed("string", 12);
        seed("leather", 48); seed("feather", 6); seed("flint", 4); seed("wool", 48);
        seed("bone", 24); seed("slime_ball", 24); seed("sugar", 8);
        seed("wheat", 16); seed("apple", 64); seed("egg", 32);
        // vanilla intermediates: keep recipe chains unbroken
        seed("planks", 8); seed("stick", 4); seed("crafting_table", 8);
        seed("furnace", 8); seed("chest", 8); seed("torch", 4);
        seed("paper", 24); seed("book", 48); seed("bread", 24);
        seed("potato", 16); seed("carrot", 16); seed("wheat_seeds", 4);
        seed("beef", 32); seed("porkchop", 32); seed("chicken", 16); seed("mutton", 16);
        seed("glass_bottle", 8); seed("bucket", 256); seed("stone", 1);
        seed("sandstone", 2); seed("clay", 4); seed("brick_block", 32);
        seed("iron_block", 2304); seed("gold_block", 18432); seed("diamond_block", 73728);
        seed("rubber", 32); seed("sticky_resin", 64);
        oreSeed("iron", 256); oreSeed("copper", 128); oreSeed("tin", 192);
        oreSeed("gold", 2048); oreSeed("silver", 384); oreSeed("lead", 256);
        oreSeed("nickel", 320); oreSeed("aluminum", 96); oreSeed("aluminium", 96);
        oreSeed("tungsten", 512); oreSeed("uranium", 1024); oreSeed("chrome", 768);
        oreSeed("platinum", 4096); oreSeed("mithril", 1024);

        // ---- 2. fixed-point propagation over edges ----
        List<IRecipe> recipes = CraftingManager.getInstance().getRecipeList();
        for (int pass = 0; pass < 12; pass++) {
            boolean changed = false;
            changed |= propagateCrafting(recipes);
            changed |= propagateFurnace();
            if (!changed) break;
        }

        // ---- 3. IC2 machine maps (optional, reflection-tolerant) ----
        try { ic2Edges(); } catch (Throwable ignored) {}
        try { ShopEdgeProvider.apply(); } catch (Throwable ignored) {}
        

        // another pass so machine edges settle
        for (int pass = 0; pass < 6; pass++) {
            if (!propagateCrafting(recipes) & !propagateFurnace()) break;
        }

        // ---- 4. fallback: everything gets a price ----
        for (Object o : Item.itemRegistry) {
            Item it = (Item) o;
            String k = key(it);
            if (!prices.containsKey(k)) {
                long v = heuristicPrice(k);
                prices.put(k, v);
            }
            keyIndex.put(k, it);
            try { displays.put(k, new ItemStack(it).getDisplayName()); } catch (Throwable ignored) {}
        }
        built = true;
    }

    public static boolean isBuilt() { return built; }

    /** seeded keys are authoritative: no propagation layer may override them
     *  (user decision 2026-09-30 — shop edges included) */
    public static boolean isSeeded(String k) { return seeded.contains(k); }

    public static Item getItem(String key) { return keyIndex.get(key); }
    public static String getDisplay(String key) { return displays.get(key); }

    private static void seed(String frag, long v) {
        for (Object o : Item.itemRegistry) {
            Item it = (Item) o;
            String k = key(it).toLowerCase();
            if (k.contains(frag)) setSeed(k, v);
        }
    }

    /** oreSeed prices any item whose name mentions the metal in ingot/ore/dust/plate form */
    private static void oreSeed(String metal, long v) {
        for (Object o : Item.itemRegistry) {
            Item it = (Item) o;
            String k = key(it).toLowerCase();
            if (k.contains(metal)) {
                long mult = 1;
                if (k.contains("ore")) mult = 1;
                else if (k.contains("nugget")) mult = v / 9L;
                else if (k.contains("dust") || k.contains("crushed")) mult = v / 2L;
                else if (k.contains("block")) mult = v * 9L;
                else mult = v; // ingot / plate / gem
                setSeed(k, Math.max(1, mult));
            }
        }
    }

    private static void setIfLower(String k, long v) {
        if (seeded.contains(k)) return; // seeds are authoritative
        Long old = prices.get(k);
        if (old == null || v < old.longValue()) prices.put(k, v);
    }

    private static void setSeed(String k, long v) {
        seeded.add(k);
        prices.put(k, v);
    }

    private static boolean propagateCrafting(List<IRecipe> recipes) {
        boolean changed = false;
        for (IRecipe r : recipes) {
            try {
                ItemStack out = r.getRecipeOutput();
                if (out == null || out.getItem() == null) continue;
                DeconRecipe dr = lookup(r, out);
                if (dr == null || dr.ingredients == null) continue;
                long sum = 0, maxSingle = 0; boolean ok = true;
                for (ItemStack ing : dr.ingredients) {
                    if (ing == null) continue;
                    Long p = prices.get(key(ing.getItem()));
                    if (p == null) { ok = false; break; }
                    sum += p.longValue() * ing.stackSize;
                    long unitVal = p.longValue() * ing.stackSize;
                    if (unitVal > maxSingle) maxSingle = unitVal;
                }
                if (!ok) continue;
                long cand = Math.max(sum, maxSingle) / Math.max(1, out.stackSize);
                if (seeded.contains(key(out.getItem()))) continue;
                if (cand > 0) { setIfLower(key(out.getItem()), cand); changed = true; }
            } catch (Throwable ignored) {}
        }
        return changed;
    }

    private static DeconRecipe lookup(IRecipe r, ItemStack out) {
        List<DeconRecipe> l = RecipeIndex.getRecipes(out);
        for (DeconRecipe d : l) {
            if (d.result.getItem() == out.getItem()
                    && (d.result.getItemDamage() == out.getItemDamage() || out.isItemStackDamageable())) {
                long cand = 0; boolean ok = true;
                for (ItemStack ing : d.ingredients) {
                    if (ing == null) continue;
                    Long p = prices.get(key(ing.getItem()));
                    if (p == null) { ok = false; break; }
                    cand += p.longValue() * ing.stackSize;
                }
                if (ok) return d;
            }
        }
        // fallback: only accept a recipe whose OUTPUT actually matches what we're pricing
        for (DeconRecipe d : l) {
            if (d.result.getItem() == out.getItem()) return d;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static boolean propagateFurnace() {
        boolean changed = false;
        try {
            Map<ItemStack, ItemStack> map = FurnaceRecipes.smelting().getSmeltingList();
            for (Map.Entry<ItemStack, ItemStack> e : map.entrySet()) {
                ItemStack in = e.getKey(), out = e.getValue();
                if (in == null || in.getItem() == null || out == null || out.getItem() == null) continue;
                Long pin = prices.get(key(in.getItem()));
                if (pin == null) continue;
                String ok2 = key(out.getItem());
                if (prices.containsKey(ok2)) continue; // never undercut
                long cand = pin.longValue() / Math.max(1, out.stackSize);
                if (cand > 0) { prices.put(ok2, cand); changed = true; }
            }
        } catch (Throwable ignored) {}
        return changed;
    }

    /** IC2 machine recipe maps via reflection on ic2.api.recipe.Recipes */
    @SuppressWarnings("unchecked")
    private static void ic2Edges() throws Exception {
        if (!Loader.isModLoaded("IC2")) return;
        Class<?> cls = Class.forName("ic2.api.recipe.Recipes");
        for (Field f : cls.getDeclaredFields()) {
            if (!f.getName().equals("macerator") && !f.getName().equals("compressor")
                    && !f.getName().equals("extractor")) continue;
            Object mapObj = f.get(null);
            if (!(mapObj instanceof Map)) continue;
            for (Object eo : ((Map) mapObj).entrySet()) {
                Map.Entry e = (Map.Entry) eo;
                List<ItemStack> ins = null;
                try { ins = (List<ItemStack>) e.getKey().getClass().getMethod("getItems").invoke(e.getKey()); }
                catch (Throwable ignored) { continue; }
                Object output = e.getValue();
                List<ItemStack> outs = null;
                try { outs = (List<ItemStack>) output.getClass().getMethod("getItems").invoke(output); }
                catch (Throwable ignored) { continue; }
                if (ins == null || ins.isEmpty() || outs == null || outs.size() != 1) continue; // single-output only
                ItemStack in = ins.get(0), out = outs.get(0);
                if (in == null || out == null || in.getItem() == null || out.getItem() == null) continue;
                Long pin = prices.get(key(in.getItem()));
                if (pin == null) continue;
                long cand = pin.longValue() * Math.max(1, in.stackSize) / Math.max(1, out.stackSize);
                String k2 = key(out.getItem());
                if (prices.containsKey(k2)) continue; // never undercut
                if (cand > 0) prices.put(k2, cand);
            }
        }
    }

    private static long heuristicPrice(String k) {
        String n = k.toLowerCase();
        if (n.contains("nugget")) return 16;
        if (n.contains("crushed") || n.contains("dust") || n.contains("dirty")) return 64;
        if (n.contains("plate") || n.contains("ingot") || n.contains("gem")) return 128;
        if (n.contains("ore")) return 256;
        if (n.contains("seed") || n.contains("food") || n.contains("berry")) return 8;
        if (n.contains("sword") || n.contains("pickaxe") || n.contains("axe") || n.contains("shovel")) return 384;
        if (n.contains("helmet") || n.contains("chestplate") || n.contains("leggings") || n.contains("boots")) return 768;
        return 1; // floor: everything is priced
    }
}
