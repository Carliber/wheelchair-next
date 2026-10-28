package wheelchair.value;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

/**
 * 1.7.10 experience arithmetic shared by the enchant page (GUI read-side) and
 * PacketEnchAction (server write-side).
 *
 * CURVE FIX (user-reported bug): the old tree's xpToNextLevel used the 1.8+
 * curve (2L+7 / 37+(L-15)*5 / 112+(L-30)*9). 1.7.10's real curve is
 * EntityPlayer.xpBarCap() (source-verified): 17 below level 15,
 * 17+(L-15)*3 in 15..29, 62+(L-30)*7 at 30+. Every conversion now uses THIS
 * curve — deposit/withdraw totals and bar fractions match what the game
 * actually charges, so one click moves exactly one level.
 *
 * experienceTotal is UNRELIABLE (only addExperience maintains it; /xp L never
 * does), so totals are always recomputed from level + bar.
 */
public final class XpMath {
    private XpMath() {}

    /** vanilla 1.7.10 EntityPlayer.xpBarCap() curve (static copy for sums) */
    public static int barCap(int level) {
        return level >= 30 ? 62 + (level - 30) * 7
             : level >= 15 ? 17 + (level - 15) * 3
             : 17;
    }

    /** total xp points the player currently holds (levels sum + bar fraction) */
    public static long totalXpOf(EntityPlayer p) {
        long total = 0;
        for (int l = 0; l < p.experienceLevel; l++) total += barCap(l);
        total += (long) (p.experience * p.xpBarCap());
        return total;
    }

    /** deposit the whole bar (levels + fraction) into storage; returns points stored */
    public static long depositAll(EntityPlayerMP p, StorageData st) {
        long total = totalXpOf(p);
        if (total <= 0) return 0;
        p.experienceLevel = 0;
        p.experience = 0.0F;
        p.experienceTotal = 0;
        st.addRes("xp", total);
        return total;
    }

    /**
     * Deposit exactly ONE LEVEL: L -> L-1, banking the L-1 level's cap. The bar
     * FRACTION is kept untouched — one click banks exactly one level, never
     * eats the partial progress. At level 0 only the fraction can be banked.
     */
    public static long depositOneLevel(EntityPlayerMP p, StorageData st) {
        if (p.experienceLevel > 0) {
            int gain = barCap(p.experienceLevel - 1);
            p.experienceLevel--;               // do NOT touch p.experience here
            p.experienceTotal = Math.max(0, p.experienceTotal - gain);
            st.addRes("xp", gain);
            return gain;
        }
        if (p.experience > 0.0F) {
            int gain = (int) (p.experience * p.xpBarCap());
            p.experience = 0.0F;
            p.experienceTotal = Math.max(0, p.experienceTotal - gain);
            if (gain > 0) st.addRes("xp", gain);
            return gain;
        }
        return 0;
    }

    /**
     * Withdraw `levels` levels from storage into the player: each iteration
     * pays the CURRENT level's real xpBarCap() and feeds it through vanilla
     * addExperience, so the bar fraction survives symmetrically.
     */
    public static int withdrawLevels(EntityPlayerMP p, StorageData st, int levels) {
        int granted = 0;
        for (int i = 0; i < levels; i++) {
            int cost = p.xpBarCap();
            if (cost <= 0 || cost > st.countRes("xp")) break;
            st.takeRes("xp", cost);
            p.addExperience(cost);
            granted++;
        }
        return granted;
    }
}
