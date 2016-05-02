package core;

import ajb.core.Object3D;

public class Block extends Object3D {
	public boolean isMine=false;
	public boolean flagged=false;
	public boolean shown=false;
	
	public static int flagDisplayList;
	public static int mineDisplayList;
	public static int blockDisplayList;
	
	public static int brickTexture;
	
	public Block(){
	}
	
	
}
