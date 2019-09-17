package learnopengl.p5_advanced_lighting.ch08_1_deferred_shading;

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
import java.nio.IntBuffer;
import java.util.logging.Logger;

import org.joml.Matrix4f;
import org.joml.Random;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.lwjgl.glfw.GLFWFramebufferSizeCallbackI;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;

import learnopengl.util.Camera;
import learnopengl.util.Camera.CameraMovement;
import learnopengl.util.Model;
import learnopengl.util.Shader;

public class DeferredShading {

	private static Logger logger = Logger.getAnonymousLogger();

	private static boolean updateProjection = true;

	// Window size
	private static int windowWidth = 1280;
	private static int windowHeight = 720;

	// Camera
	private static Camera camera = new Camera();
	static {
		camera.position.z = 5.0f;
	}
	private static float lastX = (float)windowWidth / 2.0f;
	private static float lastY = (float)windowHeight / 2.0f;
	private static boolean firstMouseInput = true;

	// Timing
	private static float deltaTime = 0.0f; // Time between current frame and last frame
	private static float lastFrame = 0.0f;

	// HDR
	private static boolean bloom = true;
	private static boolean bloomKeyPressed = false;
	private static float exposure = 1.0f;

	// Vertex Data
	private static final float[] CUBE_VERTICES = {
			// back face
			-1.0f, -1.0f, -1.0f,  0.0f,  0.0f, -1.0f, 0.0f, 0.0f, // bottom-left
			1.0f,  1.0f, -1.0f,  0.0f,  0.0f, -1.0f, 1.0f, 1.0f, // top-right
			1.0f, -1.0f, -1.0f,  0.0f,  0.0f, -1.0f, 1.0f, 0.0f, // bottom-right         
			1.0f,  1.0f, -1.0f,  0.0f,  0.0f, -1.0f, 1.0f, 1.0f, // top-right
			-1.0f, -1.0f, -1.0f,  0.0f,  0.0f, -1.0f, 0.0f, 0.0f, // bottom-left
			-1.0f,  1.0f, -1.0f,  0.0f,  0.0f, -1.0f, 0.0f, 1.0f, // top-left
			// front face
			-1.0f, -1.0f,  1.0f,  0.0f,  0.0f,  1.0f, 0.0f, 0.0f, // bottom-left
			1.0f, -1.0f,  1.0f,  0.0f,  0.0f,  1.0f, 1.0f, 0.0f, // bottom-right
			1.0f,  1.0f,  1.0f,  0.0f,  0.0f,  1.0f, 1.0f, 1.0f, // top-right
			1.0f,  1.0f,  1.0f,  0.0f,  0.0f,  1.0f, 1.0f, 1.0f, // top-right
			-1.0f,  1.0f,  1.0f,  0.0f,  0.0f,  1.0f, 0.0f, 1.0f, // top-left
			-1.0f, -1.0f,  1.0f,  0.0f,  0.0f,  1.0f, 0.0f, 0.0f, // bottom-left
			// left face
			-1.0f,  1.0f,  1.0f, -1.0f,  0.0f,  0.0f, 1.0f, 0.0f, // top-right
			-1.0f,  1.0f, -1.0f, -1.0f,  0.0f,  0.0f, 1.0f, 1.0f, // top-left
			-1.0f, -1.0f, -1.0f, -1.0f,  0.0f,  0.0f, 0.0f, 1.0f, // bottom-left
			-1.0f, -1.0f, -1.0f, -1.0f,  0.0f,  0.0f, 0.0f, 1.0f, // bottom-left
			-1.0f, -1.0f,  1.0f, -1.0f,  0.0f,  0.0f, 0.0f, 0.0f, // bottom-right
			-1.0f,  1.0f,  1.0f, -1.0f,  0.0f,  0.0f, 1.0f, 0.0f, // top-right
			// right face
			1.0f,  1.0f,  1.0f,  1.0f,  0.0f,  0.0f, 1.0f, 0.0f, // top-left
			1.0f, -1.0f, -1.0f,  1.0f,  0.0f,  0.0f, 0.0f, 1.0f, // bottom-right
			1.0f,  1.0f, -1.0f,  1.0f,  0.0f,  0.0f, 1.0f, 1.0f, // top-right         
			1.0f, -1.0f, -1.0f,  1.0f,  0.0f,  0.0f, 0.0f, 1.0f, // bottom-right
			1.0f,  1.0f,  1.0f,  1.0f,  0.0f,  0.0f, 1.0f, 0.0f, // top-left
			1.0f, -1.0f,  1.0f,  1.0f,  0.0f,  0.0f, 0.0f, 0.0f, // bottom-left     
			// bottom face
			-1.0f, -1.0f, -1.0f,  0.0f, -1.0f,  0.0f, 0.0f, 1.0f, // top-right
			1.0f, -1.0f, -1.0f,  0.0f, -1.0f,  0.0f, 1.0f, 1.0f, // top-left
			1.0f, -1.0f,  1.0f,  0.0f, -1.0f,  0.0f, 1.0f, 0.0f, // bottom-left
			1.0f, -1.0f,  1.0f,  0.0f, -1.0f,  0.0f, 1.0f, 0.0f, // bottom-left
			-1.0f, -1.0f,  1.0f,  0.0f, -1.0f,  0.0f, 0.0f, 0.0f, // bottom-right
			-1.0f, -1.0f, -1.0f,  0.0f, -1.0f,  0.0f, 0.0f, 1.0f, // top-right
			// top face
			-1.0f,  1.0f, -1.0f,  0.0f,  1.0f,  0.0f, 0.0f, 1.0f, // top-left
			1.0f,  1.0f , 1.0f,  0.0f,  1.0f,  0.0f, 1.0f, 0.0f, // bottom-right
			1.0f,  1.0f, -1.0f,  0.0f,  1.0f,  0.0f, 1.0f, 1.0f, // top-right     
			1.0f,  1.0f,  1.0f,  0.0f,  1.0f,  0.0f, 1.0f, 0.0f, // bottom-right
			-1.0f,  1.0f, -1.0f,  0.0f,  1.0f,  0.0f, 0.0f, 1.0f, // top-left
			-1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f // bottom-left        
	};


	private static final float[] QUAD_VERTICES = {
			// positions        // texture Coords
			-1.0f,  1.0f, 0.0f, 0.0f, 1.0f,
			-1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
			1.0f,  1.0f, 0.0f, 1.0f, 1.0f,
			1.0f, -1.0f, 0.0f, 1.0f, 0.0f,
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
		final String dir = DeferredShading.class.getResource(".").getFile();
		Shader shaderGeometryPass = new Shader(dir+"g_buffer.vs", dir+"g_buffer.fs");
		Shader shaderLightingPass = new Shader(dir+"deferred_shading.vs", dir+"deferred_shading.fs");
		Shader shaderLightBox = new Shader(dir+"deferred_light_box.vs", dir+"deferred_light_box.fs");

		// Load models
		final Model nanosuit = new Model("resources/objects/nanosuit/nanosuit.obj");

		final Vector3fc[] objectPositions = {
				new Vector3f(-3.0f,  -3.0f, -3.0f),
				new Vector3f( 0.0f,  -3.0f, -3.0f),
				new Vector3f( 3.0f,  -3.0f, -3.0f),
				new Vector3f(-3.0f,  -3.0f,  0.0f),
				new Vector3f( 0.0f,  -3.0f,  0.0f),
				new Vector3f( 3.0f,  -3.0f,  0.0f),
				new Vector3f(-3.0f,  -3.0f,  3.0f),
				new Vector3f( 0.0f,  -3.0f,  3.0f),
				new Vector3f( 3.0f,  -3.0f,  3.0f)
		};
		
		// Cube
		final int cubeVAO = glGenVertexArrays();
		final int cubeVBO = glGenBuffers();
		setUpVertexData(cubeVAO, cubeVBO, CUBE_VERTICES);

		// Quad
		final int quadVAO = glGenVertexArrays();
		final int quadVBO = glGenBuffers();
		glBindVertexArray(quadVAO);
		glBindBuffer(GL_ARRAY_BUFFER, quadVBO);
		glBufferData(GL_ARRAY_BUFFER, QUAD_VERTICES, GL_STATIC_DRAW);
		// Position
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0L);
		// Texture coords
		glEnableVertexAttribArray(1);
		glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);

		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindVertexArray(0);


		// Configure GBuffer Framebuffer
		final int gBuffer = glGenFramebuffers();
		glBindFramebuffer(GL_FRAMEBUFFER, gBuffer);
		// Position color buffer
		final int gPosition = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, gPosition);
		nglTexImage2D(GL_TEXTURE_2D, 0, GL_RGB16F, windowWidth, windowHeight, 0, GL_RGB, GL_FLOAT, NULL);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, gPosition, 0);
		// Normal color buffer
		final int gNormal = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, gNormal);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB16F, windowWidth, windowHeight, 0, GL_RGB, GL_FLOAT, NULL);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, gNormal, 0);
		// Color + specular color buffer
		final int gAlbedoSpec = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, gAlbedoSpec);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, windowWidth, windowHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, NULL);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT2, GL_TEXTURE_2D, gAlbedoSpec, 0);

		// Tell OpenGL which color attachments we'll use (of this framebuffer) for rendering 
		final int[] attachments = {GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1, GL_COLOR_ATTACHMENT2};
		glDrawBuffers(attachments);

		// Create and attach depth buffer (renderbuffer)
		final int rboDepth = glGenRenderbuffers();
		glBindRenderbuffer(GL_RENDERBUFFER, rboDepth);
		glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, windowWidth, windowHeight);
		glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, rboDepth);

		// Finally check if Framebuffer is complete
		if(glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
			throw new RuntimeException("Framebuffer is not complete");
		}
		glBindFramebuffer(GL_FRAMEBUFFER, 0);

		// Lighting info
		final int numberOfLights = 32;
		Vector3fc[] lightPositions = new Vector3fc[numberOfLights];
		Vector3fc[] lightColors = new Vector3fc[numberOfLights];
		Random rand = new Random(13);

		for(int i = 0;i < numberOfLights;i++) {
			// Calculate slightly random offsets
			final float x = rand.nextFloat() * 6 - 3;
			final float y = rand.nextFloat() * 6 - 4;
			final float z = rand.nextFloat() * 6 - 3;
			lightPositions[i] = new Vector3f(x, y, z);
			// Also calculate a random color (values between 0.5 and 1.0)
			final float red = rand.nextFloat() / 2 + 0.5f;
			final float green = rand.nextFloat() / 2 + 0.5f;
			final float blue = rand.nextFloat() / 2 + 0.5f;
			lightColors[i] = new Vector3f(red, green, blue); 
		}


		// Shader configuration
		shaderLightingPass.use();
		shaderLightingPass.setInt("gPosition", 0);
		shaderLightingPass.setInt("gNormal", 1);
		shaderLightingPass.setInt("gAlbedoSpec", 2);
		// Send light relevant uniforms
		for(int i = 0;i < lightPositions.length;i++) {
			shaderLightingPass.setVec3("lights["+i+"].Position", lightPositions[i]);
			shaderLightingPass.setVec3("lights["+i+"].Color", lightColors[i]);
			// Note that we don't send this to the shader, we assume it is always 1.0 (in our case)
			// shaderLightingPass.setFloat("lights["+i+"].Constant", 1.0f);
			shaderLightingPass.setFloat("lights["+i+"].Linear", 0.7f);
			shaderLightingPass.setFloat("lights["+i+"].Quadratic", 1.8f);
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

			if(updateProjection) {
				projection.setPerspective((float)Math.toRadians(camera.zoom), (float)windowWidth / (float)windowHeight, 
						0.1f, 100.0f);
				updateProjection = false;
			}
			final Matrix4f view = camera.getViewMatrix();

			// 1. Geometry pass: render scene's geometry/color data into the GBuffer
			glBindFramebuffer(GL_FRAMEBUFFER, gBuffer);

			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			shaderGeometryPass.use();
			shaderGeometryPass.setMat4("projection", projection);
			shaderGeometryPass.setMat4("view", view);
			for(int i = 0;i < objectPositions.length;i++) {
				model.translation(objectPositions[i]);
				model.scale(0.25f);
				shaderGeometryPass.setMat4("model", model);
				nanosuit.draw(shaderGeometryPass);
			}

			glBindFramebuffer(GL_FRAMEBUFFER, 0);

			// 2. Lighting pass: calculate lighting by iterating over a screen filled quad pixel-by-pixel using the GBuffer's content
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			shaderLightingPass.use();
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, gPosition);
			glActiveTexture(GL_TEXTURE1);
			glBindTexture(GL_TEXTURE_2D, gNormal);
			glActiveTexture(GL_TEXTURE2);
			glBindTexture(GL_TEXTURE_2D, gAlbedoSpec);
			shaderLightingPass.setVec3("viewPos", camera.position);
			// Finally render quad
			renderQuad(quadVAO);

			// 2.5. Copy content of geometry's depth buffer to default framebuffer's depth buffer
			glBindFramebuffer(GL_READ_FRAMEBUFFER, gBuffer);
			glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
			glBlitFramebuffer(0, 0, windowWidth, windowHeight, 0, 0, windowWidth, windowHeight, GL_DEPTH_BUFFER_BIT, GL_NEAREST);
			glBindFramebuffer(GL_FRAMEBUFFER, 0);

			// 3. Render lights on top of scene
			shaderLightBox.use();
			shaderLightBox.setMat4("projection", projection);
			shaderLightBox.setMat4("view", view);
			for(int i = 0;i < lightPositions.length;i++) {
				model.translation(lightPositions[i]);
				model.scale(0.125f);
				shaderLightBox.setMat4("model", model);
				shaderLightBox.setVec3("lightColor", lightColors[i]);
				renderCube(cubeVAO);
			}

			// Swap buffers and poll IO events (key/mouse events)
			glfwSwapBuffers(window);
			glfwPollEvents();

		}

		// Deallocate all resources when no longer necessary
		glDeleteVertexArrays(cubeVAO);
		glDeleteVertexArrays(quadVAO);
		glDeleteBuffers(cubeVBO);
		glDeleteBuffers(quadVAO);
		glDeleteTextures(gPosition);
		glDeleteTextures(gNormal);
		glDeleteTextures(gAlbedoSpec);
		glDeleteRenderbuffers(rboDepth);
		shaderGeometryPass.delete();
		shaderLightBox.delete();
		shaderLightingPass.delete();
		glDeleteFramebuffers(gBuffer);
		nanosuit.delete();

		// Clear all allocated resources by GLFW
		glfwTerminate();

	}

	private static void renderQuad(int quadVAO) {
		glBindVertexArray(quadVAO);
		glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
		glBindVertexArray(0);
	}

	private static void renderCube(int cubeVAO) {
		glBindVertexArray(cubeVAO);
		glDrawArrays(GL_TRIANGLES, 0, 36);
		glBindVertexArray(0);
	}

	private static void setUpVertexData(int vao, int vbo, float[] vertexData) {
		// Bind the Vertex Array Object first, then bind and set vertex buffer(s), and then configure vertex attributes(s).
		glBindVertexArray(vao);

		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		glBufferData(GL_ARRAY_BUFFER, vertexData, GL_STATIC_DRAW);

		// Position
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 8 * Float.BYTES, 0L);
		glEnableVertexAttribArray(0);
		// Normal
		glVertexAttribPointer(1, 3, GL_FLOAT, false, 8 * Float.BYTES, 3 * Float.BYTES);
		glEnableVertexAttribArray(1);
		// Texture coords
		glVertexAttribPointer(2, 2, GL_FLOAT, false, 8 * Float.BYTES, 6 * Float.BYTES);
		glEnableVertexAttribArray(2);

		// Note that this is allowed, the call to glVertexAttribPointer registered VBO as the vertex attribute's bound vertex buffer object so afterwards we can safely unbind
		glBindBuffer(GL_ARRAY_BUFFER, 0); 

		// You can unbind the VAO afterwards so other VAO calls won't accidentally modify this VAO, but this rarely happens. Modifying other
		// VAOs requires a call to glBindVertexArray anyways so we generally don't unbind VAOs (nor VBOs) when it's not directly necessary.
		glBindVertexArray(0); 
	}

	private static int loadTexture(String path, boolean gammaCorrection) {

		final int texture = glGenTextures();

		glBindTexture(GL_TEXTURE_2D, texture);

		// Load image, create texture and generate mipmaps
		try(MemoryStack stack = MemoryStack.stackPush()) {

			IntBuffer width = stack.ints(0);
			IntBuffer height = stack.ints(0);
			IntBuffer nrChannels = stack.ints(0);

			ByteBuffer data = stbi_load(path, width, height, nrChannels, 0);

			if(data != null) {

				int internalFormat = 0;
				int dataFormat = 0;

				switch(nrChannels.get(0)) {

				case 1:
					internalFormat = dataFormat = GL_RED;
					break;
				case 2:
					internalFormat = dataFormat = GL_RG;
					break;
				case 3:
					internalFormat = gammaCorrection ? GL_SRGB : GL_RGB;
					dataFormat = GL_RGB;
					break;
				case 4:
					internalFormat = gammaCorrection ? GL_SRGB_ALPHA : GL_RGBA;
					dataFormat = GL_RGBA;
					break;
				default:
					logger.severe("Unexpected number of channels");
				}

				glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width.get(0), height.get(0), 0, dataFormat, GL_UNSIGNED_BYTE, data);
				glGenerateMipmap(GL_TEXTURE_2D);

				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

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

		if(glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS && !bloomKeyPressed) {
			bloom = !bloom;
			bloomKeyPressed = true;

		}else if(glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_RELEASE) {
			bloomKeyPressed = false;
		}

		if(glfwGetKey(window, GLFW_KEY_Q) == GLFW_PRESS) {
			if(exposure > 0.0f) {
				// exposure -= 0.001f;
				exposure -= 0.01f;
			} else {
				exposure = 0.0f;
			}

		} else if(glfwGetKey(window, GLFW_KEY_E) == GLFW_PRESS) {
			// exposure += 0.001f;
			exposure += 0.01f;
		}

		camera.movementSpeed = speed;

	}

}
