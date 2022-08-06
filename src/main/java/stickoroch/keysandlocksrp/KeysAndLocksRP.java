package stickoroch.keysandlocksrp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public final class KeysAndLocksRP extends JavaPlugin {

    private static KeysAndLocksRP instance;
    public static KeysAndLocksRP getInstance(){return instance;}

    private static ItemManager itemManager;
    public static ItemManager getItemManager(){return itemManager;}

    public static final String NMS_VERSION = Bukkit.getServer().getClass().getPackage().getName().substring(23);
    @Override
    public void onEnable() {
        instance = this;
        itemManager = new ItemManager();
        Bukkit.getPluginManager().registerEvents(new LockManager(), this);

        saveDefaultConfig();

        getCommand("klreload").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
                reloadConfig();
                sender.sendMessage("CFG has been reloaded!");
                return true;
            }
        });

        getCommand("klgive").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

                if(sender instanceof Player){
                    Player p = (Player) sender;
                    p.getInventory().addItem(itemManager.getBunchOfKeys());
                    p.getInventory().addItem(itemManager.getLock());
                    p.getInventory().addItem(itemManager.getKey());
                }
                return true;
            }
        });
    }


    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
