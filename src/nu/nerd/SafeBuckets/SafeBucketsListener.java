package nu.nerd.SafeBuckets;

import net.minecraft.server.v1_7_R1.DispenseBehaviorItem;
import net.minecraft.server.v1_7_R1.Item;
import net.minecraft.server.v1_7_R1.SourceBlock;
import net.minecraft.server.v1_7_R1.TileEntityDispenser;
import net.minecraft.server.v1_7_R1.World;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_7_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R1.inventory.CraftInventory;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Dispenser;  //> Material because we need the getFacing method (DirectionalContainer.class)

public class SafeBucketsListener implements Listener {

    private final SafeBuckets plugin;

    SafeBucketsListener(SafeBuckets instance) {
        plugin = instance;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        Material mat = event.getBlock().getType();
        if (mat == Material.STATIONARY_LAVA || mat == Material.STATIONARY_WATER) {
            if (plugin.isSafeLiquid(event.getBlock())) {
                event.setCancelled(true);
            }
        }
        Block block = event.getBlock();
        if (plugin.blockCache.containsKey(block.getLocation())) {
        	System.out.println("Setting block safe (Physics): " + block);
        	if (block.getWorld().getTime() - plugin.blockCache.get(block.getLocation()) <= 10 || block.getType() == Material.DISPENSER) {
        		plugin.setBlockSafe(block);
        		event.setCancelled(true);
        	}
        	plugin.blockCache.remove(block.getLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDispense(BlockDispenseEvent event) {
    	if (event.getBlock().getState().getData() instanceof Dispenser) {
	        Material mat = event.getItem().getType();
	        Dispenser dispenser = (Dispenser)event.getBlock().getState().getData();
	    	Block blockDispenser = event.getBlock();
	    	Block blockDispense = blockDispenser.getRelative(dispenser.getFacing());

	        if (mat == Material.LAVA_BUCKET || mat == Material.WATER_BUCKET) {
	        	if (plugin.getConfig().getBoolean("dispenser.enabled")) {
		        	if (plugin.getConfig().getBoolean("dispenser.safe") && plugin.isSafeLiquid(blockDispenser))
		        		plugin.queueSafeBlock(blockDispense);
		        	StringBuilder message = new StringBuilder("SafeBuckets: Dispensing (").append(event.getBlock().getX()).append(",").append(event.getBlock().getY()).append(",").append(event.getBlock().getZ()).append(") ");
		        	if (!plugin.isSafeLiquid(blockDispenser))
		        		message.append("un");
		        	message.append("safe");

	        		if (plugin.getConfig().getBoolean("debug.players")) {
	        			plugin.getServer().broadcast(message.toString(), "safebuckets.debug");
	        		}

	        		if (plugin.getConfig().getBoolean("debug.console")) {
	        			SafeBuckets.log.info(message.toString());
	        		}
	        	} else {
	        		event.setCancelled(true);
	        	}
	        }
	        // Buckets are only filled if the fluid's data value is 0, so we'll
	        // emulate the behavior with safe fluids.
	        else if (mat == Material.BUCKET) {
	        	if (plugin.getConfig().getBoolean("dispenser.enabled")) {
	        		System.out.println("Step 1");
	        		if (blockDispense.isLiquid() && blockDispense.getData() == 8) {
		        		System.out.println("Step 2");
		        		Inventory inv = new CraftInventory((TileEntityDispenser)(((CraftWorld)blockDispenser.getWorld()).getTileEntityAt(blockDispenser.getX(), blockDispenser.getY(), blockDispenser.getZ())));
		        		Material m = null;
		        		if (blockDispense.getType() == Material.STATIONARY_WATER)
		        			m = Material.WATER_BUCKET;
		        		if (blockDispense.getType() == Material.STATIONARY_LAVA)
		        			m = Material.LAVA_BUCKET;
		        		System.out.println("===" + event.getItem());
		        		ItemStack stack = inv.getItem(inv.first(Material.BUCKET));
		        		System.out.println(stack.getAmount());
		        		if (stack.getAmount() == 0) {
		        			System.out.println("Setting 1");
		        			stack.setType(m);
		        		} else {
		        			System.out.println("Setting multiple");
		        			stack.setAmount(stack.getAmount() - 1);
		        			if (!inv.addItem(new ItemStack(m)).isEmpty()) {
		        				(new DispenseBehaviorItem()).a(new SourceBlock((World)blockDispenser.getWorld(), blockDispenser.getX(), blockDispenser.getY(), blockDispenser.getZ()), new net.minecraft.server.v1_7_R1.ItemStack(Item.d(m.getId()), 1));
		        			}
		        		}
		        		blockDispense.setType(Material.AIR);
		        		event.setCancelled(true);
	        		}
	        	}
	        }
		}
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        Block block = event.getBlock();

        if (plugin.isSafeLiquid(event.getBlock())) {
            event.setCancelled(true);
        }
        
        if (plugin.blockCache.containsKey(block.getLocation())) {
        	// This difference should always be 5
        	if (block.getWorld().getTime() - plugin.blockCache.get(block.getLocation()) <= 10) {
        		plugin.setBlockSafe(block);
        		event.setCancelled(true);
        	}
        	plugin.blockCache.remove(block.getLocation());
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        if (event.getBlock().getType() == Material.ICE) {
            plugin.queueSafeBlock(event.getBlock());
        } 
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.ICE) {
            if (!event.getPlayer().getItemInHand().containsEnchantment(Enchantment.SILK_TOUCH)) {
                // If we are breaking the block with an enchanted pick then don't replace it with air, we want it to drop as an item
                //event.getBlock().setTypeId(0);
                plugin.queueSafeBlock(block);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();

        if (block.getType() == Material.DISPENSER) {
            System.out.println(block.getLocation());
            plugin.queueSafeBlock(block);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        Block block = event.getBlockClicked().getRelative(event.getBlockFace());

    	if (plugin.getConfig().getBoolean("bucket.enabled")) {
        	if (plugin.getConfig().getBoolean("bucket.safe")) {
        		plugin.queueSafeBlock(block);
        	}
    	} else {
    		event.setCancelled(true);
    	}
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
    	Player player = event.getPlayer();

    	if (event.isBlockInHand() && event.getItem().getType() == Material.getMaterial(plugin.getConfig().getString("tool.block")) && player.hasPermission("safebuckets.tools.block") && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
    		Block block = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation().getBlock();
    		if (plugin.isSafeLiquid(block)) {
    			player.sendMessage("SafeBuckets: (X=" + block.getX() + ", Z=" + block.getZ() + ", Y=" + block.getY() + ") safe");
    		} else {
    			player.sendMessage("SafeBuckets: (X=" + block.getX() + ", Z=" + block.getZ() + ", Y=" + block.getY() + ") unsafe");
    		}
    		event.setCancelled(true);
    	}
    	else if (event.isBlockInHand() && event.getItem().getType() == Material.getMaterial(plugin.getConfig().getString("tool.block")) && player.hasPermission("safebuckets.tools.block") && event.getAction() == Action.LEFT_CLICK_BLOCK) {
    		Block block = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation().getBlock();
    		if (plugin.isSafeLiquid(block)) {
    			player.sendMessage("SafeBuckets: (X=" + block.getX() + ", Z=" + block.getZ() + ", Y=" + block.getY() + ") removed safe");
                plugin.setBlockUnsafe(block);
    		} else {
    			player.sendMessage("SafeBuckets: (X=" + block.getX() + ", Z=" + block.getZ() + ", Y=" + block.getY() + ") set safe");
                plugin.setBlockSafe(block);
    		}
    		event.setCancelled(true);
    	}
    	else if (event.hasItem() && event.getItem().getType() == Material.getMaterial(plugin.getConfig().getString("tool.item")) && player.hasPermission("safebuckets.tools.item") && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
    		Block block = event.getClickedBlock();
    		if (plugin.isSafeLiquid(block)) {
    			player.sendMessage("SafeBuckets: (X=" + block.getX() + ", Z=" + block.getZ() + ", Y=" + block.getY() + ") safe");
    		} else {
    			player.sendMessage("SafeBuckets: (X=" + block.getX() + ", Z=" + block.getZ() + ", Y=" + block.getY() + ") unsafe");
    		}
    		event.setCancelled(true);
    	}
    	else if (event.hasItem() && event.getItem().getType() == Material.getMaterial(plugin.getConfig().getString("tool.item")) && player.hasPermission("safebuckets.tools.item") && event.getAction() == Action.LEFT_CLICK_BLOCK) {
    		Block block = event.getClickedBlock();
    		if (plugin.isSafeLiquid(block)) {
    			player.sendMessage("SafeBuckets: (X=" + block.getX() + ", Z=" + block.getZ() + ", Y=" + block.getY() + ") removed safe");
                plugin.setBlockUnsafe(block);
    		} else {
    			player.sendMessage("SafeBuckets: (X=" + block.getX() + ", Z=" + block.getZ() + ", Y=" + block.getY() + ") set safe");
                plugin.setBlockSafe(block);
    		}
    		event.setCancelled(true);
    	}
    }
}
