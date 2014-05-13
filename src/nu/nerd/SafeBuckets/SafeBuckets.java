package nu.nerd.SafeBuckets;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;

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
    public boolean DISPENSER_WHITELIST;

    public int REGION_MAX_VOLUME;

    public int FLOW_MAX_DEPTH;

    private final SafeBucketsListener l = new SafeBucketsListener(this);
    private Set<String> toolPlayers = new HashSet<String>();
    private Set<String> toolblockPlayers = new HashSet<String>();
    private WorldEditPlugin worldedit;

    public static final Logger log = Logger.getLogger("Minecraft");
    public HashMap<Location, Long> blockCache = new HashMap<Location, Long>();
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
                                        message(player, setRegionSafe(sel.getMinimumPoint(), sel.getMaximumPoint(), false) + " blocks set unsafe.");
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
                                        message(player, setRegionSafe(sel.getMinimumPoint(), sel.getMaximumPoint(), true) + " fluid blocks affected.");
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

        PluginManager pm = this.getServer().getPluginManager();
        pm.registerEvents(l, this);

        try {
            worldedit = (WorldEditPlugin) getServer().getPluginManager().getPlugin("WorldEdit");
        } catch (Exception e) {
            log.log(Level.WARNING, "WorldEdit could not be loaded!");
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
        DISPENSER_WHITELIST = getConfig().getBoolean("dispenser.whitelist");
        REGION_MAX_VOLUME = getConfig().getInt("region.maximum-volume", 1000);
        FLOW_MAX_DEPTH = getConfig().getInt("flow.maximum-depth", 20);
    }

    public void message(CommandSender sender, String msg) {
        sender.sendMessage(ChatColor.AQUA + "SafeBuckets: " + msg);
    }

    public boolean isBlockSafe(Block block) {
        // Using block data values works better for fluids because this method
        // doesn't use any additional data.
        if (block.getType() == Material.STATIONARY_WATER || block.getType() == Material.STATIONARY_LAVA) {
            return block.getData() == 15;
        }
        return false;
    }

    public int setBlockSafe(Block block, boolean safe) {
        flag = true;
        int changed = 0;
        if (safe) {
            if (block.getData() == 0) {
                if (block.getType() == Material.WATER || block.getType() == Material.STATIONARY_WATER) {
                    changed += removeChildFlows(block, 0);
                    block.setType(Material.STATIONARY_WATER);
                    block.setData((byte) 15);
                }
                if (block.getType() == Material.LAVA || block.getType() == Material.STATIONARY_LAVA) {
                    changed += removeChildFlows(block, 0);
                    block.setType(Material.STATIONARY_LAVA);
                    block.setData((byte) 15);
                }
            }
        }
        else {
            if (block.getType() == Material.STATIONARY_WATER && block.getData() == 15) {
                changed++;
                block.setType(Material.WATER);
                block.setData((byte) 0);
            }
            if (block.getType() == Material.STATIONARY_LAVA && block.getData() == 15) {
                changed++;
                block.setType(Material.LAVA);
                block.setData((byte) 0);
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
        for (BlockFace b : sides) {
            Block adj = block.getRelative(b);
            if (adj.getType() == block.getType()) {
                if (adj.getData() == block.getData() + 1 || block.getData() >= 8 && adj.getData() == 1) {
                    count += removeChildFlows(adj, depth + 1);
                    count++;
                    adj.setType(Material.AIR);
                    adj.setData((byte) 0);
                }
            }
        }
        Block below = block.getRelative(BlockFace.DOWN);
        if (below.getType() == block.getType()) {
            if (below.getData() == block.getData() + 8 || below.getData() == block.getData() - 1 || below.getData() == block.getData()) {
                count += removeChildFlows(below, depth + 1);
                count++;
                below.setType(Material.AIR);
                below.setData((byte) 0);
            }
        }
        return count;
    }

    public Material getFlowingMaterial(Material m) {
        if (m == Material.WATER || m == Material.STATIONARY_WATER) {
            return Material.WATER;
        }
        if (m == Material.LAVA || m == Material.STATIONARY_LAVA) {
            return Material.LAVA;
        }
        return null;
    }

    public Material getStationaryMaterial(Material m) {
        if (m == Material.WATER || m == Material.STATIONARY_WATER) {
            return Material.STATIONARY_WATER;
        }
        if (m == Material.LAVA || m == Material.STATIONARY_LAVA) {
            return Material.STATIONARY_LAVA;
        }
        return null;
    }

    public int setRegionSafe(Location p1, Location p2, boolean safe) {
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
                    count += setBlockSafe(b, safe);
                }
            }
        }
        return count;
    }

    public void queueSafeBlock(Block block) {
        blockCache.put(block.getLocation(), block.getWorld().getTime());
    }

    public void registerBlock(Block block, boolean safe) {
        block.setMetadata("reg", new FixedMetadataValue(this, safe));
    }

    public boolean isRegistered(Block block) {
        if (block.getType() == Material.DISPENSER) {
            if (block.hasMetadata("reg")) {
                List<MetadataValue> meta = block.getMetadata("reg");
                return meta.size() > 0 && meta.get(0).asBoolean();
            }
            else {
                return !DISPENSER_WHITELIST;
            }
        }
        return false;
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

}
