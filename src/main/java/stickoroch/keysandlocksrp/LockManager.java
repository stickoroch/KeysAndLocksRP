package stickoroch.keysandlocksrp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.security.Key;
import java.util.*;

public class LockManager implements Listener {



    public LockManager(){
    }

    @EventHandler
    public void onCraft(CraftItemEvent e){
        if(Arrays.stream(e.getInventory().getMatrix())
                .noneMatch(x -> ItemManager.isKey(x)
                        || ItemManager.isBunchOfKeys(x)
                        || ItemManager.isLock(x))) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent e){
        if(Arrays.stream(e.getInventory().getMatrix())
                .noneMatch(x -> ItemManager.isKey(x)
                        || ItemManager.isBunchOfKeys(x)
                        || ItemManager.isLock(x))) return;
        e.getInventory().setResult(new ItemStack(Material.AIR));
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e){
        if(ItemManager.isBunchOfKeys(e.getItemInHand())
                || ItemManager.isLock(e.getItemInHand())
                || ItemManager.isKey(e.getItemInHand())){
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e){
        opensBok.remove(e.getPlayer());
    }

    @EventHandler
    public void onInteracInv(InventoryClickEvent e){
        if(!(e.getCurrentItem() != null  && !KeysAndLocksRP.getItemManager().isKey(e.getCurrentItem())))return;

        if(!opensBok.contains((Player) e.getWhoClicked()))return;

        e.setCancelled(true);

    }

    @EventHandler
    public void onClickToLockBlock(PlayerInteractEvent e){
        if(e.getItem() != null && ItemManager.isBunchOfKeys(e.getItem())) {
            if(e.getAction() == Action.RIGHT_CLICK_AIR || (e.getAction() == Action.RIGHT_CLICK_BLOCK && !isLocked(e.getClickedBlock()))){
                Inventory i = Bukkit.createInventory(null,
                        KeysAndLocksRP.getInstance().getConfig()
                                .getInt("params.bokSize"),
                        KeysAndLocksRP.getItemManager().getBunchOfKeys().getItemMeta().getDisplayName());

                i.setContents(getBOK(e.getItem()));

                e.getPlayer().openInventory(i);
                opensBok.add(e.getPlayer());
                return;
            }
        }

        if(e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if(e.getClickedBlock() == null || !isLockBlock(e.getClickedBlock())) return;

        Block b = e.getClickedBlock();
        if(b.getBlockData() instanceof Door
                && ((Bisected) b.getBlockData()).getHalf().toString().equals("TOP")) {
            b = b.getWorld().getBlockAt(b.getLocation().add(0d,-1d, 0d));
        }
        else if(b.getState() instanceof Chest
                && ((Chest)b.getState()).getInventory() instanceof DoubleChestInventory)
        {
            b = ((DoubleChest)((Chest) b.getState()).getInventory().getHolder()).getLocation().getBlock();
        }

        if(e.getItem() == null) {
            if(isLocked(b)){
                e.setCancelled(true);
                sendLockMess(e.getPlayer(), e.getClickedBlock());
            }
            return;
        }

        if(b.hasMetadata(lockTag()) && !(
                ItemManager.isLock(e.getItem())
                        || ItemManager.isBunchOfKeys(e.getItem())
                        || ItemManager.isKey(e.getItem()))){
            e.setCancelled(true);
            sendLockMess(e.getPlayer(), e.getClickedBlock());
            return;
        }

        if(b.hasMetadata(lockTag()) || (e.getItem() != null
                && (
                ItemManager.isLock(e.getItem())
                        || ItemManager.isBunchOfKeys(e.getItem())
                        || ItemManager.isKey(e.getItem())
        ))) e.setCancelled(true);

        if(ItemManager.isLock(e.getItem()))
        {
            if(isLocked(e.getClickedBlock()))
            {
                sendMessHB(e.getPlayer(), "localization.messages.blockAlreadyLocked");
            }
            else
            {
                sendMessHB(e.getPlayer(), "localization.messages.lockUp");

                e.getItem().setAmount(e.getItem().getAmount() - 1);
                b.setMetadata(firstLockTag(),
                         new FixedMetadataValue(KeysAndLocksRP.getInstance(), "1"));

                Block finalB = b;
                Bukkit.getScheduler().runTaskLater(
                         KeysAndLocksRP.getInstance(), () ->
                         {
                             if(finalB.hasMetadata(firstLockTag()))
                             {
                                 finalB.removeMetadata(firstLockTag(), KeysAndLocksRP.getInstance());
                                 finalB.getWorld().dropItem(
                                         finalB.getLocation(),
                                         KeysAndLocksRP.getItemManager().getLock());
                             }
                         }, KeysAndLocksRP.getInstance().getConfig().getInt("params.lockKd"));
            }
        }
        else if(ItemManager.isKey(e.getItem()) || ItemManager.isBunchOfKeys(e.getItem()))
        {

            if(b.hasMetadata(firstLockTag()) && !ItemManager.isBunchOfKeys(e.getItem()))
            {
                if(e.getItem().getItemMeta().getPersistentDataContainer()
                        .has(getLockTag(), PersistentDataType.STRING) ){
                    sendMessHB(e.getPlayer(), "localization.messages.keyAlreadyUp");
                    return;
                }

                sendMessHB(e.getPlayer(), "localization.messages.keyUp");

                ItemStack key = e.getItem().clone(); key.setAmount(1);
                ItemMeta meta = key.getItemMeta();
                e.getItem().setAmount(e.getItem().getAmount() - 1);

                meta.setDisplayName(meta.getDisplayName()+" #"+new Random().nextInt(1000, 9999));

                Location l = b.getLocation();

                meta.getPersistentDataContainer()
                        .set(getLockTag(), PersistentDataType.STRING,
                                l.getWorld().getName() +","+l.getBlockX()+","+l.getBlockY()+","+l.getBlockZ());
                key.setItemMeta(meta);

                e.getPlayer().getInventory().addItem(key);

                b.removeMetadata(firstLockTag(), KeysAndLocksRP.getInstance());
                b.setMetadata(lockTag(),
                        new FixedMetadataValue(KeysAndLocksRP.getInstance(), "1"));

                Configuration c = KeysAndLocksRP.getInstance().getConfig();
                protectArea(e.getClickedBlock().getLocation().add(
                                -c.getInt("params.protectAreaRadius"),
                                -c.getInt("params.protectAreaRadius"),
                                -c.getInt("params.protectAreaRadius")),
                        e.getClickedBlock().getLocation().add(
                                c.getInt("params.protectAreaRadius"),
                                c.getInt("params.protectAreaRadius"),
                                c.getInt("params.protectAreaRadius")));
            }
            else if(b.hasMetadata(lockTag()))
            {
                if(!e.getItem().hasItemMeta()) return;

                if(ItemManager.isBunchOfKeys(e.getItem())){
                    ItemStack[] keys = getBOK(e.getItem());

                    for (int i = 0; i < keys.length; i++)
                    {
                        ItemStack key = keys[i];
                        if(key == null || !key.hasItemMeta()) continue;

                        PersistentDataContainer con =  key.getItemMeta().getPersistentDataContainer();
                        if(!con.has(getLockTag(), PersistentDataType.STRING)) continue;

                        String[] loc = con.get(getLockTag(), PersistentDataType.STRING).split(",");
                        String world = loc[0];
                        int x = Integer.parseInt(loc[1]);
                        int y = Integer.parseInt(loc[2]);
                        int z = Integer.parseInt(loc[3]);

                        Location blockL = b.getLocation();

                        if(world.equals(blockL.getWorld().getName())
                                && x == blockL.getBlockX()
                                && y == blockL.getBlockY()
                                && z == blockL.getBlockZ())
                        {
                            if(e.getPlayer().isSneaking())
                            {
                                sendMessHB(e.getPlayer(), "localization.messages.keyDown");

                                con.remove(getLockTag());

                                e.getPlayer().getInventory().addItem(KeysAndLocksRP.getItemManager().getKey());


                                b.removeMetadata(lockTag(), KeysAndLocksRP.getInstance());

                                Item entItem = blockL.getWorld().dropItem(blockL, KeysAndLocksRP.getItemManager().getLock());
                                entItem.setGlowing(true);

                                Configuration c = KeysAndLocksRP.getInstance().getConfig();
                                unProtectArea(e.getClickedBlock().getLocation().add(
                                                -c.getInt("params.protectAreaRadius"),
                                                -c.getInt("params.protectAreaRadius"),
                                                -c.getInt("params.protectAreaRadius")),
                                        e.getClickedBlock().getLocation().add(
                                                c.getInt("params.protectAreaRadius"),
                                                c.getInt("params.protectAreaRadius"),
                                                c.getInt("params.protectAreaRadius")));


                                keys[i] = new ItemStack(Material.AIR);
                                e.getPlayer().setItemInHand(createBOK(keys, e.getItem()));
                            }
                            else
                            {
                                e.setCancelled(false);
                                return;
                            }
                            e.setCancelled(false);
                            return;
                        }
                    }
                    sendLockMess(e.getPlayer(), b);
                    return;

                }
                else
                {
                    ItemStack key = e.getItem();
                    PersistentDataContainer con =  key.getItemMeta().getPersistentDataContainer();
                    if(!con.has(getLockTag(), PersistentDataType.STRING)) {
                        sendMessHB(e.getPlayer(), "localization.messages.keyAlreadyUp");
                        return;
                    }

                    String[] loc = con.get(getLockTag(), PersistentDataType.STRING).split(",");
                    String world = loc[0];
                    int x = Integer.parseInt(loc[1]);
                    int y = Integer.parseInt(loc[2]);
                    int z = Integer.parseInt(loc[3]);

                    Location blockL = b.getLocation();

                    if(world.equals(blockL.getWorld().getName())
                            && x == blockL.getBlockX()
                            && y == blockL.getBlockY()
                            && z == blockL.getBlockZ())
                    {
                        if(e.getPlayer().isSneaking())
                        {
                            sendMessHB(e.getPlayer(), "localization.messages.keyDown");

                            con.remove(getLockTag());

                            key = key.clone(); key.setAmount(1);
                            e.getItem().setAmount(e.getItem().getAmount() - 1);

                            e.getPlayer().getInventory().addItem(KeysAndLocksRP.getItemManager().getKey());

                            b.removeMetadata(lockTag(), KeysAndLocksRP.getInstance());

                            Item entItem = blockL.getWorld().dropItem(blockL, KeysAndLocksRP.getItemManager().getLock());
                            entItem.setGlowing(true);

                            Configuration c = KeysAndLocksRP.getInstance().getConfig();
                            unProtectArea(e.getClickedBlock().getLocation().add(
                                            -c.getInt("params.protectAreaRadius"),
                                            -c.getInt("params.protectAreaRadius"),
                                            -c.getInt("params.protectAreaRadius")),
                                    e.getClickedBlock().getLocation().add(
                                            c.getInt("params.protectAreaRadius"),
                                            c.getInt("params.protectAreaRadius"),
                                            c.getInt("params.protectAreaRadius")));
                        }
                        else
                        {
                            e.setCancelled(false);
                        }
                    }
                    else
                    {
                        e.getPlayer().sendActionBar(KeysAndLocksRP.getInstance().getConfig().getString("localization.messages.wrongKey")
                                .replace('&', '\u00a7'));
                        return;
                    }
                }
                return;
            }
            else
            {
                e.setCancelled(false);
                return;
            }
        }
    }

    private void sendMessHB(Player p, String c) {
        String[] a = KeysAndLocksRP.getInstance().getConfig().getString(c)
                .replace('&', '\u00a7').split("@n");
        for (String s:a) {
            p.sendActionBar(s);
        }
    }

    public void sendMess(Player p, String c){
        String[] a = KeysAndLocksRP.getInstance().getConfig().getString(c)
                .replace('&', '\u00a7').split("@n");
        for (String s:a) {
            p.sendMessage(s);
        }
    }

    public void sendLockMess(Player p, Block b){
        if(b.getBlockData() instanceof TrapDoor){
            p.sendActionBar(KeysAndLocksRP.getInstance().getConfig()
                    .getString("localization.messages.blockLockedTrapDoor")
                    .replace('&', '\u00a7'));
            return;
        }
        if(b.getBlockData() instanceof Door) {
            p.sendActionBar(KeysAndLocksRP.getInstance().getConfig()
                    .getString("localization.messages.blockLockedDoor")
                    .replace('&', '\u00a7'));
            return;
        }
        if(b.getType() ==  Material.BARREL) {
            p.sendActionBar(KeysAndLocksRP.getInstance().getConfig()
                    .getString("localization.messages.blockLockedBarrel")
                    .replace('&', '\u00a7'));
            return;
        }
        p.sendActionBar(KeysAndLocksRP.getInstance().getConfig()
                .getString("localization.messages.blockLockedChest")
                .replace('&', '\u00a7'));
    }

    private List<Player> opensBok = new ArrayList<>();
    @EventHandler
    public void onCloseInventory(InventoryCloseEvent e){
        if(!opensBok.contains(e.getPlayer()))return;
        opensBok.remove(e.getPlayer());
        if(e.getInventory().getSize()
                !=  KeysAndLocksRP.getInstance().getConfig().getInt("params.bokSize")) return;
        boolean hasntItems = true;
        for (ItemStack i : e.getInventory().getContents()) {
            if(i != null && ItemManager.isKey(i)){
                hasntItems = false;
                break;
            }
        }

        if(hasntItems){
            e.getPlayer().getItemInHand().setAmount(e.getPlayer().getItemInHand().getAmount() - 1);
            e.getPlayer().getInventory().addItem(KeysAndLocksRP.getItemManager().getBunchOfKeys());
            return;
        }

        ItemStack bok = e.getPlayer().getItemInHand().clone();

        e.getPlayer().getItemInHand().setAmount(e.getPlayer().getItemInHand().getAmount() - 1);
        bok.setAmount(1);
        e.getPlayer().getInventory().addItem(
                createBOK(e.getInventory().getContents(),
                bok));
    }

    private ItemStack[] getBOK(ItemStack bok){
        String bokData = bok.getItemMeta().getPersistentDataContainer().get(
                new NamespacedKey(KeysAndLocksRP.getInstance(), "BOK"), PersistentDataType.STRING);

        ItemStack[] itemStacks = new ItemStack[ KeysAndLocksRP.getInstance().getConfig()
                .getInt("params.bokSize")];

        SimpleKey[] keys = new GsonBuilder().create().fromJson(bokData, SimpleKey[].class);
        if(keys != null){
            for (int i = 0; i < itemStacks.length; i++) {
                if(keys[i] == null)
                {
                    itemStacks[i] = new ItemStack(Material.AIR);
                    continue;
                }

                itemStacks[i] = KeysAndLocksRP.getItemManager().getKey().clone();
                ItemMeta m = itemStacks[i].getItemMeta();

                if(keys[i].world != null){
                    Location l = keys[i].loc();
                    m.getPersistentDataContainer()
                            .set(getLockTag(), PersistentDataType.STRING,
                                    l.getWorld().getName() +","+l.getBlockX()+","+l.getBlockY()+","+l.getBlockZ());
                }

                m.setDisplayName(keys[i].name);
                itemStacks[i].setItemMeta(m);
                itemStacks[i].setAmount(keys[i].count);
            }
        }
        else
        {
            for (int i = 0; i < itemStacks.length; i++) {
                itemStacks[i] = new ItemStack(Material.AIR);
            }
        }

        return itemStacks;
    }


    private ItemStack createBOK(ItemStack[] keys, ItemStack bok){
        SimpleKey[] simpleKeys = new SimpleKey[keys.length];
        for (int i = 0; i < keys.length; i++) {
            if(keys[i] == null || !ItemManager.isKey(keys[i])) continue;

            SimpleKey sk = new SimpleKey();
            if(keys[i].getItemMeta().getPersistentDataContainer()
                    .has(getLockTag(), PersistentDataType.STRING)){
                String[] loc = keys[i].getItemMeta()
                        .getPersistentDataContainer().get(getLockTag(), PersistentDataType.STRING).split(",");
                String world = loc[0];
                int x = Integer.parseInt(loc[1]);
                int y = Integer.parseInt(loc[2]);
                int z = Integer.parseInt(loc[3]);

                sk.world = world;
                sk.x = x;
                sk.y = y;
                sk.z = z;
            }
            sk.name = keys[i].getItemMeta().getDisplayName();
            sk.count = keys[i].getAmount();

            simpleKeys[i] = sk;
        }

        ItemMeta meta = bok.getItemMeta();
        meta.getPersistentDataContainer().set(
                new NamespacedKey(KeysAndLocksRP.getInstance(), "BOK"), PersistentDataType.STRING,
                new GsonBuilder().create().toJson(simpleKeys));
        bok.setItemMeta(meta);
        return bok;
    }

    private void protectArea(Location a, Location b){
        int ax = Math.min(a.getBlockX(), b.getBlockX());
        int bx = Math.max(a.getBlockX(), b.getBlockX());
        int ay = Math.min(a.getBlockY(), b.getBlockY());
        int by = Math.max(a.getBlockY(), b.getBlockY());
        int az = Math.min(a.getBlockZ(), b.getBlockZ());
        int bz = Math.max(a.getBlockZ(), b.getBlockZ());


        for (int x = ax; x < bx; x++) {
            for (int z = az; z < bz; z++) {
                for (int y = ay; y < by; y++)
                {
                    Block block = new Location(a.getWorld(), x, y, z).getBlock();

                    int protectCount = 1;
                    if(block.hasMetadata(protectTag()))
                    {
                        protectCount += block.getMetadata(protectTag()).get(0).asInt() ;
                    }

                    block.setMetadata(protectTag(),
                            new FixedMetadataValue(KeysAndLocksRP.getInstance(), protectCount));
                }
            }
        }
    }

    private void unProtectArea(Location a, Location b){
        int ax = Math.min(a.getBlockX(), b.getBlockX());
        int bx = Math.max(a.getBlockX(), b.getBlockX());
        int ay = Math.min(a.getBlockY(), b.getBlockY());
        int by = Math.max(a.getBlockY(), b.getBlockY());
        int az = Math.min(a.getBlockZ(), b.getBlockZ());
        int bz = Math.max(a.getBlockZ(), b.getBlockZ());


        for (int x = ax; x < bx; x++) {
            for (int z = az; z < bz; z++) {
                for (int y = ay; y < by; y++)
                {
                    Block block = new Location(a.getWorld(), x, y, z).getBlock();


                    if(!block.hasMetadata(protectTag())) continue;
                    int protectCount = block.getMetadata(protectTag()).get(0).asInt() - 1;

                    if(protectCount <= 0)
                    {
                        block.removeMetadata(protectTag(), KeysAndLocksRP.getInstance());
                    }
                    else
                    {
                    block.setMetadata(protectTag(),
                            new FixedMetadataValue(KeysAndLocksRP.getInstance(), protectCount));
                    }

                }
            }
        }
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreaking(BlockDamageEvent e){
        if(!isProtectedBlock(e.getBlock())) return;

        e.getPlayer().addPotionEffect(
                new PotionEffect(PotionEffectType.SLOW_DIGGING, 3600,
                        KeysAndLocksRP.getInstance().getConfig().getInt("params.breakSpeed"),
                        false,false));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAbort(BlockDamageAbortEvent e){
        if(!isProtectedBlock(e.getBlock())) return;
        e.getPlayer().removePotionEffect(PotionEffectType.SLOW_DIGGING);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        if(b.getBlockData() instanceof Door
                && ((Bisected) b.getBlockData()).getHalf().toString().equals("TOP")) {
            b = b.getWorld().getBlockAt(b.getLocation().add(0d,-1d, 0d));
        }
        else if(b.getState() instanceof Chest
                && ((Chest)b.getState()).getInventory() instanceof DoubleChestInventory)
        {
            b = ((DoubleChest)((Chest) b.getState()).getInventory().getHolder()).getLocation().getBlock();
        }
        else if(isProtectedBlock(b.getLocation().add(0, 1, 0).getBlock())){
            b = b.getLocation().add(0, 1, 0).getBlock();
        }

        if(isProtectedBlock(e.getBlock())){
            e.getBlock().removeMetadata(protectTag(), KeysAndLocksRP.getInstance());
        }

        if(isLocked(b)){
            Configuration c = KeysAndLocksRP.getInstance().getConfig();
            unProtectArea(b.getLocation().add(
                    -c.getInt("params.protectAreaRadius"),
                    -c.getInt("params.protectAreaRadius"),
                    -c.getInt("params.protectAreaRadius")),
                    b.getLocation().add(
                    c.getInt("params.protectAreaRadius"),
                    c.getInt("params.protectAreaRadius"),
                    c.getInt("params.protectAreaRadius")));
            b.removeMetadata(lockTag(), KeysAndLocksRP.getInstance());
            b.removeMetadata(firstLockTag(), KeysAndLocksRP.getInstance());

            b.getWorld().dropItem(b.getLocation(), KeysAndLocksRP.getItemManager().getLock());
        }


    }



    public static boolean isProtectedBlock(Block b){
        return b.hasMetadata(protectTag());
    }

    public static String protectTag(){return "ProtectedKAL";}

    public static boolean isLocked(Block b){
        if(b.getBlockData() instanceof Door && ((Door) b.getBlockData()).getHalf().toString().equals("TOP")){
            return b.getLocation().add(0d, -1d, 0d).getBlock().hasMetadata(firstLockTag())
                    || b.getLocation().add(0d, -1d, 0d).getBlock().hasMetadata(lockTag());
        }
        if(b.getState()  instanceof Chest && ((Chest)b.getState()).getInventory() instanceof DoubleChestInventory){
            Block dc = ((DoubleChest)((Chest) b.getState()).getInventory().getHolder()).getLocation().getBlock();

            return dc.hasMetadata(lockTag()) || dc.hasMetadata(firstLockTag());
        }
        return  (b.hasMetadata(firstLockTag()) || b.hasMetadata(lockTag()));
    }

    public static boolean canLock(Block b){
        if(isLockBlock(b)){
            return isLocked(b);
        }
        return false;
    }
    public static boolean isLockBlock(Block b){
        return (b.getBlockData() instanceof Door || b.getBlockData() instanceof TrapDoor
                || b.getBlockData() instanceof Gate || b.getState()  instanceof Chest
                || (b.getState()  instanceof Chest && ((Chest)b.getState()).getInventory() instanceof DoubleChestInventory)
                || b.getState()  instanceof Barrel);
    }


    public static String firstLockTag(){
        return "FirstLock";
    }

    public static String lockTag(){
        return "Locked";
    }

    public static NamespacedKey getFirstLockTag(){
        return new NamespacedKey(KeysAndLocksRP.getInstance(), firstLockTag());
    }

    public static NamespacedKey getLockTag(){
        return new NamespacedKey(KeysAndLocksRP.getInstance(), lockTag());
    }
}
