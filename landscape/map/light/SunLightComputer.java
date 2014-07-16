package voxel.landscape.map.light;

import java.util.ArrayList;
import java.util.List;

import voxel.landscape.BlockType;
import voxel.landscape.Chunk;
import voxel.landscape.Coord3;
import voxel.landscape.Direction;
import voxel.landscape.collection.ColumnMap;
import voxel.landscape.collection.SunLightMap;
import voxel.landscape.map.TerrainMap;

public class SunLightComputer {
	
	public final static byte MIN_LIGHT = 1;
	public final static byte MAX_LIGHT = 15;
	public final static byte STEP_LIGHT = 1;
	
	private static ArrayList<Coord3> list = new ArrayList<Coord3>();

	public static void ComputeRayAtPosition(TerrainMap map, int x, int z) {
		int maxY = map.GetMaxY( x, z );
		map.GetSunLightmap().SetSunHeight(maxY+1, x, z);
	}
	
	private static void Scatter(TerrainMap map, ArrayList<Coord3> list) { 
		SunLightMap lightmap = map.GetSunLightmap();
        for(int i=0; i<list.size(); i++) {
            Coord3 pos = list.get(i);
			if(pos.y<0) continue;
			
			byte block = map.blockAtWorldCoord(pos);
			int light = lightmap.GetLight(pos) - LightComputerUtils.GetLightStep(block);
            if(light <= MIN_LIGHT) continue;
			
            for(Coord3 dir : Direction.DirectionCoord) {
				Coord3 nextPos = pos.add(dir);
				block = map.blockAtWorldCoord(nextPos);
                if( BlockType.isTranslucent(block) && lightmap.SetMaxLight((byte)light, nextPos) ) {
                	list.add( nextPos );
                }
				if(!BlockType.IsEmpty(block)) LightComputerUtils.SetLightDirty(map, nextPos);
            }
        }
    }
	
	
	public static void RecomputeLightAtPosition(TerrainMap map, Coord3 pos) {
		SunLightMap lightmap = map.GetSunLightmap();
		int oldSunHeight = lightmap.GetSunHeight(pos.x, pos.z);
		ComputeRayAtPosition(map, pos.x, pos.z);
		int newSunHeight = lightmap.GetSunHeight(pos.x, pos.z);
		
		if(newSunHeight < oldSunHeight) { 
			list.clear();
            for (int ty = newSunHeight; ty <= oldSunHeight; ty++) {
				pos.y = ty;
                lightmap.SetLight(MIN_LIGHT, pos);
                list.add( pos );
            }
            Scatter(map, list);
		}
		if(newSunHeight > oldSunHeight) { 
			list.clear();
            for (int ty = oldSunHeight; ty <= newSunHeight; ty++) {
				pos.y = ty;
				list.add( pos );
            }
            RemoveLight(map, list);
		}
		
		if(newSunHeight == oldSunHeight) {
			if(BlockType.isTranslucent(map.blockAtWorldCoord(pos) ) ) {
				UpdateLight(map, pos);
			} else {
				RemoveLight(map, pos);
			}
		}
	}
	
	
	private static void UpdateLight(TerrainMap map, Coord3 pos) {
        list.clear();
		for(Coord3 dir : Direction.DirectionCoord) {
			list.add( pos.add(dir) );
		}
        Scatter(map, list);
	}
    
	private static void RemoveLight(TerrainMap map, Coord3 pos) {
        list.clear();
		list.add(pos);
        RemoveLight(map, list);
    }
	
	private static void RemoveLight(TerrainMap map, ArrayList<Coord3> list) {
		SunLightMap lightmap = map.GetSunLightmap();
		for(Coord3 pos : list) {
			lightmap.SetLight(MAX_LIGHT, pos);
		}
		
		ArrayList<Coord3> lightPoints = new ArrayList<Coord3>();
		for(int i=0; i<list.size(); i++) {
            Coord3 pos = list.get(i);
			if(pos.y<0) continue;
			if(lightmap.IsSunLight(pos.x, pos.y, pos.z)) {
				lightPoints.add( pos );
				continue;
			}
			byte light = (byte) (lightmap.GetLight(pos) - STEP_LIGHT);
			lightmap.SetLight(MIN_LIGHT, pos);
            if (light <= MIN_LIGHT) continue;
			
            
			for(Coord3 dir : Direction.DirectionCoord) {
				Coord3 nextPos = pos.add(dir);
				byte block = map.blockAtWorldCoord(nextPos);
				if(BlockType.isTranslucent(block)) {
					if(lightmap.GetLight(nextPos) <= light) {
						list.add( nextPos );
					} else {
						lightPoints.add( nextPos );
					}
				}
				if(!BlockType.IsEmpty(block)) LightComputerUtils.SetLightDirty(map, nextPos);
			}	
		}
		
        Scatter(map, lightPoints);
    }
	
	public static class ChunkSunLightComputer {
		
		private final static byte MIN_LIGHT = 1;
		private final static byte MAX_LIGHT = 15;
		private final static byte STEP_LIGHT = 1;
		
		private static ArrayList<Coord3> list = new ArrayList<Coord3>();
		
		public static void ComputeRays(TerrainMap map, int cx, int cz) {
			int x1 = cx*Chunk.CHUNKDIMS.x - 1; // SIZE_X-1;
			int z1 = cz*Chunk.CHUNKDIMS.z - 1; // SIZE_Z-1;
			
			int x2 = x1+Chunk.CHUNKDIMS.x + 2; //.SIZE_X+2;
			int z2 = z1+Chunk.CHUNKDIMS.z + 2; // SIZE_Z+2;
			
			for(int z=z1; z<z2; z++) {
				for(int x=x1; x<x2; x++) {
					SunLightComputer.ComputeRayAtPosition(map, x, z);
				}
			}
		}
		
		public static void Scatter(TerrainMap map, ColumnMap columnMap, int cx, int cz) {
			int x1 = cx*Chunk.CHUNKDIMS.x - 1; // SIZE_X-1;
			int z1 = cz*Chunk.CHUNKDIMS.z - 1; // SIZE_Z-1;
			
			int x2 = x1+Chunk.CHUNKDIMS.x + 2; //.SIZE_X+2;
			int z2 = z1+Chunk.CHUNKDIMS.z + 2; // SIZE_Z+2;
			
			SunLightMap lightmap = map.GetSunLightmap();
			list.clear();
			for(int x=x1; x<x2; x++) {
				for(int z=z1; z<z2; z++) {
					int maxY = ComputeMaxY(lightmap, x, z)+1;
					for(int y=0; y<maxY; y++) {
						if(lightmap.GetLight(x, y, z) > MIN_LIGHT) {
							list.add( new Coord3(x, y, z) );
						}
					}
				}
			}
			Scatter(map, columnMap, list);
		}
		
		private static void Scatter(TerrainMap map, ColumnMap columnMap, List<Coord3> list) { 
			SunLightMap lightmap = map.GetSunLightmap();
			for(int i=0; i<list.size(); i++) {
				Coord3 pos = list.get(i);
				if(pos.y<0) continue;
				
				byte block = map.blockAtWorldCoord(pos);
				int light = lightmap.GetLight(pos) - LightComputerUtils.GetLightStep(block);
				if(light <= MIN_LIGHT) continue;
				
				Coord3 chunkPos = Chunk.ToChunkPosition(pos);
				if(!columnMap.IsBuilt(chunkPos.x, chunkPos.z)) continue;
				
				for (Coord3 dir : Direction.DirectionCoord ) {
					Coord3 nextPos = pos.add(dir);
					block = map.blockAtWorldCoord(nextPos);
					if( BlockType.isTranslucent(block) && lightmap.SetMaxLight((byte)light, nextPos) ) {
						list.add( nextPos );
					}
					if( !BlockType.IsEmpty(block)) LightComputerUtils.SetLightDirty(map, nextPos);
				}
			}
		}
		
		private static int ComputeMaxY(SunLightMap lightmap, int x, int z) {
			int maxY = lightmap.GetSunHeight(x, z);
			maxY = Math.max(maxY, lightmap.GetSunHeight(x-1, z  ));
			maxY = Math.max(maxY, lightmap.GetSunHeight(x+1, z  ));
			maxY = Math.max(maxY, lightmap.GetSunHeight(x,   z-1));
			maxY = Math.max(maxY, lightmap.GetSunHeight(x,   z+1));
			return maxY;
		}
		
	}
}

