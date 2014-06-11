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

    private static Consumer lbConsumer;

    private static boolean useLogBlock = false;
    private static boolean usePrism = false;

    public static void enableLogBlock(LogBlock lb) {
        lbConsumer = lb.getConsumer();
        useLogBlock = true;
    }

    public static void enablePrism() {
        usePrism = true;
    }

    public static boolean canLog() {
        return useLogBlock || usePrism;
    }

    public static void logEvent(boolean log, Player player, BlockState before, Block after) {
        if (log) {
            logEvent(player, before, after);
        }
    }

    public static void logEvent(Player player, BlockState before, Block after) {
        if (useLogBlock) {
            logEventWithLogBlock(player, before, after);
        }
        if (usePrism) {
            logEventWithPrism(player, before, after);
        }
    }

    private static void logEventWithLogBlock(Player player, BlockState before, Block after) {
        Material mat = FluidUtils.getStationaryMaterial(after.getType());

        String name;
        if (player == null) {
            name = mat == Material.STATIONARY_WATER ? before.getType() == Material.ICE ? "SnowFade" : "WaterFlow" : "LavaFlow";
        }
        else {
            name = player.getName();
        }

        lbConsumer.queueBlockReplace(name, before, after.getState());
    }

    private static void logEventWithPrism(Player player, BlockState before, Block after) {
        String type;
        Material mat = FluidUtils.getStationaryMaterial(after.getType());
        Material beforeType = before.getType();

        if (FluidUtils.isBlockSafe(after) && (FluidUtils.getStationaryMaterial(before.getType()) == after.getType())) {
            type = "safebuckets-fluid-safe";
        }
        else if (!FluidUtils.isBlockSafe(after) && FluidUtils.getStationaryMaterial(before.getType()) == FluidUtils.getStationaryMaterial(after.getType()) && before.getRawData() == 15) {
            type = "safebuckets-fluid-unsafe";
            beforeType = FluidUtils.getStationaryMaterial(beforeType);
        }
        else {
            type = ((FluidUtils.getStationaryMaterial(before.getType()) == Material.STATIONARY_WATER || FluidUtils.getStationaryMaterial(before.getType()) == Material.STATIONARY_LAVA) && after.getType() == Material.AIR) ? "bucket-fill" : before.getType() == Material.ICE && mat == Material.STATIONARY_WATER ? "block-fade" : mat == Material.STATIONARY_WATER ? "water-bucket" : mat == Material.STATIONARY_LAVA ? "lava-bucket" : "block-place";
        }

        String name;
        if (player == null) {
            name = mat == Material.STATIONARY_WATER ? before.getType() == Material.ICE ? "Environment" : "Water" : "Lava";
        }
        else {
            name = player.getName();
        }
        System.out.println(type + ", " + after + ", " + beforeType + ", " + before + ", " + after + ", " + name);
        RecordingQueue.addToQueue(ActionFactory.createBlockChange(type, after.getLocation(), beforeType.getId(), before.getRawData(), after.getTypeId(), after.getData(), name));
    }
}
