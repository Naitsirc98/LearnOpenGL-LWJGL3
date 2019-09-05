package learnopengl.p4_advanced_opengl.ch10_2_asteroids;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.nio.FloatBuffer;
import java.util.Random;
import java.util.logging.Logger;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.lwjgl.glfw.GLFWFramebufferSizeCallbackI;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;

import learnopengl.util.Camera;
import learnopengl.util.Camera.CameraMovement;
import learnopengl.util.Mesh;
import learnopengl.util.Mesh.Texture;
import learnopengl.util.Model;
import learnopengl.util.Shader;

public class AsteroidsInstanced {

	private static Logger logger = Logger.getAnonymousLogger();

	private static boolean updateProjection = true;

	// Window size
	private static int windowWidth = 1280;
	private static int windowHeight = 720;

	// Camera
	private static Camera camera = new Camera();
	static {
		camera.position.z = 155.0f;
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
		final String dir = AsteroidsInstanced.class.getResource(".").getFile();
		Shader asteroidShader = new Shader(dir+"asteroids.vs", dir+"asteroids.fs");
		Shader planetShader = new Shader(dir+"planet.vs", dir+"planet.fs");

		// Load models
		Model rock = new Model("resources/objects/rock/rock.obj");
		Model planet = new Model("resources/objects/planet/planet.obj");

		// Generate a large list of semi-random model transformation matrices
		// As long as we need to pass the matrices to LWJGL (and thus in a form of a java.nio.Buffer object)
		// we generate directly the required buffer
		final int amount = 100000;
		FloatBuffer modelMatrices = generateTransformations(amount);

		// Configure instanced array
		final int instancedBuffer = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, instancedBuffer);
		glBufferData(GL_ARRAY_BUFFER, modelMatrices, GL_STATIC_DRAW);

		// Don't forget to free the buffer
		MemoryUtil.memFree(modelMatrices);

		// Set transformation matrices as an instance vertex attribute (with divisor 1)
		// Note: we're cheating a little by taking the, now publicly declared, VAO of the model's mesh(es)
		// and adding new vertexAttribPointers
		// Normally you'd want to do this in a more organized fashion, but for learning purposes this will do.
		for(final Mesh mesh : rock.meshes) {

			glBindVertexArray(mesh.vao);
			// Set attribute pointers (mat4 = 4 times vec4)
			glEnableVertexAttribArray(3);
			glVertexAttribPointer(3, 4, GL_FLOAT, false, 16 * Float.BYTES, 0);
			glEnableVertexAttribArray(4);
			glVertexAttribPointer(4, 4, GL_FLOAT, false, 16 * Float.BYTES, 4 * Float.BYTES);
			glEnableVertexAttribArray(5);
			glVertexAttribPointer(5, 4, GL_FLOAT, false, 16 * Float.BYTES, 2 * 4 * Float.BYTES);
			glEnableVertexAttribArray(6);
			glVertexAttribPointer(6, 4, GL_FLOAT, false, 16 * Float.BYTES, 3 * 4 * Float.BYTES);

			glVertexAttribDivisor(3, 1);
			glVertexAttribDivisor(4, 1);
			glVertexAttribDivisor(5, 1);
			glVertexAttribDivisor(6, 1);

			glBindVertexArray(0);
		}

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

			// Clear the screen
			glClearColor(0.05f, 0.05f, 0.05f, 1.0f);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			
			// Update projection matrix if necessary
			if(updateProjection) {
				projection.setPerspective((float)Math.toRadians(camera.zoom), (float)windowWidth / (float)windowHeight, 
						0.1f, 1000.0f); // Set far plane to 1000!
				updateProjection = false;
			}

			final Matrix4f view = camera.getViewMatrix();

			// Draw planet
			planetShader.use();
			planetShader.setMat4("view", view);
			planetShader.setMat4("projection", projection);
			model.translation(0.0f, -3.0f, 0.0f);
			model.scale(4.0f);
			planetShader.setMat4("model", model);
			planet.draw(planetShader);
			
			// Draw asteroids
			asteroidShader.use();
			asteroidShader.setMat4("view", view);
			asteroidShader.setMat4("projection", projection);
			asteroidShader.setInt("texture_diffuse1", 0);
			glActiveTexture(GL_TEXTURE0);
			final Texture texture = rock.texturesLoaded.values().iterator().next(); // We also made the texturesLoaded map public
			glBindTexture(GL_TEXTURE_2D, texture.id); 
			
			for(final Mesh mesh : rock.meshes) {
				glBindVertexArray(mesh.vao);
				glDrawElementsInstanced(GL_TRIANGLES, mesh.indices.size(), GL_UNSIGNED_INT, 0, amount);
				// glBindVertexArray(0);
			}
			glBindVertexArray(0);

			// Swap buffers and poll IO events (key/mouse events)
			glfwSwapBuffers(window);
			glfwPollEvents();

		}

		// Deallocate all resources when no longer necessary
		glDeleteBuffers(instancedBuffer);
		rock.delete();
		planet.delete();
		asteroidShader.delete();
		planetShader.delete();

		// Clear all allocated resources by GLFW
		glfwTerminate();

	}

	private static FloatBuffer generateTransformations(int amount) {

		FloatBuffer modelMatrices = MemoryUtil.memAllocFloat(amount * 16); // Each Matrix4f contains 16 floats
		Random rand = new Random(System.currentTimeMillis());

		float radius = 150.0f;
		float offset = 25.0f;

		final Vector3f rotationVector = new Vector3f(0.4f, 0.6f, 0.8f).normalize();

		// Create this once and change it each iteration
		final Matrix4f model = new Matrix4f();

		for(int i = 0;i < amount;i++) {

			// 1. Translation: displace along circle with 'radius' in range [-offset, offset]
			final float angle = (float)i / (float)amount * 360.0f;
			float displacement = ((float)rand.nextInt(2 * (int)offset * 100) / 100.0f - offset);

			final float x = (float)Math.sin(angle) * radius + displacement;
			displacement = ((float)rand.nextInt(2 * (int)offset * 100) / 100.0f - offset);

			final float y = displacement * 0.4f; // Keep height of asteroid field smaller compared to width of x and z
			displacement = ((float)rand.nextInt(2 * (int)offset * 100) / 100.0f - offset);

			final float z = (float)Math.cos(angle) * radius + displacement;

			model.translation(x, y, z);

			// 2. Scale: scale between 0.05 and 0.25
			final float scale = (float)rand.nextInt(20) / 100.0f + 0.05f;
			model.scale(scale);

			// 3. Rotation: add random rotation around a (semi)randomly picked rotation axis vector
			final float rotAngle = (float)rand.nextInt(360);
			model.rotate(rotAngle, rotationVector);

			// 4. Now add to the buffer
			model.get(i*16, modelMatrices);
		}

		return modelMatrices;
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
