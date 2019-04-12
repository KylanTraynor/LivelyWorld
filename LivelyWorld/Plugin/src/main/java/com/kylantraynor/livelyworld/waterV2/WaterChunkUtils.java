package com.kylantraynor.livelyworld.waterV2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;


public class WaterChunkUtils {
    final private static int sectorLength = 4096;


    public static boolean save(WaterChunk chunk){
        /**
         * -La position de la liste des futures cellules.
         * -La position du début de la liste des blocs.
         * -La liste des blocks à mettre à jour
         * -La queue des futures cellules à processer.
         * -Ensuite pour chaque bloc :
         *      - 4 bits pour la perméabilité.
         *          - 0000b pour null
         *          - 0001b pour NONE
         *          - 0010b pour LOW
         *          - 0011b pour MEDIUM
         *          - 0100b pour HIGH
         *      - 4 bits pour la salinité.
         *          - 0000b pour NONE
         *          - 0001b pour LOW
         *          - 0010b pour MEDIUM
         *          - 0011b pour HIGH
         *      - 8 bits pour les obstacles.
         *          - 0000 1111b pour les 4 obstacles du haut
         *          - 1111 0000b pour les 4 obstacles du bas
         *      - 32 bits pour l'eau.
         *          - 0000 0000 0000 0000 0000 0000 0000 1111b pour le niveau le plus haut
         *          - 1111 0000 0000 0000 0000 0000 0000 0000b pour le niveau le plus bas
         *
         *      Total par block 48 bits (6 bytes)
         *      Block inutilisé:
         *      PERM SALI OBSTACLE EAU
         *      0000 0000 00000000 00000000000000000000000000000000b
         *
         */

        int salinity = 0;

        List<Byte> list = new ArrayList<>();

        int setLocation = 8;
        int blocksLocation = 8;

        Iterator<WaterLocation> it = chunk.updateQueue.iterator();
        while(it.hasNext()){
            byte[] b = toByteArray(it.next().hashCode());
            list.add(b[0]);
            list.add(b[1]);
            list.add(b[2]);
            list.add(b[3]);
            setLocation+=4;
            blocksLocation+=4;
        }

        for(BlockLocation l : chunk.updateSet){
            byte[] b = toByteArray(l.hashCode());
            list.add(b[0]);
            list.add(b[1]);
            list.add(b[2]);
            list.add(b[3]);
            blocksLocation+=4;
        }

        for(int y =0; y < WaterChunk.yBlocks; y++){
            for(int x = 0; x < 16; x++){
                for(int z =0; z< 16; z++){
                    Permeability p = chunk.permeabilities[x][y][z];
                    if(p != null){
                        list.add((byte) ((p.getSerializedValue() << 4) + salinity));
                    } else {
                        list.add((byte) salinity);
                    }
                    list.add(obstaclesByte(chunk, x,y,z));
                    list.add(waterByte(chunk, x,y,z,0));
                    list.add(waterByte(chunk, x,y,z,1));
                    list.add(waterByte(chunk, x,y,z,2));
                    list.add(waterByte(chunk, x,y,z,3));
                }
            }
        }

        byte[] array = new byte[list.size() + 8];
        byte[] set = toByteArray(setLocation);
        array[0] = set[0];
        array[1] = set[1];
        array[2] = set[2];
        array[3] = set[3];
        byte[] blocks = toByteArray(blocksLocation);
        array[4] = blocks[0];
        array[5] = blocks[1];
        array[6] = blocks[2];
        array[7] = blocks[3];
        for(int i = 8; i < array.length; i++){
            array[i] = list.get(i-8);
        }
        /**
         *
         */

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DeflaterOutputStream dos = new DeflaterOutputStream(baos);
        try {
            dos.write(array);
            dos.flush();
        } catch (IOException e2) {
            e2.printStackTrace();
        } finally{
            try {
                dos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        RandomAccessFile f = null;

        int locationIndex = (Math.floorMod(chunk.coords.x,32) << 5) + Math.floorMod(chunk.coords.z,32) << 2;
        int sizeIndex = 4096 + ((Math.floorMod(chunk.coords.x,32) << 5) + Math.floorMod(chunk.coords.z,32) << 2);
        try {
            f = new RandomAccessFile(chunk.getFile(), "rw");
            if(f.length() % sectorLength != 0){
                // If the file was corrupted, erase it.
                f.setLength(0);
            }
            if(f.length() < 8192){
                // If the file doesn't actually have a chunk saved.
                f.seek(locationIndex);
                f.writeInt(2);
                f.seek(locationIndex);
                f.seek(sizeIndex);
                f.writeInt(baos.size());
                f.seek(sizeIndex);
                f.seek(8192);
                int padding = sectorLength - Math.floorMod(baos.size(), sectorLength);
                f.write(baos.toByteArray());
                f.write(new byte[padding]);
                return true;
            }
            f.seek(locationIndex);
            int location = f.readInt();
            int size = 0;
            if(location < 2){
                location = Math.floorDiv((Math.max((int)f.length(), 8192)), sectorLength);
                f.seek(locationIndex);
                f.writeInt(location);
            } else {
                f.seek(sizeIndex);
                size = f.readInt();
            }
            f.seek(sizeIndex);
            f.writeInt(baos.size());
            f.seek(location * sectorLength);
            if(location * sectorLength >= f.length()){
                // If the chunk does not exist.
                f.write(baos.toByteArray());
                f.write(new byte[sectorLength - Math.floorMod(baos.size(), sectorLength)]);
                return true;
            } else {
                // The chunk was already saved before. It is necessary to check if next chunks need to be moved.
                int remainingPadding = sectorLength - Math.floorMod(size, sectorLength);
                int finalPadding = sectorLength - Math.floorMod(baos.size(), sectorLength);
                int newSectors = (baos.size() + finalPadding - (size + remainingPadding)) / sectorLength;
                if(newSectors != 0){
                    // Chunk size changed. Need to move chunks in the file.
                    //LivelyWorld.getInstance().getLogger().info(getFile().getName()+": Rewriting. Initial file size: " + f.length() + ". Expected final size: " + (f.length() + newSectors*sectorLength));
                    int nextChunkIndex = location*sectorLength + size + remainingPadding;
                    byte[] nextChunks = new byte[0];
                    if(f.length() - nextChunkIndex > 0){
                        nextChunks = new byte[(int) (f.length() - nextChunkIndex)];
                    }
                    f.seek(nextChunkIndex);
                    f.readFully(nextChunks);
                    f.seek(location * sectorLength);
                    //LivelyWorld.getInstance().getLogger().info(getFile().getName()+": Rewriting at location: " + location + " (" + location*sectorLength + ") with a size of " + baos.size() + " with padding: " + finalPadding + " and " + newSectors + " sectors" + ". Moving " + nextChunks.length + " bytes.");
                    // Write Chunk Data
                    f.setLength(location * sectorLength);
                    f.write(baos.toByteArray());
                    f.write(new byte[finalPadding]);
                    // Write moved chunks
                    f.write(nextChunks);
                    //LivelyWorld.getInstance().getLogger().info(getFile().getName()+": Rewriting. Final file size: " + f.length());
                    // Update chunk locations
                    f.seek(0);
                    while(f.getFilePointer() < 4096){
                        int pos = (int)f.getFilePointer();
                        int loc = f.readInt();
                        if(loc > location){
                            f.seek(pos);
                            f.writeInt(loc + newSectors);
                        }
                    }
                    return true;
                } else {
                    // Just write down the chunk.
                    //LivelyWorld.getInstance().getLogger().info("Rewriting at location: " + location + " (" + location*sectorLength + ") with a size of " + baos.size() + " with padding: " + finalPadding);
                    f.write(baos.toByteArray());
                    f.write(new byte[finalPadding]);
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if(f!=null){
                    f.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static public int floorMod2(final int n, final int power){
        final int d = n >> power;
        return n - (d << power);
    }

    public static byte[] toByteArray(int value){
        return new byte[]{(byte) (value), (byte)(value >>> 8), (byte)(value >>> 16), (byte)(value >>> 24)};
    }
    public static int toInt(byte b0, byte b1, byte b2, byte b3){
        return (Byte.toUnsignedInt(b0)) + (Byte.toUnsignedInt(b1) << 8) + (Byte.toUnsignedInt(b2) << 16) + (Byte.toUnsignedInt(b3) << 24);
    }

    /**
     * Load the chunk data from its file.
     * @param chunk
     * @return {@code true} if data has been found and loaded, {@code false} otherwise.
     */
    public static boolean loadFromFile(WaterChunk chunk){
        RandomAccessFile f = null;
        try {
            f = new RandomAccessFile(chunk.getFile(), "r");
            if(f.length() < 8192) return false;
            if(f.length() % sectorLength != 0) {
                //LivelyWorld.getInstance().getLogger().warning(getFile().getName()+": Unexpected file size (" + f.length() + "). Chunk won't be loaded.");
                return false;
            }
            // Get the index of where the location of the chunk is stored.
            int locationIndex = (floorMod2(chunk.coords.x,5) << 5) + (floorMod2(chunk.coords.z,5)) << 2;
            // Get the index of where the size of the chunk is stored.
            int sizeIndex = 4096 + ((floorMod2(chunk.coords.x,5) << 5) + floorMod2(chunk.coords.z,5) << 2);

            f.seek(locationIndex);
            int location = f.readInt();
            if(location < 8192 / sectorLength) return false;
            f.seek(sizeIndex);
            int size = f.readInt();

            f.seek(location * sectorLength);

            //LivelyWorld.getInstance().getLogger().info("Reading from location: " + location + " (" + location*sectorLength + ") with a size of " + size);

            ByteArrayOutputStream baos;
            InflaterOutputStream ios = null;

            try{
                baos = new ByteArrayOutputStream();
                ios = new InflaterOutputStream(baos);

                byte[] buf = new byte[4096];
                int rlen = -1;
                int total = 0;
                while((rlen = f.read(buf)) >= 0 && total < size){
                    ios.write(buf, 0, rlen);
                    total += rlen;
                }

                ios.flush();
                if(baos.size() > 8){
                    byte[] array = baos.toByteArray();
                    int setLocation = toInt(array[0], array[1], array[2], array[3]);
                    int blocksLocation = toInt(array[4], array[5], array[6], array[7]);

                    int i = 8;
                    while(i < setLocation && i < array.length){
                        chunk.updateQueue.add(WaterLocation.parse(toInt(array[i], array[i+1], array[i+2], array[i+3])));
                        i+=4;
                    }
                    i = setLocation;
                    while(i< blocksLocation && i < array.length){
                         chunk.updateSet.add(BlockLocation.parse(toInt(array[i], array[i+1], array[i+2], array[i+3])));
                         i+=4;
                    }
                    i = blocksLocation;
                    int y = 0;
                    int x = 0;
                    int z = 0;
                    while(i < array.length){

                        chunk.permeabilities[x][y][z] = Permeability.parse(array[i] >>> 4);
                        // Salinity remains TODO.
                        setObstaclesByte(chunk, x,y,z, array[i+1]);
                        setWaterByte(chunk, x,y,z, 0, array[i+2]);
                        setWaterByte(chunk, x,y,z, 1, array[i+3]);
                        setWaterByte(chunk, x,y,z, 2, array[i+4]);
                        setWaterByte(chunk, x,y,z, 3, array[i+5]);

                        i+= 6;
                        z++;
                        if(z >= 16){
                            z = 0;
                            x++;
                        }
                        if(x >= 16){
                            x = 0;
                            y++;
                        }
                        if(y >= WaterChunk.yBlocks){
                            break;
                        }
                    }
                } else {
                    return false;
                }
            } finally {
                if(ios != null){
                    ios.close();
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (f != null) {
                    f.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    private static byte obstaclesByte(WaterChunk chunk, int blockX, int blockY, int blockZ){
        int sY = blockY << 1;
        int mY = sY + 2;

        int sX = blockX << 1;
        int mX = sX + 2;

        int sZ = blockZ << 1;
        int mZ = sZ + 2;

        byte b = 0;
        int shift = 7;
        for(int y = sY; y < mY; y++){
            for(int x = sX; x < mX; x++){
                for(int z = sZ; z < mZ; z++){
                    b = (byte) (b + ((chunk.obstacles[x][y][z] ? 1 : 0) << shift--));
                }
            }
        }
        return b;
    }

    private static void setObstaclesByte(WaterChunk chunk, int blockX, int blockY, int blockZ, byte values){
        int sY = blockY << 1;
        int mY = sY + 2;

        int sX = blockX << 1;
        int mX = sX + 2;

        int sZ = blockZ << 1;
        int mZ = sZ + 2;

        int shift = 7;
        for(int y = sY; y < mY; y++){
            for(int x = sX; x < mX; x++){
                for(int z = sZ; z < mZ; z++){
                    chunk.obstacles[x][y][z] = ((values >>> shift--) & 1) == 1;
                }
            }
        }
    }

    private static byte waterByte(WaterChunk chunk, int blockX, int blockY, int blockZ, int level){
        int sY = (blockY << 3) + (level << 1);
        int mY = sY + 2;

        int sX = blockX << 1;
        int mX = sX + 2;

        int sZ = blockZ << 1;
        int mZ = sZ + 2;

        byte b = 0;
        int shift = 7;
        for(int y = sY; y < mY; y++){
            for(int x = sX; x < mX; x++){
                for(int z = sZ; z < mZ; z++){
                    b = (byte) (b + ((chunk.hasWater(x,y,z) ? 1 : 0) << shift--));
                }
            }
        }
        return b;
    }

    private static void setWaterByte(WaterChunk chunk, int blockX, int blockY, int blockZ, int level, byte values){
        int sY = (blockY << 3) + (level << 1);
        int mY = sY + 2;

        int sX = blockX << 1;
        int mX = sX + 2;

        int sZ = blockZ << 1;
        int mZ = sZ + 2;

        byte b = 0;
        int shift = 7;
        for(int y = sY; y < mY; y++){
            for(int x = sX; x < mX; x++){
                for(int z = sZ; z < mZ; z++){
                    chunk.water[x][y][z] = ((values >>> shift--) & 1) == 1;
                }
            }
        }
    }
}
