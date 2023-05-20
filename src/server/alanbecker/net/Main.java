package server.alanbecker.net;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class Main extends JavaPlugin implements Listener {

    private double rewardPercentage;
    private List<String> rewards;
    private Map<String, String> rewardLoreMap;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        loadConfig();
    }

    @Override
    public void onDisable() {
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();

        config.addDefault("RewardPercentage", 50.0);
        config.addDefault("Rewards", Arrays.asList(
                "eco give %player% 10",
                "tokenadmin add %player% 10",
                "give %player% diamond 1"
        ));
        config.addDefault("RewardLore.give %player% diamond 1", "Receive a diamond!");

        config.options().copyDefaults(true);
        saveConfig();

        rewardPercentage = config.getDouble("RewardPercentage") / 100.0;
        rewards = config.getStringList("Rewards");
        rewardLoreMap = new HashMap<>();

        ConfigurationSection rewardLoreSection = config.getConfigurationSection("RewardLore");
        if (rewardLoreSection != null) {
            for (String key : rewardLoreSection.getKeys(false)) {
                String value = rewardLoreSection.getString(key);
                rewardLoreMap.put(key, value);
            }
        }
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("minecratereload")) {
            if (sender.hasPermission("minecrate.reload")) {
                reloadConfig();
                loadConfig();
                sender.sendMessage("MineCrate configuration reloaded.");
                return true;
            } else {
                sender.sendMessage("You don't have permission to reload the MineCrate configuration.");
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        Material toolType = player.getInventory().getItemInMainHand().getType();
        if (isToolForMining(toolType)) {
            ItemStack mineCrate = createMineCrate();

            player.getInventory().addItem(mineCrate);

            player.sendMessage("You received a MineCrate!");
        }
    }

    @EventHandler
    public void onInventoryClickEvent(InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item != null && item.getType() == Material.CHEST && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName() && meta.getDisplayName().equals("MineCrate")) {
                event.setCancelled(true);
                event.setCurrentItem(null);

                Player player = (Player) event.getWhoClicked();

                openRewardSelectionGUI(player);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item != null && item.getType() == Material.CHEST && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName() && meta.getDisplayName().equals("MineCrate")) {
                event.setCancelled(true);

                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().remove(item);
                }

                openRewardSelectionGUI(player);
            }
        }
    }

    private void openRewardSelectionGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, "Select a Reward");

        // Add rewards to the GUI as selectable items
        Random random = new Random();
        for (int i = 0; i < 3; i++) {
            String randomRewardCommand = rewards.get(random.nextInt(rewards.size()));
            String rewardCommand = randomRewardCommand.replace("%player%", player.getName());

            ItemStack rewardItem = new ItemStack(Material.DIAMOND);
            ItemMeta rewardMeta = rewardItem.getItemMeta();
            rewardMeta.setDisplayName("Reward #" + (i + 1));
            String rewardLore = rewardLoreMap.getOrDefault(randomRewardCommand, "");
            rewardMeta.setLore(Arrays.asList(rewardLore));
            rewardItem.setItemMeta(rewardMeta);

            gui.setItem(i, rewardItem);
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onRewardSelection(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("Select a Reward")) {
            event.setCancelled(true);

            Player player = (Player) event.getWhoClicked();
            ItemStack selectedReward = event.getCurrentItem();

            if (selectedReward != null && selectedReward.getType() == Material.DIAMOND && selectedReward.hasItemMeta()) {
                ItemMeta meta = selectedReward.getItemMeta();
                List<String> lore = meta.getLore();

                if (lore != null && !lore.isEmpty()) {
                    String rewardLore = lore.get(0);

                    for (Map.Entry<String, String> entry : rewardLoreMap.entrySet()) {
                        if (entry.getValue().equals(rewardLore)) {
                            String rewardCommand = entry.getKey().replace("%player%", player.getName());

                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rewardCommand);
                            player.sendMessage("You received a reward!");

                            player.closeInventory();
                            return;
                        }
                    }
                }
            }
        }
    }

    private boolean isToolForMining(Material material) {
        Material[] miningTools = { Material.DIAMOND_PICKAXE, Material.IRON_PICKAXE, Material.STONE_PICKAXE,
                Material.WOODEN_PICKAXE };

        for (Material tool : miningTools) {
            if (tool == material) {
                return true;
            }
        }

        return false;
    }

    private ItemStack createMineCrate() {
        ItemStack mineCrate = new ItemStack(Material.CHEST);

        ItemMeta meta = mineCrate.getItemMeta();
        meta.setDisplayName("MineCrate");
        mineCrate.setItemMeta(meta);

        return mineCrate;
    }
}
