package voxel.landscape.map;

import voxel.landscape.BlockType;
import voxel.landscape.Chunk;
import voxel.landscape.Coord3;
import voxel.landscape.collection.List3D;
import voxel.landscape.collection.SunLightMap;
import voxel.landscape.map.light.LightComputer;
import voxel.landscape.map.light.LightMap;
import voxel.landscape.map.light.SunLightComputer;
import voxel.landscape.noise.IBlockDataProvider;
import voxel.landscape.noise.TerrainDataProvider;

public class TerrainMap implements IBlockDataProvider
{
	private static final int MIN_DIM = 0;
	private static final int MAX_DIM = 4; // limited dimension world for now.
	public static Coord3 MIN_CHUNK_COORD = new Coord3(MIN_DIM);
	public static Coord3 MAX_CHUNK_COORD = new Coord3(MAX_DIM);
	List3D<Chunk> chunks = new List3D<Chunk> (MIN_CHUNK_COORD, MAX_CHUNK_COORD, Chunk.class );
	
	TerrainDataProvider terrainDataProvider = new TerrainDataProvider();
	
	private SunLightMap sunLightmap = new SunLightMap();
	private LightMap lightmap = new LightMap();
	
//	public Chunk chunkAt(Coord3 co) { return chunkAt(co.x, co.y, co.z); }
//	
//	public Chunk chunkAt(int x, int y, int z) 
//	{
//		if (!chunks.IndexWithinBounds(x, y, z)) return null;
//		
//		Chunk res = chunks.SafeGet(x, y, z);
//		if (res == null) {
//			res = new Chunk( new Coord3 (x,y,z), this);
//			chunks.Set(res, x, y, z); 
//		}
//		return res;
//	}
	
	public static int GetWorldHeightInChunks() { return MAX_CHUNK_COORD.minus(MIN_CHUNK_COORD).y; }
	
	public static int GetWorldHeightInBlocks() { return GetWorldHeightInChunks() * Chunk.CHUNKDIMS.y; }
	
	public byte blockAtWorldCoord(Coord3 co) { return blockAtWorldCoord(co.x, co.y, co.z); }
	
	public byte blockAtWorldCoord(int x, int y, int z) {
		Chunk chunk = GetChunk(Chunk.ToChunkPosition(x, y, z));
		if (chunk == null) return (byte) BlockType.NON_EXISTENT.ordinal();
		return chunk.blockAt(Chunk.toChunkLocalCoord(x, y, z));
	}
	
	public boolean blockAtWorldCoordIsTranslucent(Coord3 co) { return blockAtWorldCoordIsTranslucent(co.x, co.y, co.z); }
	
	public boolean blockAtWorldCoordIsTranslucent(int x, int y, int z) {
		Chunk chunk = GetChunk(Chunk.ToChunkPosition(x, y, z));
		if (chunk == null) return false; 
		return BlockType.isTranslucent(chunk.blockAt(Chunk.toChunkLocalCoord(x, y, z)));
	}
	
	public void setBlockAtWorldCoord(byte block, Coord3 pos) {
		setBlockAtWorldCoord(block, pos.x, pos.y, pos.z);
	}
	public void setBlockAtWorldCoord(byte block, int x, int y, int z) {
		Chunk chunk = GetChunkInstance (Chunk.ToChunkPosition(x, y, z));
		if (chunk != null) {
			chunk.setBlockAt(block, Chunk.toChunkLocalCoord(x, y, z));
		}
	}
	
	public Coord3 getMinChunkCoord() { return MIN_CHUNK_COORD.copy(); }
	public Coord3 getMaxChunkCoord() { return MAX_CHUNK_COORD.copy(); }
	
	@Override
	public int blockDataAtPosition(Coord3 woco) {
		return blockDataAtPosition(woco.x, woco.y, woco.z);
	}
	@Override
	public int blockDataAtPosition(int xin, int yin, int zin) {
		byte block = blockAtWorldCoord(xin, yin, zin);
		if (BlockType.NON_EXISTENT.equals((int) block) && chunks.IndexWithinBounds( Chunk.ToChunkPosition(xin, yin, zin) )) {
			block = (byte) terrainDataProvider.getBlockDataAtPosition(xin, yin, zin);
			setBlockAtWorldCoord(block, xin, yin, zin);	
		}
		return block;
	}
	
	/*
	 * Mr. Wishmaster methods
	 */
	
	public void SetBlockAndRecompute(byte block, Coord3 pos) {
		setBlockAtWorldCoord( block, pos );
		
		Coord3 chunkPos = Chunk.ToChunkPosition(pos);
		Coord3 localPos = Chunk.toChunkLocalCoord(pos);
		
		SetDirty( chunkPos );
		
		if(localPos.x == 0) SetDirty( chunkPos.minus(Coord3.right ));
		if(localPos.y == 0) SetDirty( chunkPos.minus(Coord3.up ));
		if(localPos.z == 0) SetDirty( chunkPos.minus(Coord3.forward ));
		
		if(localPos.x == Chunk.CHUNKDIMS.x-1) SetDirty( chunkPos.add(Coord3.right ));
		if(localPos.y == Chunk.CHUNKDIMS.y-1) SetDirty( chunkPos.add(Coord3.up ));
		if(localPos.z == Chunk.CHUNKDIMS.z-1) SetDirty( chunkPos.add(Coord3.forward ));
		
		SunLightComputer.RecomputeLightAtPosition(this, pos);
		LightComputer.RecomputeLightAtPosition(this, pos);
	}
	
	private void SetDirty(Coord3 chunkPos) {
		Chunk chunk = GetChunk( chunkPos );
		// TODO: decide how to update chunk meshes...
		if(chunk != null) chunk.SetDirty();
	}
	

	public int GetMaxY(int x, int z) {
		Coord3 chunkPos = Chunk.ToChunkPosition(x, 0, z);
		chunkPos.y = chunks.GetMax().y;
		Coord3 localPos = Chunk.toChunkLocalCoord(x, 0, z);
		
		for(;chunkPos.y >= 0; chunkPos.y--) {
			localPos.y = Chunk.CHUNKDIMS.y-1;
			for(;localPos.y >= 0; localPos.y--) {
				Chunk chunk = chunks.SafeGet(chunkPos);
				if(chunk == null) break;
				byte block = chunk.blockAt(localPos);
				if(!BlockType.IsEmpty(block)) return Chunk.ToWorldPosition(chunkPos, localPos).y;
			}
		}
		
		return 0;
	}
	
	public Chunk GetChunkInstance(int x, int y, int z) {
		return GetChunkInstance(new Coord3(x,y,z));
	}
	
	public Chunk GetChunkInstance(Coord3 chunkPos) {
		if (!chunks.IndexWithinBounds(chunkPos)) return null; //Limited world right now...
		if(chunkPos.y < 0) return null;
		Chunk chunk = GetChunk(chunkPos);
		if(chunk == null) {
			chunk = new Chunk( chunkPos, this);
			chunks.AddOrReplace(chunk, chunkPos);
		}
		return chunk;
	}
	public Chunk GetChunk(Coord3 chunkPos) {
		return chunks.SafeGet(chunkPos);
	}
	
	public List3D<Chunk> GetChunks() {
		return chunks;
	}
	
	public SunLightMap GetSunLightmap() {
		return sunLightmap;
	}
	
	public LightMap GetLightmap() {
		return lightmap;
	}

	
}
