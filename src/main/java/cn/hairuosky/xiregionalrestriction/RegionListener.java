package cn.hairuosky.xiregionalrestriction;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;

public class RegionListener implements Listener {

    private final XiRegionalRestriction plugin;
    private final Map<Player, Boolean> playerRegionStatus = new HashMap<>();  // 缓存玩家的区域状态
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
            playerMoveCounter.put(player, moveCount + 1);
            player.sendMessage("移动次数：" + moveCount);
            return;  // 跳过本次区域检查
        }

        // 计数器达到 n 后进行区域检查
        boolean isInRestrictedRegion = false;
        for (Region region : plugin.getRegions()) {
            if (region.isInside(to)) {
                isInRestrictedRegion = true;
                break;  // 一旦找到区域就可以退出循环
            }
        }

        // 获取玩家之前的区域状态
        boolean wasInRestrictedRegion = playerRegionStatus.getOrDefault(player, false);  // 使用缓存

        // 如果区域状态发生变化
        if (isInRestrictedRegion != wasInRestrictedRegion) {
            if (isInRestrictedRegion) {
                // 玩家进入受限区域
                player.sendMessage(plugin.getMessage("region-enter-restricted"));
                plugin.getLogger().info(player.getName() + " 进入受限区域");
            } else {
                // 玩家离开受限区域
                player.sendMessage(plugin.getMessage("region-leave-restricted"));
                plugin.getLogger().info(player.getName() + " 离开受限区域");
            }

            // 更新缓存状态
            playerRegionStatus.put(player, isInRestrictedRegion);
        }

        // 重置玩家的移动计数器
        playerMoveCounter.put(player, 0);
    }

}
