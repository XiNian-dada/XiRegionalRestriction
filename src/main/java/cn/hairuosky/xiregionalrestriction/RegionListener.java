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
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import java.util.*;

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

    private Map<Player, Location> playerLastSafeLocation = new HashMap<>();  // 用于存储每个玩家上一次的安全位置

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();  // 获取玩家当前的位置

        // 获取当前玩家的移动计数器，如果没有计数器则初始化为 0
        int moveCount = playerMoveCounter.getOrDefault(player, 0);

        // 如果计数器未达到指定次数n，则跳过区域检查
        if (moveCount < n) {
            playerMoveCounter.put(player, moveCount + 1); // 增加计数器
            return;  // 跳过本次区域检查
        }

        // 重置玩家的移动计数器
        playerMoveCounter.put(player, 0);

        // 检查玩家是否在受限区域内
        String regionName = null;
        for (Region region : plugin.getRegions()) {
            if (region.isInside(to)) {
                regionName = region.getName();  // 获取所在区域的名字
                break;  // 一旦找到区域就可以退出循环
            }
        }

        if (regionName != null) {
            Region region = plugin.getRegionByName(regionName);
            if (region != null && !region.isAllowMove()) {
                // 找到玩家上一次的安全位置并传送
                Location safeLocation = playerLastSafeLocation.getOrDefault(player, to);
                // 创建一个新的Location对象，使用上一次的安全位置的坐标，但保留当前的视角方向
                Location teleportLocation = new Location(safeLocation.getWorld(), safeLocation.getX(), safeLocation.getY(), safeLocation.getZ(), player.getLocation().getYaw(), player.getLocation().getPitch());
                player.teleport(teleportLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
                player.sendMessage(plugin.getMessage("teleported-outside-restricted"));
                // 更新玩家状态为不在受限区域
                playerRegionStatus.put(player, "0");
            } else {
                // 更新玩家状态为当前区域
                playerRegionStatus.put(player, regionName);
                // 更新玩家上一次的安全位置
                playerLastSafeLocation.put(player, to);
            }
        } else {
            // 更新玩家状态为不在受限区域
            playerRegionStatus.put(player, "0");
            // 更新玩家上一次的安全位置
            playerLastSafeLocation.put(player, to);
        }
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
        Block clickedBlock = event.getClickedBlock();

        if (clickedBlock == null) {
            return;  // 如果没有点击方块，直接返回
        }

        Material blockType = clickedBlock.getType();

        // 定义需要处理的红石器件和容器类型
        // 使用 EnumSet 替代 Set.of()
        Set<Material> INTERACTABLE_MATERIALS = EnumSet.of(
                Material.STONE_PRESSURE_PLATE, Material.OAK_PRESSURE_PLATE, Material.SPRUCE_PRESSURE_PLATE,
                Material.BIRCH_PRESSURE_PLATE, Material.JUNGLE_PRESSURE_PLATE, Material.ACACIA_PRESSURE_PLATE,
                Material.DARK_OAK_PRESSURE_PLATE, Material.CRIMSON_PRESSURE_PLATE, Material.WARPED_PRESSURE_PLATE,
                Material.HEAVY_WEIGHTED_PRESSURE_PLATE, Material.LIGHT_WEIGHTED_PRESSURE_PLATE,
                Material.OAK_DOOR, Material.IRON_DOOR, Material.SPRUCE_DOOR, Material.BIRCH_DOOR,
                Material.JUNGLE_DOOR, Material.ACACIA_DOOR, Material.DARK_OAK_DOOR, Material.CRIMSON_DOOR,
                Material.WARPED_DOOR, Material.LEVER, Material.STONE_BUTTON, Material.OAK_BUTTON,
                Material.SPRUCE_BUTTON, Material.BIRCH_BUTTON, Material.JUNGLE_BUTTON, Material.ACACIA_BUTTON,
                Material.DARK_OAK_BUTTON, Material.CRIMSON_BUTTON, Material.WARPED_BUTTON, Material.REPEATER,
                Material.COMPARATOR, Material.DAYLIGHT_DETECTOR, Material.HOPPER, Material.DROPPER,
                Material.DISPENSER, Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL, Material.FURNACE,
                Material.BLAST_FURNACE, Material.SMOKER, Material.BREWING_STAND, Material.ENCHANTING_TABLE,
                Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL, Material.GRINDSTONE,
                Material.CARTOGRAPHY_TABLE, Material.FLETCHING_TABLE, Material.SMITHING_TABLE, Material.STONECUTTER,
                Material.LOOM, Material.CAULDRON, Material.BEACON, Material.SHULKER_BOX, Material.BLACK_SHULKER_BOX,
                Material.BLUE_SHULKER_BOX, Material.BROWN_SHULKER_BOX, Material.CYAN_SHULKER_BOX, Material.GRAY_SHULKER_BOX,
                Material.GREEN_SHULKER_BOX, Material.LIGHT_BLUE_SHULKER_BOX, Material.LIME_SHULKER_BOX, Material.MAGENTA_SHULKER_BOX,
                Material.ORANGE_SHULKER_BOX, Material.PINK_SHULKER_BOX, Material.PURPLE_SHULKER_BOX, Material.RED_SHULKER_BOX,
                Material.WHITE_SHULKER_BOX, Material.YELLOW_SHULKER_BOX
        );


        // 检查点击的方块是否在需要处理的类型中
        if (!INTERACTABLE_MATERIALS.contains(blockType)) {
            return;  // 如果不是需要处理的方块类型，直接返回
        }

        Location blockLocation = clickedBlock.getLocation();

        // 检查玩家是否在限制区域内
        for (Region region : plugin.getRegions()) {
            if (region.isInside(blockLocation)) {
                // 判断配置文件中的方块交互限制
                if (!region.isAllowInteractBlocks()) {
                    event.setCancelled(true);  // 禁止与方块交互
                    player.sendMessage(plugin.getMessage("region-cannot-interact-block"));
                    return;  // 退出方法，避免进一步处理
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
