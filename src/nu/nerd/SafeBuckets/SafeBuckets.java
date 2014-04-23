package nu.nerd.SafeBuckets;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class SafeBuckets extends JavaPlugin {

    public boolean DEBUG_CONSOLE;
    public boolean DEBUG_PLAYERS;

    public Material TOOL_BLOCK;
    public Material TOOL_ITEM;

    public boolean BUCKET_ENABLED;
    public boolean BUCKET_SAFE;
    public boolean BUCKET_PLACE_SAFE;

    public boolean DISPENSER_ENABLED;
    public boolean DISPENSER_SAFE;
    public boolean DISPENSER_PLACE_SAFE;
    public boolean DISPENSER_WHITELIST;

    private final SafeBucketsListener l = new SafeBucketsListener(this);
    public static final Logger log = Logger.getLogger("Minecraft");
    public HashMap<Location, Long> blockCache = new HashMap<Location, Long>();
    public boolean flag = false;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String name, String[] args) {
        if (command.getName().equalsIgnoreCase("sbrl")) {
            reloadConfig();
            loadConfig();
            sender.sendMessage("SafeBuckets: reloaded config");
            log.info("SafeBuckets: reloaded config");
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
        System.out.println(block.getMetadata("reg"));
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

}
