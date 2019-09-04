package learnopengl.p1_getting_started.ch07_2_camera_keyboard_dt;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.logging.Logger;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.glfw.GLFWFramebufferSizeCallbackI;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;

import learnopengl.util.Shader2;

public class CameraKeyboardDT {

	private static Logger logger = Logger.getAnonymousLogger();

	private static boolean updateProjection = true;
	
	// Callbacks
	private static final GLFWFramebufferSizeCallbackI FRAMEBUFFER_SIZE_CALLBACK = (window, width, height) -> {
		// make sure the viewport matches the new window dimensions; note that width and 
		// height will be significantly larger than specified on retina displays.
		glViewport(0, 0, width, height);
		// Also update the window width and height variables to correctly set the projection matrix
		windowWidth = width;
		windowHeight = height;
		updateProjection = true;
	};

	// Window size
	private static int windowWidth = 800;
	private static int windowHeight = 600;
	
	// Camera parameters
	private static Vector3f cameraPos = new Vector3f(0.0f, 0.0f, 3.0f);
	private static Vector3f cameraFront = new Vector3f(0.0f, 0.0f, -1.0f);
	private static Vector3f cameraUp = new Vector3f(0.0f, 1.0f, 0.0f);
	
	// Timing
	private static float deltaTime = 0.0f; // Time between current frame and last frame
	private static float lastFrame = 0.0f;
	

	private static final float[] VERTICES = {
			-0.5f, -0.5f, -0.5f,  0.0f, 0.0f,
			0.5f, -0.5f, -0.5f,  1.0f, 0.0f,
			0.5f,  0.5f, -0.5f,  1.0f, 1.0f,
			0.5f,  0.5f, -0.5f,  1.0f, 1.0f,
			-0.5f,  0.5f, -0.5f,  0.0f, 1.0f,
			-0.5f, -0.5f, -0.5f,  0.0f, 0.0f,

			-0.5f, -0.5f,  0.5f,  0.0f, 0.0f,
			0.5f, -0.5f,  0.5f,  1.0f, 0.0f,
			0.5f,  0.5f,  0.5f,  1.0f, 1.0f,
			0.5f,  0.5f,  0.5f,  1.0f, 1.0f,
			-0.5f,  0.5f,  0.5f,  0.0f, 1.0f,
			-0.5f, -0.5f,  0.5f,  0.0f, 0.0f,

			-0.5f,  0.5f,  0.5f,  1.0f, 0.0f,
			-0.5f,  0.5f, -0.5f,  1.0f, 1.0f,
			-0.5f, -0.5f, -0.5f,  0.0f, 1.0f,
			-0.5f, -0.5f, -0.5f,  0.0f, 1.0f,
			-0.5f, -0.5f,  0.5f,  0.0f, 0.0f,
			-0.5f,  0.5f,  0.5f,  1.0f, 0.0f,

			0.5f,  0.5f,  0.5f,  1.0f, 0.0f,
			0.5f,  0.5f, -0.5f,  1.0f, 1.0f,
			0.5f, -0.5f, -0.5f,  0.0f, 1.0f,
			0.5f, -0.5f, -0.5f,  0.0f, 1.0f,
			0.5f, -0.5f,  0.5f,  0.0f, 0.0f,
			0.5f,  0.5f,  0.5f,  1.0f, 0.0f,

			-0.5f, -0.5f, -0.5f,  0.0f, 1.0f,
			0.5f, -0.5f, -0.5f,  1.0f, 1.0f,
			0.5f, -0.5f,  0.5f,  1.0f, 0.0f,
			0.5f, -0.5f,  0.5f,  1.0f, 0.0f,
			-0.5f, -0.5f,  0.5f,  0.0f, 0.0f,
			-0.5f, -0.5f, -0.5f,  0.0f, 1.0f,

			-0.5f,  0.5f, -0.5f,  0.0f, 1.0f,
			0.5f,  0.5f, -0.5f,  1.0f, 1.0f,
			0.5f,  0.5f,  0.5f,  1.0f, 0.0f,
			0.5f,  0.5f,  0.5f,  1.0f, 0.0f,
			-0.5f,  0.5f,  0.5f,  0.0f, 0.0f,
			-0.5f, 0.5f, -0.5f, 0.0f, 1.0f
	}; 
	
	// World space positions of our cubes
	private static Vector3fc[] cubePositions = {
	        new Vector3f( 0.0f,  0.0f,  0.0f),
	        new Vector3f( 2.0f,  5.0f, -15.0f),
	        new Vector3f(-1.5f, -2.2f, -2.5f),
	        new Vector3f(-3.8f, -2.0f, -12.3f),
	        new Vector3f( 2.4f, -0.4f, -3.5f),
	        new Vector3f(-1.7f,  3.0f, -7.5f),
	        new Vector3f( 1.3f, -2.0f, -2.5f),
	        new Vector3f( 1.5f,  2.0f, -2.5f),
	        new Vector3f( 1.5f,  0.2f, -1.5f),
	        new Vector3f(-1.3f, 1.0f, -1.5f)
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

		// Load OpenGL functions
		final GLCapabilities gl = GL.createCapabilities();
		if(gl == null) {
			logger.severe("Failed to initialize OpenGL");
			glfwTerminate();
			return;
		}

		// Build and compile our shader program
		final String dir = CameraKeyboardDT.class.getResource(".").getFile();
		Shader2 ourShader = new Shader2(dir+"ch72_camera.vs", dir+"ch72_camera.fs");

		// Set up vertex data, the Vertex Buffer Object (VBO) and the Vertex Array Object (VAO)
		final int vao = glGenVertexArrays();
		final int vbo = glGenBuffers();
		setUpVertexData(vao, vbo);

		// Load Textures
		// Note that we set the container wrapping method to GL_CLAMP_TO_EDGE
		// Set texture filtering to nearest neighbor to clearly see the texels/pixels
		final int texture1 = loadTexture("resources/textures/container.jpg", true, GL_REPEAT, GL_LINEAR); 
		final int texture2 = loadTexture("resources/textures/awesomeface.png", true, GL_REPEAT, GL_LINEAR);

		ourShader.use();

		// Tell opengl for each sampler to which texture unit it belongs to (only has to be done once)
		ourShader.use(); // Don't forget to activate/use the shader before setting uniforms!
		ourShader.setInt("texture1", 0);
		ourShader.setInt("texture2", 1);
		
		// Configure global OpenGL state
		glEnable(GL_DEPTH_TEST);
		
		// Pass projection matrix to shader (as projection matrix rarely changes there's no need to do this per frame)
		// ** This is true as long as you don't change the window size!
		// That's why I check every frame if the projection matrix has to be changed
		Matrix4f projection = new Matrix4f();

		// JOML needs the rotation vector to be normalized
		final Vector3fc rotationAxis = new Vector3f(1.0f, 0.3f, 0.5f).normalize();
		
		Vector3f vecDst = new Vector3f(); // Destination for vector operations
		
		// Render loop
		while(!glfwWindowShouldClose(window)) {
			
			// Per-frame time logic
			final float currentFrame = (float)glfwGetTime();
			deltaTime = currentFrame - lastFrame;
			lastFrame = currentFrame;

			// Input
			processInput(window);

			// Clear the screen
			glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Also clear the depth buffer now!

			// Bind textures
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, texture1);
			glActiveTexture(GL_TEXTURE1);
			glBindTexture(GL_TEXTURE_2D, texture2);

			// Activate shader
			ourShader.use();
			
			// Update projection matrix if necessary
			if(updateProjection) {
				projection.setPerspective((float)Math.toRadians(45.0f), (float)windowWidth / (float)windowHeight, 0.1f, 100.0f);
				ourShader.setMat4("projection", projection);
				updateProjection = false;
			}

			// Camera/view transformations
			Matrix4f view = new Matrix4f();
			view.lookAt(cameraPos, cameraPos.add(cameraFront, vecDst), cameraUp);

			// Update the matrix uniforms
			ourShader.setMat4("view", view);
			// Note: currently we set the projection matrix each frame, but since the projection matrix 
			// rarely changes it's often best practice to set it outside the main loop only once.
			ourShader.setMat4("projection", projection);

			// Render cubes
			glBindVertexArray(vao);
			
			for(int i = 0;i < cubePositions.length;i++) {
				// Calculate the model matrix for each object and pass it to shader before drawing
				Matrix4f model = new Matrix4f();
				model.translate(cubePositions[i]);
				final float angle = 20.0f * i;
				model.rotate((float)Math.toRadians(angle), rotationAxis);
				ourShader.setMat4("model", model);
				
				glDrawArrays(GL_TRIANGLES, 0, 36);
			}

			// Swap buffers and poll IO events (key/mouse events)
			glfwSwapBuffers(window);
			glfwPollEvents();

		}

		// Deallocate all resources when no longer necessary
		glDeleteVertexArrays(vao);
		glDeleteBuffers(vbo);
		glDeleteTextures(texture1);
		glDeleteTextures(texture2);
		ourShader.delete();

		// Clear all allocated resources by GLFW
		glfwTerminate();

	}

	private static void setUpVertexData(int vao, int vbo) {
		// Bind the Vertex Array Object first, then bind and set vertex buffer(s), and then configure vertex attributes(s).
		glBindVertexArray(vao);

		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		glBufferData(GL_ARRAY_BUFFER, VERTICES, GL_STATIC_DRAW);

		// Position
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);
		glEnableVertexAttribArray(0);

		// Texture coordinates
		glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
		glEnableVertexAttribArray(1);

		// Note that this is allowed, the call to glVertexAttribPointer registered VBO as the vertex attribute's bound vertex buffer object so afterwards we can safely unbind
		glBindBuffer(GL_ARRAY_BUFFER, 0); 

		// You can unbind the VAO afterwards so other VAO calls won't accidentally modify this VAO, but this rarely happens. Modifying other
		// VAOs requires a call to glBindVertexArray anyways so we generally don't unbind VAOs (nor VBOs) when it's not directly necessary.
		glBindVertexArray(0); 
	}

	private static int loadTexture(String path, boolean flipY, int wrapping, int filtering) {

		final int texture = glGenTextures();

		glBindTexture(GL_TEXTURE_2D, texture); // All upcoming GL_TEXTURE_2D operations now have effect on this texture object
		// Set the texture wrapping parameters
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, wrapping); // Set texture wrapping to GL_REPEAT (default wrapping method)
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, wrapping);
		// Set texture filtering parameters
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filtering);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filtering);

		// Load image, create texture and generate mipmaps
		try(MemoryStack stack = MemoryStack.stackPush()) {

			IntBuffer width = stack.ints(0);
			IntBuffer height = stack.ints(0);
			IntBuffer nrChannels = stack.ints(0);

			stbi_set_flip_vertically_on_load(flipY); // Tell stb_image.h whether to flip loaded texture's on the y-axis or not.

			ByteBuffer data = stbi_load(path, width, height, nrChannels, 0);

			if(data != null) {

				int format = 0;

				switch(nrChannels.get(0)) {

				case 1:
					format = GL_RED;
					break;
				case 2:
					format = GL_RG;
					break;
				case 3:
					format = GL_RGB;
					break;
				case 4:
					format = GL_RGBA;
					break;
				default:
					logger.severe("Unexpected number of channels");
				}

				glTexImage2D(GL_TEXTURE_2D, 0, format, width.get(0), height.get(0), 0, format, GL_UNSIGNED_BYTE, data);
				glGenerateMipmap(GL_TEXTURE_2D);

			} else {
				logger.severe("Failed to load texture: " + path);
			}

			stbi_image_free(data);

		}

		return texture;
	}


	private static void processInput(long window) {
		// Close window when ESC key is pressed
		if(glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS) {
			glfwSetWindowShouldClose(window, true);
			
		}
		
		float cameraSpeed = 2.5f * deltaTime;
		
		// Bonus: if left shift key is pressed, you double the speed!
		if(glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) {
			cameraSpeed *= 2.0f;
		}
		
		if(glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) {
			cameraPos.add(cameraFront.mul(cameraSpeed, new Vector3f()));
			
		}
		
		if(glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) {
			cameraPos.sub(cameraFront.mul(cameraSpeed, new Vector3f()));
			
		}
		
		if(glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) {
			final Vector3f pos = cameraFront.cross(cameraUp, new Vector3f()).normalize();
			cameraPos.sub(pos.mul(cameraSpeed));
			
		}
		
		if(glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) {
			final Vector3f pos = cameraFront.cross(cameraUp, new Vector3f()).normalize();
			cameraPos.add(pos.mul(cameraSpeed));
		}

	}

}
