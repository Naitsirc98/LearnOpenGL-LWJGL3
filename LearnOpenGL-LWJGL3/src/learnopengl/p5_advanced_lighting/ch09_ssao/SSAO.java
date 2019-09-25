package learnopengl.p5_advanced_lighting.ch09_ssao;

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

public class SSAO {

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
		final String dir = SSAO.class.getResource(".").getFile();
		Shader shaderGeometryPass = new Shader(dir+"ssao_geometry.vs", dir+"ssao_geometry.fs");
		Shader shaderLightingPass = new Shader(dir+"ssao.vs", dir+"ssao_lighting.fs");
		Shader shaderSSAO = new Shader(dir+"ssao.vs", dir+"ssao.fs");
		Shader shaderSSAOBlur = new Shader(dir+"ssao.vs", dir+"ssao_blur.fs");

		// Cube
		final int cubeVAO = glGenVertexArrays();
		final int cubeVBO = glGenBuffers();
		setUpVertexData(cubeVAO, cubeVBO, CUBE_VERTICES);

		// Load model
		final Model nanosuit = new Model("resources/objects/nanosuit/nanosuit.obj");

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
		final int gAlbedo = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, gAlbedo);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, windowWidth, windowHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, NULL);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT2, GL_TEXTURE_2D, gAlbedo, 0);
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


		// Also create framebuffer to hold SSAO processing stage
		final int ssaoFBO = glGenFramebuffers();
		glBindFramebuffer(GL_FRAMEBUFFER, ssaoFBO);
		final int ssaoColorBuffer = glGenTextures();
		// SSAO color buffer
		glBindTexture(GL_TEXTURE_2D, ssaoColorBuffer);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, windowWidth, windowHeight, 0, GL_RGB, GL_FLOAT, NULL);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, ssaoColorBuffer, 0);
		if(glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
			throw new RuntimeException("Framebuffer is not complete");
		}
		// and blur stage
		final int ssaoBlurFBO = glGenFramebuffers();
		final int ssaoColorBufferBlur = glGenTextures();
		glBindFramebuffer(GL_FRAMEBUFFER, ssaoBlurFBO);
		glBindTexture(GL_TEXTURE_2D, ssaoColorBufferBlur);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, windowWidth, windowHeight, 0, GL_RGB, GL_FLOAT, NULL);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, ssaoColorBufferBlur, 0);
		if(glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
			throw new RuntimeException("Framebuffer is not complete");
		}
		glBindFramebuffer(GL_FRAMEBUFFER, 0);

		// Generate sample kernel
		Random rand = new Random(System.nanoTime());
		Vector3fc[] ssaoKernel = new Vector3fc[64];

		for(int i = 0;i < ssaoKernel.length;i++) {

			Vector3f sample = new Vector3f(rand.nextFloat() * 2 - 1, rand.nextFloat() * 2 - 1, rand.nextFloat());
			sample.normalize();
			sample.mul(rand.nextFloat());

			float scale = (float)i / 64.0f;

			scale = lerp(0.1f, 1.0f, scale * scale);

			sample.mul(scale);

			ssaoKernel[i] = sample;
		}

		// Generate noise texture
		final int noiseTexture = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, noiseTexture);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

		try(MemoryStack stack = MemoryStack.stackPush()) {

			FloatBuffer noiseTextureData = stack.mallocFloat(16 * 3);

			for(int i = 0;i < noiseTextureData.capacity();i+=3) {
				// Rotate around z axis
				noiseTextureData.put(i, rand.nextFloat() * 2 - 1);
				noiseTextureData.put(i+1, rand.nextFloat() * 2 - 1);
				noiseTextureData.put(i+2, 0.0f);
			}

			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB32F, 4, 4, 0, GL_RGB, GL_FLOAT, noiseTextureData);
		}

		// Lighting info
		Vector3fc lightPos = new Vector3f(2, 4, -2);
		Vector3fc lightColor = new Vector3f(0.2f, 0.2f, 0.7f);

		// Shader configuration
		shaderLightingPass.use();
		shaderLightingPass.setInt("gPosition", 0);
		shaderLightingPass.setInt("gNormal", 1);
		shaderLightingPass.setInt("gAlbedo", 2);
		shaderLightingPass.setInt("ssao", 3);
		shaderSSAO.use();
		shaderSSAO.setInt("gPosition", 0);
		shaderSSAO.setInt("gNormal", 1);
		shaderSSAO.setInt("texNoise", 2);
		// Send kernel + rotation 
		for (int i = 0; i < ssaoKernel.length; ++i) {
			shaderSSAO.setVec3("samples[" + i + "]", ssaoKernel[i]);
		}
		shaderSSAOBlur.use();
		shaderSSAOBlur.setInt("ssaoInput", 0);


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
						0.1f, 50.0f);
				updateProjection = false;
			}
			final Matrix4f view = camera.getViewMatrix();
			
	        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
	        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			// 1. Geometry pass: render scene's geometry/color data into the GBuffer
			glBindFramebuffer(GL_FRAMEBUFFER, gBuffer);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			shaderGeometryPass.use();
			shaderGeometryPass.setMat4("projection", projection);
			shaderGeometryPass.setMat4("view", view);
			// Room cube
			model.translation(0, 7, 0);
			model.scale(7.5f, 7.5f, 7.5f);
			shaderGeometryPass.setMat4("model", model);
			// Invert normals as we're inside the cube
			shaderGeometryPass.setBool("invertedNormals", true);
			renderCube(cubeVAO);
			shaderGeometryPass.setBool("invertedNormals", false);
			// Nanosuit on the floor
			model.translation(0, 0, 5);
			model.rotate((float)Math.toRadians(-90.0f), 1, 0, 0);
			model.scale(0.5f);
			shaderGeometryPass.setMat4("model", model);
			nanosuit.draw(shaderGeometryPass);

			// 2. Generate SSAO texture
			glBindFramebuffer(GL_FRAMEBUFFER, ssaoFBO);
			glClear(GL_COLOR_BUFFER_BIT);
			shaderSSAO.use();
			shaderSSAO.setMat4("projection", projection);
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, gPosition);
			glActiveTexture(GL_TEXTURE1);
			glBindTexture(GL_TEXTURE_2D, gNormal);
			glActiveTexture(GL_TEXTURE2);
			glBindTexture(GL_TEXTURE_2D, noiseTexture);
			renderQuad(quadVAO);

			// 3. Blur SSAO texture to remove noise
			glBindFramebuffer(GL_FRAMEBUFFER, ssaoBlurFBO);
			glClear(GL_COLOR_BUFFER_BIT);
			shaderSSAOBlur.use();
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, ssaoColorBuffer);
			renderQuad(quadVAO);

			// 4. Lighting pass: traditional deferred Blinn-Phong lighting with added screen-space ambient occlusion
			glBindFramebuffer(GL_FRAMEBUFFER, 0);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			shaderLightingPass.use();
			// Send light relevant uniforms
			Vector3f lightPosView = view.transformPosition(lightPos, new Vector3f());
			shaderLightingPass.setVec3("light.Position", lightPosView);
			shaderLightingPass.setVec3("light.Color", lightColor);
			// Update attenuation parameters
			final float linear = 0.09f;
			final float quadratic = 0.032f;
			shaderLightingPass.setFloat("light.Linear", linear);
			shaderLightingPass.setFloat("light.Quadratic", quadratic);
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, gPosition);
			glActiveTexture(GL_TEXTURE1);
			glBindTexture(GL_TEXTURE_2D, gNormal);
			glActiveTexture(GL_TEXTURE2);
			glBindTexture(GL_TEXTURE_2D, gAlbedo);
			glActiveTexture(GL_TEXTURE3); // Add extra SSAO texture to lighting pass
			glBindTexture(GL_TEXTURE_2D, ssaoColorBufferBlur);
			renderQuad(quadVAO);

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
		glDeleteTextures(gAlbedo);
		glDeleteTextures(ssaoColorBuffer);
		glDeleteTextures(ssaoColorBufferBlur);
		glDeleteRenderbuffers(rboDepth);
		shaderGeometryPass.delete();
		shaderSSAO.delete();
		shaderSSAOBlur.delete();
		shaderLightingPass.delete();
		glDeleteFramebuffers(gBuffer);
		glDeleteFramebuffers(ssaoFBO);
		glDeleteFramebuffers(ssaoBlurFBO);
		nanosuit.delete();

		// Clear all allocated resources by GLFW
		glfwTerminate();

	}

	private static float lerp(float a, float b, float f) {
		return a + f * (b - a);
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

		camera.movementSpeed = speed;

	}

}
