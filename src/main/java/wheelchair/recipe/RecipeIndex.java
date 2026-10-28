package wheelchair.recipe;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.lang.reflect.Field;
import net.minecraftforge.oredict.OreDictionary;
import java.util.ArrayList;
import java.util.List;

public class RecipeIndex {
    public static final Multimap<String, DeconRecipe> map = HashMultimap.create();
    public static int skipped = 0;
    public static int generic = 0;

    public static class DeconRecipe {
        public final ItemStack result;
        public final ItemStack[] ingredients;
        /** per-slot ore-dictionary variants (null entry = single fixed item);
         *  lets the decon table cycle and yield EVERY registered variant */
        public final List<ItemStack>[] variants;
        public final int width, height;

        @SuppressWarnings("unchecked")
        DeconRecipe(ItemStack result, ItemStack[] ingredients, List<ItemStack>[] variants, int width, int height) {
            this.result = result;
            this.ingredients = ingredients;
            this.variants = variants == null ? new List[ingredients.length] : variants;
            this.width = width;
            this.height = height;
        }
    }

    /** one grid slot after normalization: fixed stack OR ore-dictionary variant list */
    private static class Norm {
        final ItemStack primary;
        final List<ItemStack> variants; // null unless >1 distinct item+meta
        Norm(ItemStack primary, List<ItemStack> variants) {
            this.primary = primary; this.variants = variants;
        }
    }

    public static void build() {
        map.clear();
        skipped = 0;
        generic = 0;
        List list = CraftingManager.getInstance().getRecipeList();
        for (Object o : list) {
            try {
                IRecipe r = (IRecipe) o;
                ItemStack out = r.getRecipeOutput();
                if (out == null || out.getItem() == null) { skipped++; continue; }

                if (r instanceof ShapedRecipes) {
                    ShapedRecipes s = (ShapedRecipes) r;
                    add(out, normalize(s.recipeItems), s.recipeWidth, s.recipeHeight);
                } else if (r instanceof ShapelessRecipes) {
                    add(out, normalize(((ShapelessRecipes) r).recipeItems.toArray()), 1, 1);
                } else if (isShapedOre(r)) {
                    ShapedOreRecipe s = (ShapedOreRecipe) r;
                    int[] wh = shapedOreSize(s);
                    add(out, normalize(s.getInput()), wh[0], wh[1]);
                } else if (r instanceof ShapelessOreRecipe) {
                    add(out, normalize(((ShapelessOreRecipe) r).getInput().toArray()), 1, 1);
                } else {
                    // generic mod recipes (IC2, and any mod implementing IRecipe directly):
                    // probe fields reflectively
                    Object[] probed = probeGeneric(r);
                    if (probed != null) generic++;
                    if (probed != null) {
                        add(out, normalize(probed), 3, (probed.length + 2) / 3);
                    } else {
                        skipped++;
                    }
                }
            } catch (Throwable t) {
                skipped++;
            }
        }
    }

    /** ShapedOreRecipe subclasses (many mods extend it) */
    private static boolean isShapedOre(IRecipe r) {
        Class<?> c = r.getClass();
        while (c != null) {
            if (c == ShapedOreRecipe.class) return true;
            c = c.getSuperclass();
        }
        return false;
    }

    /**
     * best-effort extraction for unknown IRecipe implementations: finds an
     * ItemStack[]/Object[]/List field (grid shape is not needed — decon repacks).
     * @return the raw ingredient objects, or null
     */
    private static Object[] probeGeneric(IRecipe r) {
        try {
            for (Class<?> k = r.getClass(); k != null && k != Object.class; k = k.getSuperclass()) {
                for (Field f : k.getDeclaredFields()) {
                    // output/result-named fields are usually the PRODUCT or a cache,
                    // not ingredients — taking the first array/List field hit one of
                    // those, making some mod recipes deconstruct into wrong materials
                    String fn = f.getName().toLowerCase();
                    if (fn.contains("output") || fn.contains("result")) continue;
                    f.setAccessible(true);
                    Object v;
                    try { v = f.get(r); } catch (Exception ignored) { continue; }
                    if (v == null) continue;
                    if (v instanceof ItemStack[]) return (ItemStack[]) v;
                    if (v instanceof Object[]) return (Object[]) v;
                    if (v instanceof List) return ((List) v).toArray();
                }
            }
        } catch (Throwable t) {
            return null;
        }
        return null;
    }
    private static int[] shapedOreSize(ShapedOreRecipe r) {
        try {
            Field w = ShapedOreRecipe.class.getDeclaredField("width");
            Field h = ShapedOreRecipe.class.getDeclaredField("height");
            w.setAccessible(true);
            h.setAccessible(true);
            return new int[]{w.getInt(r), h.getInt(r)};
        } catch (Exception e) {
            return new int[]{1, 1};
        }
    }

    /** normalize raw recipe objects into per-slot Norms (fixed stack or ore variants) */
    private static Norm[] normalize(Object[] in) {
        Norm[] out = new Norm[in.length];
        for (int i = 0; i < in.length; i++) {
            Object o = in[i];
            if (o == null) { out[i] = new Norm(null, null); continue; }
            if (o instanceof ItemStack) {
                ItemStack is = (ItemStack) o;
                if (is.getItem() != null && is.getItemDamage() == OreDictionary.WILDCARD_VALUE) {
                    // wildcard meta (vanilla boat: 5x planks@32767): enumerate every
                    // subtype so the decon band can cycle birch/jungle/... planks too
                    List<ItemStack> subs = new ArrayList<ItemStack>();
                    // tabAllSearch: vanilla getSubBlocks ignores a null tab, which
                    // is why the boat recipe only ever saw one plank type
                    try {
                        is.getItem().getSubItems(is.getItem(),
                                net.minecraft.creativetab.CreativeTabs.tabAllSearch, subs);
                    } catch (Throwable t) { subs.clear(); }
                    out[i] = new Norm(firstValid(subs), distinctVariants(subs));
                } else {
                    out[i] = new Norm(is, null);
                }
                continue;
            }
            List ores = null;
            if (o instanceof List) ores = (List) o;
            else if (o instanceof String) ores = OreDictionary.getOres((String) o); // IC2 AdvRecipe style
            if (ores != null) {
                out[i] = new Norm(firstValid(ores), distinctVariants(ores));
                continue;
            }
            out[i] = new Norm(null, null);
        }
        return out;
    }

    private static ItemStack firstValid(List ores) {
        for (Object o : ores) {
            if (o instanceof ItemStack && ((ItemStack) o).getItem() != null) return (ItemStack) o;
        }
        return null;
    }

    /** deduped (item+meta) candidate list; null unless more than one distinct variant */
    private static List<ItemStack> distinctVariants(List ores) {
        List<ItemStack> vs = new ArrayList<ItemStack>();
        java.util.Set<String> seen = new java.util.HashSet<String>();
        for (Object o : ores) {
            if (!(o instanceof ItemStack)) continue;
            ItemStack s = (ItemStack) o;
            if (s.getItem() == null) continue;
            String k = s.getItem() + "@" + s.getItemDamage();
            if (seen.add(k)) vs.add(s.copy());
        }
        return vs.size() > 1 ? vs : null;
    }

    private static void add(ItemStack result, Norm[] norms, int w, int h) {
        if (norms == null || w < 1 || h < 1) { skipped++; return; }
        int len = 0;
        for (Norm n : norms) if (n != null) len++;
        if (len == 0) { skipped++; return; }
        if (w > 3 || w * h < norms.length) { // repack packed/shapeless/too-wide into a 3-col grid
            w = 3;
            h = (norms.length + 2) / 3;
        }
        if (h > 3) { skipped++; return; }
        List<ItemStack>[] variants = new List[norms.length];
        for (int i = 0; i < norms.length; i++) {
            Norm n = norms[i];
            if (n != null && n.variants != null
                    && n.primary != null
                    && n.primary.getItem() != null) {
                variants[i] = n.variants;
            }
        }
        map.put(itemKey(result), new DeconRecipe(result, flatten(norms), variants, w, h));
    }

    private static ItemStack[] flatten(Norm[] norms) {
        ItemStack[] out = new ItemStack[norms.length];
        for (int i = 0; i < norms.length; i++) {
            // COPY: primaries may be live references into vanilla's recipe list /
            // OreDictionary caches — a future consumer mutating one would corrupt
            // the crafting system globally
            out[i] = norms[i] == null || norms[i].primary == null ? null : norms[i].primary.copy();
        }
        return out;
    }

    private static String itemKey(ItemStack s) {
        return s.getItem() + "@" + s.getItemDamage();
    }

    public static List<DeconRecipe> getRecipes(ItemStack stack) {
        List<DeconRecipe> res = new ArrayList<DeconRecipe>();
        if (stack == null || stack.getItem() == null) return res;
        for (DeconRecipe r : new ArrayList<DeconRecipe>(map.get(itemKey(stack)))) {
            if (r.result.stackSize > stack.stackSize) continue;
            if (!stack.isItemStackDamageable()
                    && r.result.getItemDamage() != stack.getItemDamage()
                    && r.result.getItemDamage() != 32767) continue;
            if (r.ingredients == null) continue;
            boolean selfRef = false;
            for (ItemStack ing : r.ingredients)
                if (ing != null && ing.getItem() == r.result.getItem()) { selfRef = true; break; }
            if (selfRef) continue;
            res.add(r);
        }
        return res;
    }
}