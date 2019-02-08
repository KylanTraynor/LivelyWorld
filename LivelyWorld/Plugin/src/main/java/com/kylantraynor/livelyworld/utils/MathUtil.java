package com.kylantraynor.livelyworld.utils;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public class MathUtil {

    private static int CHUNK_VALUES = 16;
    private static int CHUNK_BITS = 4;

    /**
     * Gets the floor long value from a double value
     *
     * @param value to get the floor of
     * @return floor value
     */
    public static long longFloor(double value) {
        long l = (long) value;
        return value < l ? l - 1L : l;
    }

    /**
     * Gets the floor integer value from a double value
     *
     * @param value to get the floor of
     * @return floor value
     */
    public static int floor(double value) {
        int i = (int) value;
        return value < (double) i ? i - 1 : i;
    }

    /**
     * Gets the floor integer value from a float value
     *
     * @param value to get the floor of
     * @return floor value
     */
    public static int floor(float value) {
        int i = (int) value;
        return value < (float) i ? i - 1 : i;
    }

    /**
     * Gets the ceiling integer value from a double value
     *
     * @param value to get the ceiling of
     * @return ceiling value
     */
    public static int ceil(double value) {
        return -floor(-value);
    }

    /**
     * Gets the ceiling integer value from a float value
     *
     * @param value to get the ceiling of
     * @return ceiling value
     */
    public static int ceil(float value) {
        return -floor(-value);
    }

    /**
     * Converts a location value into a chunk coordinate
     *
     * @param loc to convert
     * @return chunk coordinate
     */
    public static int toChunk(double loc) {
        return floor(loc / (double) CHUNK_VALUES);
    }

    /**
     * Converts a location value into a chunk coordinate
     *
     * @param loc to convert
     * @return chunk coordinate
     */
    public static int toChunk(int loc) {
        return loc >> CHUNK_BITS;
    }

    public static double useOld(double oldvalue, double newvalue, double peruseold) {
        return oldvalue + (peruseold * (newvalue - oldvalue));
    }

    public static double lerp(double d1, double d2, double stage) {
        if (Double.isNaN(stage) || stage > 1) {
            return d2;
        } else if (stage < 0) {
            return d1;
        } else {
            return d1 * (1 - stage) + d2 * stage;
        }
    }

    public static Vector lerp(Vector vec1, Vector vec2, double stage) {
        Vector newvec = new Vector();
        newvec.setX(lerp(vec1.getX(), vec2.getX(), stage));
        newvec.setY(lerp(vec1.getY(), vec2.getY(), stage));
        newvec.setZ(lerp(vec1.getZ(), vec2.getZ(), stage));
        return newvec;
    }

    public static Location lerp(Location loc1, Location loc2, double stage) {
        Location newloc = new Location(loc1.getWorld(), 0, 0, 0);
        newloc.setX(lerp(loc1.getX(), loc2.getX(), stage));
        newloc.setY(lerp(loc1.getY(), loc2.getY(), stage));
        newloc.setZ(lerp(loc1.getZ(), loc2.getZ(), stage));
        newloc.setYaw((float) lerp(loc1.getYaw(), loc2.getYaw(), stage));
        newloc.setPitch((float) lerp(loc1.getPitch(), loc2.getPitch(), stage));
        return newloc;
    }
}
