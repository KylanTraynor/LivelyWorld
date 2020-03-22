package com.kylantraynor.livelyworld.waterV2;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;

import java.util.Random;

public class WaterUtils {
    private static Random rdm = new Random();
    private static int seed = rdm.nextInt(50000);

    public static boolean getRandomBoolean(){
        return rdm.nextBoolean();
    }

    public static float getRandomFloat(){
        return rdm.nextFloat();
    }

    public static int getRandomInt(int max){
        return rdm.nextInt(max);
    }

    public static int getGeneratorHeight(float x, float z, int base, float amplitude, float persistence, int octaves){
        return (int) (base + perlinNoise2D(x + seed, z + seed, persistence, octaves) * amplitude);
    }

    public static float perlinNoise2D(float x, float z, float persistence, int octaves){
        float n0 = perlinNoise1D(x, persistence, octaves);
        float n1 = perlinNoise1D(z, persistence, octaves);
        return (n0 + n1) * .5f;
    }

    public static float noise(int x)
    {
        x = (x<<13) ^ x;
        return (float) ( 1.0 - ( (x * (x * x * 15731 + 789221) + 1376312589) & 0x7fffffff) / 1073741824.0);
    }
    public static float perlinNoise1D(float x, float persistence, int octaves)
    {
        float total = 0;
        float p = persistence;
        int n = octaves - 1;

        for (int i = 0; i <= n; i++)
        {

            float frequency = (float) Math.pow(2, i);
            double amplitude = Math.pow(p, i);
            total += interpolatedNoise(x * frequency) * amplitude;
        }

        return total;

    }

    private static float interpolatedNoise(float x)
    {
        int integer_X = (int) x;
        float fractional_X = x - integer_X;

        float v1 = smoothNoise1D(integer_X);
        float v2 = smoothNoise1D(integer_X + 1);

        return cosineInterpolate(v1, v2, fractional_X);

    }

    public static float cosineInterpolate(float a, float b, float x)
    {
        float ft = (float) (x * Math.PI);
        float f = (float) ((1 - Math.cos(ft)) * 0.5);

        return a * (1 - f) + b * f;
    }
    public static float smoothNoise1D(int x)
    {
        return noise(x)/2  +  noise(x-1)/4  +  noise(x+1)/4;
    }


    private static int fastfloor(double x) {    return x>0 ? (int)x : (int)x-1;  }

    public static boolean[] dataToObstacles(BlockData data){
        if(data instanceof Slab){
            Slab s = (Slab) data;
            switch(s.getType()){
                case TOP:
                    return new boolean[]{false, false, false, false, true, true, true, true};
                case BOTTOM:
                    return new boolean[] {true, true, true, true, false, false, false, false};
                default:
                    return new boolean[] {true, true, true, true, true, true, true, true};
            }
        } else if(data instanceof Stairs){
            /**
             * TODO make stairs passable
             */
            return new boolean[] {true, true, true, true, true, true, true, true};
        } else {
            return new boolean[] {true, true, true, true, true, true, true, true};
        }
    }

    public static boolean isReplaceable(Material material){
        return minLevelReplaceable(material) <= 8;
    }

    public static int minLevelReplaceable(Material material){
        switch (material){
            case WATER:
            case AIR:
                return 0;
            case GRASS: case FERN: case DEAD_BUSH:
            case OXEYE_DAISY: case POPPY: case BLUE_ORCHID:
            case ORANGE_TULIP: case PINK_TULIP: case RED_TULIP: case WHITE_TULIP:
                return 3;
            case TALL_GRASS: case LARGE_FERN:
                return 4;
            case SNOW: case SNOW_BLOCK:
                return 5;
            default:
                return 9; // can never reach 9, so basically that means will never be replaceable.
        }
    }

    public static Permeability materialToPermeability(Material material){
        switch(material){
            case SAND: case RED_SAND: case SOUL_SAND: case GRAVEL:
            case CYAN_CONCRETE_POWDER: case BLACK_CONCRETE_POWDER: case BLUE_CONCRETE_POWDER:
            case BROWN_CONCRETE_POWDER: case GRAY_CONCRETE_POWDER: case GREEN_CONCRETE_POWDER:
            case LIGHT_BLUE_CONCRETE_POWDER: case LIGHT_GRAY_CONCRETE_POWDER: case LIME_CONCRETE_POWDER:
            case MAGENTA_CONCRETE_POWDER: case ORANGE_CONCRETE_POWDER: case PINK_CONCRETE_POWDER:
            case PURPLE_CONCRETE_POWDER: case RED_CONCRETE_POWDER: case WHITE_CONCRETE_POWDER:
            case YELLOW_CONCRETE_POWDER: case SNOW_BLOCK: case SNOW:
            case SPONGE:
                return Permeability.HIGH;

            case GRASS_BLOCK: case DIRT: case COARSE_DIRT: case FARMLAND: case PODZOL:
            case GRASS_PATH: case CLAY: case PETRIFIED_OAK_SLAB:
                return Permeability.MEDIUM;

            case SANDSTONE: case SANDSTONE_SLAB: case SANDSTONE_STAIRS: case CHISELED_SANDSTONE: case CUT_SANDSTONE:
            case SMOOTH_SANDSTONE: case RED_SANDSTONE: case RED_SANDSTONE_SLAB: case RED_SANDSTONE_STAIRS: case CHISELED_RED_SANDSTONE: case CUT_RED_SANDSTONE:
            case SMOOTH_RED_SANDSTONE:
            case CRACKED_STONE_BRICKS:
            case COBBLESTONE: case COBBLESTONE_SLAB: case COBBLESTONE_STAIRS: case COBBLESTONE_WALL: case MOSSY_COBBLESTONE: case MOSSY_COBBLESTONE_WALL:
                return Permeability.LOW;

            case STONE: case DIORITE: case POLISHED_DIORITE: case GRANITE: case POLISHED_GRANITE: case ANDESITE: case POLISHED_ANDESITE:
            case STONE_BRICKS: case STONE_BRICK_SLAB: case STONE_BRICK_STAIRS: case CHISELED_STONE_BRICKS:
            case MOSSY_STONE_BRICKS:
            case TERRACOTTA: case BLACK_GLAZED_TERRACOTTA: case BLACK_TERRACOTTA: case BLUE_GLAZED_TERRACOTTA:
            case BLUE_TERRACOTTA: case BROWN_GLAZED_TERRACOTTA: case BROWN_TERRACOTTA: case CYAN_GLAZED_TERRACOTTA:
            case CYAN_TERRACOTTA: case GRAY_GLAZED_TERRACOTTA: case GRAY_TERRACOTTA: case GREEN_GLAZED_TERRACOTTA:
            case GREEN_TERRACOTTA: case LIGHT_BLUE_GLAZED_TERRACOTTA: case LIGHT_BLUE_TERRACOTTA: case LIGHT_GRAY_GLAZED_TERRACOTTA:
            case LIGHT_GRAY_TERRACOTTA: case LIME_GLAZED_TERRACOTTA: case LIME_TERRACOTTA: case MAGENTA_GLAZED_TERRACOTTA:
            case MAGENTA_TERRACOTTA: case ORANGE_GLAZED_TERRACOTTA: case ORANGE_TERRACOTTA: case PINK_GLAZED_TERRACOTTA:
            case PINK_TERRACOTTA: case PURPLE_GLAZED_TERRACOTTA: case PURPLE_TERRACOTTA: case RED_GLAZED_TERRACOTTA:
            case RED_TERRACOTTA: case WHITE_GLAZED_TERRACOTTA: case WHITE_TERRACOTTA: case YELLOW_GLAZED_TERRACOTTA:
            case YELLOW_TERRACOTTA:
            case HAY_BLOCK:
            case ACACIA_WOOD: case BIRCH_WOOD: case DARK_OAK_WOOD: case JUNGLE_WOOD: case OAK_WOOD: case SPRUCE_WOOD:
            case STRIPPED_ACACIA_WOOD: case STRIPPED_BIRCH_WOOD: case STRIPPED_DARK_OAK_WOOD: case STRIPPED_JUNGLE_WOOD: case STRIPPED_OAK_WOOD: case STRIPPED_SPRUCE_WOOD:
            case ACACIA_LOG: case BIRCH_LOG: case DARK_OAK_LOG: case JUNGLE_LOG: case OAK_LOG: case SPRUCE_LOG:
            case STRIPPED_ACACIA_LOG: case STRIPPED_BIRCH_LOG: case STRIPPED_DARK_OAK_LOG: case STRIPPED_JUNGLE_LOG: case STRIPPED_OAK_LOG: case STRIPPED_SPRUCE_LOG:
            case ACACIA_PLANKS: case BIRCH_PLANKS: case DARK_OAK_PLANKS: case JUNGLE_PLANKS: case OAK_PLANKS: case SPRUCE_PLANKS:
            case SPRUCE_SLAB: case ACACIA_SLAB: case BIRCH_SLAB: case DARK_OAK_SLAB: case JUNGLE_SLAB: case OAK_SLAB:
            case STONE_SLAB: case BRICK_SLAB: case DARK_PRISMARINE_SLAB: case NETHER_BRICK_SLAB: case PRISMARINE_BRICK_SLAB:
            case PRISMARINE_SLAB: case PURPUR_SLAB: case QUARTZ_SLAB:
            case SEA_LANTERN:
            case ACACIA_STAIRS: case SPRUCE_STAIRS: case BIRCH_STAIRS: case DARK_OAK_STAIRS: case JUNGLE_STAIRS: case OAK_STAIRS:
            case BRICK_STAIRS: case DARK_PRISMARINE_STAIRS: case NETHER_BRICK_STAIRS: case PRISMARINE_BRICK_STAIRS: case PRISMARINE_STAIRS:
            case PURPUR_STAIRS: case QUARTZ_STAIRS:
            case BLACK_CONCRETE: case BLUE_CONCRETE: case BROWN_CONCRETE: case CYAN_CONCRETE: case GRAY_CONCRETE: case GREEN_CONCRETE:
            case LIGHT_BLUE_CONCRETE: case LIGHT_GRAY_CONCRETE: case LIME_CONCRETE: case MAGENTA_CONCRETE: case ORANGE_CONCRETE:
            case PINK_CONCRETE: case PURPLE_CONCRETE: case RED_CONCRETE: case WHITE_CONCRETE: case YELLOW_CONCRETE:
            case BEDROCK:
            case BLUE_ICE: case FROSTED_ICE: case PACKED_ICE: case ICE:
            case BONE_BLOCK: case BRICKS: case END_STONE_BRICKS: case NETHER_BRICKS: case RED_NETHER_BRICKS:
            case CHISELED_QUARTZ_BLOCK: case COAL_BLOCK: case SLIME_BLOCK:
            case LAPIS_BLOCK: case LAPIS_ORE:
            case IRON_ORE: case IRON_BLOCK:
            case GOLD_BLOCK: case GOLD_ORE:
            case REDSTONE_BLOCK: case REDSTONE_ORE:
            case EMERALD_BLOCK: case EMERALD_ORE:
            case GLOWSTONE:
            case REDSTONE_LAMP:
            case BLACK_STAINED_GLASS:case BLUE_STAINED_GLASS: case BROWN_STAINED_GLASS: case CYAN_STAINED_GLASS: case GRAY_STAINED_GLASS:
            case GREEN_STAINED_GLASS: case LIGHT_BLUE_STAINED_GLASS: case LIGHT_GRAY_STAINED_GLASS: case LIME_STAINED_GLASS:
            case MAGENTA_STAINED_GLASS: case ORANGE_STAINED_GLASS: case PINK_STAINED_GLASS: case PURPLE_STAINED_GLASS:
            case RED_STAINED_GLASS: case WHITE_STAINED_GLASS: case YELLOW_STAINED_GLASS:
            case WET_SPONGE:
            case BOOKSHELF:
                return Permeability.NONE;

            default:
                return null;
        }
    }
}
