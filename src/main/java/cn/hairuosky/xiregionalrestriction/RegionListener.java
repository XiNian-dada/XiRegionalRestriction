package cn.hairuosky.xiregionalrestriction;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import java.util.List;

public class RegionListener implements Listener {

    private final XiRegionalRestriction plugin;

    public RegionListener(XiRegionalRestriction plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();  // 获取玩家上次的位置
        Location to = event.getTo();      // 获取玩家当前的位置

        // 输出玩家当前坐标和世界
        plugin.getLogger().info(player.getName() + " 当前坐标: " + to.getX() + ", " + to.getY() + ", " + to.getZ() + " 世界: " + to.getWorld().getName());

        // 防止玩家位置变化太小导致频繁触发
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;  // 如果位置没变，直接跳过
        }

        boolean isInRestrictedRegion = false;

        // 遍历所有区域，检查玩家是否进入了受限区域
        for (Region region : plugin.getRegions()) {
            if (region.isInside(to)) {
                isInRestrictedRegion = true;

                // 输出当前区域的名称和区域坐标范围
                plugin.getLogger().info(player.getName() + " 进入区域: " + region.getName() + " 范围: " +
                        region.getMinX() + ", " + region.getMinZ() + " - " + region.getMaxX() + ", " + region.getMaxZ());
                break;
            }
        }

        // 获取玩家之前的区域状态
        boolean wasInRestrictedRegion = false;
        List<MetadataValue> metadataValues = player.getMetadata("inRestrictedRegion");
        if (!metadataValues.isEmpty()) {
            MetadataValue metadataValue = metadataValues.get(0);
            wasInRestrictedRegion = metadataValue.asBoolean();
        }

        // 输出玩家之前是否在限制区域
        plugin.getLogger().info(player.getName() + " 之前是否在受限区域: " + wasInRestrictedRegion);

        // 如果玩家当前处于受限区域并且之前不在受限区域
        if (isInRestrictedRegion && !wasInRestrictedRegion) {
            player.sendMessage(plugin.getMessage("region-enter-restricted"));
            player.setMetadata("inRestrictedRegion", new FixedMetadataValue(plugin, true));

            // 输出玩家进入受限区域
            plugin.getLogger().info(player.getName() + " 进入受限区域");
        }

        // 如果玩家当前不在受限区域并且之前在受限区域
        if (!isInRestrictedRegion && wasInRestrictedRegion) {
            player.sendMessage(plugin.getMessage("region-leave-restricted"));
            player.removeMetadata("inRestrictedRegion", plugin);  // 明确移除元数据

            // 输出玩家离开受限区域
            plugin.getLogger().info(player.getName() + " 离开受限区域");
        }
    }

}
