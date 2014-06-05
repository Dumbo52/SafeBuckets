package nu.nerd.SafeBuckets;

import java.lang.reflect.Method;
import java.util.Arrays;

import net.minecraft.server.v1_7_R3.MathHelper;
import net.minecraft.server.v1_7_R3.Vec3D;
import net.minecraft.server.v1_7_R3.DispenseBehaviorItem;
import net.minecraft.server.v1_7_R3.EntityHuman;
import net.minecraft.server.v1_7_R3.EnumMovingObjectType;
import net.minecraft.server.v1_7_R3.Item;
import net.minecraft.server.v1_7_R3.MovingObjectPosition;
import net.minecraft.server.v1_7_R3.SourceBlock;
import net.minecraft.server.v1_7_R3.TileEntityDispenser;
import net.minecraft.server.v1_7_R3.World;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_7_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_7_R3.inventory.CraftInventory;
import org.bukkit.craftbukkit.v1_7_R3.inventory.CraftItemStack;
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
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Dispenser; //> Material because we need the getFacing method (DirectionalContainer.class)

public class SafeBucketsListener implements Listener {

    private final SafeBuckets plugin;

    SafeBucketsListener(SafeBuckets instance) {
        plugin = instance;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (plugin.BUCKET_SAFE) {
            if (plugin.flag) {
                event.setCancelled(true);
                return;
            }
            Block block = event.getBlock();
            if (block.isLiquid()) {
                if (plugin.isBlockSafe(block)) {
                    if (event.getChangedType() == Material.AIR) {
                        Block above = block.getRelative(BlockFace.UP);
                        if (above.getType() == plugin.getFlowingMaterial(block.getType())) {
                            plugin.setBlockSafe(block, false, plugin.LOG_NATURAL_FLOW, plugin.getVirtualName(block));
                        }
                        event.setCancelled(true);
                    }
                    else if (plugin.getStationaryMaterial(event.getChangedType()) != Material.STATIONARY_WATER && plugin.getStationaryMaterial(event.getChangedType()) != Material.STATIONARY_LAVA) {
                        event.setCancelled(true);
                    }
                    else if (block.getType() == event.getChangedType()) {
                        Block[] adj = new Block[] { block.getRelative(BlockFace.NORTH), block.getRelative(BlockFace.SOUTH), block.getRelative(BlockFace.WEST), block.getRelative(BlockFace.EAST) };
                        for (Block b : adj) {
                            if (b.getType() == plugin.getFlowingMaterial(block.getType()) && b.getData() == block.getData() || b.getType() == block.getType() && b.getData() == 0) {
                                plugin.setBlockSafe(b, true, plugin.LOG_NATURAL_FLOW, plugin.getVirtualName(b));
                            }
                        }
                        event.setCancelled(true);
                    }
                }
                else if (block.getData() == 15) {
                    if (event.getChangedType() == plugin.getStationaryMaterial(block.getType())) {
                        plugin.setBlockSafe(block, true, plugin.LOG_NATURAL_FLOW, plugin.getVirtualName(block));
                        event.setCancelled(true);
                    }
                    else if (event.getChangedType() == block.getType()) {
                        plugin.simplifyBelow(block);
                        event.setCancelled(true);
                    }
                }
                else if (block.getData() == 7 && event.getChangedType() == plugin.getFlowingMaterial(block.getType())) {
                    Block below = block.getRelative(BlockFace.DOWN);
                    if (below.getType() == block.getType() && below.getData() == 15) {
                        Block[] adj = new Block[] { block.getRelative(BlockFace.NORTH), block.getRelative(BlockFace.SOUTH), block.getRelative(BlockFace.WEST), block.getRelative(BlockFace.EAST) };
                        for (Block b : adj) {
                            if (b.getData() == 6 && b.getType() == block.getType()) {
                                return;
                            }
                        }
                        plugin.simplifyBelow(block);
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDispense(BlockDispenseEvent event) {
        if (event.getBlock().getState().getData() instanceof Dispenser) {
            Material mat = event.getItem().getType();
            Dispenser dispenser = (Dispenser) event.getBlock().getState().getData();
            Block blockDispenser = event.getBlock();
            Block blockDispense = blockDispenser.getRelative(dispenser.getFacing());

            if (mat == Material.LAVA_BUCKET || mat == Material.WATER_BUCKET) {
                if (plugin.DISPENSER_ENABLED) {
                    if (plugin.DISPENSER_SAFE && !plugin.isRegistered(blockDispenser)) {
                        plugin.queueSafeBlock(blockDispense, false, null);
                    }
                    StringBuilder message = new StringBuilder("SafeBuckets: Dispensing (").append(event.getBlock().getX()).append(",").append(event.getBlock().getY()).append(",").append(event.getBlock().getZ()).append(") ");
                    if (plugin.isRegistered(blockDispenser)) {
                        message.append("un");
                    }
                    message.append("safe");

                    if (plugin.DEBUG_PLAYERS) {
                        plugin.getServer().broadcast(message.toString(), "safebuckets.debug");
                    }

                    if (plugin.DEBUG_CONSOLE) {
                        SafeBuckets.log.info(message.toString());
                    }
                }
                else {
                    event.setCancelled(true);
                }
            }
            // Buckets are only filled if the fluid's data value is 0, so we'll
            // emulate the behavior with safe fluids.
            else if (mat == Material.BUCKET) {
                if (plugin.DISPENSER_ENABLED) {
                    if (blockDispense.isLiquid() && plugin.isBlockSafe(blockDispense)) {
                        Inventory inv = new CraftInventory((TileEntityDispenser) (((CraftWorld) blockDispenser.getWorld()).getTileEntityAt(blockDispenser.getX(), blockDispenser.getY(), blockDispenser.getZ())));
                        Material m = null;
                        if (blockDispense.getType() == Material.STATIONARY_WATER)
                            m = Material.WATER_BUCKET;
                        if (blockDispense.getType() == Material.STATIONARY_LAVA)
                            m = Material.LAVA_BUCKET;
                        ItemStack stack = inv.getItem(inv.first(Material.BUCKET));
                        if (stack.getAmount() == 0) {
                            stack.setType(m);
                        }
                        else {
                            stack.setAmount(stack.getAmount() - 1);
                            if (!inv.addItem(new ItemStack(m)).isEmpty()) {
                                (new DispenseBehaviorItem()).a(new SourceBlock((World) blockDispenser.getWorld(), blockDispenser.getX(), blockDispenser.getY(), blockDispenser.getZ()), new net.minecraft.server.v1_7_R3.ItemStack(Item.d(m.getId()), 1));
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
        if (plugin.blockCache.containsKey(block.getLocation())) {
            if (plugin.BUCKET_PLACE_SAFE) {
                SafeEntry entry = plugin.blockCache.get(block.getLocation());
                // This difference should always be 5 for water and 30 for lava
                // (10 in Nether).
                if (block.getWorld().getTime() - entry.getTime() <= 40) {
                    plugin.setBlockSafe(block, true, entry.isLogged(), entry.getPlayer());
                    event.setCancelled(true);
                }
            }
            plugin.blockCache.remove(block.getLocation());
            return;
        }
        if (plugin.BUCKET_SAFE) {
            if (plugin.isBlockSafe(block)) {
                if (event.getToBlock().getType() == Material.AIR) {
                    event.setCancelled(true);
                }
            }
            else {
                if (plugin.isBlockSafe(event.getToBlock())) {
                    plugin.setBlockSafe(event.getToBlock(), false, plugin.LOG_NATURAL_FLOW, plugin.getVirtualName(event.getToBlock()));
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        if (plugin.BUCKET_PLACE_SAFE) {
            Block b = event.getBlock();
            if (b.getType() == Material.ICE) {
                // In order to catch this properly, we'll need to cancel the
                // event and push it along a little.
                if (plugin.LOG_NATURAL_FLOW) {
                    plugin.getConsumer().queueBlockReplace("SnowFade", b.getState(), 9, (byte) 15);
                }
                b.setType(Material.STATIONARY_WATER);
                b.setData((byte) 15);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (plugin.BUCKET_PLACE_SAFE) {
            if (block.getType() == Material.ICE) {
                if (!event.getPlayer().getItemInHand().containsEnchantment(Enchantment.SILK_TOUCH) && !((CraftPlayer) event.getPlayer()).getHandle().abilities.canInstantlyBuild) {
                    // If we are breaking the block with an enchanted pick then
                    // don't replace it with air, we want it to drop as an item
                    // event.getBlock().setTypeId(0);
                    plugin.queueSafeBlock(block, plugin.LOG_MANUAL_FLOW, event.getPlayer().getName());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        Block block = event.getBlockClicked().getRelative(event.getBlockFace());

        if (plugin.BUCKET_ENABLED) {
            if (plugin.BUCKET_PLACE_SAFE) {
                if (plugin.getStationaryMaterial(block.getType()) != plugin.getStationaryMaterial(event.getBucket()) || block.getData() != 0 && block.getData() != 15) {
                    // Create only 1 LB entry instead of 3.
                    if (plugin.LOG_MANUAL_FLOW) {
                        plugin.getConsumer().queueBlockReplace(event.getPlayer().getName(), block.getState(), event.getBucket() == Material.WATER_BUCKET ? 9 : 11, (byte) 15);
                    }
                    plugin.flag = true;
                    block.setType(plugin.getStationaryMaterial(event.getBucket()));
                    block.setData((byte) 15);
                    plugin.flag = false;
                    if (!((CraftPlayer) event.getPlayer()).getHandle().abilities.canInstantlyBuild) {
                        event.getPlayer().getItemInHand().setType(Material.BUCKET);
                    }
                }
                event.setCancelled(true);
            }
        }
        else {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getMaterial() == Material.BUCKET && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            MovingObjectPosition pos = plugin.raytrace(((CraftPlayer) player).getHandle(), ((CraftWorld) player.getWorld()).getHandle());
            if (pos != null && pos.type == EnumMovingObjectType.BLOCK) {
                Block clicked = player.getWorld().getBlockAt(pos.b, pos.c, pos.d);
                if (plugin.isBlockSafe(clicked)) {
                    if (!((CraftPlayer) event.getPlayer()).getHandle().abilities.canInstantlyBuild) {
                        event.getPlayer().getItemInHand().setType(plugin.getBucketMaterial(clicked.getType()));
                    }
                    clicked.setType(Material.AIR);
                    clicked.setData((byte) 0);
                    event.setCancelled(true);
                }
            }
        }

        if (event.isBlockInHand() && event.getItem().getType() == plugin.TOOL_BLOCK && player.hasPermission("safebuckets.tools.block.use") && event.getAction() == Action.RIGHT_CLICK_BLOCK && plugin.canUseToolBlock(player)) {
            Block block = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation().getBlock();
            StringBuilder msg = new StringBuilder();
            String coords = new StringBuilder("(X=").append(block.getX()).append(", Y=").append(block.getY()).append(", Z=").append(block.getZ()).append(")").toString();
            if (block.isLiquid()) {
                msg.append("Fluid at ").append(coords).append(" is ").append(plugin.isBlockSafe(block) ? "safe." : "unsafe.");
            }
            else {
                msg.append(block.getType().toString()).append(" at ").append(coords).append(" is not a fluid!");
            }
            plugin.message(player, msg.toString());
            event.setCancelled(true);
        }
        else if (event.isBlockInHand() && event.getItem().getType() == plugin.TOOL_BLOCK && player.hasPermission("safebuckets.tools.block.set") && event.getAction() == Action.LEFT_CLICK_BLOCK && plugin.canUseToolBlock(player)) {
            Block block = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation().getBlock();
            StringBuilder msg = new StringBuilder();
            String coords = new StringBuilder("(X=").append(block.getX()).append(", Y=").append(block.getY()).append(", Z=").append(block.getZ()).append(")").toString();
            if (block.isLiquid() && (block.getData() == 0 || block.getData() == 15)) {
                boolean safe = plugin.isBlockSafe(block);
                msg.append(coords).append(" set ").append(safe ? "unsafe." : "safe. ");
                if (!safe) {
                    msg.append(plugin.setBlockSafe(block, true, plugin.LOG_MANUAL_FLOW, player.getName())).append(" child blocks affected.");
                }
                else {
                    plugin.setBlockSafe(block, false, plugin.LOG_MANUAL_FLOW, player.getName());
                }
            }
            else {
                msg.append(block.getType().toString()).append(" at ").append(coords).append(" is not a source fluid!");
            }
            plugin.message(player, msg.toString());
            event.setCancelled(true);
        }
        else if (event.hasItem() && event.getItem().getType() == plugin.TOOL_ITEM && player.hasPermission("safebuckets.tools.item.use") && event.getAction() == Action.RIGHT_CLICK_BLOCK && plugin.canUseTool(player)) {
            Block block = event.getClickedBlock();
            StringBuilder msg = new StringBuilder();
            String coords = new StringBuilder("(X=").append(block.getX()).append(", Y=").append(block.getY()).append(", Z=").append(block.getZ()).append(")").toString();
            if (block.getType() == Material.DISPENSER) {
                msg.append("Dispenser at ").append(coords).append(" is ").append(plugin.isRegistered(block) ? "registered." : "unregistered.");
            }
            else {
                msg.append(block.getType().toString()).append(" at ").append(coords).append(" is not a dispenser!");
            }
            plugin.message(player, msg.toString());
            event.setCancelled(true);
        }
        else if (event.hasItem() && event.getItem().getType() == plugin.TOOL_ITEM && player.hasPermission("safebuckets.tools.item.set") && event.getAction() == Action.LEFT_CLICK_BLOCK && plugin.canUseTool(player)) {
            Block block = event.getClickedBlock();
            StringBuilder msg = new StringBuilder();
            String coords = new StringBuilder("(X=").append(block.getX()).append(", Y=").append(block.getY()).append(", Z=").append(block.getZ()).append(")").toString();
            if (block.getType() == Material.DISPENSER) {
                msg.append("Dispenser at ").append(coords).append(plugin.isRegistered(block) ? " unregistered." : " registered.");
                plugin.registerBlock(block, !plugin.isRegistered(block));
            }
            else {
                msg.append(block.getType().toString()).append(" at ").append(coords).append(" is not a dispenser!");
            }
            plugin.message(player, msg.toString());
            event.setCancelled(true);
        }
    }
}
