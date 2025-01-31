package cn.hairuosky.xiregionalrestriction;

import org.bukkit.Location;

public class Region {

    private final String name;
    private final String worldName;
    private final int minX, maxX, minZ, maxZ;

    // 构造方法
    public Region(String name, String worldName, int minX, int maxX, int minZ, int maxZ) {
        this.name = name;
        this.worldName = worldName;
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
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

    // 打印区域信息，方便调试
    @Override
    public String toString() {
        return String.format("Region{name='%s', world='%s', minX=%d, maxX=%d, minZ=%d, maxZ=%d}",
                name, worldName, minX, maxX, minZ, maxZ);
    }
}
