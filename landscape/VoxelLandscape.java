package voxel.landscape;

import java.util.*;
import java.awt.List;
import java.util.ArrayList;

import voxel.landscape.map.TerrainMap;
import static java.lang.System.out;

import jme3test.helloworld.HelloJME3;

import com.jme3.app.SimpleApplication;
import com.jme3.app.Application;
//import com.jme3.bounding.BoundingVolume.Type.Position;
//import com.jme3.bounding.BoundingVolume.Type;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.shape.Box;
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;
import com.jme3.math.ColorRGBA;

// TODO: Separate world builder and game logic, etc., everything else...
public class VoxelLandscape extends SimpleApplication
{
//	private static final int WORLD_DIMS_CHUNKS = 1;
//	private Chunk[][] chunks = new Chunk[WORLD_DIMS_CHUNKS][WORLD_DIMS_CHUNKS];
	
	private static boolean UseTextureMap = true;
	
	private TerrainMap terrainMap = new TerrainMap();
	
	private void attachMeshToScene(Chunk testChunk)
	{
		Geometry geo = testChunk.getGeometryObject(); // new Geometry("OurMesh", mesh); // using our custom mesh object
		this.addGeometryToScene(geo);
	}
	
	private void addGeometryToScene(Geometry geo)
	{
		Material mat; 
		if (UseTextureMap)
		{
			mat = new Material(assetManager, "MatDefs/BlockTex2.j3md");
			
			Texture blockTex = assetManager.loadTexture("Textures/dog_64d.jpg");
			
			blockTex.setMagFilter(Texture.MagFilter.Nearest);
			blockTex.setWrap(Texture.WrapMode.Repeat);
			
	    	mat.setTexture("ColorMap", blockTex);
		} else {
			mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
			mat.setBoolean("VertexColor", true);
		}
    	
    	geo.setMaterial(mat);
    	rootNode.attachChild(geo);
	}

	
	private void loadChunks()
	{
		Coord3 minChCo = terrainMap.getMinChunkCoord();
		Coord3 maxChCo = terrainMap.getMaxChunkCoord();

		for(int i = minChCo.x; i < maxChCo.x; ++i)
		{
			for(int j = minChCo.z; j < maxChCo.z; ++j)
			{
				for (int k = minChCo.y; k < maxChCo.y; ++k )
				{
					System.out.println("make chunk starting");
					Chunk ch = terrainMap.GetChunkInstance(i, k, j);
					attachMeshToScene(ch);
				}
			}
		}
	}
	
	/* Use the main event loop to trigger repeating actions. */
    @Override
    public void simpleUpdate(float tpf) 
    {
        // make the player rotate:

//        testGeom.rotate(0, 0, 2f*tpf);
//        testGeom.rotate(4f*tpf, 0, 0);
//        testGeom.move(2f * tpf, 0f, 0f);
    }
    
    /*
     * Everything start here
     */
    @Override
    public void simpleInitApp() 
    {
    	flyCam.setMoveSpeed(5);
    	loadChunks();
//    	flyCam.setLocation(new Vector3f(24,288,24));
//    	Camera cam = new Camera();
    	cam.setLocation(new Vector3f(20,50,-20));
    	cam.lookAt(new Vector3f(0, 30, 0), Vector3f.UNIT_Y);
    }
    
	/*
	 * OK. Really everything start here... 
	 */
    public static void main(String[] args)
    {
        VoxelLandscape app = new VoxelLandscape();
        app.start(); // start the game
    }
}
