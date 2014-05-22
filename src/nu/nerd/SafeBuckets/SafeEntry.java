package nu.nerd.SafeBuckets;

public class SafeEntry {
    private long time;
    private boolean log;
    private String player;
    public SafeEntry(long time, boolean log, String player) {
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
    public String getPlayer() {
        return player;
    }
}
