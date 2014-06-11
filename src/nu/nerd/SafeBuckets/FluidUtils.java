package nu.nerd.SafeBuckets;

import net.minecraft.server.v1_7_R3.TileEntityDispenser;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.craftbukkit.v1_7_R3.block.CraftDispenser;
import org.bukkit.craftbukkit.v1_7_R3.inventory.CraftInventory;
import org.bukkit.entity.Player;

public final class FluidUtils {
    
    public static final byte safeData = 15;
    private static final String registeredCode = ChatColor.STRIKETHROUGH.toString() + ChatColor.BOLD.toString() + ChatColor.RESET.toString();
    
    private static boolean lock = false;
    
    
    
    public static Material getFlowingMaterial(Material m) {
        if (m == Material.WATER || m == Material.STATIONARY_WATER || m == Material.WATER_BUCKET) {
            return Material.WATER;
        }
        if (m == Material.LAVA || m == Material.STATIONARY_LAVA || m == Material.LAVA_BUCKET) {
            return Material.LAVA;
        }
        return m;
    }

    public static Material getStationaryMaterial(Material m) {
        if (m == Material.WATER || m == Material.STATIONARY_WATER || m == Material.WATER_BUCKET) {
            return Material.STATIONARY_WATER;
        }
        if (m == Material.LAVA || m == Material.STATIONARY_LAVA || m == Material.LAVA_BUCKET) {
            return Material.STATIONARY_LAVA;
        }
        return m;
    }

    public static Material getBucketMaterial(Material m) {
        if (m == Material.WATER || m == Material.STATIONARY_WATER || m == Material.WATER_BUCKET) {
            return Material.WATER_BUCKET;
        }
        if (m == Material.LAVA || m == Material.STATIONARY_LAVA || m == Material.LAVA_BUCKET) {
            return Material.LAVA_BUCKET;
        }
        return Material.BUCKET;
    }
    
    
    
    public static boolean isBlockSafe(Block block) {
        // Using block data values works better for fluids because this method
        // doesn't use any additional data.
        if (getStationaryMaterial(block.getType()) == Material.STATIONARY_WATER || getStationaryMaterial(block.getType()) == Material.STATIONARY_LAVA) {
            return block.getData() == safeData && !hasEdgeCap(block);
        }
        return false;
    }

    public static boolean isRegistered(Block block) {
        if (block.getType() == Material.DISPENSER) {
            return isRegistered((TileEntityDispenser) ((CraftInventory) ((CraftDispenser) (Dispenser) block.getState()).getInventory()).getInventory());
        }
        return false;
    }

    public static boolean isRegistered(TileEntityDispenser d) {
        return d.getInventoryName().startsWith(registeredCode);
    }
    
    public static boolean hasEdgeCap(Block block) {
        Block above = block.getRelative(BlockFace.UP);
        if (getStationaryMaterial(above.getType()) == getStationaryMaterial(block.getType())) {
            if (above.getData() == block.getData()) {
                return hasEdgeCap(above);
            }
            else if (above.getData() + 8 == block.getData()) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean isLocked() {
        return lock;
    }
    
    public static boolean isSource(Block block) {
        return block.isLiquid() && (isBlockSafe(block) || block.getData() == 0);
    }
    
    
    
    public static int setBlockSafe(Block block, boolean safe, boolean log, int depth) {
        return setBlockSafe(block, safe, log, null, depth);
    }

    public static int setBlockSafe(Block block, boolean safe, boolean log, Player player, int depth) {
        System.out.println("Setting block safe: " + block);
        lock = true;
        int changed = 0;
        if (safe) {
            if (block.getData() == 0 || block.getType() == getFlowingMaterial(block.getType())) {
                if (block.getType() == Material.WATER || block.getType() == Material.STATIONARY_WATER) {
                    BlockState prev = block.getState();
                    changed += removeChildFlows(block, 0, depth);
                    block.setType(Material.STATIONARY_WATER);
                    block.setData(safeData);
                    EventLogger.logEvent(log, player, prev, block);
                }
                if (block.getType() == Material.LAVA || block.getType() == Material.STATIONARY_LAVA) {
                    BlockState prev = block.getState();
                    changed += removeChildFlows(block, 0, depth);
                    block.setType(Material.STATIONARY_LAVA);
                    block.setData(safeData);
                    EventLogger.logEvent(log, player, prev, block);
                }
            }
        }
        else {
            if (isBlockSafe(block)) {
                BlockState prev = block.getState();
                changed++;
                block.setType(getFlowingMaterial(block.getType()));
                block.setData((byte) 0);
                EventLogger.logEvent(log, player, prev, block);
            }
        }
        lock = false;
        return changed;
    }
    
    public static int removeChildFlows(Block block, int depth, int maxDepth) {
        if (depth == maxDepth) {
            return 0;
        }
        int count = 0;
        BlockFace[] sides = new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST };
        int factor = block.getWorld().getEnvironment() != Environment.NETHER && getStationaryMaterial(block.getType()) == Material.STATIONARY_LAVA ? 2 : 1;
        for (BlockFace b : sides) {
            Block adj = block.getRelative(b);
            if (getStationaryMaterial(adj.getType()) == getStationaryMaterial(block.getType())) {
                if (adj.getData() == block.getData() + factor || block.getData() >= 8 && adj.getData() == factor) {
                    count += removeChildFlows(adj, depth + 1, maxDepth);
                    count++;
                    adj.setType(Material.AIR);
                    adj.setData((byte) 0);
                }
            }
        }
        Block below = block.getRelative(BlockFace.DOWN);
        if (getStationaryMaterial(below.getType()) == getStationaryMaterial(block.getType())) {
            if (below.getData() == block.getData() + 8 || below.getData() == block.getData()) {
                count += removeChildFlows(below, depth + 1, maxDepth);
                count++;
                below.setType(Material.AIR);
                below.setData((byte) 0);
            }
        }
        return count;
    }

    public static void simplifyBelow(Block b, boolean f) {
        if (b.isLiquid()) {
            if (b.getData() == safeData - 8) {
                b.setType(Material.AIR);
                b.setData((byte) 0);
            }
            else if (b.getData() == safeData) {
                b.setData((byte) 8);
                if (f) {
                    b.setType(getFlowingMaterial(b.getType()));
                }
                f = false;
            }
            simplifyBelow(b.getRelative(BlockFace.DOWN), f);
        }
    }

    public static void simplifyBelow(Block b) {
        simplifyBelow(b, true);
    }
    
    public static void updateNeighbors(Block block) {
        updateNeighbors(block, block.getType());
    }
    
    public static void updateNeighbors(Block block, Material type) {
        BlockFace[] adj = new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST, BlockFace.UP, BlockFace.DOWN };
        for (BlockFace face : adj) {
            Block b = block.getRelative(face);
            if (b.isLiquid() && getStationaryMaterial(b.getType()) == getStationaryMaterial(type) && !isBlockSafe(b)) {
                b.setType(getFlowingMaterial(b.getType()));
            }
        }
    }
    
    public static void fizz(Block block) {
        block.getWorld().playSound(block.getLocation().add(0.5, 0.5, 0.5), Sound.FIZZ, 0.5F, 2.6F + (float) Math.random() * 0.8F);
    }

    public static void registerBlock(Block block, boolean reg) {
        TileEntityDispenser d = (TileEntityDispenser) ((CraftInventory) ((CraftDispenser) (Dispenser) block.getState()).getInventory()).getInventory();
        boolean isNamed = d != null && !d.getInventoryName().endsWith("container.dispenser");
        if (FluidUtils.isRegistered(d)) {
            if (!reg) {
                d.a(isNamed ? d.getInventoryName().substring(registeredCode.length()) : null);
            }
        }
        else if (reg) {
            d.a(registeredCode + (isNamed ? d.getInventoryName() : "Dispenser"));
        }
    }
    
    public static void lock() {
        lock = true;
    }
    
    public static void unlock() {
        lock = false;
    }
    
}
