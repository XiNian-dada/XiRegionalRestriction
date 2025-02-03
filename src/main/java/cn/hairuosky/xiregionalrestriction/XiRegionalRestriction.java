package cn.hairuosky.xiregionalrestriction;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

//TODO 性能审查
//TODO 功能审查
//TODO MESSAGES.YML
//TODO CONFIG.yml
//TODO 小的功能BUG，比如开启interaction后，place和break被覆盖
//TODO TAB COMPLETER 指令
//TODO PERMISSION
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class XiRegionalRestriction extends JavaPlugin implements Listener {

    // 存储所有区域的列表
    private List<Region> regions = new ArrayList<>();
    private RegionListener regionListener;

    @Override
    public void onEnable() {
        // 创建 config.yml 配置文件
        saveDefaultConfig();

        // 创建 messages.yml 配置文件
        saveResource("messages.yml", false);

        // 创建 regions 文件夹，如果不存在则创建
        File regionsFolder = new File(getDataFolder(), "regions");
        if (!regionsFolder.exists()) {
            regionsFolder.mkdirs();
        }

        // 加载区域数据
        loadRegions();
        // 实例化 RegionListener 并注册事件
        regionListener = new RegionListener(this);
        getServer().getPluginManager().registerEvents(regionListener, this);


        // 注册命令处理器
        getCommand("xrr").setExecutor(new RegionCommandExecutor(this));

        getLogger().info("XiRegionalRestriction 插件已启用！");
    }

    @Override
    public void onDisable() {
        // 插件关闭时的逻辑（如果需要）
    }

    // 从配置文件加载区域数据
    private void loadRegions() {
        File regionsFolder = new File(getDataFolder(), "regions");
        File[] regionFiles = regionsFolder.listFiles((dir, name) -> name.endsWith(".yml"));

        if (regionFiles != null) {
            for (File regionFile : regionFiles) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(regionFile);

                String regionName = config.getString("name");
                String worldName = config.getString("world");
                int minX = config.getInt("min-x");
                int maxX = config.getInt("max-x");
                int minZ = config.getInt("min-z");
                int maxZ = config.getInt("max-z");

                // 读取 allow-player-action 配置
                boolean allowBreakBlocks = config.getBoolean("allow-player-action.break-blocks", true);
                boolean allowPlaceBlocks = config.getBoolean("allow-player-action.place-blocks", true);
                boolean allowMove = config.getBoolean("allow-player-action.move", true);  // 新增的配置
                boolean allowInteractEntities = config.getBoolean("allow-player-action.interaction.interact-entities", true);  // 新增的配置
                boolean allowInteractBlocks = config.getBoolean("allow-player-action.interaction.interact-blocks", true);  // 新增的配置
                boolean allowInteractItems = config.getBoolean("allow-player-action.interaction.interact-items", true);  // 新增的配置

                // 创建并保存区域对象
                Region region = new Region(regionName, worldName, minX, maxX, minZ, maxZ,
                        allowBreakBlocks, allowPlaceBlocks, allowMove,
                        allowInteractEntities, allowInteractBlocks, allowInteractItems);
                regions.add(region);
            }
        }
    }

    // 获取所有区域（供其他类使用）
    public List<Region> getRegions() {
        return regions;
    }
    public Region getRegionByName(String regionName) {
        for (Region region : getRegions()) {
            if (region.getName().equals(regionName)) {
                return region;
            }
        }
        return null;
    }

    // 获取消息
    public String getMessage(String key) {
        return getConfig().getString("messages." + key, "默认消息: " + key);
    }
    public RegionListener getRegionListener() {
        return regionListener;
    }



}
