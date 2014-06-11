package nu.nerd.SafeBuckets;

import net.minecraft.server.v1_7_R3.DispenseBehaviorItem;
import net.minecraft.server.v1_7_R3.EnumMovingObjectType;
import net.minecraft.server.v1_7_R3.Item;
import net.minecraft.server.v1_7_R3.Items;
import net.minecraft.server.v1_7_R3.MovingObjectPosition;
import net.minecraft.server.v1_7_R3.SourceBlock;
import net.minecraft.server.v1_7_R3.TileEntityDispenser;
import net.minecraft.server.v1_7_R3.World;

import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.v1_7_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_7_R3.inventory.CraftInventory;
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
            if (FluidUtils.isLocked()) {
                event.setCancelled(true);
                return;
            }
            Block block = event.getBlock();
            if (block.isLiquid()) {
                if (FluidUtils.isBlockSafe(block)) {
                    if (event.getChangedType() == Material.AIR) {
                        Block above = block.getRelative(BlockFace.UP);
                        if (above.getType() == FluidUtils.getFlowingMaterial(block.getType())) {
                            plugin.setBlockSafe(block, false, plugin.LOG_NATURAL_FLOW);
                        }
                        else if (block.getType() == Material.WATER) {
                            FluidUtils.setBlockSafe(block, true, false, 0);
                        }
                        event.setCancelled(true);
                    }
                    else if (FluidUtils.getStationaryMaterial(event.getChangedType()) != Material.STATIONARY_WATER && FluidUtils.getStationaryMaterial(event.getChangedType()) != Material.STATIONARY_LAVA) {
                        event.setCancelled(true);
                    }
                    else if (block.getType() == event.getChangedType()) {
                        Block[] adj = new Block[] { block.getRelative(BlockFace.NORTH), block.getRelative(BlockFace.SOUTH), block.getRelative(BlockFace.WEST), block.getRelative(BlockFace.EAST) };
                        for (Block b : adj) {
                            if (b.getType() == FluidUtils.getFlowingMaterial(block.getType()) && b.getData() == block.getData() || b.getType() == block.getType() && b.getData() == 0) {
                                plugin.setBlockSafe(b, true, plugin.LOG_NATURAL_FLOW);
                            }
                        }
                        event.setCancelled(true);
                    }
                    else if (block.getType() == FluidUtils.getStationaryMaterial(block.getType()) && event.getChangedType() == FluidUtils.getStationaryMaterial(event.getChangedType()) && block.getType() != event.getChangedType()) {
                        event.setCancelled(true);
                    }
                    if (event.isCancelled() && block.getType() == Material.STATIONARY_LAVA) {
                        boolean flag = FluidUtils.getStationaryMaterial(block.getRelative(BlockFace.UP).getType()) == Material.STATIONARY_WATER && !FluidUtils.isBlockSafe(block.getRelative(BlockFace.UP));
                        flag |= FluidUtils.getStationaryMaterial(block.getRelative(BlockFace.NORTH).getType()) == Material.STATIONARY_WATER && !FluidUtils.isBlockSafe(block.getRelative(BlockFace.NORTH));
                        flag |= FluidUtils.getStationaryMaterial(block.getRelative(BlockFace.SOUTH).getType()) == Material.STATIONARY_WATER && !FluidUtils.isBlockSafe(block.getRelative(BlockFace.SOUTH));
                        flag |= FluidUtils.getStationaryMaterial(block.getRelative(BlockFace.WEST).getType()) == Material.STATIONARY_WATER && !FluidUtils.isBlockSafe(block.getRelative(BlockFace.WEST));
                        flag |= FluidUtils.getStationaryMaterial(block.getRelative(BlockFace.EAST).getType()) == Material.STATIONARY_WATER && !FluidUtils.isBlockSafe(block.getRelative(BlockFace.EAST));
                        if (flag) {
                            BlockState prev = block.getState();
                            FluidUtils.lock();
                            block.setType(Material.OBSIDIAN);
                            block.setData((byte) 0);
                            FluidUtils.unlock();
                            EventLogger.logEvent(plugin.LOG_NATURAL_FLOW, null, prev, block);
                            FluidUtils.fizz(block);
                        }
                    }
                }
                else if (block.getData() == FluidUtils.safeData) {
                    if (event.getChangedType() == FluidUtils.getStationaryMaterial(block.getType())) {
                        plugin.setBlockSafe(block, true, plugin.LOG_NATURAL_FLOW);
                        event.setCancelled(true);
                    }
                    else if (event.getChangedType() == block.getType()) {
                        FluidUtils.simplifyBelow(block);
                        event.setCancelled(true);
                    }
                }
                else if (block.getData() == FluidUtils.safeData - 8 && event.getChangedType() == FluidUtils.getFlowingMaterial(block.getType())) {
                    Block below = block.getRelative(BlockFace.DOWN);
                    if (below.getType() == FluidUtils.getFlowingMaterial(block.getType()) && below.getData() == FluidUtils.safeData) {
                        Block[] adj = new Block[] { block.getRelative(BlockFace.NORTH), block.getRelative(BlockFace.SOUTH), block.getRelative(BlockFace.WEST), block.getRelative(BlockFace.EAST) };
                        for (Block b : adj) {
                            if (b.getData() == FluidUtils.safeData - 8 - 1 && b.getType() == block.getType()) {
                                return;
                            }
                        }
                        FluidUtils.simplifyBelow(block);
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
                    // Event only fires if the block in front of the dispenser can be replaced with water.
                    if (plugin.DISPENSER_SAFE && !FluidUtils.isRegistered(blockDispenser)) {
                        if (blockDispense.getType() != Material.AIR) {
                            blockDispense.breakNaturally();
                        }
                        blockDispense.setType(FluidUtils.getStationaryMaterial(mat));
                        blockDispense.setData(FluidUtils.safeData);
                        event.setCancelled(true);
                    }
                    StringBuilder message = new StringBuilder("SafeBuckets: Dispensing (").append(event.getBlock().getX()).append(",").append(event.getBlock().getY()).append(",").append(event.getBlock().getZ()).append(") ");
                    if (FluidUtils.isRegistered(blockDispenser)) {
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
                    if (blockDispense.isLiquid() && FluidUtils.isBlockSafe(blockDispense)) {
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
        if (plugin.BUCKET_SAFE) {
            if (FluidUtils.isBlockSafe(block)) {
                if (event.getToBlock().getType() == Material.AIR) {
                    event.setCancelled(true);
                }
            }
            else {
                if (FluidUtils.isBlockSafe(event.getToBlock()) && !(!FluidUtils.isBlockSafe(block) && block.getType() == Material.STATIONARY_LAVA && event.getToBlock().getType() == Material.STATIONARY_WATER)) {
                    plugin.setBlockSafe(event.getToBlock(), false, plugin.LOG_NATURAL_FLOW);
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
                BlockState prev = b.getState();
                b.setType(Material.STATIONARY_WATER);
                b.setData(FluidUtils.safeData);
                event.setCancelled(true);
                EventLogger.logEvent(plugin.LOG_NATURAL_FLOW, null, prev, b);
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
                    BlockState prev = block.getState();
                    block.setType(Material.STATIONARY_WATER);
                    block.setData(FluidUtils.safeData);
                    EventLogger.logEvent(plugin.LOG_MANUAL_FLOW, null, prev, block);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        Block block = event.getBlockClicked().getRelative(event.getBlockFace());

        if (plugin.BUCKET_ENABLED) {
            if (plugin.BUCKET_PLACE_SAFE) {
                if (FluidUtils.getStationaryMaterial(block.getType()) != FluidUtils.getStationaryMaterial(event.getBucket()) || !FluidUtils.isSource(block)) {
                    if (!(block.getWorld().getEnvironment() == Environment.NETHER && event.getBucket() == Material.WATER_BUCKET) && (block.getType().isTransparent() || block.isLiquid())) {
                        if (block.getType() != Material.AIR && !block.isLiquid()) {
                            block.breakNaturally();
                        }
                        FluidUtils.updateNeighbors(block);
                        BlockState prev = block.getState();
                        FluidUtils.lock();
                        block.setType(FluidUtils.getStationaryMaterial(event.getBucket()));
                        block.setData(FluidUtils.safeData);
                        FluidUtils.unlock();
                        if (!((CraftPlayer) event.getPlayer()).getHandle().abilities.canInstantlyBuild) {
                            event.getPlayer().getItemInHand().setType(Material.BUCKET);
                        }
                        EventLogger.logEvent(plugin.LOG_MANUAL_FLOW, event.getPlayer(), prev, block);
                        if (event.getBucket() == Material.WATER_BUCKET) {
                            BlockFace[] adj = new BlockFace[] {BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST};
                            for (BlockFace face : adj) {
                                Block b = block.getRelative(face);
                                if (FluidUtils.getStationaryMaterial(b.getType()) == Material.STATIONARY_LAVA) {
                                    prev = b.getState();
                                    FluidUtils.lock();
                                    if (FluidUtils.isSource(b)) {
                                        b.setType(Material.OBSIDIAN);
                                        b.setData((byte) 0);
                                    }
                                    else {
                                        b.setType(Material.COBBLESTONE);
                                        b.setData((byte) 0);
                                    }
                                    FluidUtils.unlock();
                                    EventLogger.logEvent(plugin.LOG_MANUAL_FLOW, event.getPlayer(), prev, b);
                                    FluidUtils.fizz(b);
                                    FluidUtils.updateNeighbors(b, Material.STATIONARY_LAVA);
                                }
                            }
                        }
                        else if (event.getBucket() == Material.LAVA_BUCKET) {
                            BlockFace[] adj = new BlockFace[] {BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST};
                            for (BlockFace face : adj) {
                                Block b = block.getRelative(face);
                                if (FluidUtils.getStationaryMaterial(b.getType()) == Material.STATIONARY_WATER) {
                                    prev = block.getState();
                                    FluidUtils.lock();
                                    block.setType(Material.OBSIDIAN);
                                    block.setData((byte) 0);
                                    FluidUtils.unlock();
                                    EventLogger.logEvent(plugin.LOG_MANUAL_FLOW, event.getPlayer(), prev, block);
                                    FluidUtils.fizz(block);
                                    FluidUtils.updateNeighbors(block, Material.STATIONARY_WATER);
                                    break;
                                }
                            }
                        }
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
                if (FluidUtils.isBlockSafe(clicked)) {
                    if (!((CraftPlayer) event.getPlayer()).getHandle().abilities.canInstantlyBuild) {
                        event.getPlayer().getItemInHand().setType(FluidUtils.getBucketMaterial(clicked.getType()));
                    }
                    BlockState prev = clicked.getState();
                    clicked.setType(Material.AIR);
                    clicked.setData((byte) 0);
                    EventLogger.logEvent(event.getPlayer(), prev, clicked);
                    event.setCancelled(true);
                }
            }
        }
        
        if (event.getMaterial() == Material.GLASS_BOTTLE && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            MovingObjectPosition pos = plugin.raytrace(((CraftPlayer) player).getHandle(), ((CraftWorld) player.getWorld()).getHandle());
            if (pos != null && pos.type == EnumMovingObjectType.BLOCK) {
                Block clicked = player.getWorld().getBlockAt(pos.b, pos.c, pos.d);
                if (FluidUtils.isBlockSafe(clicked) && clicked.getType() == Material.STATIONARY_WATER) {
                    ItemStack stack = player.getItemInHand();
                    boolean creative = ((CraftPlayer) player).getHandle().abilities.canInstantlyBuild;
                    if (!creative) {
                        stack.setAmount(stack.getAmount() - 1);
                    }
                    if (stack.getAmount() <= 0 || creative && stack.getAmount() == 1) {
                        stack.setType(Material.POTION);
                        stack.setAmount(1);
                    }
                    else {
                        if (!player.getInventory().addItem(new ItemStack(Material.POTION)).isEmpty()) {
                            ((CraftPlayer) player).getHandle().drop(new net.minecraft.server.v1_7_R3.ItemStack(Items.POTION, 1, 0), false);
                        }
                        else {
                            player.updateInventory();
                        }
                    }
                    event.setCancelled(true);
                }
            }
        }

        if (event.isBlockInHand() && event.getItem().getType() == plugin.TOOL_BLOCK && player.hasPermission("safebuckets.tools.block.use") && event.getAction() == Action.RIGHT_CLICK_BLOCK && plugin.canUseToolBlock(player)) {
            Block block = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation().getBlock();
            StringBuilder msg = new StringBuilder();
            String coords = new StringBuilder("(X=").append(block.getX()).append(", Y=").append(block.getY()).append(", Z=").append(block.getZ()).append(")").toString();
            if (block.isLiquid()) {
                msg.append("Fluid at ").append(coords).append(" is ").append(FluidUtils.isBlockSafe(block) ? "safe." : "unsafe.");
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
            if (block.isLiquid() && (block.getData() == 0 || block.getData() == FluidUtils.safeData)) {
                boolean safe = FluidUtils.isBlockSafe(block);
                msg.append(coords).append(" set ").append(safe ? "unsafe." : "safe. ");
                if (!safe) {
                    msg.append(plugin.setBlockSafe(block, true, plugin.LOG_MANUAL_FLOW, player)).append(" child blocks affected.");
                }
                else {
                    plugin.setBlockSafe(block, false, plugin.LOG_MANUAL_FLOW, player);
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
                msg.append("Dispenser at ").append(coords).append(" is ").append(FluidUtils.isRegistered(block) ? "registered." : "unregistered.");
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
                msg.append("Dispenser at ").append(coords).append(FluidUtils.isRegistered(block) ? " unregistered." : " registered.");
                FluidUtils.registerBlock(block, !FluidUtils.isRegistered(block));
            }
            else {
                msg.append(block.getType().toString()).append(" at ").append(coords).append(" is not a dispenser!");
            }
            plugin.message(player, msg.toString());
            event.setCancelled(true);
        }
    }

    // Prism events
    // Prism only provides an event call for rollbacks, not restores.
    // TODO Make this work in the future?
    /*
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPrismBlocksRollbackEvent(PrismBlocksRollbackEvent event) {
        for (BlockStateChange change : event.getBlockStateChanges()) {
            BlockState before = change.getOriginalBlock();
            BlockState after = change.getNewBlock();
            Material mat = plugin.getStationaryMaterial(after.getType());
            if ((mat == Material.STATIONARY_WATER || mat == Material.STATIONARY_LAVA) && plugin.getStationaryMaterial(before.getType()) == mat) {
                if (before.getRawData() == 0 && after.getRawData() == 15) {
                    plugin.removeChildFlows(after.getBlock(), 0);
                }
            }
        }
    }
    */

}
