package nu.nerd.SafeBuckets;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

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

    private final SafeBucketsListener l = new SafeBucketsListener(this);
    private Set<String> toolPlayers = new HashSet<String>();
    private Set<String> toolblockPlayers = new HashSet<String>();

    public static final Logger log = Logger.getLogger("Minecraft");
    public HashMap<Location, Long> blockCache = new HashMap<Location, Long>();
    public boolean flag = false;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String name, String[] args) {
        if (command.getName().equalsIgnoreCase("sb") || command.getName().equalsIgnoreCase("safebuckets")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("reload")) {
                    reloadConfig();
                    loadConfig();
                    sender.sendMessage("SafeBuckets: reloaded config");
                    log.info("SafeBuckets: reloaded config");
                    return true;
                }
                if (args[0].equalsIgnoreCase("tool")) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        if (args.length == 1) {
                            if (player.hasPermission("safebuckets.tools.item.use")) {
                                if (canUseTool(player)) {
                                    setToolStatus(toolPlayers, player.getName(), false);
                                    player.sendMessage("Tool disabled.");
                                }
                                else {
                                    setToolStatus(toolPlayers, player.getName(), true);
                                    player.sendMessage("Tool enabled.");
                                }
                            }
                            else {
                                player.sendMessage("You do not have permission to run this command.");
                            }
                        }
                        else if (args.length == 2) {
                            if (args[1].equalsIgnoreCase("give")) {
                                if (player.hasPermission("safebuckets.tools.item.give")) {
                                    player.getInventory().addItem(new ItemStack(TOOL_ITEM));
                                }
                                else {
                                    player.sendMessage("You do not have permission to run this command.");
                                }
                            }
                            if (args[1].equalsIgnoreCase("on")) {
                                if (player.hasPermission("safebuckets.tools.item.use")) {
                                    setToolStatus(toolPlayers, player.getName(), true);
                                    player.sendMessage("Tool enabled.");
                                }
                                else {
                                    player.sendMessage("You do not have permission to run this command.");
                                }
                            }
                            if (args[1].equalsIgnoreCase("off")) {
                                if (player.hasPermission("safebuckets.tools.item.use")) {
                                    setToolStatus(toolPlayers, player.getName(), false);
                                    player.sendMessage("Tool disabled.");
                                }
                                else {
                                    player.sendMessage("You do not have permission to run this command.");
                                }
                            }
                        }
                        else {
                            player.sendMessage("Too many arguments. Type \"/sb help\" for help.");
                        }
                    }
                    else {
                        sender.sendMessage("You must be a player to run this command.");
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
                                    player.sendMessage("Tool block disabled.");
                                }
                                else {
                                    setToolStatus(toolblockPlayers, player.getName(), true);
                                    player.sendMessage("Tool block enabled.");
                                }
                            }
                            else {
                                player.sendMessage("You do not have permission to run this command.");
                            }
                        }
                        else if (args.length == 2) {
                            if (args[1].equalsIgnoreCase("give")) {
                                if (player.hasPermission("safebuckets.tools.block.give")) {
                                    player.getInventory().addItem(new ItemStack(TOOL_BLOCK));
                                }
                                else {
                                    player.sendMessage("You do not have permission to run this command.");
                                }
                            }
                            if (args[1].equalsIgnoreCase("on")) {
                                if (player.hasPermission("safebuckets.tools.block.use")) {
                                    setToolStatus(toolblockPlayers, player.getName(), true);
                                    player.sendMessage("Tool block enabled.");
                                }
                                else {
                                    player.sendMessage("You do not have permission to run this command.");
                                }
                            }
                            if (args[1].equalsIgnoreCase("off")) {
                                if (player.hasPermission("safebuckets.tools.block.use")) {
                                    setToolStatus(toolblockPlayers, player.getName(), false);
                                    player.sendMessage("Tool block disabled.");
                                }
                                else {
                                    player.sendMessage("You do not have permission to run this command.");
                                }
                            }
                        }
                        else {
                            player.sendMessage("Too many arguments. Type \"/sb help\" for help.");
                        }
                    }
                    else {
                        sender.sendMessage("You must be a player to run this command.");
                    }
                    return true;
                }
            }
            sender.sendMessage("Unknown command. Type \"/sb help\" for help.");
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
    }

    public boolean isBlockSafe(Block block) {
        // Using block data values works better for fluids because this method
        // doesn't use any additional data.
        if (block.getType() == Material.STATIONARY_WATER || block.getType() == Material.STATIONARY_LAVA) {
            return block.getData() == 15;
        }
        return false;
    }

    public void setBlockSafe(Block block, boolean safe) {
        flag = true;
        if (safe) {
            if (block.getType() == Material.WATER || block.getType() == Material.STATIONARY_WATER) {
                block.setType(Material.STATIONARY_WATER);
                block.setData((byte) 15);
            }
            if (block.getType() == Material.LAVA || block.getType() == Material.STATIONARY_LAVA) {
                block.setType(Material.STATIONARY_LAVA);
                block.setData((byte) 15);
            }
        }
        else {
            if (block.getType() == Material.STATIONARY_WATER && block.getData() == 15) {
                block.setType(Material.WATER);
                block.setData((byte) 0);
            }
            if (block.getType() == Material.STATIONARY_LAVA && block.getData() == 15) {
                block.setType(Material.LAVA);
                block.setData((byte) 0);
            }
        }
        flag = false;
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
