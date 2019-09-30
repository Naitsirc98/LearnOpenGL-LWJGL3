package learnopengl.p6_pbr.ch01_1_lighting;

import static java.lang.Math.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.logging.Logger;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.lwjgl.glfw.GLFWFramebufferSizeCallbackI;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;

import learnopengl.util.Camera;
import learnopengl.util.Camera.CameraMovement;
import learnopengl.util.Shader;

public class PBRLighting {

	private static Logger logger = Logger.getAnonymousLogger();

	private static boolean updateProjection = true;

	// Window size
	private static int windowWidth = 1280;
	private static int windowHeight = 720;

	// Camera
	private static Camera camera = new Camera();
	static {
		camera.position.z = 3.0f;
	}
	private static float lastX = (float)windowWidth / 2.0f;
	private static float lastY = (float)windowHeight / 2.0f;
	private static boolean firstMouseInput = true;

	// Timing
	private static float deltaTime = 0.0f; // Time between current frame and last frame
	private static float lastFrame = 0.0f;

	// ============== Callbacks ==============

	private static final GLFWFramebufferSizeCallbackI FRAMEBUFFER_SIZE_CALLBACK = (window, width, height) -> {
		// make sure the viewport matches the new window dimensions; note that width and 
		// height will be significantly larger than specified on retina displays.
		glViewport(0, 0, width, height);
		// Also update the window width and height variables to correctly set the projection matrix
		windowWidth = width;
		windowHeight = height;
		updateProjection = true;
	};

	private static final GLFWCursorPosCallbackI MOUSE_MOVE_CALLBACK = (window, xpos, ypos) -> {

		if(firstMouseInput) {
			lastX = (float)xpos;
			lastY = (float)ypos;
			firstMouseInput = false;
		}

		float xoffset = (float)xpos - lastX;
		float yoffset = lastY - (float)ypos; // Reversed since y-coordinates go from bottom to top
		lastX = (float)xpos;
		lastY = (float)ypos;

		camera.processMouseMovement(xoffset, yoffset, true);
	};

	private static final GLFWScrollCallbackI SCROLL_CALLBACK = (window, xoffset, yoffset) -> {
		camera.processMouseScroll((float)yoffset);
		updateProjection = true;
	};

	public static void main(String[] args) {

		// Initialize GLFW
		glfwInit();
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
		glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
		glfwWindowHint(GLFW_SAMPLES, 4);

		// #ifdef __APPLE__
		if(Platform.get() == Platform.MACOSX) {
			glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
		}

		// Window creation

		final long window = glfwCreateWindow(windowWidth, windowHeight, "LearnOpenGL", NULL, NULL);

		if(window == NULL) {
			logger.severe("Failed to create GLFW Window");
			glfwTerminate();
			return;
		}

		glfwMakeContextCurrent(window);
		glfwSetFramebufferSizeCallback(window, FRAMEBUFFER_SIZE_CALLBACK);
		glfwSetCursorPosCallback(window, MOUSE_MOVE_CALLBACK);
		glfwSetScrollCallback(window, SCROLL_CALLBACK);

		// Tell GLFW to capture our mouse
		glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

		// Load OpenGL functions
		final GLCapabilities gl = GL.createCapabilities();
		if(gl == null) {
			logger.severe("Failed to initialize OpenGL");
			glfwTerminate();
			return;
		}

		// Configure global OpenGL state
		glEnable(GL_DEPTH_TEST);

		// Build and compile our shader program
		final String dir = PBRLighting.class.getResource(".").getFile();
		Shader shader = new Shader(dir+"pbr.vs", dir+"pbr.fs");

		shader.use();
		shader.setVec3("albedo", new Vector3f(0.5f, 0, 0));
		shader.setFloat("ao", 1);

		// Sphere
		final int sphereVAO = glGenVertexArrays();
		final int sphereVBO = glGenBuffers();
		final int sphereEBO = glGenBuffers();
		final int indicesCount = createSphere(sphereVAO, sphereVBO, sphereEBO);
		
		// Lights
		Vector3fc[] lightPositions = {
				new Vector3f(-10.0f,  10.0f, 10.0f),
				new Vector3f( 10.0f,  10.0f, 10.0f),
				new Vector3f(-10.0f, -10.0f, 10.0f),
				new Vector3f( 10.0f, -10.0f, 10.0f)
		};

		Vector3fc lightColor = new Vector3f(300.0f);
		
		final int nRows = 7;
		final int nColumns = 7;
		final float spacing = 2.5f;
		
		// Pass projection matrix to shader (as projection matrix rarely changes there's no need to do this per frame)
		// ** This is true as long as you don't change the window size!
		// That's why I check every frame if the projection matrix has to be changed
		Matrix4f projection = new Matrix4f();

		// Create the model matrix before enter the loop to avoid calling new every frame
		Matrix4f model = new Matrix4f();

		// Render loop
		while(!glfwWindowShouldClose(window)) {

			// Per-frame time logic
			final float currentFrame = (float)glfwGetTime();
			deltaTime = currentFrame - lastFrame;
			lastFrame = currentFrame;

			// Input
			processInput(window);

			if(updateProjection) {
				projection.setPerspective((float)Math.toRadians(camera.zoom), (float)windowWidth / (float)windowHeight, 
						0.1f, 100.0f);
				updateProjection = false;
			}
			final Matrix4f view = camera.getViewMatrix();

			glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			shader.use();
			shader.setMat4("view", view);
			shader.setMat4("projection", projection);
			shader.setVec3("camPos", camera.position);
			
			// Render rows * columns number of spheres with varying matallic/roughness values scaled by rows and columns respectively
			for(int row = 0;row < nRows;row++) {
				
				shader.setFloat("metallic", (float)row/(float)nRows);
				
				for(int col = 0;col < nColumns;col++) {
					
					// We clamp the roughness to 0.025 - 1.0 as perfectly smooth surfaces (roughness of 0.0) tend to look a bit off
					// on direct lighting.
					shader.setFloat("roughness", min(max((float)col/(float)nColumns, 0.025f), 1.0f));
					
					model.translation(
							(col - (nColumns / 2)) * spacing, 
		                    (row - (nRows / 2)) * spacing, 
		                    0.0f);
					shader.setMat4("model", model);
					
					renderSphere(sphereVAO, indicesCount);
				}
				
			}
			
			// Render light source (simply re-render sphere at light positions)
			// This looks a bit off as we use the same shader, but it'll make their positions obvious and
			// keeps the codeprint small
			for(int i = 0;i < lightPositions.length;i++) {
				
				// Vector3f newPos = lightPositions[i].add((float)sin(glfwGetTime()*5)*5, 0, 0, new Vector3f());
				Vector3fc newPos = lightPositions[i];
				
				shader.setVec3("lightPositions[" + i + "]", newPos);
				shader.setVec3("lightColors["+i+"]", lightColor);
				
				model.translation(newPos);
				model.scale(0.5f);
				shader.setMat4("model", model);
				renderSphere(sphereVAO, indicesCount);
			}

			// Swap buffers and poll IO events (key/mouse events)
			glfwSwapBuffers(window);
			glfwPollEvents();

		}

		// Deallocate all resources when no longer necessary
		glDeleteVertexArrays(sphereVAO);
		glDeleteBuffers(sphereVBO);
		glDeleteBuffers(sphereVBO);
		glDeleteBuffers(sphereEBO);
		shader.delete();

		// Clear all allocated resources by GLFW
		glfwTerminate();

	}

	private static int createSphere(int sphereVAO, int sphereVBO, int sphereEBO) {

		final int xSegments = 64;
		final int ySegments = 64;

		final int size = (xSegments + 1) * (ySegments + 1);

		FloatBuffer data = MemoryUtil.memAllocFloat(size * 3 + size * 2 + size * 3);

		for(int y = 0;y <= ySegments;y++) {

			for(int x = 0;x <= xSegments;x++) {

				final float xSeg = (float)x / (float)xSegments;
				final float ySeg = (float)y / (float)ySegments;
				
				final float xPos = (float)(cos(xSeg * 2 * PI) * sin(ySeg * PI));
				final float yPos = (float)(cos(ySeg * PI));
				final float zPos = (float)(sin(xSeg * 2 * PI) * sin(ySeg * PI));

				// Position
				data.put(xPos);
				data.put(yPos);
				data.put(zPos);
				// UV
				data.put(xSeg);
				data.put(ySeg);
				// Normal
				data.put(xPos);
				data.put(yPos);
				data.put(zPos);

			}
		}

		data.rewind();

		final int indicesCount = ySegments * (xSegments + 1) * 2;

		IntBuffer indices = MemoryUtil.memAllocInt(indicesCount);

		for(int y = 0;y < ySegments;y++) {

			if(y % 2 == 0) {

				for(int x = 0;x <= xSegments;x++) {
					indices.put(y * (xSegments + 1) + x);
					indices.put((y + 1) * (xSegments + 1) + x);
				}

			} else {

				for(int x = xSegments;x >= 0;x--) {
					indices.put((y + 1) * (xSegments + 1) + x);
					indices.put(y * (xSegments + 1) + x);
				}

			}

		}
		
		indices.rewind();

		glBindVertexArray(sphereVAO);
		glBindBuffer(GL_ARRAY_BUFFER, sphereVBO);
		glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, sphereEBO);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
		final int stride = (3 + 2 + 3) * Float.BYTES;
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
		glEnableVertexAttribArray(1);
		glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3 * Float.BYTES);
		glEnableVertexAttribArray(2);
		glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, 5 * Float.BYTES);

		// Don't forget to free the buffers!
		MemoryUtil.memFree(data);
		MemoryUtil.memFree(indices);

		return indicesCount;
	}

	private static void renderSphere(int sphereVAO, int indicesCount) {
		glBindVertexArray(sphereVAO);
		glDrawElements(GL_TRIANGLE_STRIP, indicesCount, GL_UNSIGNED_INT, NULL);
	}

	private static void processInput(long window) {
		// Close window when ESC key is pressed
		if(glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS) {
			glfwSetWindowShouldClose(window, true);

		}

		final float speed = camera.movementSpeed;

		// Bonus: if left shift key is pressed, you double the speed!
		if(glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) {
			camera.movementSpeed *= 2.0f;
		}

		if(glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) {
			camera.processKeyboard(CameraMovement.FORWARD, deltaTime);
		}

		if(glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) {
			camera.processKeyboard(CameraMovement.BACKWARD, deltaTime);
		}

		if(glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) {
			camera.processKeyboard(CameraMovement.LEFT, deltaTime);
		}

		if(glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) {
			camera.processKeyboard(CameraMovement.RIGHT, deltaTime);
		}

		camera.movementSpeed = speed;

	}


}
