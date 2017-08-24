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
				WaterChunk c = WaterChunk.chunks.get(i).get();
				if(c == null){
					i++; continue;
				}
				if(c.isUnrequested() && c.isLoaded()){
					c.unload();
					c.setUnrequested(false);
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
				WaterChunk c = WaterChunk.chunks.get(i).get();
				if(c == null){
					i++; continue;
				}
				if(c.isRequested() && !c.isLoaded()){
					c.load();
					c.setRequested(false);
					return;
				}
				i++;
			}
		}
	}
	
}
