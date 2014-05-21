package nu.nerd.SafeBuckets;

import org.bukkit.entity.Player;

public class SafeEntry {
    private long time;
    private Player player;
    public SafeEntry(long time, Player player) {
        this.time = time;
        this.player = player;
    }
    public long getTime() {
        return time;
    }
    public Player getPlayer() {
        return player;
    }
}
