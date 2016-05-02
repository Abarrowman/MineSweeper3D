package core;

import java.awt.AWTException;
import java.awt.GraphicsDevice;
import java.awt.MouseInfo;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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

import ajb.core.MatrixMath;
import ajb.core.Object3D;
import ajb.core.RenderUtils;
import ajb.core.Shader;

public class DisplayExample {

	private FloatBuffer pos;
	private FloatBuffer white;
	private int texture;
	private FloatBuffer red;
	private FloatBuffer green;
	private FloatBuffer blue;
	private FloatBuffer yellow;
	private Object3D cube1;
	private Object3D cube2;
	private final int width = 800;
	private final int height = 600;
	private final float widthf = 800;
	private final float heightf = 600;
	private DisplayMode screen;
	private FloatBuffer low;
	private FloatBuffer gray;

	private final float roundOffCorrection = 100000;
	private Shader shader;

	public void start() {
		try {
			Display.setDisplayMode(new DisplayMode(width, height));
			Display.setTitle("Abarrow");
			DisplayMode[] modes = Display.getAvailableDisplayModes();
			screen = null;
			for (int n = 0; n < modes.length; n++) {
				if (screen == null) {
					screen = modes[n];
				} else if (screen.getWidth() < modes[n].getWidth()) {
					screen = modes[n];
				}
			}
			if (screen != null) {
				Display.setLocation((screen.getWidth() - width) / 2, (screen.getHeight() - height) / 2);
			}
			Display.create();
		} catch (LWJGLException e) {
			e.printStackTrace();
			System.exit(0);
		}

		// setup colors
		red = BufferUtils.createFloatBuffer(4).put(new float[] { 1, 0, 0, 1 });
		green = BufferUtils.createFloatBuffer(4).put(new float[] { 0, 1, 0, 1 });
		blue = BufferUtils.createFloatBuffer(4).put(new float[] { 0, 0, 1, 1 });
		yellow = BufferUtils.createFloatBuffer(4).put(new float[] { 1, 1, 0, 1 });
		white = BufferUtils.createFloatBuffer(4).put(new float[] { 1, 1, 1, 1 });
		low = BufferUtils.createFloatBuffer(4).put(new float[] { 0.1f, 0.1f, 0.1f, 1 });
		gray = BufferUtils.createFloatBuffer(4).put(new float[] { 0.1f, 0.1f, 0.1f, 1 });
		// flip
		red.flip();
		green.flip();
		blue.flip();
		yellow.flip();
		white.flip();
		low.flip();
		gray.flip();
		// setup positions
		pos = BufferUtils.createFloatBuffer(4).put(new float[] { 5.0f, 5.0f, 10.0f, 1.0f });
		pos.flip();

		// projection
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		GLU.gluPerspective(45, widthf / height, 0.1f, 100);
		// model view
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glLoadIdentity();
		// lighting
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_LIGHT0);
		// other
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_DEPTH_TEST);

		// load assests
		String path = getClass().getResource("DisplayExample.class").getPath();
		int index = path.lastIndexOf("/");
		if (index != -1) {
			path = path.substring(0, index);
		}
		index = path.lastIndexOf("/");
		if (index != -1) {
			path = path.substring(0, index);
		}
		index = path.lastIndexOf("/");
		if (index != -1) {
			path = path.substring(0, index);
		}

		System.out.println("filePath: "+path);
		
		// load a texture
		try {
			BufferedImage image=ImageIO.read(new File(new URI(path + "/texture.png").getPath()));
			texture=RenderUtils.createTextureFromImage(image);
		} catch (URISyntaxException e1) {
			texture=-1;
		} catch (IOException e) {
			texture=-1;
		}

		// load shader
		URL fragPath;
		URL vertPath;
		try {
			fragPath = new File(new URI(path + "/pointlight.frag").getPath()).toURI().toURL();
			vertPath = new File(new URI(path + "/pointlight.vert").getPath()).toURI().toURL();
			shader = new Shader(vertPath, fragPath);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		// setup world
		cube1 = new Object3D();
		//cube1.x = 2;
		//cube1.z = -10;
		cube1.matrix=MatrixMath.translation3D(2, 0, -10);
		cube1.displayList=GL11.glGenLists(1);
		GL11.glNewList(cube1.displayList, GL11.GL_COMPILE);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glShadeModel(GL11.GL_SMOOTH);
		GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE, green);
		GL11.glMaterialf(GL11.GL_FRONT_AND_BACK, GL11.GL_SHININESS, 10);
		GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_SPECULAR, white);
		ARBShaderObjects.glUseProgramObjectARB(shader.getShader());
		RenderUtils.createUVSphere(2, 20, null);
		
		GL11.glPushMatrix();
		GL11.glTranslatef(0, 2, 0);
		RenderUtils.createCylinder(4, 1,20, null);
		GL11.glPopMatrix();
				 
		ARBShaderObjects.glUseProgramObjectARB(0);
		GL11.glEndList();

		cube2 = new Object3D();
		//cube2.x = -2;
		//cube2.z = -10;
		cube2.matrix=MatrixMath.translation3D(-2, 0, -10);
		cube2.displayList=GL11.glGenLists(1);
		GL11.glNewList(cube2.displayList, GL11.GL_COMPILE);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
		GL11.glShadeModel(GL11.GL_SMOOTH);
		GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE, red);
		GL11.glMaterialf(GL11.GL_FRONT_AND_BACK, GL11.GL_SHININESS, 100);
		GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_SPECULAR, white);
		RenderUtils.createCube(1.3f, null);
		GL11.glEndList();
		
		/*
		int imageModel = GL11.glGenLists(1);
		GL11.glNewList(imageModel, GL11.GL_COMPILE);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		ARBShaderObjects.glUseProgramObjectARB(shader.getShader());
		// render
		GL11.glShadeModel(GL11.GL_SMOOTH);
		GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE, low);
		GL11.glMaterialf(GL11.GL_FRONT_AND_BACK, GL11.GL_SHININESS, 10);
		GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_SPECULAR, gray);
		
		
		float[][] image=new float[][]{
				new float[]{1,1,1},
				new float[]{1,1,1},
		};
		float imageWidth=image.length;
		float imageHeight=image[0].length;
		
		float xAdj=-(imageWidth-1f)/2f;
		float yAdj=-(imageHeight-1f)/2f;
		
		Vector<float[][]> polygons=new Vector<float[][]>();
		float[][][] textureCords=new float[(int)((imageWidth-1)*(imageHeight-1))][][];
		for(int yn=0;yn<imageHeight-1;yn++){
			for(int xn=0;xn<imageWidth-1;xn++){
				polygons.add(new float[][]{
						new float[]{xn+xAdj,yn+yAdj,image[xn][yn]},
						new float[]{xn+1+xAdj,yn+yAdj,image[xn+1][yn]},
						new float[]{xn+1+xAdj,yn+1+yAdj,image[xn+1][yn+1]},
						new float[]{xn+xAdj,yn+1+yAdj,image[xn][yn+1]}
				});
				textureCords[(int) (xn+(imageWidth-1)*yn)]=new float[][]{
						new float[]{xn,yn},
						new float[]{xn+1,yn},
						new float[]{xn+1,yn+1},
						new float[]{xn,yn+1}
				};
			}
		}
		RenderUtils.cleanUpAndRenderPolyheadron(polygons, null, textureCords);
		// post render
		ARBShaderObjects.glUseProgramObjectARB(0);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glEndList();
		
		Object3D imageObj = new Object3D();
		imageObj.x = 0;
		imageObj.z = -10f;
		imageObj.y = 0;
		imageObj.list=imageModel;
		imageObj.xScale=imageObj.yScale=1;
		addChild(imageObj);
		*/

		loop();
	}

	private int indexOfVector(Vector<float[]> list, float[] vector) {
		for (int n = 0; n < list.size(); n++) {
			if (Arrays.equals(list.get(n), vector)) {
				return n;
			}
		}
		return -1;
	}

	private String vectorToString(float[] vector) {
		String string = "";
		for (int n = 0; n < vector.length; n++) {
			string += vector[n];
			if (n < vector.length - 1) {
				string += ",";
			}
		}
		return string;
	}

	private float distance(float[] vector1, float[] vector2) {
		float distance = 0;
		for (int n = 0; n < vector1.length && n < vector2.length; n++) {
			distance += Math.pow(vector1[n] - vector2[n], 2);
		}
		return (float) Math.sqrt(distance);
	}

	private float[] addVectors(float[] vector1, float[] vector2) {
		float[] sum = new float[Math.max(vector1.length, vector2.length)];
		for (int n = 0; n < sum.length; n++) {
			sum[n] = 0;
			if (n < vector1.length) {
				sum[n] += vector1[n];
			}
			if (n < vector2.length) {
				sum[n] += vector2[n];
			}
		}
		return sum;
	}

	private float[] subtractVectors(float[] vector1, float[] vector2) {
		float[] difference = new float[Math.max(vector1.length, vector2.length)];
		for (int n = 0; n < difference.length; n++) {
			difference[n] = 0;
			if (n < vector1.length) {
				difference[n] += vector1[n];
			}
			if (n < vector2.length) {
				difference[n] -= vector2[n];
			}
		}
		return difference;
	}

	private float absVectorDifference(float[] vector1, float[] vector2) {
		float difference = 0;
		int len = Math.min(vector1.length, vector2.length);
		for (int n = 0; n < len; n++) {
			difference += Math.abs(vector1[n] - vector2[n]);
		}
		if (vector1.length > vector2.length) {
			for (int n = len + 1; n < vector1.length; n++) {
				difference += Math.abs(vector1[n]);
			}
		} else {
			for (int n = len + 1; n < vector2.length; n++) {
				difference += Math.abs(vector2[n]);
			}
		}
		return difference;
	}

	private float[] multiplyVectors(float[] vector, float amount) {
		float[] product = new float[vector.length];
		for (int n = 0; n < product.length; n++) {
			product[n] = vector[n] * amount;
		}
		return product;
	}

	private float[] divideVectors(float[] vector, float denomenator) {
		float[] quotient = new float[vector.length];
		for (int n = 0; n < quotient.length; n++) {
			quotient[n] = vector[n] / denomenator;
		}
		return quotient;
	}

	private float[] divideVectors(float[] vector1, float[] vector2) {
		float[] quotient = new float[Math.max(vector1.length, vector2.length)];
		for (int n = 0; n < quotient.length; n++) {
			quotient[n] = 0;
			if (n < vector1.length) {
				quotient[n] += vector1[n];
			}
			if (n < vector2.length) {
				quotient[n] /= vector2[n];
			}
		}
		return quotient;
	}

	private float[] normalize(float[] vector) {
		// find the vector magnitude
		float magnitude = getMagnitude(vector);
		// normalize the vector
		for (int n = 0; n < vector.length; n++) {
			vector[n] = vector[n] / magnitude;
		}
		// return the normalized vector
		return vector;
	}

	private float getMagnitudeSquared(float[] vector) {
		float magnitudeSquared = 0;
		for (int n = 0; n < vector.length; n++) {
			magnitudeSquared += (float) Math.pow(vector[n], 2);
		}
		return magnitudeSquared;
	}

	private float getMagnitude(float[] vector) {
		return (float) Math.sqrt(getMagnitudeSquared(vector));
	}

	private float[] reverse(float[] vector) {
		// reverse the vector
		float[] reverse = new float[vector.length];
		for (int n = 0; n < vector.length; n++) {
			reverse[n] = -vector[n];
		}
		// return the reversed vector
		return reverse;
	}

	private void loop() {
		float spot = 0;
		float centerX = screen.getWidth() / 2;
		float centerY = screen.getHeight() / 2;
		float heroX = 0;
		float heroY = 0;
		float heroZ = 0;
		Robot rob = null;
		try {
			rob = new Robot();
			rob.mouseMove((int) centerX, (int) centerY);
		} catch (AWTException e) {
		}
		float yFacing = 0;
		float xFacing = 0;
		while (!Display.isCloseRequested()) {
			// code
			float x = (float) MouseInfo.getPointerInfo().getLocation().getX();
			float y = (float) MouseInfo.getPointerInfo().getLocation().getY();

			float dx = x - centerX;
			float dy = y - centerY;
			if (rob != null) {
				yFacing += dx / 600f;
				xFacing += dy / 600f;
				rob.mouseMove((int) centerX, (int) centerY);
			}

			if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {

				heroX -= (float) (Math.sin((float) yFacing) * Math.cos((float) xFacing));
				 heroY-=(float)-Math.sin((float)xFacing);
				heroZ -= (float) -(Math.cos((float) yFacing) * Math.cos((float) xFacing));

			} else if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {

				heroX += (float) (Math.sin((float) yFacing) * Math.cos((float) xFacing));
				 heroY+=(float)-Math.sin((float)xFacing);
				heroZ += (float) -(Math.cos((float) yFacing) * Math.cos((float) xFacing));

			}

			spot += 0.5;
			if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
				// exit
				break;
			}

			// render

			// Clear the screen and depth buffer
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

			// reset projection
			GL11.glMatrixMode(GL11.GL_PROJECTION);
			GL11.glLoadIdentity();
			GLU.gluPerspective(45, widthf / height, 0.1f, 100);
			GL11.glRotatef((float) (xFacing / Math.PI * 180), 1, 0, 0);
			GL11.glRotatef((float) (yFacing / Math.PI * 180), 0, 1, 0);
			GL11.glTranslatef(heroX, heroY, heroZ);

			// reset model view
			GL11.glMatrixMode(GL11.GL_MODELVIEW);
			GL11.glLoadIdentity();
			GL11.glLight(GL11.GL_LIGHT0, GL11.GL_POSITION, pos);

			
			//cube1.xRotation = spot;
			float[] properties=cube1.getProperties();
			cube1.matrix=MatrixMath.multiplyMatracies(MatrixMath.translation3D(properties[0], properties[1], properties[2]),
					MatrixMath.rotation3D((float)(((double)spot)/180d*Math.PI), 0, 0));
			cube1.draw();
			//cube2.yRotation = spot;
			properties=cube2.getProperties();
			cube2.matrix=MatrixMath.multiplyMatracies(MatrixMath.translation3D(properties[0], properties[1], properties[2]),
					MatrixMath.rotation3D(0, (float)(((double)spot)/180d*Math.PI), 0));
			cube2.draw();

			// render the graphics
			Display.update();
			// cap at 60 fps
			Display.sync(60);
		}

		Display.destroy();
	}

	public static void main(String[] argv) {
		DisplayExample displayExample = new DisplayExample();
		displayExample.start();
	}
}
