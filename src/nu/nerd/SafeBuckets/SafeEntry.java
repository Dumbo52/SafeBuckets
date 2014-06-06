package nu.nerd.SafeBuckets;

import org.bukkit.entity.Player;

public class SafeEntry {
    private long time;
    private boolean log;
    private Player player;
    public SafeEntry(long time, boolean log, Player player) {
        this.time = time;
        this.log = log;
        this.player = player;
    }
    public long getTime() {
        return time;
    }
    public boolean isLogged() {
        return log;
    }
    public Player getPlayer() {
        return player;
    }
}
