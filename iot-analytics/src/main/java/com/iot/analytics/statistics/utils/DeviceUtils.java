package com.iot.analytics.statistics.utils;

public class DeviceUtils {
    public static double calculateCoverageVolume(
            int minX, int maxX,
            int minY, int maxY,
            int minZ, int maxZ,
            long deviceCount) {

        if (deviceCount == 0 ||
                minX == Integer.MAX_VALUE || maxX == Integer.MIN_VALUE ||
                minY == Integer.MAX_VALUE || maxY == Integer.MIN_VALUE ||
                minZ == Integer.MAX_VALUE || maxZ == Integer.MIN_VALUE) {
            return 0;
        }

        if (deviceCount == 1) {
            double radius = 50.0;
            return (4.0 / 3.0) * Math.PI * Math.pow(radius, 3);
        }

        double coverageRadius = 50.0;
        double width = Math.max(0, (maxX - minX) + 2 * coverageRadius);
        double height = Math.max(0, (maxY - minY) + 2 * coverageRadius);
        double depth = Math.max(0, (maxZ - minZ) + 2 * coverageRadius);

        return width * height * depth;
    }
}


