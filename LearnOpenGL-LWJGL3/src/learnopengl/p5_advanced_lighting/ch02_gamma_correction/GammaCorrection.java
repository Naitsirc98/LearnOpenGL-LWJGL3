package learnopengl.p5_advanced_lighting.ch02_gamma_correction;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL21.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.logging.Logger;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.lwjgl.glfw.GLFWFramebufferSizeCallbackI;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;

import learnopengl.util.Camera;
import learnopengl.util.Camera.CameraMovement;
import learnopengl.util.Shader2;

public class GammaCorrection {

	private static Logger logger = Logger.getAnonymousLogger();

	private static boolean updateProjection = true;

	// Window size
	private static int windowWidth = 800;
	private static int windowHeight = 600;

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

	// Gamma
	private static boolean gammaEnabled = false;
	private static boolean gammaKeyPressed = false;

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


	private static final float[] PLANE_VERTICES = {
			// positions            // normals         // texcoords
			10.0f, -0.5f,  10.0f,  0.0f, 1.0f, 0.0f,  10.0f,  0.0f,
			-10.0f, -0.5f,  10.0f,  0.0f, 1.0f, 0.0f,   0.0f,  0.0f,
			-10.0f, -0.5f, -10.0f,  0.0f, 1.0f, 0.0f,   0.0f, 10.0f,

			10.0f, -0.5f,  10.0f,  0.0f, 1.0f, 0.0f,  10.0f,  0.0f,
			-10.0f, -0.5f, -10.0f,  0.0f, 1.0f, 0.0f,   0.0f, 10.0f,
			10.0f, -0.5f, -10.0f, 0.0f, 1.0f, 0.0f, 10.0f, 10.0f
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

		// Build and compile our shader programs
		final String dir = GammaCorrection.class.getResource(".").getFile();
		Shader2 shader = new Shader2(dir+"gamma_correction.vs", dir+"gamma_correction.fs");

		// First, configure the cube's VAO (and VBO)
		final int planeVAO = glGenVertexArrays();
		final int planeVBO = glGenBuffers();
		setUpVertexData(planeVAO, planeVBO);

		// Load textures
		final int floorTexture = loadTexture("resources/textures/wood.png", false);
		final int floorTextureGammaCorrected = loadTexture("resources/textures/wood.png", true);

		// Configure global OpenGL state
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		shader.use();
		shader.setInt("floorTexture", 0);

		// Lighting info
		final Vector3f[] lightPositions = {
				new Vector3f(-3.0f, 0.0f, 0.0f),
				new Vector3f(-1.0f, 0.0f, 0.0f),
				new Vector3f(1.0f, 0.0f, 0.0f),
				new Vector3f(3.0f, 0.0f, 0.0f)
		};

		final Vector3f[] lightColors = {
				new Vector3f(0.25f),
				new Vector3f(0.50f),
				new Vector3f(0.75f),
				new Vector3f(1.00f)	
		};
		
		// LWJGL requires a java.nio.Buffer (or an array) to pass uniform data to the shaders, so 
		// we have to create one buffer (or multiple) to put the data in
		// Since we would need to do this every frame, lets create the buffer before the render
		// loop and set it once to increase performance
		FloatBuffer lightBuffer = MemoryUtil.memAllocFloat(24); // 2 Vector3f[4]
		
		int index = 0;
		
		// Fill in the buffer with the positions array
		for(int i = 0;i < lightPositions.length;i++, index++) {
			lightPositions[i].get(index * 3, lightBuffer);
		}

		// Fill in the buffer with the colors array
		for(int i = 0;i < lightColors.length;i++, index++) {
			lightColors[i].get(index * 3, lightBuffer);
		}
		
		// In order to send array uniforms in LWJGL, we need the buffer's memory address
		final long positionsAddress = MemoryUtil.memAddress(lightBuffer.position(0)); // Address to positions data
		final long colorsAddress = MemoryUtil.memAddress(lightBuffer.position(12)); // Address to colors data

		// Pass projection matrix to shader (as projection matrix rarely changes there's no need to do this per frame)
		// ** This is true as long as you don't change the window size!
		// That's why I check every frame if the projection matrix has to be changed
		Matrix4f projection = new Matrix4f();

		// Render loop
		while(!glfwWindowShouldClose(window)) {

			// Per-frame time logic
			final float currentFrame = (float)glfwGetTime();
			deltaTime = currentFrame - lastFrame;
			lastFrame = currentFrame;

			// Input
			processInput(window);

			// Clear the screen
			glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Also clear the depth buffer now!

			shader.use();
			// Update projection matrix if necessary
			if(updateProjection) {
				projection.setPerspective((float)Math.toRadians(camera.zoom), (float)windowWidth / (float)windowHeight, 
						0.1f, 100.0f);
				updateProjection = false;
			}
			shader.setMat4("projection", projection);
			// Camera/view transformations
			final Matrix4f view = camera.getViewMatrix();
			shader.setMat4("view", view);

			// Set light uniforms
			nglUniform3fv(glGetUniformLocation(shader.id, "lightPositions"), 4, positionsAddress);
			nglUniform3fv(glGetUniformLocation(shader.id, "lightColors"), 4, colorsAddress);
			shader.setVec3("viewPos", camera.position);
			shader.setBool("gamma", gammaEnabled);

			// Floor
			glBindVertexArray(planeVAO);
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, gammaEnabled ? floorTextureGammaCorrected : floorTexture);
			glDrawArrays(GL_TRIANGLES, 0, 6);

			System.out.println(gammaEnabled ? "Gamma enabled" : "Gamma disabled");

			// Swap buffers and poll IO events (key/mouse events)
			glfwSwapBuffers(window);
			glfwPollEvents();

		}
		
		// Don't forget to free the buffer!
		MemoryUtil.memFree(lightBuffer);

		// Deallocate all resources when no longer necessary
		glDeleteVertexArrays(planeVAO);
		glDeleteBuffers(planeVBO);
		glDeleteTextures(floorTexture);
		glDeleteTextures(floorTextureGammaCorrected);
		shader.delete();

		// Clear all allocated resources by GLFW
		glfwTerminate();

	}

	private static int loadTexture(String path, boolean gammaCorrected) {

		final int texture = glGenTextures();

		glBindTexture(GL_TEXTURE_2D, texture); // All upcoming GL_TEXTURE_2D operations now have effect on this texture object
		// Set the texture wrapping parameters
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
		// Set texture filtering parameters
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

		// Load image, create texture and generate mipmaps
		try(MemoryStack stack = MemoryStack.stackPush()) {

			IntBuffer width = stack.ints(0);
			IntBuffer height = stack.ints(0);
			IntBuffer nrChannels = stack.ints(0);

			ByteBuffer data = stbi_load(path, width, height, nrChannels, 0);

			if(data != null) {

				final int format = getImageFormat(nrChannels.get(0));
				final int internalFormat = getImageInternalFormat(format, gammaCorrected);

				glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width.get(0), height.get(0), 0, format, GL_UNSIGNED_BYTE, data);
				glGenerateMipmap(GL_TEXTURE_2D);

			} else {
				logger.severe("Failed to load texture: " + path);
			}

			stbi_image_free(data);

		}

		return texture;
	}

	private static int getImageInternalFormat(int format, boolean gammaCorrected) {
		if(!gammaCorrected) {
			return format;
		}
		switch(format) {
		case GL_RGB:
			return GL_SRGB;
		case GL_RGBA:
			return GL_SRGB_ALPHA;
		default:
			throw new RuntimeException("Unsupported image format");
		}
	}

	private static int getImageFormat(int nrChannels) {

		switch(nrChannels) {
		case 1:
			return GL_RED;
		case 2:
			return GL_RG;
		case 3:
			return GL_RGB;
		case 4:
			return GL_RGBA;
		default:
			throw new RuntimeException("Unexpected number of channels");
		}
	}

	private static void setUpVertexData(int vao, int vbo) {
		// Bind the Vertex Array Object first, then bind and set vertex buffer(s), and then configure vertex attributes(s).
		glBindVertexArray(vao);

		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		glBufferData(GL_ARRAY_BUFFER, PLANE_VERTICES, GL_STATIC_DRAW);

		// Position
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 8 * Float.BYTES, 0);
		glEnableVertexAttribArray(0);

		// Normal
		glVertexAttribPointer(1, 3, GL_FLOAT, false, 8 * Float.BYTES, 3 * Float.BYTES);
		glEnableVertexAttribArray(1);

		// Texture coordinates
		glVertexAttribPointer(2, 2, GL_FLOAT, false, 8 * Float.BYTES, 6 * Float.BYTES);
		glEnableVertexAttribArray(2);

		// Note that this is allowed, the call to glVertexAttribPointer registered VBO as the vertex attribute's bound vertex buffer object so afterwards we can safely unbind
		glBindBuffer(GL_ARRAY_BUFFER, 0); 

		// You can unbind the VAO afterwards so other VAO calls won't accidentally modify this VAO, but this rarely happens. Modifying other
		// VAOs requires a call to glBindVertexArray anyways so we generally don't unbind VAOs (nor VBOs) when it's not directly necessary.
		glBindVertexArray(0); 
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

		if(glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS && !gammaKeyPressed) {
			gammaEnabled = !gammaEnabled;
			gammaKeyPressed = true;
		} else if(glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_RELEASE) {
			gammaKeyPressed = false;
		}

	}

}
