package voxel.landscape;

public enum BlockType {
	NON_EXISTENT, AIR, DIRT, GRASS, SAND, STONE, CAVESTONE, BEDROCK;
	
	public boolean equals(int i) {
		return this.ordinal() == i;
	}
	
	public static boolean isTranslucent(int i) {
		return i == BlockType.AIR.ordinal() || i == BlockType.NON_EXISTENT.ordinal();
	}
	public float getFloat() { return (float) this.ordinal(); }
	
	public static boolean IsEmpty(int i) {
		return NON_EXISTENT.ordinal() == i;
	}
	
	public static int LightLevelForType(int type) {
		return 0;
	}
}