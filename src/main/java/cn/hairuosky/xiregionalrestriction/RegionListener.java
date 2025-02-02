package cn.hairuosky.xiregionalrestriction;

import org.bukkit.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class RegionListener implements Listener {

    private final XiRegionalRestriction plugin;
    private final Map<Player, String> playerRegionStatus = new HashMap<>();  // 缓存玩家的区域状态
    private Map<Player, Location> firstPoints = new HashMap<>();
    private Map<Player, Location> secondPoints = new HashMap<>();
    private int n = 5;

    public RegionListener(XiRegionalRestriction plugin) {
        this.plugin = plugin;
    }

    private Map<Player, Integer> playerMoveCounter = new HashMap<>();  // 用于存储每个玩家的移动次数

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();  // 获取玩家上次的位置
        Location to = event.getTo();      // 获取玩家当前的位置

        // 防止玩家位置变化太小导致频繁触发
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;  // 如果位置没变，直接跳过
        }

        // 获取当前玩家的移动计数器，如果没有计数器则初始化为 0
        int moveCount = playerMoveCounter.getOrDefault(player, 0);

        // 如果计数器未达到指定次数n，则跳过区域检查
        if (moveCount < n) {
            playerMoveCounter.put(player, moveCount + 1); // 增加计数器
            return;  // 跳过本次区域检查
        }

        // 计数器达到 n 后进行区域检查
        String regionName = null;  // 存储玩家所在的受限区域名称
        for (Region region : plugin.getRegions()) {
            if (region.isInside(to)) {
                regionName = region.getName();  // 获取所在区域的名字
                break;  // 一旦找到区域就可以退出循环
            }
        }

        // 获取玩家之前的区域状态
        String previousRegionName = playerRegionStatus.getOrDefault(player, "0");  // 默认为 "0"，表示不在受限区域

        // 如果区域状态发生变化
        if ((regionName == null && !previousRegionName.equals("0")) || (regionName != null && !regionName.equals(previousRegionName))) {
            if (regionName == null || regionName.equals("0")) {
                // 玩家离开受限区域
                player.sendMessage(plugin.getMessage("region-leave-restricted"));
                plugin.getLogger().info(player.getName() + " 离开受限区域");
            } else {
                // 玩家进入受限区域
                player.sendMessage(plugin.getMessage("region-enter-restricted"));
                plugin.getLogger().info(player.getName() + " 进入受限区域：" + regionName);
            }

            // 更新缓存状态，存储当前区域名称
            playerRegionStatus.put(player, regionName != null ? regionName : "0");
        }

        // 重置玩家的移动计数器
        playerMoveCounter.put(player, 0);
    }





    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        // 检查玩家是否手持指定物品，这里以钻石作为示例
        if (itemInHand.getType() == Material.DIAMOND) {
            event.setCancelled(true);  // 防止破坏方块

            if (action == Action.LEFT_CLICK_BLOCK) {
                // 左键选择第一个点
                firstPoints.put(player, event.getClickedBlock().getLocation());
                player.sendMessage("已选择第一个点：" + event.getClickedBlock().getLocation().toString());
            } else if (action == Action.RIGHT_CLICK_BLOCK) {
                // 右键选择第二个点
                if (firstPoints.containsKey(player)) {
                    secondPoints.put(player, event.getClickedBlock().getLocation());
                    player.sendMessage("已选择第二个点：" + event.getClickedBlock().getLocation().toString());

                    // 生成粒子效果
                    startParticleEffect(player);

                } else {
                    player.sendMessage("请先选择第一个点！");
                }
            }
        }
    }
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location blockLocation = event.getBlock().getLocation();


        // 检查方块是否在受限区域内
        for (Region region : plugin.getRegions()) {
            if (region.isInside(blockLocation)) {
                if (!region.isAllowBreakBlocks()) {
                    event.setCancelled(true);  // 禁止破坏
                    player.sendMessage(plugin.getMessage("region-cannot-break"));
                    return;  // 退出方法，避免进一步处理
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Location blockLocation = event.getBlock().getLocation();

        // 检查方块是否在受限区域内
        for (Region region : plugin.getRegions()) {
            if (region.isInside(blockLocation)) {
                if (!region.isAllowPlaceBlocks()) {
                    event.setCancelled(true);  // 禁止放置
                    player.sendMessage(plugin.getMessage("region-cannot-place"));
                    return;  // 退出方法，避免进一步处理
                }
            }
        }
    }


    public Map<Player, Location> getFirstPoints() {
        return firstPoints;
    }

    public Map<Player, Location> getSecondPoints() {
        return secondPoints;
    }
    private void spawnParticleEffect(Player player, Location firstPoint, Location secondPoint) {
        World world = player.getWorld();

        // 获取长方体的边界
        int minX = Math.min(firstPoint.getBlockX(), secondPoint.getBlockX());
        int maxX = Math.max(firstPoint.getBlockX(), secondPoint.getBlockX());
        int minZ = Math.min(firstPoint.getBlockZ(), secondPoint.getBlockZ());
        int maxZ = Math.max(firstPoint.getBlockZ(), secondPoint.getBlockZ());

        // 以玩家为中心，设置Y坐标范围
        int minY = player.getLocation().getBlockY() - 10; // 粒子Y坐标范围下限
        int maxY = player.getLocation().getBlockY() + 10; // 粒子Y坐标范围上限

        // 生成粒子效果的间隔
        int particleSpacing = 1; // 可以根据需要调整粒子生成的间隔

        // 生成四个竖直边的粒子
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y += particleSpacing) {
                world.spawnParticle(Particle.VILLAGER_HAPPY, x + 0.5, y + 0.5, minZ + 0.5, 1);
                world.spawnParticle(Particle.VILLAGER_HAPPY, x + 0.5, y + 0.5, maxZ + 0.5, 1);
            }
        }

        for (int z = minZ; z <= maxZ; z++) {
            for (int y = minY; y <= maxY; y += particleSpacing) {
                world.spawnParticle(Particle.VILLAGER_HAPPY, minX + 0.5, y + 0.5, z + 0.5, 1);
                world.spawnParticle(Particle.VILLAGER_HAPPY, maxX + 0.5, y + 0.5, z + 0.5, 1);
            }
        }

        // 生成四个水平边的粒子
        /*for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                // 在Y坐标的上下边界位置生成粒子
                world.spawnParticle(Particle.VILLAGER_HAPPY, x + 0.5, minY + 0.5, z + 0.5, 1);
                world.spawnParticle(Particle.VILLAGER_HAPPY, x + 0.5, maxY + 0.5, z + 0.5, 1);
            }
        }*/
    }
    private void startParticleEffect(Player player) {
        // 运行20次，每秒执行一次
        new BukkitRunnable() {
            int counter = 0;

            @Override
            public void run() {
                if (counter < 20) {
                    // 生成粒子效果
                    spawnParticleEffect(player, firstPoints.get(player), secondPoints.get(player));
                    counter++;
                } else {
                    // 任务完成，取消定时任务
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);  // 0L 延迟0个tick执行，20L 每20tick执行一次（即每秒1次）
    }

}
