package cn.hairuosky.xiregionalrestriction;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RegionListener implements Listener {

    private final XiRegionalRestriction plugin;
    private final Map<Player, String> playerRegionStatus = new HashMap<>();  // 缓存玩家的区域状态
    private Map<Player, Location> firstPoints = new HashMap<>();
    private Map<Player, Location> secondPoints = new HashMap<>();
    private int n = 3;

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

                if (!Objects.requireNonNull(plugin.getRegionByName(regionName)).isAllowMove()) {
                    // 找到距离玩家最近的安全点并传送
                    Location safeLocation = findSafeLocationOutsideRegion(player, regionName);
                    if (safeLocation != null) {
                        player.teleport(safeLocation);
                        player.sendMessage(plugin.getMessage("teleported-outside-restricted"));
                        // 更新玩家状态为不在受限区域
                        playerRegionStatus.put(player, "0");
                    }
                }
            }

            // 更新缓存状态，存储当前区域名称
            playerRegionStatus.put(player, regionName != null ? regionName : "0");
        }

        // 重置玩家的移动计数器
        playerMoveCounter.put(player, 0);
    }

    private Location findSafeLocationOutsideRegion(Player player, String regionName) {
        // 获取玩家当前的位置
        Location currentLocation = player.getLocation();

        // 获取区域对象
        Region region = plugin.getRegionByName(regionName);
        if (region == null) {
            return null;  // 如果找不到区域对象，返回 null
        }

        // 获取区域的边界坐标（假设区域是矩形或者类似形状，你需要根据你的插件实现来调整）
        BoundingBox boundingBox = region.getBoundingBox();  // 这个假设你有一个BoundingBox类来定义区域

        // 计算玩家当前位置的外部位置，远离区域的边界
        Location safeLocation = getSafeLocationFromBoundingBox(currentLocation, boundingBox);
        return safeLocation;
    }

    private Location getSafeLocationFromBoundingBox(Location currentLocation, BoundingBox boundingBox) {
        // 获取玩家当前位置的坐标
        double playerX = currentLocation.getX();
        double playerY = currentLocation.getY();
        double playerZ = currentLocation.getZ();

        // 获取区域边界的坐标（假设区域是矩形，返回最小和最大X、Y、Z值）
        double minX = boundingBox.getMinX();
        double maxX = boundingBox.getMaxX();
        double minY = boundingBox.getMinY();
        double maxY = boundingBox.getMaxY();
        double minZ = boundingBox.getMinZ();
        double maxZ = boundingBox.getMaxZ();

        // 计算玩家应该传送到的最近边界位置
        double safeX = playerX;
        double safeY = playerY;
        double safeZ = playerZ;

        if (playerX < minX) {
            safeX = minX - 11;  // 往外移动10格
        } else if (playerX > maxX) {
            safeX = maxX + 11;  // 往外移动10格
        }

        if (playerY < minY) {
            safeY = minY - 11;  // 往外移动10格
        } else if (playerY > maxY) {
            safeY = maxY + 11;  // 往外移动10格
        }

        if (playerZ < minZ) {
            safeZ = minZ - 11;  // 往外移动10格
        } else if (playerZ > maxZ) {
            safeZ = maxZ + 11;  // 往外移动10格
        }

        // 创建一个新的安全位置
        Location safeLocation = new Location(currentLocation.getWorld(), safeX, safeY, safeZ);

        // 确保安全位置不在悬崖或者其他危险区域，并且不在区域内
        int maxAttempts = 10;  // 设置最大尝试次数
        int attempts = 0;

        while (isInsideRegion(safeLocation, boundingBox) || !isLocationSafe(safeLocation) && attempts < maxAttempts) {
            // 如果位置在区域内或不安全，尝试进一步偏移
            safeLocation = safeLocation.add(5, 0, 5);
            attempts++;
        }

        if (isInsideRegion(safeLocation, boundingBox)) {
            plugin.getLogger().warning("无法找到安全且不在受限区域的位置");
            return null;  // 如果找不到安全且不在受限区域的位置，返回 null
        }

        return safeLocation;
    }

    private boolean isInsideRegion(Location location, BoundingBox boundingBox) {
        // 检查位置是否在区域内
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        return x >= boundingBox.getMinX() && x <= boundingBox.getMaxX() &&
                y >= boundingBox.getMinY() && y <= boundingBox.getMaxY() &&
                z >= boundingBox.getMinZ() && z <= boundingBox.getMaxZ();
    }


    private boolean isLocationSafe(Location location) {
        // 检查该位置是否安全，例如是否有悬崖、熔岩、怪物等
        Block block = location.getBlock();
        return block.getType() != Material.LAVA && block.getType() != Material.FIRE && !block.isLiquid();
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
    @EventHandler
    public void onPlayerInteractHandler(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // 检查玩家与方块的交互
        if (event.getClickedBlock() != null) {
            for (Region region : plugin.getRegions()) {
                if (region.isInside(event.getClickedBlock().getLocation())) {
                    // 判断配置文件中的方块交互限制
                    if (!region.isAllowInteractBlocks()) {
                        event.setCancelled(true);  // 禁止与方块交互
                        player.sendMessage(plugin.getMessage("region-cannot-interact-block"));
                    }
                }
            }
        }
        // 检查玩家是否在限制区域内，并判断是否允许使用物品
        for (Region region : plugin.getRegions()) {
            if (region.isInside(player.getLocation())) {
                if (!region.isAllowInteractItems() && event.getItem() != null) {
                    event.setCancelled(true);  // 禁止使用物品
                    player.sendMessage(plugin.getMessage("region-cannot-interact-item"));
                }
            }
        }
    }

    @EventHandler
    public void onEntityInteractHandler(EntityInteractEvent event) {
        Entity entity = event.getEntity();
        // 遍历所有区域
        for (Region region : plugin.getRegions()) {
            if (region.isInside(entity.getLocation())) {
                // 判断配置文件中的实体交互限制
                if (!region.isAllowInteractEntities()) {
                    event.setCancelled(true);  // 禁止与实体交互
                    if (entity instanceof Player) {
                        Player player = (Player) entity;
                        player.sendMessage(plugin.getMessage("region-cannot-interact-entity"));
                    }
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
        // 检查 firstPoint 和 secondPoint 是否为 null
        Location firstPoint = firstPoints.get(player);
        Location secondPoint = secondPoints.get(player);

        if (firstPoint == null || secondPoint == null) {
            plugin.getLogger().warning("粒子效果生成任务启动失败：firstPoint 或 secondPoint 为 null");
            return;  // 如果 firstPoint 或 secondPoint 为 null，直接返回
        }

        // 运行20次，每秒执行一次
        new BukkitRunnable() {
            int counter = 0;

            @Override
            public void run() {
                // 再次检查 firstPoint 和 secondPoint 是否为 null
                Location currentFirstPoint = firstPoints.get(player);
                Location currentSecondPoint = secondPoints.get(player);

                if (currentFirstPoint == null || currentSecondPoint == null) {
                    cancel();  // 如果 firstPoint 或 secondPoint 为 null，取消任务
                    plugin.getLogger().warning("粒子效果生成任务取消：firstPoint 或 secondPoint 为 null");
                    return;
                }

                if (counter < 20) {
                    // 生成粒子效果
                    spawnParticleEffect(player, currentFirstPoint, currentSecondPoint);
                    counter++;
                } else {
                    // 任务完成，取消定时任务
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);  // 0L 延迟0个tick执行，20L 每20tick执行一次（即每秒1次）
    }


}
