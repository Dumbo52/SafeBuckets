package nu.nerd.SafeBuckets;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class SafeBuckets extends JavaPlugin {

    private final SafeBucketsListener l = new SafeBucketsListener(this);
    public static final Logger log = Logger.getLogger("Minecraft");
    private final FixedMetadataValue SAFE = new FixedMetadataValue(this, true);
    private final FixedMetadataValue UNSAFE = new FixedMetadataValue(this, false);
    public HashMap<Location, Long> blockCache = new HashMap<Location, Long>();
    public boolean flag = false;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String name, String[] args) {
        if (command.getName().equalsIgnoreCase("sbrl")) {
        	reloadConfig();
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

        PluginManager pm = this.getServer().getPluginManager();
        pm.registerEvents(l, this);

        log.log(Level.INFO, "[" + getDescription().getName() + "] " + getDescription().getVersion() + " enabled.");
    }
    
    public boolean isBlockSafe(Block block) {
    	// Using block data values works better for fluids because this method
    	// doesn't use any additional data. Metadata seems to work better for
    	// dispensers, which isn't a problem since dispensers won't be used
    	// nearly as commonly as water or lava.
        return ((block.getType() == Material.STATIONARY_WATER || block.getType() == Material.STATIONARY_LAVA) && block.getData() == 15) || (block.getType() == Material.DISPENSER && block.hasMetadata("safe") && block.getMetadata("safe").get(0).asBoolean());
    }
    
    public void setBlockSafe(Block block) {
        flag = true;
    	if (block.getType() == Material.WATER || block.getType() == Material.STATIONARY_WATER) {
    		block.setType(Material.STATIONARY_WATER);
    		block.setData((byte)15);
    	}
    	if (block.getType() == Material.LAVA || block.getType() == Material.STATIONARY_LAVA) {
    		block.setType(Material.STATIONARY_LAVA);
    		block.setData((byte)15);
    	}
    	if (block.getType() == Material.DISPENSER) {
    		block.setMetadata("safe", SAFE);
    	}
    	flag = false;
    }
    
    public void setBlockUnsafe(Block block) {
        flag = true;
    	if (block.getType() == Material.STATIONARY_WATER && block.getData() == 15) {
    		block.setType(Material.WATER);
    		block.setData((byte)0);
    	}
    	if (block.getType() == Material.STATIONARY_LAVA && block.getData() == 15) {
    		block.setType(Material.LAVA);
    		block.setData((byte)0);
    	}
    	if (block.getType() == Material.DISPENSER) {
    		block.setMetadata("safe", UNSAFE);
    	}
    	flag = false;
    }
    
    public void queueSafeBlock(Block block) {
    	blockCache.put(block.getLocation(), block.getWorld().getTime());
    }
    
}
