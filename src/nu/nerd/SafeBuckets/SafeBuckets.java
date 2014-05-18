package nu.nerd.SafeBuckets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionType;
import net.minecraft.server.v1_7_R3.EntityHuman;
import net.minecraft.server.v1_7_R3.MathHelper;
import net.minecraft.server.v1_7_R3.MovingObjectPosition;
import net.minecraft.server.v1_7_R3.TileEntityDispenser;
import net.minecraft.server.v1_7_R3.Vec3D;
import nu.nerd.SafeBuckets.database.SafeLiquid;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_7_R3.block.CraftDispenser;
import org.bukkit.craftbukkit.v1_7_R3.inventory.CraftInventory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;

import de.diddiz.LogBlock.LogBlock;

public class SafeBuckets extends JavaPlugin {

    public boolean DEBUG_CONSOLE;
    public boolean DEBUG_PLAYERS;

    public Material TOOL_BLOCK;
    public Material TOOL_ITEM;
    public boolean TOOL_DEFAULT_ON;

    public boolean BUCKET_ENABLED;
    public boolean BUCKET_SAFE;
    public boolean BUCKET_PLACE_SAFE;

    public boolean DISPENSER_ENABLED;
    public boolean DISPENSER_SAFE;
    public boolean DISPENSER_PLACE_SAFE;

    public int REGION_MAX_VOLUME;

    public int FLOW_MAX_DEPTH;

    public boolean LOG_MANUAL_FLOW;
    public boolean LOG_REGION_FLOW;
    public boolean LOG_NATURAL_FLOW;

    private final SafeBucketsListener l = new SafeBucketsListener(this);
    private final String registeredCode = ChatColor.STRIKETHROUGH.toString() + ChatColor.RESET.toString();
    private Set<String> toolPlayers = new HashSet<String>();
    private Set<String> toolblockPlayers = new HashSet<String>();
    private WorldEditPlugin worldedit;
    public EventLogger eventLogger;

    public static final Logger log = Logger.getLogger("Minecraft");
    public HashMap<Location, SafeEntry> blockCache = new HashMap<Location, SafeEntry>();
    public boolean flag = false;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String name, String[] args) {
        if (command.getName().equalsIgnoreCase("sb") || command.getName().equalsIgnoreCase("safebuckets")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("reload")) {
                    if (sender.hasPermission("safebuckets.reload")) {
                        reloadConfig();
                        loadConfig();
                        message(sender, "Reloaded config.");
                        log.info("SafeBuckets: Reloaded config.");
                    }
                    else {
                        message(sender, "You do not have permission to run this command.");
                    }
                    return true;
                }
                if (args[0].equalsIgnoreCase("tool")) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        if (args.length == 1) {
                            if (player.hasPermission("safebuckets.tools.item.use")) {
                                if (canUseTool(player)) {
                                    setToolStatus(toolPlayers, player.getName(), false);
                                    message(player, "Tool disabled.");
                                }
                                else {
                                    setToolStatus(toolPlayers, player.getName(), true);
                                    message(player, "Tool enabled.");
                                }
                            }
                            else {
                                message(player, "You do not have permission to run this command.");
                            }
                        }
                        else if (args.length == 2) {
                            if (args[1].equalsIgnoreCase("give")) {
                                if (player.hasPermission("safebuckets.tools.item.give")) {
                                    player.getInventory().addItem(new ItemStack(TOOL_ITEM));
                                }
                                else {
                                    message(player, "You do not have permission to run this command.");
                                }
                            }
                            if (args[1].equalsIgnoreCase("on")) {
                                if (player.hasPermission("safebuckets.tools.item.use")) {
                                    setToolStatus(toolPlayers, player.getName(), true);
                                    message(player, "Tool enabled.");
                                }
                                else {
                                    message(player, "You do not have permission to run this command.");
                                }
                            }
                            if (args[1].equalsIgnoreCase("off")) {
                                if (player.hasPermission("safebuckets.tools.item.use")) {
                                    setToolStatus(toolPlayers, player.getName(), false);
                                    message(player, "Tool disabled.");
                                }
                                else {
                                    message(player, "You do not have permission to run this command.");
                                }
                            }
                        }
                        else {
                            message(player, "Too many arguments. Type \"/sb help\" for help.");
                        }
                    }
                    else {
                        message(sender, "You must be a player to run this command.");
                    }
                    return true;
                }
                if (args[0].equalsIgnoreCase("toolblock")) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        if (args.length == 1) {
                            if (player.hasPermission("safebuckets.tools.block.use")) {
                                if (canUseToolBlock(player)) {
                                    setToolStatus(toolblockPlayers, player.getName(), false);
                                    message(player, "Tool block disabled.");
                                }
                                else {
                                    setToolStatus(toolblockPlayers, player.getName(), true);
                                    message(player, "Tool block enabled.");
                                }
                            }
                            else {
                                message(player, "You do not have permission to run this command.");
                            }
                        }
                        else if (args.length == 2) {
                            if (args[1].equalsIgnoreCase("give")) {
                                if (player.hasPermission("safebuckets.tools.block.give")) {
                                    player.getInventory().addItem(new ItemStack(TOOL_BLOCK));
                                }
                                else {
                                    message(player, "You do not have permission to run this command.");
                                }
                            }
                            if (args[1].equalsIgnoreCase("on")) {
                                if (player.hasPermission("safebuckets.tools.block.use")) {
                                    setToolStatus(toolblockPlayers, player.getName(), true);
                                    message(player, "Tool block enabled.");
                                }
                                else {
                                    message(player, "You do not have permission to run this command.");
                                }
                            }
                            if (args[1].equalsIgnoreCase("off")) {
                                if (player.hasPermission("safebuckets.tools.block.use")) {
                                    setToolStatus(toolblockPlayers, player.getName(), false);
                                    message(player, "Tool block disabled.");
                                }
                                else {
                                    message(player, "You do not have permission to run this command.");
                                }
                            }
                        }
                        else {
                            message(player, "Too many arguments. Type \"/sb help\" for help.");
                        }
                    }
                    else {
                        message(sender, "You must be a player to run this command.");
                    }
                    return true;
                }
                if (args[0].equalsIgnoreCase("setunsafe") || args[0].equalsIgnoreCase("flow")) {
                    if (worldedit != null) {
                        if (sender instanceof Player) {
                            Player player = (Player) sender;
                            if (player.hasPermission("safebuckets.region.unsafe")) {
                                Selection sel = worldedit.getSelection(player);
                                if (sel != null && sel instanceof CuboidSelection) {
                                    if (sel.getArea() <= REGION_MAX_VOLUME) {
                                        message(player, setRegionSafe(sel.getMinimumPoint(), sel.getMaximumPoint(), false, player) + " blocks set unsafe.");
                                    }
                                    else {
                                        message(player, "The selected area exceeds the maximum volume (" + REGION_MAX_VOLUME + ").");
                                    }
                                }
                                else {
                                    message(player, "You must first make a cuboid selection.");
                                }
                            }
                            else {
                                message(player, "You do not have permission to run this command.");
                            }
                        }
                        else {
                            message(sender, "You must be a player to run this command.");
                        }
                    }
                    else {
                        message(sender, "The WorldEdit plugin could not be accessed!");
                    }
                    return true;
                }
                if (args[0].equalsIgnoreCase("setsafe") || args[0].equalsIgnoreCase("static")) {
                    if (worldedit != null) {
                        if (sender instanceof Player) {
                            Player player = (Player) sender;
                            if (player.hasPermission("safebuckets.region.safe")) {
                                Selection sel = worldedit.getSelection(player);
                                if (sel != null && sel instanceof CuboidSelection) {
                                    if (sel.getArea() <= REGION_MAX_VOLUME || REGION_MAX_VOLUME < 0) {
                                        message(player, setRegionSafe(sel.getMinimumPoint(), sel.getMaximumPoint(), true, player) + " fluid blocks affected.");
                                    }
                                    else {
                                        message(player, "The selected area exceeds the maximum volume (" + REGION_MAX_VOLUME + ").");
                                    }
                                }
                                else {
                                    message(player, "You must first make a cuboid selection.");
                                }
                            }
                            else {
                                message(player, "You do not have permission to run this command.");
                            }
                        }
                        else {
                            message(sender, "You must be a player to run this command.");
                        }
                    }
                    else {
                        message(sender, "The WorldEdit plugin could not be accessed!");
                    }
                    return true;
                }
                if (args[0].equalsIgnoreCase("help")) {
                    sender.sendMessage("SafeBuckets Commands:");
                    sender.sendMessage("/sb reload");
                    sender.sendMessage(ChatColor.GRAY + "    Reloads SafeBuckets configuration.");
                    sender.sendMessage("/sb tool [on|off|give]");
                    sender.sendMessage(ChatColor.GRAY + "    Toggles, turns on, turns off, or gives you the SafeBuckets");
                    sender.sendMessage(ChatColor.GRAY + "    dispenser inspector tool.");
                    sender.sendMessage("/sb toolblock [on|off|give]");
                    sender.sendMessage(ChatColor.GRAY + "    Toggles, turns on, turns off, or gives you the SafeBuckets");
                    sender.sendMessage(ChatColor.GRAY + "    fluid inspector tool.");
                    sender.sendMessage("/sb setunsafe, /sb flow");
                    sender.sendMessage(ChatColor.GRAY + "    Sets unsafe all safe sources within the WorldEdit selection.");
                    sender.sendMessage("/sb setsafe, /sb static");
                    sender.sendMessage(ChatColor.GRAY + "    Sets safe all unsafe sources within the WorldEdit selection.");
                    sender.sendMessage("/sb help");
                    sender.sendMessage(ChatColor.GRAY + "    Displays this help message.");
                    return true;
                }
            }
            message(sender, "Unknown command. Type \"/sb help\" for help.");
            return true;
        }

        return false;
    }

    @Override
    public void onDisable() {
        log.log(Level.INFO, "[" + getDescription().getName() + "] " + getDescription().getVersion() + " disabled.");
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        migrateSources();

        PluginManager pm = this.getServer().getPluginManager();
        pm.registerEvents(l, this);

        Plugin wepl = pm.getPlugin("WorldEdit");
        if (wepl == null) {
            log.log(Level.WARNING, "[" + getDescription().getName() + "] WorldEdit could not be loaded!");
        }
        else {
            worldedit = (WorldEditPlugin) wepl;
        }

        eventLogger = new EventLogger(this);

        Plugin prpl = pm.getPlugin("Prism");
        if (prpl != null) {
            try {
                Prism.getActionRegistry().registerCustomAction(this, new ActionType("safebuckets-fluid-safe", true, true, true, "BlockChangeAction", "set safe"));
                Prism.getActionRegistry().registerCustomAction(this, new ActionType("safebuckets-fluid-unsafe", true, true, true, "BlockChangeAction", "set unsafe"));
                // pm.registerEvents(l, (Prism) prpl);
                eventLogger.enablePrism();
                log.log(Level.INFO, "[" + getDescription().getName() + "] Logging events using Prism.");
            } catch (Exception e) {
            }
        }

        Plugin lbpl = pm.getPlugin("LogBlock");
        if (lbpl != null) {
            eventLogger.enableLogBlock((LogBlock) lbpl);
            log.log(Level.INFO, "[" + getDescription().getName() + "] Logging events using LogBlock.");
        }

        if (!eventLogger.canLog()) {
            log.log(Level.WARNING, "[" + getDescription().getName() + "] Neither Prism nor LogBlock found - logging disabled!");
        }

        log.log(Level.INFO, "[" + getDescription().getName() + "] " + getDescription().getVersion() + " enabled.");
    }

    public void loadConfig() {
        DEBUG_CONSOLE = getConfig().getBoolean("debug.console");
        DEBUG_PLAYERS = getConfig().getBoolean("debug.players");
        TOOL_BLOCK = Material.getMaterial(getConfig().getString("tool.block"));
        TOOL_ITEM = Material.getMaterial(getConfig().getString("tool.item"));
        TOOL_DEFAULT_ON = getConfig().getBoolean("tool.default-on");
        BUCKET_ENABLED = getConfig().getBoolean("bucket.enabled");
        BUCKET_SAFE = getConfig().getBoolean("bucket.safe");
        BUCKET_PLACE_SAFE = getConfig().getBoolean("bucket.place-safe");
        DISPENSER_ENABLED = getConfig().getBoolean("dispenser.enabled");
        DISPENSER_SAFE = getConfig().getBoolean("dispenser.safe");
        DISPENSER_PLACE_SAFE = getConfig().getBoolean("dispenser.place-safe");
        REGION_MAX_VOLUME = getConfig().getInt("region.maximum-volume", 1000);
        FLOW_MAX_DEPTH = getConfig().getInt("flow.maximum-depth", 20);
        LOG_MANUAL_FLOW = getConfig().getBoolean("logging.manual-flow");
        LOG_REGION_FLOW = getConfig().getBoolean("logging.region-flow");
        LOG_NATURAL_FLOW = getConfig().getBoolean("logging.natural-flow");
    }
    
    public void migrateSources() {
        try {
            getDatabase().find(SafeLiquid.class).findRowCount();
            List<SafeLiquid> liquids = getDatabase().find(SafeLiquid.class).findList();
            log.log(Level.INFO, "[SafeBuckets] Migrating sources from " + liquids.size() + " entries...");
            int c = 0;
            for (SafeLiquid l : liquids) {
                Block b = getServer().getWorld(l.getWorld()).getBlockAt(l.getX(), l.getY(), l.getZ());
                if ((b.getType() == Material.STATIONARY_WATER || b.getType() == Material.STATIONARY_LAVA) && b.getData() == 0) {
                    setBlockSafe(b, true, false);
                    c++;
                }
            }
            log.log(Level.INFO, "[SafeBuckets] " + c + " sources migrated.");
        } catch (PersistenceException ex) {
            log.log(Level.INFO, "[SafeBuckets] No database found; use SafeBuckets v1.0 instead of v1.0c (SafeBucketsConverter).");
        }
    }

    public void message(CommandSender sender, String msg) {
        sender.sendMessage(ChatColor.AQUA + "SafeBuckets: " + msg);
    }

    public boolean isBlockSafe(Block block) {
        // Using block data values works better for fluids because this method
        // doesn't use any additional data.
        if (getStationaryMaterial(block.getType()) == Material.STATIONARY_WATER || getStationaryMaterial(block.getType()) == Material.STATIONARY_LAVA) {
            return block.getData() == 15 && !hasEdgeCap(block);
        }
        return false;
    }

    public int setBlockSafe(Block block, boolean safe, boolean log) {
        return setBlockSafe(block, safe, log, null);
    }

    public int setBlockSafe(Block block, boolean safe, boolean log, Player player) {
        flag = true;
        int changed = 0;
        if (safe) {
            if (block.getData() == 0) {
                if (block.getType() == Material.WATER || block.getType() == Material.STATIONARY_WATER) {
                    BlockState prev = block.getState();
                    changed += removeChildFlows(block, 0);
                    block.setType(Material.STATIONARY_WATER);
                    block.setData((byte) 15);
                    eventLogger.logEvent(log, player, prev, block);
                }
                if (block.getType() == Material.LAVA || block.getType() == Material.STATIONARY_LAVA) {
                    BlockState prev = block.getState();
                    changed += removeChildFlows(block, 0);
                    block.setType(Material.STATIONARY_LAVA);
                    block.setData((byte) 15);
                    eventLogger.logEvent(log, player, prev, block);
                }
            }
        }
        else {
            if (isBlockSafe(block)) {
                BlockState prev = block.getState();
                changed++;
                block.setType(getFlowingMaterial(block.getType()));
                block.setData((byte) 0);
                eventLogger.logEvent(log, player, prev, block);
            }
        }
        flag = false;
        return changed;
    }

    public int removeChildFlows(Block block, int depth) {
        if (depth == FLOW_MAX_DEPTH) {
            return 0;
        }
        int count = 0;
        BlockFace[] sides = new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST };
        int factor = block.getWorld().getEnvironment() != Environment.NETHER && getStationaryMaterial(block.getType()) == Material.STATIONARY_LAVA ? 2 : 1;
        for (BlockFace b : sides) {
            Block adj = block.getRelative(b);
            if (getStationaryMaterial(adj.getType()) == getStationaryMaterial(block.getType())) {
                if (adj.getData() == block.getData() + factor || block.getData() >= 8 && adj.getData() == factor) {
                    count += removeChildFlows(adj, depth + 1);
                    count++;
                    adj.setType(Material.AIR);
                    adj.setData((byte) 0);
                }
            }
        }
        Block below = block.getRelative(BlockFace.DOWN);
        if (getStationaryMaterial(below.getType()) == getStationaryMaterial(block.getType())) {
            if (below.getData() == block.getData() + 8 || below.getData() == block.getData()) {
                count += removeChildFlows(below, depth + 1);
                count++;
                below.setType(Material.AIR);
                below.setData((byte) 0);
            }
        }
        return count;
    }

    public void simplifyBelow(Block b, boolean f) {
        if (b.isLiquid()) {
            if (b.getData() == 7) {
                b.setType(Material.AIR);
                b.setData((byte) 0);
            }
            else if (b.getData() == 15) {
                b.setData((byte) 8);
                if (f) {
                    b.setType(getFlowingMaterial(b.getType()));
                }
                f = false;
            }
            simplifyBelow(b.getRelative(BlockFace.DOWN), f);
        }
    }

    public void simplifyBelow(Block b) {
        simplifyBelow(b, true);
    }

    public Material getFlowingMaterial(Material m) {
        if (m == Material.WATER || m == Material.STATIONARY_WATER || m == Material.WATER_BUCKET) {
            return Material.WATER;
        }
        if (m == Material.LAVA || m == Material.STATIONARY_LAVA || m == Material.LAVA_BUCKET) {
            return Material.LAVA;
        }
        return null;
    }

    public Material getStationaryMaterial(Material m) {
        if (m == Material.WATER || m == Material.STATIONARY_WATER || m == Material.WATER_BUCKET) {
            return Material.STATIONARY_WATER;
        }
        if (m == Material.LAVA || m == Material.STATIONARY_LAVA || m == Material.LAVA_BUCKET) {
            return Material.STATIONARY_LAVA;
        }
        return null;
    }

    public Material getBucketMaterial(Material m) {
        if (m == Material.WATER || m == Material.STATIONARY_WATER || m == Material.WATER_BUCKET) {
            return Material.WATER_BUCKET;
        }
        if (m == Material.LAVA || m == Material.STATIONARY_LAVA || m == Material.LAVA_BUCKET) {
            return Material.LAVA_BUCKET;
        }
        return Material.BUCKET;
    }

    public String getVirtualName(Block b) {
        if (getStationaryMaterial(b.getType()) == Material.STATIONARY_WATER) {
            return "WaterFlow";
        }
        if (getStationaryMaterial(b.getType()) == Material.STATIONARY_LAVA) {
            return "LavaFlow";
        }
        return null;
    }

    public boolean hasEdgeCap(Block block) {
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

    public MovingObjectPosition raytrace(EntityHuman nmsPlayer, net.minecraft.server.v1_7_R3.World nmsWorld) {
        // TODO Clean me up! This code modified from MC source.

        // net.minecraft.server.v1_7_R3.Item.a(World, EntityHuman, boolean)
        Vec3D vec3d;
        Vec3D vec3d1;
        {
            float f = 1.0F;
            float f1 = nmsPlayer.lastPitch + (nmsPlayer.pitch - nmsPlayer.lastPitch) * f;
            float f2 = nmsPlayer.lastYaw + (nmsPlayer.yaw - nmsPlayer.lastYaw) * f;
            double d0 = nmsPlayer.lastX + (nmsPlayer.locX - nmsPlayer.lastX) * (double) f;
            double d1 = nmsPlayer.lastY + (nmsPlayer.locY - nmsPlayer.lastY) * (double) f + 1.62D - (double) nmsPlayer.height;
            double d2 = nmsPlayer.lastZ + (nmsPlayer.locZ - nmsPlayer.lastZ) * (double) f;
            vec3d = Vec3D.a(d0, d1, d2);
            float f3 = MathHelper.cos(-f2 * 0.017453292F - 3.1415927F);
            float f4 = MathHelper.sin(-f2 * 0.017453292F - 3.1415927F);
            float f5 = -MathHelper.cos(-f1 * 0.017453292F);
            float f6 = MathHelper.sin(-f1 * 0.017453292F);
            float f7 = f4 * f5;
            float f8 = f3 * f5;
            double d3 = 5.0D;
            vec3d1 = vec3d.add((double) f7 * d3, (double) f6 * d3, (double) f8 * d3);
        }

        // net.minecraft.server.v1_7_R3.World.rayTrace(Vec3D, Vec3D, boolean,
        // boolean, boolean) {
        {
            int i = MathHelper.floor(vec3d1.a);
            int j = MathHelper.floor(vec3d1.b);
            int k = MathHelper.floor(vec3d1.c);
            int l = MathHelper.floor(vec3d.a);
            int i1 = MathHelper.floor(vec3d.b);
            int j1 = MathHelper.floor(vec3d.c);
            net.minecraft.server.v1_7_R3.Block block = nmsWorld.getType(l, i1, j1);
            int k1 = nmsWorld.getData(l, i1, j1);

            if (block.a(k1, true) || isBlockSafe(nmsPlayer.getBukkitEntity().getWorld().getBlockAt(l, i1, j1))) {
                return block.a(nmsWorld, l, i1, j1, vec3d, vec3d1);
            }

            k1 = 200;

            while (k1-- >= 0) {
                if (Double.isNaN(vec3d.a) || Double.isNaN(vec3d.b) || Double.isNaN(vec3d.c)) {
                    return null;
                }

                if (l == i && i1 == j && j1 == k) {
                    return null;
                }

                boolean flag3 = true;
                boolean flag4 = true;
                boolean flag5 = true;
                double d0 = 999.0D;
                double d1 = 999.0D;
                double d2 = 999.0D;

                if (i > l) {
                    d0 = (double) l + 1.0D;
                }
                else if (i < l) {
                    d0 = (double) l + 0.0D;
                }
                else {
                    flag3 = false;
                }

                if (j > i1) {
                    d1 = (double) i1 + 1.0D;
                }
                else if (j < i1) {
                    d1 = (double) i1 + 0.0D;
                }
                else {
                    flag4 = false;
                }

                if (k > j1) {
                    d2 = (double) j1 + 1.0D;
                }
                else if (k < j1) {
                    d2 = (double) j1 + 0.0D;
                }
                else {
                    flag5 = false;
                }

                double d3 = 999.0D;
                double d4 = 999.0D;
                double d5 = 999.0D;
                double d6 = vec3d1.a - vec3d.a;
                double d7 = vec3d1.b - vec3d.b;
                double d8 = vec3d1.c - vec3d.c;

                if (flag3) {
                    d3 = (d0 - vec3d.a) / d6;
                }

                if (flag4) {
                    d4 = (d1 - vec3d.b) / d7;
                }

                if (flag5) {
                    d5 = (d2 - vec3d.c) / d8;
                }

                byte b0;

                if (d3 < d4 && d3 < d5) {
                    if (i > l) {
                        b0 = 4;
                    }
                    else {
                        b0 = 5;
                    }

                    vec3d.a = d0;
                    vec3d.b += d7 * d3;
                    vec3d.c += d8 * d3;
                }
                else if (d4 < d5) {
                    if (j > i1) {
                        b0 = 0;
                    }
                    else {
                        b0 = 1;
                    }

                    vec3d.a += d6 * d4;
                    vec3d.b = d1;
                    vec3d.c += d8 * d4;
                }
                else {
                    if (k > j1) {
                        b0 = 2;
                    }
                    else {
                        b0 = 3;
                    }

                    vec3d.a += d6 * d5;
                    vec3d.b += d7 * d5;
                    vec3d.c = d2;
                }

                Vec3D vec3d2 = Vec3D.a(vec3d.a, vec3d.b, vec3d.c);

                l = (int) (vec3d2.a = (double) MathHelper.floor(vec3d.a));
                if (b0 == 5) {
                    --l;
                    ++vec3d2.a;
                }

                i1 = (int) (vec3d2.b = (double) MathHelper.floor(vec3d.b));
                if (b0 == 1) {
                    --i1;
                    ++vec3d2.b;
                }

                j1 = (int) (vec3d2.c = (double) MathHelper.floor(vec3d.c));
                if (b0 == 3) {
                    --j1;
                    ++vec3d2.c;
                }

                net.minecraft.server.v1_7_R3.Block block1 = nmsWorld.getType(l, i1, j1);
                int l1 = nmsWorld.getData(l, i1, j1);

                if (block1.a(l1, true) || isBlockSafe(nmsPlayer.getBukkitEntity().getWorld().getBlockAt(l, i1, j1))) {
                    MovingObjectPosition movingobjectposition2 = block1.a(nmsWorld, l, i1, j1, vec3d, vec3d1);

                    if (movingobjectposition2 != null) {
                        return movingobjectposition2;
                    }
                }
            }
            return null;
        }
    }

    public int setRegionSafe(Location p1, Location p2, boolean safe, Player issuer) {
        World world = p1.getWorld();
        int count = 0;
        int minX = Math.min(p1.getBlockX(), p2.getBlockX());
        int minY = Math.min(p1.getBlockY(), p2.getBlockY());
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
        int maxY = Math.max(p1.getBlockY(), p2.getBlockY());
        int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block b = world.getBlockAt(x, y, z);
                    count += setBlockSafe(b, safe, LOG_REGION_FLOW, issuer);
                }
            }
        }
        return count;
    }

    public void queueSafeBlock(Block block, boolean log, Player player) {
        blockCache.put(block.getLocation(), new SafeEntry(block.getWorld().getTime(), log, player));
    }

    public void registerBlock(Block block, boolean reg) {
        TileEntityDispenser d = (TileEntityDispenser) ((CraftInventory) ((CraftDispenser) (Dispenser) block.getState()).getInventory()).getInventory();
        boolean isNamed = d != null && !d.getInventoryName().endsWith("container.dispenser");
        if (isRegistered(d)) {
            if (!reg) {
                d.a(isNamed ? d.getInventoryName().substring(registeredCode.length()) : null);
            }
        }
        else if (reg) {
            d.a(registeredCode + (isNamed ? d.getInventoryName() : "Dispenser"));
        }
    }

    public boolean isRegistered(Block block) {
        if (block.getType() == Material.DISPENSER) {
            return isRegistered((TileEntityDispenser) ((CraftInventory) ((CraftDispenser) (Dispenser) block.getState()).getInventory()).getInventory());
        }
        return false;
    }

    public boolean isRegistered(TileEntityDispenser d) {
        return d.getInventoryName().startsWith(registeredCode);
    }

    public void setToolStatus(Set<String> pList, String player, boolean status) {
        if (status == TOOL_DEFAULT_ON) {
            pList.remove(player);
        }
        else {
            pList.add(player);
        }
    }

    public boolean canUseTool(Player player) {
        return toolPlayers.contains(player.getName()) != TOOL_DEFAULT_ON;
    }

    public boolean canUseToolBlock(Player player) {
        return toolblockPlayers.contains(player.getName()) != TOOL_DEFAULT_ON;
    }
    
    @Override
    public ArrayList<Class<?>> getDatabaseClasses() {
        ArrayList<Class<?>> list = new ArrayList<Class<?>>();
        list.add(SafeLiquid.class);
        return list;
    }

}
