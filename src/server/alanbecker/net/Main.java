package server.alanbecker.net;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Main extends JavaPlugin implements Listener {

    private double rewardPercentage;
    private List<String> rewards;

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

        config.options().copyDefaults(true);
        saveConfig();

        rewardPercentage = config.getDouble("RewardPercentage") / 100.0; 
        rewards = config.getStringList("Rewards");
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

                Random random = new Random();
                double randomValue = random.nextDouble();
                if (randomValue < rewardPercentage) {
                    giveRandomReward(player);
                } else {
                    player.sendMessage("The MineCrate was empty!");
                }
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
                
                Random random = new Random();
                double randomValue = random.nextDouble();
                if (randomValue < rewardPercentage) {
                    giveRandomReward(player);
                } else {
                    player.sendMessage("The MineCrate was empty!");
                }
            }
        }
    }



    private void giveRandomReward(Player player) {
        String randomRewardCommand = rewards.get(new Random().nextInt(rewards.size()));
        String rewardCommand = randomRewardCommand.replace("%player%", player.getName());

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rewardCommand);
    }

    private boolean isToolForMining(Material material) {
        Material[] miningTools = {Material.DIAMOND_PICKAXE, Material.IRON_PICKAXE, Material.STONE_PICKAXE, Material.WOODEN_PICKAXE};

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
