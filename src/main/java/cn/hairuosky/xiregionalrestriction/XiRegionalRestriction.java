package cn.hairuosky.xiregionalrestriction;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class XiRegionalRestriction extends JavaPlugin implements Listener {

    // 存储所有区域的列表
    private List<Region> regions = new ArrayList<>();

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
        getServer().getPluginManager().registerEvents(new RegionListener(this), this);


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

                // 创建并保存区域对象（忽略y坐标）
                Region region = new Region(regionName, worldName, minX, maxX, minZ, maxZ);
                regions.add(region);
            }
        }
    }
    // 获取所有区域（供其他类使用）
    public List<Region> getRegions() {
        return regions;
    }

    // 获取消息
    public String getMessage(String key) {
        return getConfig().getString("messages." + key, "默认消息: " + key);
    }




}
