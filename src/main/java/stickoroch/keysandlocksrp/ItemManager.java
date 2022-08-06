package stickoroch.keysandlocksrp;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemManager {

    private static NamespacedKey keyName = new NamespacedKey(KeysAndLocksRP.getInstance(), "key");
    private static NamespacedKey lockName = new NamespacedKey(KeysAndLocksRP.getInstance(), "lock");
    private static NamespacedKey bofName = new NamespacedKey(KeysAndLocksRP.getInstance(), "bunchOfKeys");
    private ItemStack key;
    private ItemStack lock;
    private ItemStack bunchOfKeys;

    public ItemStack getKey(){
        return key.clone();
    }

    public ItemStack getBunchOfKeys(){
        return bunchOfKeys.clone();
    }

    public ItemStack getLock(){
        return lock.clone();
    }

    public void loadFromConfig(){
        ConfigurationSection section = KeysAndLocksRP.getInstance().getConfig()
               .getConfigurationSection("localization");


        key = new ItemStack(Material.valueOf(section.getString("key.type")));
        ItemMeta meta = key.getItemMeta();
        meta.setDisplayName(section.getString("key.name")
                .replaceAll("&","\u00a7"));
        List<String> lore = section.getStringList("key.lore");
        for (int i = 0; i < lore.size(); i++) {
            lore.set(i, lore.get(i).replace('&', '\u00a7'));
        }
        if(lore.size() >= 1 && !lore.get(0).equals("@not")){
            meta.setLore(lore);}
        meta.getPersistentDataContainer().set(keyName, PersistentDataType.INTEGER, 1);
        meta.setCustomModelData(section.getInt("key.data"));
        key.setItemMeta(meta);

        String[] craft = new String[9];
        for (int i = 0; i < 9; i++) {
            craft[i] = section.getStringList("key.craft").get(i);
        }
        loadRecipe(key, craft, "key");

        bunchOfKeys = new ItemStack(Material.valueOf(section.getString("bunchOfKeys.type")));
        meta = bunchOfKeys.getItemMeta();
        meta.setDisplayName(section.getString("bunchOfKeys.name")
                .replaceAll("&","\u00a7"));
        lore = section.getStringList("bunchOfKeys.lore");
        for (int i = 0; i < lore.size(); i++) {
            lore.set(i, lore.get(i).replace('&', '\u00a7'));
        }

        if(lore.size() >= 1 && !lore.get(0).equals("@not")){
            meta.setLore(lore);}
        meta.getPersistentDataContainer().set(bofName, PersistentDataType.INTEGER, 1);
        meta.setCustomModelData(section.getInt("bunchOfKeys.data"));
        bunchOfKeys.setItemMeta(meta);

        craft = new String[9];
        for (int i = 0; i < 9; i++) {
            craft[i] = section.getStringList("bunchOfKeys.craft").get(i);
        }
        loadRecipe(bunchOfKeys, craft, "bunchOfKeys");

        lock = new ItemStack(Material.valueOf(section.getString("lock.type")));
        meta = lock.getItemMeta();
        meta.setDisplayName(section.getString("lock.name")
                .replaceAll("&","\u00a7"));
        lore = section.getStringList("lock.lore");
        for (int i = 0; i < lore.size(); i++) {
            lore.set(i, lore.get(i).replace('&', '\u00a7'));
        }
        if(lore.size() >= 1 && !lore.get(0).equals("@not")){
            meta.setLore(lore);}
        meta.getPersistentDataContainer().set(lockName, PersistentDataType.INTEGER, 1);
        meta.setCustomModelData(section.getInt("lock.data"));
        lock.setItemMeta(meta);

        craft = new String[9];
        for (int i = 0; i < 9; i++) {
            craft[i] = section.getStringList("lock.craft").get(i);
        }
        loadRecipe(lock, craft, "lock");
    }

    public void loadRecipe(ItemStack item, String[] recipe, String id){
        ShapedRecipe shapedRecipe = new ShapedRecipe(new NamespacedKey(KeysAndLocksRP.getInstance(), id), item);

        if(Bukkit.getServer().getRecipe(shapedRecipe.getKey()) != null)
            Bukkit.getServer().removeRecipe(shapedRecipe.getKey());

        char[] craft = new char[9];
        String symbols = "ASDFGHJKL";
        HashMap<Material, Character> buff = new HashMap<>();

        for (int i = 0; i < 9; i++) {
            String c = recipe[i];
            Material mat = Material.getMaterial(c.toUpperCase());
            char j = symbols.toCharArray()[i];
            if(mat != null && mat != Material.AIR){
                if(!buff.containsKey(mat)) {
                    buff.put(mat, j);
                    craft[i] = j;
                }else{
                    craft[i] = buff.get(mat);
                }
            }else {
                craft[i] = ' ';
            }
        }

        shapedRecipe.shape(String.valueOf(craft[0]) + craft[1] + craft[2],
                String.valueOf(craft[3]) + craft[4] + craft[5],
                String.valueOf(craft[6]) + craft[7] + craft[8]);

        for (Map.Entry<Material, Character> e : buff.entrySet()) {
            shapedRecipe.setIngredient(e.getValue(), e.getKey());
        }
        Bukkit.getServer().addRecipe(shapedRecipe);
    }

    public static boolean isKey(ItemStack i){
        return i != null && i.hasItemMeta() && i.getItemMeta().getPersistentDataContainer().has(keyName, PersistentDataType.INTEGER);
    }

    public static boolean isBunchOfKeys(ItemStack i){
        return  i != null && i.hasItemMeta() && i.getItemMeta().getPersistentDataContainer().has(bofName, PersistentDataType.INTEGER);
    }

    public static boolean isLock(ItemStack i){
        return  i != null && i.hasItemMeta() && i.getItemMeta().getPersistentDataContainer().has(lockName, PersistentDataType.INTEGER);
    }

    public ItemManager(){
        loadFromConfig();
    }
}
