package cn.hairuosky.xiregionalrestriction;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

public class RegionCommandExecutor implements CommandExecutor {

    private final XiRegionalRestriction plugin;

    public RegionCommandExecutor(XiRegionalRestriction plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 只有玩家可以执行该命令
        if (!(sender instanceof Player)) {
            sender.sendMessage("此命令只能由玩家执行！");
            return false;
        }

        Player player = (Player) sender;

        // 检查指令参数是否合法
        if (args.length < 6) {
            player.sendMessage("用法: /xrr create <区域名称> <min-x> <min-z> <max-x> <max-z> [世界名]");
            return false;
        }

        String regionName = args[1];
        try {
            // 解析坐标
            int minX = Integer.parseInt(args[2]);
            int minZ = Integer.parseInt(args[3]);
            int maxX = Integer.parseInt(args[4]);
            int maxZ = Integer.parseInt(args[5]);

            // 获取世界名，若未指定，则使用玩家当前世界
            String worldName = (args.length > 6) ? args[6] : player.getWorld().getName();

            // 创建并保存区域配置
            createRegionConfig(regionName, minX, minZ, maxX, maxZ, worldName);

            // 提示玩家区域创建成功
            player.sendMessage("区域 " + regionName + " 已成功创建！");
            return true;
        } catch (NumberFormatException e) {
            player.sendMessage("坐标必须是有效的整数！");
            return false;
        }
    }

    // 创建并保存区域配置文件
    private void createRegionConfig(String regionName, int minX, int minZ, int maxX, int maxZ, String worldName) {
        File regionsFolder = new File(plugin.getDataFolder(), "regions");
        if (!regionsFolder.exists()) {
            regionsFolder.mkdirs();
        }

        // 使用 regionName 作为新的区域配置文件名
        File regionFile = new File(regionsFolder, regionName + ".yml");
        if (regionFile.exists()) {
            throw new IllegalArgumentException("区域配置文件已存在！");
        }

        // 从资源文件夹加载模板配置（example_region.yml）
        File templateFile = new File(plugin.getDataFolder(), "example_region.yml");
        if (!templateFile.exists()) {
            plugin.saveResource("example_region.yml", false); // 确保模板文件存在
            templateFile = new File(plugin.getDataFolder(), "example_region.yml");
        }

        // 加载模板配置文件
        YamlConfiguration config = YamlConfiguration.loadConfiguration(templateFile);

        // 修改模板中的值
        config.set("name", regionName);
        config.set("world", worldName); // 使用动态获取的世界名
        config.set("min-x", minX);
        config.set("max-x", maxX);
        config.set("min-z", minZ);
        config.set("max-z", maxZ);

        // 保存修改后的配置文件，文件名为 regionName.yml
        try {
            config.save(regionFile);
        } catch (IOException e) {
            plugin.getLogger().warning("无法保存区域配置文件 " + regionFile.getName());
        }

        // 加载新的区域到内存
        Region region = new Region(regionName, worldName, minX, maxX, minZ, maxZ);
        plugin.getRegions().add(region);
    }


}
