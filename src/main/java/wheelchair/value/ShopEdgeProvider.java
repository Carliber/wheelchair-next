package wheelchair.value;

import net.minecraft.item.ItemStack;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * NPC-shop edge extractor. Convention-based: tries a list of known shop-recipe classes
 * (mana metal's ShamanStoreRecipes, etc.), reads Map<costStack, ...> fields,
 * and prices the goods: price = cost EMC (upper bound / unpriced fill).
 */
public class ShopEdgeProvider {
    /** candidate classes: "class name -> field holding the recipe map" */
    private static final String[][] CANDIDATES = {
            {"project.studio.manametalmod.items.crafting.ShamanStoreRecipes", "list"},
    };

    public static int apply() {
        int edges = 0;
        for (String[] cand : CANDIDATES) {
            try {
                Class<?> cls = Class.forName(cand[0]);
                Object inst = null;
                try {
                    Field instF = cls.getField("instance");
                    inst = instF.get(null);
                } catch (NoSuchFieldException ignored) {
                    inst = cls.newInstance(); // maybe static-only methods with instance state
                }
                Field mapF = null;
                for (Field f : cls.getDeclaredFields()) {
                    if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                        if (inst == null) continue;
                    }
                    if (Map.class.isAssignableFrom(f.getType())) { mapF = f; break; }
                }
                if (mapF == null && inst != null) {
                    for (Field f : cls.getDeclaredFields()) {
                        if (Map.class.isAssignableFrom(f.getType())) { mapF = f; break; }
                    }
                }
                if (mapF == null) continue;
                mapF.setAccessible(true);
                Object mapObj = inst != null ? mapF.get(inst) : mapF.get(null);
                if (!(mapObj instanceof Map)) continue;
                edges += process((Map) mapObj);
            } catch (Throwable ignored) {
            }
        }
        return edges;
    }

    @SuppressWarnings("rawtypes")
    private static int process(Map shopMap) {
        int edges = 0;
        for (Object eo : shopMap.entrySet()) {
            Map.Entry e = (Map.Entry) eo;
            if (!(e.getKey() instanceof ItemStack)) continue;
            ItemStack cost = (ItemStack) e.getKey();

            // depth-limited scan for product ItemStacks inside the value
            Set<ItemStack> products = new HashSet<ItemStack>();
            collectStacks(e.getValue(), products, 2);
            if (products.size() != 1) continue; // only unambiguous single-product offers

            ItemStack goods = products.iterator().next();
            long costEmc = PriceRegistry.getPrice(cost);
            long per = costEmc / Math.max(1, goods.stackSize);
            if (per <= 0) per = 1;

            String k = PriceRegistry.key(goods.getItem());
            // SEED AUTHORITY: seeded keys are never touched by shop edges — a shop
            // selling diamonds for cobblestone must not crater the seeded diamond price
            if (PriceRegistry.isSeeded(k)) continue;
            Long cur = PriceRegistry.view().get(k);
            if (cur == null) {
                PriceRegistry.view().put(k, per); // unpriced -> priced by shop
                edges++;
            } else if (per < cur.longValue()) {
                PriceRegistry.view().put(k, per); // shop cheaper -> cap (upper bound)
                edges++;
            }
        }
        return edges;
    }

    /** collect ItemStacks from an opaque structure up to depth levels */
    @SuppressWarnings("rawtypes")
    private static void collectStacks(Object o, Set<ItemStack> out, int depth) {
        if (o == null || depth < 0) return;
        if (o instanceof ItemStack) {
            ItemStack s = (ItemStack) o;
            if (s.getItem() != null) out.add(s);
            return;
        }
        if (o instanceof Map) {
            for (Object eo : ((Map) o).entrySet()) {
                Map.Entry e = (Map.Entry) eo;
                collectStacks(e.getKey(), out, depth - 1);
                collectStacks(e.getValue(), out, depth - 1);
            }
            return;
        }
        if (o instanceof Iterable) {
            for (Object x : (Iterable) o) collectStacks(x, out, depth - 1);
        }
    }
}