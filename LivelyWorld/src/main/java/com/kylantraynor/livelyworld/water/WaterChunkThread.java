package com.kylantraynor.livelyworld.water;

public class WaterChunkThread extends Thread {
	
	private String name = "WaterChunk Thread";
	
	public void run(){
		try{
			while (!isInterrupted()) {
				unloadChunks();
			    loadChunks();
			    updateChunks();
			}
		} catch (Exception e){
			e.printStackTrace();
			WaterChunk.unloadAll();
		}
	}

	private void updateChunks() {
		
	}

	private void unloadChunks() {
		synchronized(WaterChunk.chunks){
			int i = 0;
			while(i < WaterChunk.chunks.size()){
				WaterChunk c = WaterChunk.chunks.get(i);
				if(c == null){
					i++; continue;
				}
				if(c.isLoaded() && !c.getWorld().isChunkLoaded(c.getX(), c.getZ())){
					c.unload();
					WaterChunk.chunks.remove(i);
					return;
				}
				i++;
			}
		}
	}

	private void loadChunks() {
		synchronized(WaterChunk.chunks){
			int i = 0;
			while(i < WaterChunk.chunks.size()){
				WaterChunk c = WaterChunk.chunks.get(i);
				if(c == null){
					i++; continue;
				}
				if(!c.isLoaded() && c.getWorld().isChunkLoaded(c.getX(), c.getZ())){
					c.load();
					return;
				}
				i++;
			}
		}
	}
	
}
