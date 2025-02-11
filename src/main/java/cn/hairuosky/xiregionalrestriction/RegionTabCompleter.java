package cn.hairuosky.xiregionalrestriction;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class RegionTabCompleter implements TabCompleter {

    private final XiRegionalRestriction plugin;

    public RegionTabCompleter(XiRegionalRestriction plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (sender instanceof Player) {
            //Player player = (Player) sender;

            if (command.getName().equalsIgnoreCase("xiregionalrestriction")) {
                if (args.length == 1) {
                    // 补全子命令
                    completions.add("create");
                    completions.add("reload");
                    completions.add("delete");

                    // 过滤补全列表
                    return filterCompletions(completions, args[0]);
                } else if (args.length == 2) {
                    String subCommand = args[0].toLowerCase();

                    if (subCommand.equals("delete")) {
                        // 补全区域名称
                        for (Region region : plugin.getRegions()) {
                            completions.add(region.getName());
                        }

                        // 过滤补全列表
                        return filterCompletions(completions, args[1]);
                    }
                }
            }
        }

        return completions;
    }

    // 过滤补全列表
    private List<String> filterCompletions(List<String> completions, String arg) {
        List<String> filteredCompletions = new ArrayList<>();
        for (String completion : completions) {
            if (completion.toLowerCase().startsWith(arg.toLowerCase())) {
                filteredCompletions.add(completion);
            }
        }
        return filteredCompletions;
    }
}
