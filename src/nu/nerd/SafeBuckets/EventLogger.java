package nu.nerd.SafeBuckets;

import me.botsko.prism.actionlibs.ActionFactory;
import me.botsko.prism.actionlibs.RecordingQueue;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;

import de.diddiz.LogBlock.Consumer;
import de.diddiz.LogBlock.LogBlock;

public class EventLogger {

    private final SafeBuckets plugin;
    private Consumer lbConsumer;

    private boolean useLogBlock;
    private boolean usePrism;

    public EventLogger(SafeBuckets pl) {
        plugin = pl;
        useLogBlock = false;
        usePrism = false;
    }

    public void enableLogBlock(LogBlock lb) {
        lbConsumer = lb.getConsumer();
        useLogBlock = true;
    }

    public void enablePrism() {
        usePrism = true;
    }

    public boolean canLog() {
        return useLogBlock || usePrism;
    }

    public void logEvent(boolean log, Player player, BlockState before, Block after) {
        if (log) {
            logEvent(player, before, after);
        }
    }

    public void logEvent(Player player, BlockState before, Block after) {
        if (useLogBlock) {
            logEventWithLogBlock(player, before, after);
        }
        if (usePrism) {
            logEventWithPrism(player, before, after);
        }
    }

    private void logEventWithLogBlock(Player player, BlockState before, Block after) {
        Material mat = plugin.getStationaryMaterial(after.getType());

        String name;
        if (player == null) {
            name = mat == Material.STATIONARY_WATER ? before.getType() == Material.ICE ? "SnowFade" : "WaterFlow" : "LavaFlow";
        }
        else {
            name = player.getName();
        }

        lbConsumer.queueBlockReplace(name, before, after.getState());
    }

    private void logEventWithPrism(Player player, BlockState before, Block after) {
        String type;
        Material mat = plugin.getStationaryMaterial(after.getType());
        Material beforeType = before.getType();

        if (plugin.isBlockSafe(after) && (plugin.getStationaryMaterial(before.getType()) == after.getType())) {
            type = "safebuckets-fluid-safe";
        }
        else if (!plugin.isBlockSafe(after) && plugin.getStationaryMaterial(before.getType()) == plugin.getStationaryMaterial(after.getType()) && before.getRawData() == 15) {
            type = "safebuckets-fluid-unsafe";
            beforeType = plugin.getStationaryMaterial(beforeType);
        }
        else {
            type = ((plugin.getStationaryMaterial(before.getType()) == Material.STATIONARY_WATER || plugin.getStationaryMaterial(before.getType()) == Material.STATIONARY_LAVA) && after.getType() == Material.AIR) ? "bucket-fill" : before.getType() == Material.ICE && mat == Material.STATIONARY_WATER ? "block-fade" : mat == Material.STATIONARY_WATER ? "water-bucket" : mat == Material.STATIONARY_LAVA ? "lava-bucket" : "block-place";
        }

        String name;
        if (player == null) {
            name = mat == Material.STATIONARY_WATER ? before.getType() == Material.ICE ? "Environment" : "Water" : "Lava";
        }
        else {
            name = player.getName();
        }

        RecordingQueue.addToQueue(ActionFactory.createBlockChange(type, after.getLocation(), beforeType.getId(), before.getRawData(), after.getTypeId(), after.getData(), name));
    }
}
