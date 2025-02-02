package cn.hairuosky.xiregionalrestriction;

import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

public class Region {

    private final String name;
    private final String worldName;
    private final int minX, maxX, minZ, maxZ;

    // 玩家动作控制的配置
    private final boolean allowBreakBlocks;
    private final boolean allowPlaceBlocks;
    private final boolean allowMove;
    private final boolean allowInteractEntities;
    private final boolean allowInteractBlocks;
    private final boolean allowInteractItems;

    // 构造方法
    public Region(String name, String worldName, int minX, int maxX, int minZ, int maxZ,
                  boolean allowBreakBlocks, boolean allowPlaceBlocks, boolean allowMove,
                  boolean allowInteractEntities, boolean allowInteractBlocks, boolean allowInteractItems) {
        this.name = name;
        this.worldName = worldName;
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.allowBreakBlocks = allowBreakBlocks;
        this.allowPlaceBlocks = allowPlaceBlocks;
        this.allowMove = allowMove;
        this.allowInteractEntities = allowInteractEntities;
        this.allowInteractBlocks = allowInteractBlocks;
        this.allowInteractItems = allowInteractItems;
    }

    // 判断位置是否在区域内
    public boolean isInside(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }

        // 确保位置在正确的世界和区域范围内
        boolean isInXRange = (location.getX() >= Math.min(minX, maxX)) && (location.getX() <= Math.max(minX, maxX));
        boolean isInZRange = (location.getZ() >= Math.min(minZ, maxZ)) && (location.getZ() <= Math.max(minZ, maxZ));

        return location.getWorld().getName().equals(worldName) && isInXRange && isInZRange;
    }

    // 返回区域的边界作为一个BoundingBox
    public BoundingBox getBoundingBox() {
        return new BoundingBox(Math.min(minX, maxX), 0, Math.min(minZ, maxZ),
                Math.max(minX, maxX), 256, Math.max(minZ, maxZ));  // 高度为 0 到 256，可以根据需要调整
    }

    // 获取区域外的一个安全位置，假设返回的安全位置在区域外10格
    public Location getSafeLocationOutside(Location currentLocation) {
        double playerX = currentLocation.getX();
        double playerZ = currentLocation.getZ();

        double safeX = playerX > maxX ? playerX + 10 : (playerX < minX ? playerX - 10 : playerX);
        double safeZ = playerZ > maxZ ? playerZ + 10 : (playerZ < minZ ? playerZ - 10 : playerZ);
        double safeY = currentLocation.getY();  // 保持 Y 不变

        // 创建安全位置
        return new Location(currentLocation.getWorld(), safeX, safeY, safeZ);
    }

    // Getter方法
    public String getName() {
        return name;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getMinX() {
        return minX;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public boolean isAllowBreakBlocks() {
        return allowBreakBlocks;
    }

    public boolean isAllowPlaceBlocks() {
        return allowPlaceBlocks;
    }

    public boolean isAllowMove() {
        return allowMove;
    }

    public boolean isAllowInteractEntities() {
        return allowInteractEntities;
    }

    public boolean isAllowInteractBlocks() {
        return allowInteractBlocks;
    }

    public boolean isAllowInteractItems() {
        return allowInteractItems;
    }

    // 打印区域信息，方便调试
    @Override
    public String toString() {
        return String.format("Region{name='%s', world='%s', minX=%d, maxX=%d, minZ=%d, maxZ=%d, allowBreakBlocks=%b, allowPlaceBlocks=%b, allowMove=%b, allowInteractEntities=%b, allowInteractBlocks=%b, allowInteractItems=%b}",
                name, worldName, minX, maxX, minZ, maxZ, allowBreakBlocks, allowPlaceBlocks, allowMove, allowInteractEntities, allowInteractBlocks, allowInteractItems);
    }
}
