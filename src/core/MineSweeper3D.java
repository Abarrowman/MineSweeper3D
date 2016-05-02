package core;

import java.applet.Applet;
import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.MouseInfo;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Vector;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import ajb.core.Camera3D;
import ajb.core.MatrixMath;
import ajb.core.Object2D;
import ajb.core.Object3D;
import ajb.core.Object3DContainer;
import ajb.core.RenderUtils;
import ajb.core.Shader;
import ajb.core.VectorMath;

public class MineSweeper3D extends Applet implements Object3DContainer  {
	/**
	 * Unique Id of MineSweeper3D
	 */
	private static final long serialVersionUID = 1780597610799654237L;

	// where the light is comming from
	private FloatBuffer lightPos;

	// colours
	private FloatBuffer white;
	private FloatBuffer gray;
	private FloatBuffer low;
	private FloatBuffer red;
	private FloatBuffer green;
	private FloatBuffer blue;
	private FloatBuffer yellow;
	private FloatBuffer brick;
	private FloatBuffer black;

	// texture related
	private int splashPageTexture=-1;
	private int losePageTexture=-1;
	private int winPageTexture=-1;
	private BufferedImage brickImage;
	private int[] numTexs;

	// shader
	private Shader shader;

	// children
	private Vector<Object3D> children;

	// window width and height
	private final float width = 800;
	private final float height = 600;

	private final float zNear = 1;
	private final float zFar = 100;

	private float[] mouseWorldCord = null;
	
	//game
	private Vector<Vector<Vector<Block>>> blocks;
	private Vector<Object3D> blockList;
	private Vector<int[]> possibleLocations;
	private int numMines=24;
	private boolean lost;
	private boolean won;
	private boolean playing;
	
	
	private Object3D blockHolder;
	
	private Object3D yCylinder;
	private Object3D xCylinder;
	private Object3D zCylinder;
	
	private Object2D splashScreen;
	private Object2D loseScreen;
	private Object2D winScreen;
	
	
	private int selectionStage=0;
	
	private boolean clicking=false;
	private boolean rightClicking=false;
	private boolean mouse0WasUp=true;
	private boolean mouse1WasUp=true;
	
	//graphics lists
	private int redCylinder;
	private int greenCylinder;
	private int blueCylinder;
	
	//record gif
	private int numFrames=72;
	private BufferedImage[] frames =new BufferedImage[numFrames];
	private int currentFrame=0;	

	//applet stuff
	private Canvas display_parent;
	private Thread thread;
	private boolean running=false;
	
	private Toolkit tk;
	
	private String filePath;
	
	protected void initGL() {
		RenderUtils.createDefaultTexturesForPrimitives=false;
		
		tk = Toolkit.getDefaultToolkit();

		
		// setup colors
		red = BufferUtils.createFloatBuffer(4).put(new float[] { 1, 0, 0, 1 });
		green = BufferUtils.createFloatBuffer(4).put(new float[] { 0, 1, 0, 1 });
		blue = BufferUtils.createFloatBuffer(4).put(new float[] { 0, 0, 1, 1 });
		yellow = BufferUtils.createFloatBuffer(4).put(new float[] { 1, 1, 0, 1 });
		white = BufferUtils.createFloatBuffer(4).put(new float[] { 1, 1, 1, 1 });
		low = BufferUtils.createFloatBuffer(4).put(new float[] { 0.1f, 0.1f, 0.1f, 1 });
		gray = BufferUtils.createFloatBuffer(4).put(new float[] { 0.5f, 0.5f, 0.5f, 1 });
		brick = BufferUtils.createFloatBuffer(4).put(new float[] { 223f / 225f, 127 / 255f, 103 / 255f, 1 });
		black = BufferUtils.createFloatBuffer(4).put(new float[] { 0, 0, 0, 1 });
		

		// flip
		red.flip();
		green.flip();
		blue.flip();
		yellow.flip();
		white.flip();
		low.flip();
		gray.flip();
		brick.flip();
		black.flip();
		// setup light position
		lightPos = BufferUtils.createFloatBuffer(4).put(new float[] { 5.0f, 5.0f, 10.0f, 1.0f });
		lightPos.flip();

		// find path
		filePath=getCodeBase().getPath();
		int index = filePath.lastIndexOf("/");
		for(int n=0;n<2;n++){
			if (index != -1) {
				filePath = filePath.substring(0, index);
			}
			index = filePath.lastIndexOf("/");
		}
		
		//load a texture with the png loader
		try {
			MediaTracker tracker=new MediaTracker(this);
			int imageCount=0;
			
			URL baseDir = new URL(getCodeBase(), "resources/");
			
			Image unbufferedBrick=getImage(baseDir, "brick.png");
			tracker.addImage(unbufferedBrick, imageCount);
			imageCount++;
			
			Image unbufferedSplashPage=getImage(baseDir, "splashScreen.png");
			tracker.addImage(unbufferedSplashPage, imageCount);
			imageCount++;
			
			Image unbufferedLosePage=getImage(baseDir, "loseScreen.png");
			tracker.addImage(unbufferedLosePage, imageCount);
			imageCount++;
			
			Image unbufferedWinPage=getImage(baseDir, "winScreen.png");
			tracker.addImage(unbufferedWinPage, imageCount);
			imageCount++;
			
			tracker.waitForAll();
			
			brickImage=RenderUtils.makeBufferedImage(unbufferedBrick);
			Block.brickTexture=RenderUtils.createTextureFromImage(brickImage);
			
			BufferedImage splashPageImage=RenderUtils.makeBufferedImage(unbufferedSplashPage);
			splashPageTexture=RenderUtils.createTextureFromImage(splashPageImage);
			
			BufferedImage losePageImage=RenderUtils.makeBufferedImage(unbufferedLosePage);
			losePageTexture=RenderUtils.createTextureFromImage(losePageImage);
			
			BufferedImage winPageImage=RenderUtils.makeBufferedImage(unbufferedWinPage);
		    winPageTexture=RenderUtils.createTextureFromImage(winPageImage);
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		//create number textures
		numTexs=new int[27];
		Font font=new Font("Impact",Font.BOLD,30);
		for(int n=0;n<27;n++){
			BufferedImage img=new BufferedImage(128,128,BufferedImage.TYPE_INT_ARGB);
			Graphics2D graph=img.createGraphics();
			graph.setFont(font);
			
			graph.setBackground(Color.WHITE);
			graph.clearRect(0, 0, 128, 128);
			
			
			int col;
			if(n==0||n==26){
				col=0;				
			}else{
				float en=n-1;
				float h=0.67f*(1f-en%5/4f);
				float b;
				if(en<5){
					b=1f;
				}else if(en<10){
					b=0.8f;
				}else if(en<15){
					b=0.6f;
				}else if(en<20){
					b=0.4f;
				}else{
					b=0.2f;
				}
				col=Color.HSBtoRGB(h, 1, b);
			}
			
			graph.setColor(new Color(col));
			graph.drawImage(img, 0, 0, null);
			FontRenderContext fontRenderContext=graph.getFontRenderContext();
			String text=Integer.toString(n);
			
			LineMetrics lineMetrics= font.getLineMetrics(text, fontRenderContext);
			Rectangle2D fontBounds=font.getStringBounds(text, fontRenderContext);
				
			int lineY=(int)(20.5+lineMetrics.getAscent()/2-lineMetrics.getDescent()/2);
			for(int x=0;x<3;x++){
				
				for(int y=0;y<2;y++){
					//transform
					float ang=90;
					float scal=1;
					switch(3*y+x){
						case 0:
							
							break;
						case 1:
							scal=-1;
							break;
						case 2:
							
							break;
						case 3:
							
							break;
						case 4:
							
							break;
						case 5:
							scal=-1;
							ang=270;
							break;
					}
					graph.translate(41*x+20.5, 41*y+21);
					graph.rotate(ang/180*Math.PI);
					graph.scale(scal, 1);
					//render
					graph.drawChars(text.toCharArray(), 0, text.length(), (int)(-fontBounds.getWidth()/2),lineY-21);
					graph.fillRect(-15, 2+lineY-21, 30, 2);
					//graph.drawOval(-2, -2, 4, 4);
					//transform
					graph.scale(1/scal,1);
					graph.rotate(-ang/180*Math.PI);
					graph.translate(-41*x-20.5, -41*y-21);
					
					
				}
			}
			numTexs[n]=RenderUtils.createTextureFromImage(img);
		}

		// load shader
		/*URL fragPath;
		URL vertPath;
		try {
			fragPath = new File(getResourceURI("/pointlight.frag").getPath()).toURI().toURL();
			vertPath = new File(getResourceURI("/pointlight.vert").getPath()).toURI().toURL();
			shader = new Shader(vertPath, fragPath);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}*/

		// setup world
		// lighting
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_LIGHT0);
		
		// other
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);
		
		 //GL11.glPolygonMode(GL11.GL_FRONT, GL11.GL_LINE);
		 //GL11.glPolygonMode(GL11.GL_BACK, GL11.GL_LINE);
		
		
		
		
		// setup objects
		children = new Vector<Object3D>();

		
		//display lists
		
		//block list
		Block.blockDisplayList= GL11.glGenLists(1);
		GL11.glNewList(Block.blockDisplayList, GL11.GL_COMPILE);
		// pre render
		//ARBShaderObjects.glUseProgramObjectARB(shader.getShader());
		GL11.glShadeModel(GL11.GL_SMOOTH);
		GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE, white);
		GL11.glMaterialf(GL11.GL_FRONT_AND_BACK, GL11.GL_SHININESS, 100);
		GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_SPECULAR, white);
		// render
		float ratio=41f/128f;
		float[][][] textureCords=new float[][][]{
			//2 right
			new float[][] { new float[] { ratio, 0 }, new float[] { 2f*ratio, 0 }, new float[] { 2f*ratio, ratio }, new float[] { ratio, ratio } },
			//5 right
			new float[][] { new float[] { ratio, ratio }, new float[] { 2f*ratio, ratio }, new float[] { 2f*ratio, 2f*ratio }, new float[] { ratio, 2f*ratio } },
			//4 right
			new float[][] { new float[] { 0, ratio }, new float[] { ratio, ratio }, new float[] { ratio, 2f*ratio }, new float[] { 0, 2f*ratio } },
			//3 right
			new float[][] { new float[] { 2f*ratio, ratio }, new float[] { 3f*ratio, ratio }, new float[] { 3f*ratio, 0 }, new float[] { 2f*ratio, 0 } },
			//1 right
			new float[][] { new float[] { 0, 0 }, new float[] { ratio, 0 }, new float[] { ratio, ratio }, new float[] { 0, ratio } },
			//6 right
			new float[][] { new float[] { 2f*ratio, ratio }, new float[] { 3f*ratio, ratio }, new float[] { 3f*ratio, 2f*ratio }, new float[] { 2f*ratio, 2f*ratio } }
		};
		RenderUtils.createCube(0.4f, textureCords);
		// post render
		//ARBShaderObjects.glUseProgramObjectARB(0);
		GL11.glEndList();
		
		
		//mine list
		Block.mineDisplayList=GL11.glGenLists(1);
		GL11.glNewList(Block.mineDisplayList, GL11.GL_COMPILE);
		// render
		GL11.glShadeModel(GL11.GL_SMOOTH);
		GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE, white);
		GL11.glMaterialf(GL11.GL_FRONT_AND_BACK, GL11.GL_SHININESS, 100);
		GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_SPECULAR, white);
		RenderUtils.createUVSphere(0.175f, 8, null);
		for(int xn=0;xn<4;xn++){
			for(int yn=0;yn<3;yn++){
				float xAngle=(float)(((float)xn)*90);
				float yAngle=(float)(((float)yn)*90);
				GL11.glPushMatrix();
				GL11.glRotatef(yAngle, 0, 0, 1);
				GL11.glRotatef(xAngle, 1, 0, 0);
				GL11.glTranslatef(0, 0.2f, 0);
				RenderUtils.createCone(0.4f, 0.1f, 4, null);
				GL11.glPopMatrix();
			}
		}
		GL11.glEndList();
		
		//flag list
		Block.flagDisplayList=GL11.glGenLists(1);
		GL11.glNewList(Block.flagDisplayList, GL11.GL_COMPILE);
		GL11.glShadeModel(GL11.GL_SMOOTH);
		GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE, red);
		GL11.glMaterialf(GL11.GL_FRONT_AND_BACK, GL11.GL_SHININESS, 100);
		GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_SPECULAR, white);
		float[][] flagCords=new float[][]{
				new float[]{0,0},
				new float[]{0,0.3f},
				new float[]{-0.2f,0.15f}};
		GL11.glPushMatrix();
		GL11.glRotatef(90, 1, 0, 0);
		GL11.glTranslatef(0, 0, -0.20f);
		RenderUtils.extrudePolygon(flagCords, 0.05f, null);
		GL11.glPopMatrix();
		GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE, low);
		RenderUtils.createCylinder(0.4f, 0.05f, 8, null);
		GL11.glEndList();
		
		
		
		redCylinder=GL11.glGenLists(1);
		GL11.glNewList(redCylinder, GL11.GL_COMPILE);
		// render
		GL11.glShadeModel(GL11.GL_SMOOTH);
		GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE, red);
		GL11.glMaterialf(GL11.GL_FRONT_AND_BACK, GL11.GL_SHININESS, 100);
		GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_SPECULAR, white);
		RenderUtils.createCylinder(5, 0.1f, 12, null);
		GL11.glEndList();
		
		greenCylinder=GL11.glGenLists(1);
		GL11.glNewList(greenCylinder, GL11.GL_COMPILE);
		// render
		GL11.glShadeModel(GL11.GL_SMOOTH);
		GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE, green);
		GL11.glMaterialf(GL11.GL_FRONT_AND_BACK, GL11.GL_SHININESS, 100);
		GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_SPECULAR, white);
		RenderUtils.createCylinder(5, 0.1f, 12, null);
		GL11.glEndList();
		
		blueCylinder=GL11.glGenLists(1);
		GL11.glNewList(blueCylinder, GL11.GL_COMPILE);
		// render
		GL11.glShadeModel(GL11.GL_SMOOTH);
		GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE, blue);
		GL11.glMaterialf(GL11.GL_FRONT_AND_BACK, GL11.GL_SHININESS, 100);
		GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_SPECULAR, white);
		RenderUtils.createCylinder(5, 0.1f, 12, null);
		GL11.glEndList();
		
		
		//build hud
		splashScreen=new Object2D(1024,1024,splashPageTexture);
		splashScreen.setMask(new Rectangle(0,0,800,600));
		addChild(splashScreen);
		
		loseScreen=new Object2D(1024,1024,losePageTexture);
		loseScreen.setMask(new Rectangle(0,0,800,600));
		addChild(loseScreen);
		
		winScreen=new Object2D(1024,1024,winPageTexture);
		winScreen.setMask(new Rectangle(0,0,800,600));
		addChild(winScreen);
		
		
		//build 3d objects
		
		blockHolder=new Object3D();
		//blockHolder.z=-9.5f;
		blockHolder.matrix=MatrixMath.translation3D(0, 0, -9.5f);
		addChild(blockHolder);
		
		blocks=new Vector<Vector<Vector<Block>>>();
		blockList=new Vector<Object3D>();
		
		for (int xn = 0; xn < 6; xn++) {
			Vector<Vector<Block>> slice=new Vector<Vector<Block>>();
			blocks.add(slice);
			
			for (int yn = 0; yn < 6; yn++) {
				Vector<Block> subSlice=new Vector<Block>();
				slice.add(subSlice);
				
				for (int zn = 0; zn < 6; zn++) {
					Block block = new Block();
					//block.x = -2.5f + xn;
					//block.z = 2.5f - zn;
					//block.y = -2.5f + yn;
					block.matrix=MatrixMath.translation3D(-2.5f + xn, -2.5f + yn, 2.5f - zn);
					block.displayList=Block.blockDisplayList;
					block.textureId=Block.brickTexture;
					
					//add the block
					blockHolder.addChild(block);
					subSlice.add(block);
					blockList.add(block);
					
					
					
					/*System.out.println("---");
					
					System.out.println("("+xn+","+yn+","+zn+") -> "+
							VectorMath.vectorToString(block.globalToLocal(new float[]{0f,0f,0f})));
					
					block.xRotation=90;
					block.unCacheMatrix();
					
					System.out.println("("+xn+","+yn+","+zn+") -> "+
					VectorMath.vectorToString(block.globalToLocal(new float[]{0f,0f,0f})));*/
				}
			}
		}
		
		yCylinder=new Object3D();
		yCylinder.displayList=redCylinder;
		//yCylinder.z=2.5f;
		//yCylinder.x=-2.5f;
		yCylinder.matrix=MatrixMath.translation3D(-2.5f, 0, 2.5f);
		yCylinder.visible=false;
		blockHolder.addChild(yCylinder);
		
		xCylinder=new Object3D();
		xCylinder.displayList=greenCylinder;
		//xCylinder.z=2.5f;
		//xCylinder.y=-2.5f;
		//xCylinder.zRotation=90;
		xCylinder.matrix=MatrixMath.multiplyMatracies(MatrixMath.translation3D(0, -2.5f, 2.5f), MatrixMath.rotation3D(0, 0, (float)(Math.PI/2d)));
		xCylinder.visible=false;
		blockHolder.addChild(xCylinder);
		
		zCylinder=new Object3D();
		zCylinder.displayList=blueCylinder;
		//zCylinder.z=0f;
		//zCylinder.y=-2.5f;
		//zCylinder.x=-2.5f;
		//zCylinder.xRotation=90;
		zCylinder.matrix=MatrixMath.multiplyMatracies(MatrixMath.translation3D(-2.5f, -2.5f, 0), MatrixMath.rotation3D((float)(Math.PI/2d), 0, 0));
		zCylinder.visible=false;
		blockHolder.addChild(zCylinder);
		
		
		//final variable initialization
		playing=false;
		//set up game
		reset();
		//start game
		loop();
	}
	
	public void startLWJGL() {

		// make a window
		//not used by applet
		/*
		try {
			Display.setDisplayMode(new DisplayMode((int) width, (int) height));
			Display.setTitle("MineSweeper 3D");
			Display.create();
		} catch (LWJGLException e) {
			e.printStackTrace();
			System.exit(0);
		}*/
		//applet version
		thread = new Thread() {
			public void run() {
				running=true;
				try {
					Display.setParent(display_parent);
					Display.create();
				} catch (LWJGLException e) {
					e.printStackTrace();
				}
				initGL();
			}
		};
		thread.start();
	}
	
	private void loop() {
		float spot = 0;
		Camera3D camera = new Camera3D();
		float[] properties;
		while (running) {
			// render
			render(camera);
			// cap at 60 fps
			Display.sync(60);
			// render the graphics
			Display.update();
			

			//update
			//time
			spot += 1;
			
			/*
			if(currentFrame<numFrames){
				IntBuffer frameBuffer = BufferUtils.createIntBuffer(800*600*4);
				GL11.glReadPixels(0, 0, 800, 600, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, frameBuffer);
				BufferedImage frameImage=new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
				
				for( int ix=0;ix<800;ix++){
				      for( int iy=0;iy<600;iy++){
				            frameImage.setRGB(ix,iy,frameBuffer.get((600-iy-1)*800+ix));
				      }
				}			
				
				BufferedImage scalledImage=new BufferedImage(400,300,BufferedImage.TYPE_INT_RGB);
				scalledImage.getGraphics().drawImage(frameImage, 0, 0, 400, 300, null);
				frames[currentFrame]=scalledImage;
				
				//frames[currentFrame]=frameImage;
				
				currentFrame++;
				
				if(currentFrame==numFrames){
					
					//png
					try
					{
					      File out=new File("still.png");
					      ImageIO.write(frames[0],"png",out);
					}
					catch(Exception e)
					{
					      e.printStackTrace();
					}
			
					//gif
					AnimatedGifEncoder e = new AnimatedGifEncoder();
					e.start("animation.gif");
					//e.setSize(800, 600);
					e.setRepeat(0);
					e.setDelay(50);   // 20 frame per sec
					for(int n=0;n<currentFrame;n++){
						e.addFrame(frames[n]);
					}
					e.finish();
				}
			}
			blockHolder.xRotation+=5;
			*/
			
			
			if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
				// exit fullscreen
				try {
					Display.setFullscreen(false);
				} catch (LWJGLException e) {
					e.printStackTrace();
				}
			}
			
			
			//refresh input
			clicking=false;
			rightClicking=false;
			if(Mouse.isButtonDown(0)&&mouse0WasUp){
				clicking=true;
				mouse0WasUp=false;
			}else if(Mouse.isButtonDown(1)&&mouse1WasUp){
				clicking=true;
				mouse1WasUp=false;
				rightClicking=true;
			}
			if(!Mouse.isButtonDown(0)&&!mouse0WasUp){
				mouse0WasUp=true;
			}
			if(!Mouse.isButtonDown(1)&&!mouse1WasUp){
				mouse1WasUp=true;
			}
			
			
			//display
			float oneDegree=(float)(1d/180d*Math.PI);
			if (Keyboard.isKeyDown(Keyboard.KEY_UP)&&playing) {
				blockHolder.applyMatrix(MatrixMath.rotation3D(-oneDegree, 0, 0));
			}else if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)&&playing) {
				blockHolder.applyMatrix(MatrixMath.rotation3D(oneDegree, 0, 0));
			}
			if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)&&playing) {
				blockHolder.applyMatrix(MatrixMath.rotation3D(0, -oneDegree, 0));
			}else if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)&&playing) {
				blockHolder.applyMatrix(MatrixMath.rotation3D(0, oneDegree, 0));
			}
			
			
			//handle input
			if(clicking&&!lost&&!won&&playing){
				if(selectionStage==3){
					//get x y z coords
									
					int x= (int)Math.round(yCylinder.getProperty(0)+2.5f);
					int y= (int)Math.round(zCylinder.getProperty(1)+2.5f);
					int z= (int)Math.round(-(xCylinder.getProperty(2)-2.5f));
					if(rightClicking){
						//flag
						clickedBlock(x, y, z, true, true, false);
					}else{
						//reveal
						clickedBlock(x, y, z, true, false, false);
					}
					//Have you won?
					boolean haveWon=true;
					for(int xn=0;xn<6;xn++){
						for(int yn=0;yn<6;yn++){
							for(int zn=0;zn<6;zn++){
								//warning this is no longer the original block
								Block block=blocks.get(xn).get(yn).get(zn);
								if(!((block.isMine&&block.flagged)||(block.shown&&!block.isMine))){
									haveWon=false;
									xn=6;
									yn=6;
									zn=6;
								}
							}
						}
					}
					if(haveWon){
						setAllBlocksVisible(true);
						winScreen.visible=true;
						won=true;
					}
				}
				//move on to next selection stage
				selectionStage=(selectionStage+1)%4;
				if(selectionStage==1){
					yCylinder.visible=true;
				}else if(selectionStage==2){
					xCylinder.visible=true;
				}else if(selectionStage==3){
					zCylinder.visible=true;
				}else{
					yCylinder.visible=false;
					xCylinder.visible=false;
					zCylinder.visible=false;
				}
			}else if(lost||won||!playing){
				if(Keyboard.isKeyDown(Keyboard.KEY_RETURN)){
					if(!playing){
						playing=true;
						splashScreen.visible=false;
					}else{
						reset();
					}
				}
			}
			
			
			float[] mouseCord2D=new float[]{Mouse.getX(),Mouse.getY()};
			float[] mouseCord3D=screenToWorld(mouseCord2D);
			if(mouseCord3D!=null){
				//System.out.println("3D Mouse Coordinates:\n" + VectorMath.vectorToString(mouseCord3D));
				Object3D object=getClosestObject(mouseCord3D, blockList);
				if(object!=null){
					//object.xRotation=90;
					if(selectionStage==1){
						float obX=object.getProperty(0);
						zCylinder.setProperty(0, obX);
						yCylinder.setProperty(0, obX);
						//zCylinder.x=yCylinder.x=object.x;
					}else if(selectionStage==2){
						float obZ=object.getProperty(2);
						xCylinder.setProperty(2, obZ);
						yCylinder.setProperty(2, obZ);
						//yCylinder.z=xCylinder.z=object.z;
					}else if(selectionStage==3){
						float obY=object.getProperty(1);
						zCylinder.setProperty(1, obY);
						xCylinder.setProperty(1, obY);
						//xCylinder.y=zCylinder.y=object.y;
					}
					
					if(!lost&&!won&&playing){
						if(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)&&
								VectorMath.distance(object.localToGlobal(new float[]{0f,0f,0f}), mouseCord3D)<0.4f){
							setAllBlocksVisible(false);
							for(int xn=-1;xn<2;xn++){
								for(int yn=-1;yn<2;yn++){
									for(int zn=-1;zn<2;zn++){
										properties=object.getProperties();
										int ex=(int)Math.round(properties[0]+xn+2.5f);
										int ey=(int)Math.round(properties[1]+yn+2.5f);
										int ez=(int)Math.round(-(properties[2]+zn-2.5f));
										if(ex>=0&&ex<6&&ey>=0&&ey<6&&ez>=0&&ez<6){
											blocks.get(ex).get(ey).get(ez).visible=true;
										}
									}
								}
							}
						}else{
							setAllBlocksVisible(true);
						}
					}
				}else{
					setAllBlocksVisible(true);
				}
			}else{
				if(!Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)){
						setAllBlocksVisible(true);
				}	
			}
		}

		Display.destroy();
	}
	
	private void setAllBlocksVisible(boolean visible){
		for(int xn=0;xn<6;xn++){
			for(int yn=0;yn<6;yn++){
				for(int zn=0;zn<6;zn++){
					blocks.get(xn).get(yn).get(zn).visible=visible;
				}
			}
		}
	}

	private Object3D getClosestObject(float[] point, Vector<Object3D> list) {
		float smallestDistance=Float.MAX_VALUE;
		Object3D object=null;
		for(int n=0;n<list.size();n++){
			Object3D child=list.get(n);
			//old without nested support
			//float distance=VectorMath.distance(point, new float[]{child.x,child.y,child.z});
			float[] transPoint=child.localToGlobal(new float[]{0,0,0});
			float distance=VectorMath.distance(point, transPoint);
			
			if(distance<smallestDistance){
				object=child;
				smallestDistance=distance;
			}
		}
		//System.out.println(VectorMath.vectorToString(object.globalToLocal(point)));
		
		return object;
	}

	private float[] worldToScreen(float[] world){
		float[][] projectMatrix = getMatrix(GL11.GL_PROJECTION_MATRIX, 4, 4);
		float[][] modelMatrix = getMatrix(GL11.GL_MODELVIEW_MATRIX, 4, 4);
		float[][] sumMatrix=MatrixMath.multiplyMatracies(modelMatrix, projectMatrix);
		float[][] initialCord = new float[][] { new float[] { world[0] }, new float[] { world[1] }, 
				new float[] { world[2] }, new float[] { 1 } };
		float[][] clipCord = MatrixMath.multiplyMatracies(sumMatrix, initialCord);
		float[] normCord = new float[clipCord.length - 1];
		for (int n = 0; n < normCord.length; n++) {
			normCord[n] = clipCord[n][0] / clipCord[clipCord.length - 1][0];
		}
		return new float[] { (normCord[0] + 1) * width / 2f, (normCord[1] + 1) * height / 2f, (normCord[2] + 1) / 2 };
	}
	
	private float[] screenToWorld(float[] screen){
		float mX = screen[0];
		float mY = screen[1];
		//reads the float buffer for the z coordinate at the specified x and y coordinate
		FloatBuffer mouseBuffer = BufferUtils.createFloatBuffer(1);
		GL11.glReadPixels((int) mX, (int) mY, 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, mouseBuffer);
		mouseBuffer.rewind();
		float mZed = mouseBuffer.get();
		float clipZ = mZed * 2f - 1;
		float worldZ = 2f * zFar * zNear / (clipZ * (zFar - zNear) - (zFar + zNear));
		//are we mouse overing an object
		if (worldZ != -zFar) {
			//gets the projection matrix
			float[][] modelMatrix = getMatrix(GL11.GL_MODELVIEW_MATRIX, 4, 4);
			float[][] projectMatrix = getMatrix(GL11.GL_PROJECTION_MATRIX, 4, 4);
						
			float[][] sumMatrix=MatrixMath.multiplyMatracies(modelMatrix, projectMatrix);
			//inverts the projection matrix
			float[][] invertedSumMatrix = MatrixMath.invertMatrix(sumMatrix);
			//System.out.println(invertedProjectionMatrix==null);
			
			
			if(invertedSumMatrix!=null){
				//reverses the homogenisation and the view
				float[][] mouseCord = MatrixMath.multiplyMatrix(MatrixMath.matrixFromColumnVector(new float[] { mX / width * 2f - 1, mY / height * 2f - 1, clipZ, 1 }), -worldZ);
				//applies the inverted projection matrix onto it
				float[] cord4D = VectorMath.vectorFromColumnVectorMatrix(MatrixMath.multiplyMatracies(invertedSumMatrix, mouseCord));
				return new float[]{cord4D[0],cord4D[1],cord4D[2]};
			}else{
				//projection matrix cannot be inverted
				return null;
			}
		}else{
			//ambiguous
			return null;
		}
	}
	
	private float[][] getMatrix(int type, int height, int width) {
		FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(height*width);
		GL11.glGetFloat(type, matrixBuffer);
		matrixBuffer.rewind();
		float[][] matrix = MatrixMath.zeroMatrix(height, width);
		for (int n = 0; n < width * height; n++) {
			matrix[n % height][(n - n % height) / height] = matrixBuffer.get();
		}
		return matrix;
	}

	private float[] getVector(int type, int length) {
		FloatBuffer vectorBuffer = BufferUtils.createFloatBuffer(16);
		GL11.glGetFloat(type, vectorBuffer);
		vectorBuffer.rewind();
		float[] vector = new float[length];
		for (int n = 0; n < length; n++) {
			vector[n] = vectorBuffer.get();
		}
		return vector;
	}
	
	private void clickedBlock(int x, int y, int z, boolean spreading, boolean flagging, boolean revealAll){
		Block block=blocks.get(x).get(y).get(z);
		//System.out.println("("+x+", "+y+", "+z+") : "+block.isMine);
		if(flagging&&!revealAll&&!block.shown){
			//turn on/off
			block.flagged=!block.flagged;
			if(block.flagged){
				block.displayList=Block.flagDisplayList;
				block.textureId=-1;
			}else{
				block.displayList=Block.blockDisplayList;
				block.textureId=Block.brickTexture;
			}
		}else if(!block.shown&&(!block.flagged||revealAll)){
			block.shown=true;
			if(block.isMine){
				block.displayList=Block.mineDisplayList;
				block.textureId=-1;
				//show all
				if(!revealAll){
					setAllBlocksVisible(true);
					lost=true;
					loseScreen.visible=true;
					for(int xn=0;xn<6;xn++){
						for(int yn=0;yn<6;yn++){
							for(int zn=0;zn<6;zn++){
								clickedBlock(xn, yn, zn, false, false, true);
							}
						}
					}
				}
			}else{
				//block.visible=false;
			
				int adjacentMines=0;
				for(int xn=-1;xn<2;xn++){
					for(int yn=-1;yn<2;yn++){
						for(int zn=-1;zn<2;zn++){
							if(zn!=0||xn!=0||yn!=0){
								int ex=x+xn;
								int ey=y+yn;
								int ez=z+zn;
								if(ex>=0&&ex<6&&ey>=0&&ey<6&&ez>=0&&ez<6){
									if(blocks.get(ex).get(ey).get(ez).isMine){
										adjacentMines++;
									}
								}
							}
						}
					}
				}
			
				//show adjacent mines
				block.textureId=numTexs[adjacentMines];
				block.displayList=Block.blockDisplayList;
				
				if(adjacentMines==0&&spreading&&!revealAll){
					for(int xn=-1;xn<2;xn++){
						for(int yn=-1;yn<2;yn++){
							for(int zn=-1;zn<2;zn++){
								if(zn!=0||xn!=0||yn!=0){
									int ex=x+xn;
									int ey=y+yn;
									int ez=z+zn;
									if(ex>=0&&ex<6&&ey>=0&&ey<6&&ez>=0&&ez<6){
										clickedBlock(ex, ey, ez, spreading, false, false);
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private void render(Camera3D camera) {
		// Clear the screen and depth buffer
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		
		//3d rendering
		GL11.glDisable(GL11.GL_BLEND);
		
		// reset projection
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		//GL11.glOrtho(-400, 400, 300, -300, 5, -20);
		GLU.gluPerspective(45, width / height, zNear, zFar);
		// reset model view
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glLoadIdentity();
		camera.createView();
		//turn on light
		GL11.glLight(GL11.GL_LIGHT0, GL11.GL_POSITION, lightPos);
		//render 3D objects
		for (int n = 0; n < children.size(); n++) {
			Object3D object = children.get(n);
			if(!object.isHud){
				object.draw();
			}
		}
		
		//2d rendering
		GL11.glEnable(GL11.GL_BLEND); 
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		
		
		// reset projection
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPushMatrix();
		//set 2d projection
		GL11.glLoadIdentity();
		GL11.glOrtho(-width/2, width/2, -height/2, height/2, -1, 1);
		//reset modelview
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glPushMatrix();
		GL11.glLoadIdentity();
		//render 2D ojects
		for (int n = 0; n < children.size(); n++) {
			Object3D object = children.get(n);
			if(object.isHud){
				object.draw();
			}
		}
		//get back modelview matrix
		GL11.glPopMatrix();
		
		//get back camera matrix
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPopMatrix();
		
		
		if (mouseWorldCord != null) {
			/*
			 * GL11.glPushMatrix(); GL11.glTranslatef(0, 0, -5);
			 * GL11.glBegin(GL11.GL_LINES); GL11.glVertex3f(0, 0, 0);
			 * GL11.glVertex3f
			 * (mouseWorldCord[0]*10,mouseWorldCord[1]*10,mouseWorldCord[2]*10);
			 * 
			 * GL11.glEnd(); GL11.glPopMatrix();
			 */
		}

	}

	private void reset(){
		winScreen.visible=false;
		loseScreen.visible=false;
		lost=false;
		won=false;
		//reset
		possibleLocations=new Vector<int[]>();
		for (int xn = 0; xn < 6; xn++) {
			for (int yn = 0; yn < 6; yn++) {
				for (int zn = 0; zn < 6; zn++) {
					//add possible location
					possibleLocations.add(new int[]{xn,yn,zn});
					//reset cube
					Block block=blocks.get(xn).get(yn).get(zn);
					block.displayList=Block.blockDisplayList;
					block.textureId=Block.brickTexture;
					block.flagged=false;
					block.isMine=false;
					block.shown=false;
				}
			}
		}
		for(int n=0;n<numMines;n++){
			int mineIndex=(int)Math.round(Math.random()*(possibleLocations.size()-1));
			int[] mineLocation=possibleLocations.get(mineIndex);
			blocks.get(mineLocation[0]).get(mineLocation[1]).get(mineLocation[2]).isMine=true;
			possibleLocations.remove(mineIndex);
			//blocks.get(mineLocation[0]).get(mineLocation[1]).get(mineLocation[2]).displayList=Block.mineDisplayList;
			//blocks.get(mineLocation[0]).get(mineLocation[1]).get(mineLocation[2]).textureId=-1;
		}
	}
	//not used by applet
	/*
	public static void main(String[] argv) {
		MineSweeper3D sweeper = new MineSweeper3D();
		sweeper.startLWJGL();
	}
	*/
	
	public void start() {
		
	}

	public void stop() {
		
	}
	
	private void stopLWJGL() {
		running=false;
		try {
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void destroy() {
		remove(display_parent);
		super.destroy();
	}
	
	public void init() {
		resize(800,600);
		setLayout(new BorderLayout());
		try {
			display_parent = new Canvas() {
				private static final long serialVersionUID = 1L;
				public final void addNotify() {
					super.addNotify();
					startLWJGL();
				}
				public final void removeNotify() {
					stopLWJGL();
					super.removeNotify();
				}
			};
			display_parent.setSize(getWidth(),getHeight());
			add(display_parent);
			display_parent.setFocusable(true);
			display_parent.requestFocus();
			display_parent.setIgnoreRepaint(true);
			setVisible(true);
		} catch (Exception e) {
			System.err.println(e);
			throw new RuntimeException("Unable to create display");
		}
	}
	
	public URI getResourceURI(String file) throws MalformedURLException, URISyntaxException{
		/*URI fileURI;
		URL fileURL=getClass().getResource("Abarrow3D"+file);
		if(fileURL==null){
			fileURI=new URI(filePath+file);
		}else{
			fileURI=fileURL.toURI();
		}*/
		return new URI(filePath+file);
	}

	@Override
	public void addChild(Object3D object) {
		if(!hasChild(object)){
			children.add(object);
			object.parent=this;
		}
	}

	@Override
	public void removeChild(Object3D object) {
		if(hasChild(object)){
			children.remove(object);
			object.parent=null;
		}
	}

	@Override
	public boolean hasChild(Object3D object) {
		return children.contains(object);
	}

	@Override
	public void removeAllChildren() {
		for(int n=0;n<children.size();n++){
			removeChild(children.get(n));
		}
	}

	@Override
	public float[][] getTransformationMatrix() {
		return MatrixMath.identityMatrix(4, 4);
	}

}
