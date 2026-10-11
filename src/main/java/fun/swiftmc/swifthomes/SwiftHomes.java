package fun.swiftmc.swifthomes;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public final class SwiftHomes extends JavaPlugin implements Listener {
    private File homesFile;
    private YamlConfiguration homes;
    private NamespacedKey actionKey;
    private NamespacedKey homeKey;
    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_-]+");
    private static final int[] HOME_SLOTS = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};
    private final Map<UUID, Integer> homePages = new HashMap<>();
    private final Map<UUID, Integer> deletePages = new HashMap<>();

    @Override public void onEnable() {
        saveDefaultConfig();
        actionKey = new NamespacedKey(this, "action");
        homeKey = new NamespacedKey(this, "home");
        homesFile = new File(getDataFolder(), "homes.yml");
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        if (!homesFile.exists()) {
            try { homesFile.createNewFile(); } catch (IOException e) { getLogger().severe("Could not create homes.yml: " + e.getMessage()); }
        }
        homes = YamlConfiguration.loadConfiguration(homesFile);
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("SwiftHomes enabled.");
    }

    @Override public void onDisable() { saveHomes(); }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s); }
    private void msg(Player p, String key, Map<String, String> vars) {
        String s = getConfig().getString("messages." + key, "&cMissing message: " + key);
        for (Map.Entry<String,String> e : vars.entrySet()) s = s.replace("%" + e.getKey() + "%", e.getValue());
        p.sendMessage(color("&bSwiftHomes &8» &r" + s));
    }
    private void msg(Player p, String key) { msg(p, key, Collections.emptyMap()); }
    private void sound(Player p, Sound s) { p.playSound(p.getLocation(), s, 0.8f, 1.15f); }
    private String base(Player p) { return p.getUniqueId().toString(); }
    private ConfigurationSection playerHomes(Player p) { return homes.getConfigurationSection(base(p)); }
    private Set<String> getNames(Player p) {
        ConfigurationSection sec = playerHomes(p);
        if (sec == null) return new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Set<String> result = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        result.addAll(sec.getKeys(false)); return result;
    }
    private String path(Player p, String name) { return base(p) + "." + name.toLowerCase(Locale.ROOT); }
    private String storedName(Player p, String requested) {
        for (String name : getNames(p)) if (name.equalsIgnoreCase(requested)) return name;
        return null;
    }
    private int homeLimit(Player p) {
        if (p.hasPermission(getConfig().getString("unlimited-permission", "swifthomes.admin"))) return Integer.MAX_VALUE;
        int max = 0, check = Math.max(1, getConfig().getInt("permission-check-limit", 100));
        for (int i = 1; i <= check; i++) if (p.hasPermission("swifthomes.home." + i)) max = i;
        return max;
    }
    private boolean canUse(Player p) { if (p.hasPermission("swifthomes.use")) return true; msg(p,"no-permission"); return false; }
    private boolean setHome(Player p, String name) {
        int maxLen = Math.max(1, getConfig().getInt("max-name-length", 16));
        if (name.length() > maxLen || !VALID_NAME.matcher(name).matches()) {
            msg(p, "invalid-name", Map.of("length", String.valueOf(maxLen))); sound(p, Sound.ENTITY_VILLAGER_NO); return false;
        }
        String existing = storedName(p, name);
        int limit = homeLimit(p);
        if (existing == null && getNames(p).size() >= limit) {
            msg(p, "limit", Map.of("limit", limit == Integer.MAX_VALUE ? "∞" : String.valueOf(limit))); sound(p, Sound.ENTITY_VILLAGER_NO); return false;
        }
        Location l = p.getLocation(); String key = path(p, existing == null ? name : existing);
        homes.set(key + ".world", l.getWorld().getName()); homes.set(key + ".x", l.getX()); homes.set(key + ".y", l.getY()); homes.set(key + ".z", l.getZ());
        homes.set(key + ".yaw", l.getYaw()); homes.set(key + ".pitch", l.getPitch()); homes.set(key + ".display-name", existing == null ? name : existing);
        saveHomes(); msg(p, "set", Map.of("home", existing == null ? name : existing)); sound(p, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE); return true;
    }
    private void saveHomes() { try { homes.save(homesFile); } catch (IOException e) { getLogger().severe("Could not save homes.yml: " + e.getMessage()); } }
    private Location getHome(Player p, String name) {
        String stored = storedName(p, name); if (stored == null) return null;
        String key = path(p, stored); World w = Bukkit.getWorld(homes.getString(key + ".world", "")); if (w == null) return null;
        return new Location(w, homes.getDouble(key + ".x"), homes.getDouble(key + ".y"), homes.getDouble(key + ".z"), (float)homes.getDouble(key + ".yaw"), (float)homes.getDouble(key + ".pitch"));
    }
    private boolean teleport(Player p, String name) {
        Location l = getHome(p, name); if (l == null) { msg(p,"missing",Map.of("home",name)); sound(p,Sound.ENTITY_VILLAGER_NO); return false; }
        p.closeInventory(); p.teleportAsync(l).thenAccept(ok -> Bukkit.getScheduler().runTask(this, () -> { if (ok && p.isOnline()) { msg(p,"teleported",Map.of("home",name)); sound(p,Sound.ENTITY_ENDERMAN_TELEPORT); } })); return true;
    }
    private ItemStack item(Material material, String name, List<String> lore, String action, String home) {
        ItemStack it = new ItemStack(material); ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(color(name)); List<String> colored = new ArrayList<>(); for (String s : lore) colored.add(color(s)); meta.setLore(colored);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        if (home != null) meta.getPersistentDataContainer().set(homeKey, PersistentDataType.STRING, home);
        it.setItemMeta(meta); return it;
    }
    private void fill(Inventory inv) {
        ItemStack pane = item(Material.BLACK_STAINED_GLASS_PANE, "&0", List.of(), "decor", null);
        for (int i=0;i<inv.getSize();i++) inv.setItem(i,pane);
        for (int slot : HOME_SLOTS) inv.setItem(slot, null);
    }
    private void openHomes(Player p) {
        int page = Math.max(0, homePages.getOrDefault(p.getUniqueId(), 0));
        Inventory inv = Bukkit.createInventory(p,54,color(getConfig().getString("titles.homes","&8✦ &b&lSWIFT HOMES &8✦")));
        fill(inv); List<String> names = new ArrayList<>(getNames(p)); int limit=homeLimit(p);
        int visibleCapacity = Math.max(names.size(), limit == Integer.MAX_VALUE ? names.size() : limit);
        int pages = Math.max(1, (visibleCapacity + HOME_SLOTS.length - 1) / HOME_SLOTS.length);
        if (page >= pages) page = pages - 1; homePages.put(p.getUniqueId(), page);
        for (int i=0; i<HOME_SLOTS.length; i++) {
            int global = page * HOME_SLOTS.length + i, slot = HOME_SLOTS[i];
            if (global < names.size()) {
                String name = names.get(global); Location l = getHome(p,name);
                String world = l == null ? "Unknown world" : l.getWorld().getName();
                inv.setItem(slot,item(Material.RED_BED,"&b&l"+name,List.of("&7Your personal home", "", "&fWorld: &b"+world,"&fCoordinates: &b"+(l==null?"Unavailable":l.getBlockX()+", "+l.getBlockY()+", "+l.getBlockZ()),"", "&a▸ LEFT-CLICK to teleport", "&c▸ RIGHT-CLICK to delete (confirm required)"),"home",name));
            } else if (global < limit) {
                String name = "Home" + (global + 1);
                inv.setItem(slot,item(Material.LIME_BED,"&a&lSet Home #"+(global+1),List.of("&7Empty home slot", "", "&eClick to set a home here", "&7Name: &f"+name),"set",name));
            } else {
                inv.setItem(slot,item(Material.GRAY_DYE,"&7Locked Home Slot",List.of("&8You need a higher home permission", "", "&7LuckPerms: &fswifthomes.home."+(global+1)),"locked",null));
            }
        }
        inv.setItem(45,item(Material.BOOK,"&b&lHome Information",List.of("&7Homes: &f"+names.size()+"&7 / &f"+(limit==Integer.MAX_VALUE?"∞":limit),"&7Page: &f"+(page+1)+"&7 / &f"+pages,"&7Use &f/sethome <name> &7to create a named home."),"info",null));
        if (page > 0) inv.setItem(48,item(Material.ARROW,"&e&l◀ Previous Page",List.of("&7Go to page &f"+page),"homes-prev",null));
        inv.setItem(49,item(Material.BARRIER,"&c&lRemove Home",List.of("&7Open the home removal menu", "", "&cDeletion requires confirmation."),"delete-menu",null));
        if (page + 1 < pages) inv.setItem(50,item(Material.ARROW,"&e&lNext Page ▶",List.of("&7Go to page &f"+(page+2)),"homes-next",null));
        inv.setItem(53,item(Material.OAK_DOOR,"&e&lClose Menu",List.of("&7Click to close this menu."),"close",null));
        p.openInventory(inv); sound(p,Sound.BLOCK_CHEST_OPEN);
    }
    private void openDelete(Player p) {
        Inventory inv = Bukkit.createInventory(p,54,color(getConfig().getString("titles.delete","&8✦ &c&lREMOVE HOME &8✦"))); fill(inv);
        List<String> names = new ArrayList<>(getNames(p)); int page = Math.max(0, deletePages.getOrDefault(p.getUniqueId(),0));
        int pages = Math.max(1,(names.size()+HOME_SLOTS.length-1)/HOME_SLOTS.length); if(page>=pages) page=pages-1; deletePages.put(p.getUniqueId(),page);
        for(int i=0;i<HOME_SLOTS.length;i++){ int index=page*HOME_SLOTS.length+i; if(index>=names.size()) break; String name=names.get(index); inv.setItem(HOME_SLOTS[i],item(Material.TNT,"&c&lDelete: "+name,List.of("&7Select this home", "", "&eYou will be asked to confirm."),"choose-delete",name)); }
        if(page>0) inv.setItem(48,item(Material.ARROW,"&e&l◀ Previous Page",List.of("&7Go to page &f"+page),"delete-prev",null));
        inv.setItem(49,item(Material.ARROW,"&b&lBack to Homes",List.of("&7Return to your homes."),"back",null));
        if(page+1<pages) inv.setItem(50,item(Material.ARROW,"&e&lNext Page ▶",List.of("&7Go to page &f"+(page+2)),"delete-next",null));
        inv.setItem(53,item(Material.OAK_DOOR,"&e&lClose Menu",List.of(),"close",null)); p.openInventory(inv); sound(p,Sound.BLOCK_CHEST_OPEN);
    }
    private void openConfirm(Player p, String name) {
        Inventory inv = Bukkit.createInventory(p,27,color(getConfig().getString("titles.confirm","&8✦ &c&lCONFIRM DELETE? &8✦"))); fill(inv);
        inv.setItem(11,item(Material.LIME_CONCRETE,"&a&lCONFIRM DELETE",List.of("&7Permanently delete home &f"+name,"", "&cThis action cannot be undone."),"confirm-delete",name));
        inv.setItem(15,item(Material.RED_CONCRETE,"&c&lCANCEL",List.of("&7Keep this home."),"cancel-delete",name));
        inv.setItem(13,item(Material.RED_BED,"&fHome: &c"+name,List.of("&7Are you sure you want to remove it?"),"decor",null)); p.openInventory(inv); sound(p,Sound.BLOCK_NOTE_BLOCK_PLING);
    }
    @EventHandler public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = e.getView().getTitle();
        if (!title.equals(color(getConfig().getString("titles.homes","&8✦ &b&lSWIFT HOMES &8✦"))) && !title.equals(color(getConfig().getString("titles.delete","&8✦ &c&lREMOVE HOME &8✦"))) && !title.equals(color(getConfig().getString("titles.confirm","&8✦ &c&lCONFIRM DELETE? &8✦")))) return;
        e.setCancelled(true); if (e.getClickedInventory()==null || e.getClickedInventory()!=e.getView().getTopInventory()) return;
        ItemStack it=e.getCurrentItem(); if (it==null || !it.hasItemMeta()) return;
        String action=it.getItemMeta().getPersistentDataContainer().get(actionKey,PersistentDataType.STRING);
        String name=it.getItemMeta().getPersistentDataContainer().get(homeKey,PersistentDataType.STRING); if (action==null) return;
        switch(action) {
            case "home" -> { if(name!=null) { if (e.getClick().isRightClick()) { openConfirm(p,name); } else if (e.getClick().isLeftClick()) { teleport(p,name); } else { sound(p,Sound.UI_BUTTON_CLICK); } } }
            case "set" -> { if(name!=null && setHome(p,name)) openHomes(p); }
            case "delete-menu" -> { deletePages.put(p.getUniqueId(),0); openDelete(p); }
            case "homes-prev" -> { homePages.put(p.getUniqueId(), Math.max(0, homePages.getOrDefault(p.getUniqueId(),0)-1)); openHomes(p); }
            case "homes-next" -> { homePages.put(p.getUniqueId(), homePages.getOrDefault(p.getUniqueId(),0)+1); openHomes(p); }
            case "delete-prev" -> { deletePages.put(p.getUniqueId(), Math.max(0, deletePages.getOrDefault(p.getUniqueId(),0)-1)); openDelete(p); }
            case "delete-next" -> { deletePages.put(p.getUniqueId(), deletePages.getOrDefault(p.getUniqueId(),0)+1); openDelete(p); }
            case "choose-delete" -> { if(name!=null) openConfirm(p,name); }
            case "confirm-delete" -> { if(name!=null && storedName(p,name)!=null) { homes.set(path(p,name),null); saveHomes(); msg(p,"deleted",Map.of("home",name)); sound(p,Sound.ENTITY_ITEM_BREAK); openDelete(p); } }
            case "cancel-delete" -> { openDelete(p); sound(p,Sound.UI_BUTTON_CLICK); }
            case "back" -> openHomes(p);
            case "close" -> { p.closeInventory(); sound(p,Sound.UI_BUTTON_CLICK); }
            case "locked" -> sound(p,Sound.ENTITY_VILLAGER_NO);
            default -> { }
        }
    }
    @EventHandler public void onDrag(InventoryDragEvent e) {
        String title=e.getView().getTitle();
        if (title.equals(color(getConfig().getString("titles.homes","&8✦ &b&lSWIFT HOMES &8✦"))) || title.equals(color(getConfig().getString("titles.delete","&8✦ &c&lREMOVE HOME &8✦"))) || title.equals(color(getConfig().getString("titles.confirm","&8✦ &c&lCONFIRM DELETE? &8✦")))) e.setCancelled(true);
    }
    @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("swifthomes")) {
            if (!sender.hasPermission("swifthomes.admin")) { sender.sendMessage(color("&cNo permission.")); return true; }
            if (args.length>0 && args[0].equalsIgnoreCase("reload")) { reloadConfig(); sender.sendMessage(color("&bSwiftHomes &8» &aConfiguration reloaded.")); }
            else sender.sendMessage(color("&bSwiftHomes &8» &f/swifthomes reload")); return true;
        }
        if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
        if (!canUse(p)) return true;
        switch(cmd.getName().toLowerCase(Locale.ROOT)) {
            case "home", "homes" -> { if (cmd.getName().equalsIgnoreCase("home") && args.length>0) teleport(p,args[0]); else openHomes(p); return true; }
            case "sethome" -> { if(args.length>1) { p.sendMessage(color("&cUsage: /sethome [name]")); return true; } if(args.length==0) { msg(p,"set-prompt"); return true; } setHome(p,args[0]); return true; }
            case "delhome" -> { if(args.length!=1) { p.sendMessage(color("&cUsage: /delhome <name>")); return true; } String stored=storedName(p,args[0]); if(stored==null) msg(p,"missing",Map.of("home",args[0])); else openConfirm(p,stored); return true; }
        }
        return false;
    }
}
