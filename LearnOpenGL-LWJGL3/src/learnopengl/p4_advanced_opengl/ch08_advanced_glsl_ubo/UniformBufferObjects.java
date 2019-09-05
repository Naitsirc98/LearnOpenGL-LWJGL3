package learnopengl.p4_advanced_opengl.ch08_advanced_glsl_ubo;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL44.*;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.logging.Logger;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.lwjgl.glfw.GLFWFramebufferSizeCallbackI;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;

import learnopengl.util.Camera;
import learnopengl.util.Camera.CameraMovement;
import learnopengl.util.Shader;

public class UniformBufferObjects {

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


	// Vertex Data
	private static final float CUBE_VERTICES[] = {
			// positions         
			-0.5f, -0.5f, -0.5f, 
			0.5f, -0.5f, -0.5f,  
			0.5f,  0.5f, -0.5f,  
			0.5f,  0.5f, -0.5f,  
			-0.5f,  0.5f, -0.5f, 
			-0.5f, -0.5f, -0.5f, 

			-0.5f, -0.5f,  0.5f, 
			0.5f, -0.5f,  0.5f,  
			0.5f,  0.5f,  0.5f,  
			0.5f,  0.5f,  0.5f,  
			-0.5f,  0.5f,  0.5f, 
			-0.5f, -0.5f,  0.5f, 

			-0.5f,  0.5f,  0.5f, 
			-0.5f,  0.5f, -0.5f, 
			-0.5f, -0.5f, -0.5f, 
			-0.5f, -0.5f, -0.5f, 
			-0.5f, -0.5f,  0.5f, 
			-0.5f,  0.5f,  0.5f, 

			0.5f,  0.5f,  0.5f,  
			0.5f,  0.5f, -0.5f,  
			0.5f, -0.5f, -0.5f,  
			0.5f, -0.5f, -0.5f,  
			0.5f, -0.5f,  0.5f,  
			0.5f,  0.5f,  0.5f,  

			-0.5f, -0.5f, -0.5f, 
			0.5f, -0.5f, -0.5f,  
			0.5f, -0.5f,  0.5f,  
			0.5f, -0.5f,  0.5f,  
			-0.5f, -0.5f,  0.5f, 
			-0.5f, -0.5f, -0.5f, 

			-0.5f,  0.5f, -0.5f, 
			0.5f,  0.5f, -0.5f,  
			0.5f,  0.5f,  0.5f,  
			0.5f,  0.5f,  0.5f,  
			-0.5f,  0.5f,  0.5f, 
			-0.5f, 0.5f, -0.5f, 
	};


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
		final String dir = UniformBufferObjects.class.getResource(".").getFile();
		Shader shaderRed = new Shader(dir+"advanced_glsl.vs", dir+"red.fs");
		Shader shaderGreen = new Shader(dir+"advanced_glsl.vs", dir+"green.fs");
		Shader shaderBlue = new Shader(dir+"advanced_glsl.vs", dir+"blue.fs");
		Shader shaderYellow = new Shader(dir+"advanced_glsl.vs", dir+"yellow.fs");

		// Cube
		final int cubeVAO = glGenVertexArrays();
		final int cubeVBO = glGenBuffers();
		setUpVertexData(cubeVAO, cubeVBO, CUBE_VERTICES);

		// Create the model matrix before enter the loop to avoid calling new every frame
		Matrix4f model = new Matrix4f();

		// Configure a Uniform Buffer Object
		// First of all, get the relevant block indices
		final int blockIndexRed = glGetUniformBlockIndex(shaderRed.id, "Matrices");
		final int blockIndexGreen = glGetUniformBlockIndex(shaderGreen.id, "Matrices");
		final int blockIndexBlue = glGetUniformBlockIndex(shaderBlue.id, "Matrices");
		final int blockIndexYellow = glGetUniformBlockIndex(shaderYellow.id, "Matrices");
		// Then we link each shader's uniform block to this uniform binding point
		glUniformBlockBinding(shaderRed.id, blockIndexRed, 0);
		glUniformBlockBinding(shaderGreen.id, blockIndexGreen, 0);
		glUniformBlockBinding(shaderBlue.id, blockIndexBlue, 0);
		glUniformBlockBinding(shaderYellow.id, blockIndexYellow, 0);
		// Now actually create the buffer
		final int uboMatrices = glGenBuffers();
		glBindBuffer(GL_UNIFORM_BUFFER, uboMatrices);
		final int sizeofMat4 = 16 * Float.BYTES;
		nglBufferData(GL_UNIFORM_BUFFER, 2 * sizeofMat4, NULL, GL_STATIC_DRAW); // Reserve enough memory
		glBindBuffer(GL_UNIFORM_BUFFER, 0);
		// Define the range of the buffer that links to a uniform binding point
		glBindBufferRange(GL_UNIFORM_BUFFER, 0, uboMatrices, 0, 2 * sizeofMat4);

		// Store the projection matrix (we only do this once now) 
		// Note: we are not using zoom anymore by changing the FoV
		Matrix4f projection = new Matrix4f();
		projection.perspective((float)Math.toRadians(45.0f), (float)windowWidth / (float)windowHeight, 0.1f, 100.0f);
		glBindBuffer(GL_UNIFORM_BUFFER, uboMatrices);
		try(MemoryStack stack = MemoryStack.stackPush()) {
			glBufferSubData(GL_UNIFORM_BUFFER, 0, projection.get(stack.mallocFloat(16)));		
		}
		glBindBuffer(GL_UNIFORM_BUFFER, 0);

		// Render loop
		while(!glfwWindowShouldClose(window)) {

			// Per-frame time logic
			final float currentFrame = (float)glfwGetTime();
			deltaTime = currentFrame - lastFrame;
			lastFrame = currentFrame;

			// Input
			processInput(window);

			// Make sure we clear the framebuffer's content
			glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			// Set the view matrix in the uniform block - we only have to do this once per render iteration
			final Matrix4f view = camera.getViewMatrix();
			glBindBuffer(GL_UNIFORM_BUFFER, uboMatrices);
			try(MemoryStack stack = MemoryStack.stackPush()) {
				glBufferSubData(GL_UNIFORM_BUFFER, sizeofMat4, view.get(stack.mallocFloat(16)));
			}
			glBindBuffer(GL_UNIFORM_BUFFER, 0);

			// Draw 4 cubes
			// RED
			glBindVertexArray(cubeVAO);
			shaderRed.use();
			model.translation(-0.75f, 0.75f, 0.0f); // Move top-left
			shaderRed.setMat4("model", model);
			glDrawArrays(GL_TRIANGLES, 0, 36);
			// GREEN
			shaderGreen.use();
			model.translation(0.75f, 0.75f, 0.0f); // Move top-right
			shaderGreen.setMat4("model", model);
			glDrawArrays(GL_TRIANGLES, 0, 36);
			// YELLOW
			shaderYellow.use();
			model.translation(-0.75f, -0.75f, 0.0f); // Move bottom-left
			shaderYellow.setMat4("model", model);
			glDrawArrays(GL_TRIANGLES, 0, 36);
			// BLUE
			shaderBlue.use();
			model.translation(0.75f, -0.75f, 0.0f); // Move bottom-right
			shaderBlue.setMat4("model", model);
			glDrawArrays(GL_TRIANGLES, 0, 36);

			// Swap buffers and poll IO events (key/mouse events)
			glfwSwapBuffers(window);
			glfwPollEvents();

		}

		// Deallocate all resources when no longer necessary
		glDeleteVertexArrays(cubeVAO);
		glDeleteBuffers(cubeVBO);
		glDeleteBuffers(uboMatrices);
		shaderRed.delete();
		shaderGreen.delete();
		shaderBlue.delete();
		shaderYellow.delete();

		// Clear all allocated resources by GLFW
		glfwTerminate();

	}

	private static void setUpVertexData(int vao, int vbo, float[] vertexData) {
		// Bind the Vertex Array Object first, then bind and set vertex buffer(s), and then configure vertex attributes(s).
		glBindVertexArray(vao);

		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		glBufferData(GL_ARRAY_BUFFER, vertexData, GL_STATIC_DRAW);

		// Position
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
		glEnableVertexAttribArray(0);

		// Note that this is allowed, the call to glVertexAttribPointer registered VBO as the vertex attribute's bound vertex buffer object so afterwards we can safely unbind
		glBindBuffer(GL_ARRAY_BUFFER, 0); 

		// You can unbind the VAO afterwards so other VAO calls won't accidentally modify this VAO, but this rarely happens. Modifying other
		// VAOs requires a call to glBindVertexArray anyways so we generally don't unbind VAOs (nor VBOs) when it's not directly necessary.
		glBindVertexArray(0); 
	}

	private static int loadTexture(String path, boolean flipY) {

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

			stbi_set_flip_vertically_on_load(flipY); // Tell stb_image.h whether to flip loaded texture's on the y-axis or not.

			ByteBuffer data = stbi_load(path, width, height, nrChannels, 0);

			if(data != null) {

				int format = getImageFormat(nrChannels.get(0));

				glTexImage2D(GL_TEXTURE_2D, 0, format, width.get(0), height.get(0), 0, format, GL_UNSIGNED_BYTE, data);
				glGenerateMipmap(GL_TEXTURE_2D);

			} else {
				logger.severe("Failed to load texture: " + path);
			}

			stbi_image_free(data);

		}

		return texture;
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
