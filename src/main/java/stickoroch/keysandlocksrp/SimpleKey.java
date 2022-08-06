package stickoroch.keysandlocksrp;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class SimpleKey {
    public String name;
    public int x, y ,z;
    public String world;
    public int count;

    public Location loc(){
        return world != null ? new Location(Bukkit.getWorld(world), x, y, z) : null;
    }
}
