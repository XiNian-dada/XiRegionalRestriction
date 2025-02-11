package cn.hairuosky.xiregionalrestriction;

import org.bukkit.Location;
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
            sender.sendMessage(plugin.getMessage("only-player-can-do", "只有玩家可以执行此命令！"));
            return false;
        }

        Player player = (Player) sender;

        // 检查指令参数是否合法
        if (args.length < 1) {
            player.sendMessage(plugin.getMessage("command-usage", "用法: /xrr create <区域名称> 或 /xrr create <区域名称> <min-x> <min-z> <max-x> <max-z> [世界名] 或 /xrr reload"));
            return false;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                if (!player.hasPermission("xiregionalrestriction.create")) {
                    player.sendMessage(plugin.getMessage("no-permission", "你没有权限执行此命令！"));
                    return false;
                }
                if (args.length < 2) {
                    player.sendMessage(plugin.getMessage("command-usage", "用法: /xrr create <区域名称> 或 /xrr create <区域名称> <min-x> <min-z> <max-x> <max-z> [世界名]"));
                    return false;
                }

                String regionName = args[1];

                // 如果玩家已经通过交互选择了两个点
                if (plugin.getRegionListener().getFirstPoints().containsKey(player) &&
                        plugin.getRegionListener().getSecondPoints().containsKey(player)) {

                    Location firstPoint = plugin.getRegionListener().getFirstPoints().get(player);
                    Location secondPoint = plugin.getRegionListener().getSecondPoints().get(player);

                    int minX = Math.min(firstPoint.getBlockX(), secondPoint.getBlockX());
                    int maxX = Math.max(firstPoint.getBlockX(), secondPoint.getBlockX());
                    int minZ = Math.min(firstPoint.getBlockZ(), secondPoint.getBlockZ());
                    int maxZ = Math.max(firstPoint.getBlockZ(), secondPoint.getBlockZ());

                    String worldName = player.getWorld().getName();  // 默认使用玩家所在的世界

                    // 创建并保存区域配置
                    createRegionConfig(regionName, minX, minZ, maxX, maxZ, worldName);

                    // 提示玩家区域创建成功
                    player.sendMessage(plugin.getMessage("region-created", "区域 {name} 创建成功！")
                            .replace("{name}", regionName));

                    // 清除选定的坐标
                    plugin.getRegionListener().getFirstPoints().remove(player);
                    plugin.getRegionListener().getSecondPoints().remove(player);

                    return true;
                }

                // 如果没有选择点，检查是否有手动输入坐标
                if (args.length >= 6) {
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
                        player.sendMessage(plugin.getMessage("region-created", "区域 {name} 创建成功！")
                                .replace("{name}", regionName));
                        return true;
                    } catch (NumberFormatException e) {
                        player.sendMessage(plugin.getMessage("location-should-be-integer", "坐标必须是整数！"));
                        return false;
                    }
                }

                // 如果没有选择点也没有输入坐标，提示用户
                player.sendMessage(plugin.getMessage("operation-error", "操作错误！"));
                return false;

            case "reload":
                // 检查玩家是否有权限
                if (!player.hasPermission("xiregionalrestriction.reload")) {
                    player.sendMessage(plugin.getMessage("no-permission", "你没有权限执行此命令！"));
                    return false;
                }

                // 调用 reload 方法
                plugin.reload();
                player.sendMessage(plugin.getMessage("reload.success", "插件数据已成功重新加载！"));
                return true;

            default:
                player.sendMessage(plugin.getMessage("command-usage", "用法: /xrr create <区域名称> 或 /xrr create <区域名称> <min-x> <min-z> <max-x> <max-z> [世界名] 或 /xrr reload"));
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
            throw new IllegalArgumentException(plugin.getMessage("region-already-exists","区域 {region} 已经存在！")
                    .replace("{region}", regionName));
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
            plugin.getLogger().warning(plugin.getMessage("cannot-save-region","保存区域 {region} 时出错！")
                    .replace("{region}", regionFile.getName()));
        }

        // 加载新的区域到内存
        Region region = new Region(regionName, worldName, minX, maxX, minZ, maxZ,true,true,true,true,true,true);
        plugin.getRegions().add(region);
    }


}
