package learnopengl.p6_pbr.ch02_1_ibl_irradiance_conversion;

import static java.lang.Math.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
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
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
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
import learnopengl.util.Shader;

public class IBLIrradianceConversion {

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
		// Set depth function to less than AND equal for skybox depth trick
		glDepthFunc(GL_LEQUAL);

		// Build and compile our shader program
		final String dir = IBLIrradianceConversion.class.getResource(".").getFile();
		Shader pbrShader = new Shader(dir+"pbr.vs", dir+"pbr.fs");
		Shader backgroundShader = new Shader(dir+"background.vs", dir+"background.fs");

		pbrShader.use();
		pbrShader.setVec3("albedo", 0.5f, 0.0f, 0.0f);
		pbrShader.setFloat("ao", 1.0f);

		backgroundShader.use();
		backgroundShader.setInt("environmentMap", 0);

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
		
		// Create Cube
		final int cubeVAO = glGenVertexArrays();
		final int cubeVBO = glGenBuffers();
		setUpVertexData(cubeVAO, cubeVBO, CUBE_VERTICES);

		final int environmentMap = createEnvironmentMap(cubeVAO);
		
		// Configure the viewport to the original framebuffer's screen dimensions before rendering
		 glViewport(0, 0, windowWidth, windowHeight);

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

			pbrShader.use();
			pbrShader.setMat4("projection", projection);
			pbrShader.setMat4("view", view);
			pbrShader.setVec3("camPos", camera.position);

			// Render rows * columns number of spheres with varying matallic/roughness values scaled by rows and columns respectively
			for(int row = 0;row < nRows;row++) {

				pbrShader.setFloat("metallic", (float)row/(float)nRows);

				for(int col = 0;col < nColumns;col++) {

					// We clamp the roughness to 0.025 - 1.0 as perfectly smooth surfaces (roughness of 0.0) tend to look a bit off
					// on direct lighting.
					pbrShader.setFloat("roughness", min(max((float)col/(float)nColumns, 0.05f), 1.0f));

					model.translation(
							(col - (nColumns / 2)) * spacing, 
							(row - (nRows / 2)) * spacing, 
							0.0f);
					pbrShader.setMat4("model", model);

					renderSphere(sphereVAO, indicesCount);
				}

			}

			// Render light source (simply re-render sphere at light positions)
			// This looks a bit off as we use the same shader, but it'll make their positions obvious and
			// keeps the codeprint small
			for(int i = 0;i < lightPositions.length;i++) {

				// Vector3f newPos = lightPositions[i].add((float)sin(glfwGetTime()*5)*5, 0, 0, new Vector3f());
				Vector3fc newPos = lightPositions[i];

				pbrShader.setVec3("lightPositions[" + i + "]", newPos);
				pbrShader.setVec3("lightColors["+i+"]", lightColor);

				model.translation(newPos);
				model.scale(0.5f);
				pbrShader.setMat4("model", model);
				renderSphere(sphereVAO, indicesCount);
			}
			
	        // Render skybox (render as last to prevent overdraw)
	        backgroundShader.use();
	        backgroundShader.setMat4("projection", projection);
	        backgroundShader.setMat4("view", view);
	        glActiveTexture(GL_TEXTURE0);
	        glBindTexture(GL_TEXTURE_CUBE_MAP, environmentMap);
	        renderCube(cubeVAO);

			// Swap buffers and poll IO events (key/mouse events)
			glfwSwapBuffers(window);
			glfwPollEvents();

		}

		// Deallocate all resources when no longer necessary
		glDeleteVertexArrays(sphereVAO);
		glDeleteVertexArrays(cubeVAO);
		glDeleteBuffers(cubeVBO);
		glDeleteBuffers(sphereVBO);
		glDeleteBuffers(sphereEBO);
		glDeleteTextures(environmentMap);
		pbrShader.delete();
		backgroundShader.delete();

		// Clear all allocated resources by GLFW
		glfwTerminate();

	}

	private static int createEnvironmentMap(int cubeVAO) {
		
		final String dir = IBLIrradianceConversion.class.getResource(".").getFile();
		final Shader equirectToCubemapShader = new Shader(dir+"cubemap.vs", dir+"equirectangular_to_cubemap.fs");

		final int captureFBO = glGenFramebuffers();
		final int captureRBO = glGenRenderbuffers();
		setUpCaptureFramebuffer(captureFBO, captureRBO);

		final int hdrTexture = loadHDRTexture("resources/textures/hdr/newport_loft.hdr");

		final int environmentMap = createEnvironmentCubemap(hdrTexture);

		bakeEnvironmentalMap(environmentMap, hdrTexture, captureFBO, equirectToCubemapShader, cubeVAO);

		equirectToCubemapShader.delete();
		glDeleteFramebuffers(captureFBO);
		glDeleteRenderbuffers(captureRBO);
		glDeleteTextures(hdrTexture);
		
		return environmentMap;
	}

	private static void bakeEnvironmentalMap(int environmentMap, int hdrTexture, int captureFBO, Shader shader, int cubeVAO) {

		// Set up projection and view matrices for capturing data onto the 6 cubemap face directions
		final Matrix4fc captureProj = new Matrix4f().perspective((float)Math.toRadians(90), 1.0f, 0.1f, 10.0f);

		final Matrix4fc[] captureViews = {
				new Matrix4f().lookAt(0.0f, 0.0f, 0.0f, 1.0f,  0.0f,  0.0f, 0.0f, -1.0f,  0.0f),
				new Matrix4f().lookAt(0.0f, 0.0f, 0.0f,-1.0f,  0.0f,  0.0f, 0.0f, -1.0f,  0.0f),
				new Matrix4f().lookAt(0.0f, 0.0f, 0.0f, 0.0f,  1.0f,  0.0f, 0.0f,  0.0f,  1.0f),
				new Matrix4f().lookAt(0.0f, 0.0f, 0.0f, 0.0f, -1.0f,  0.0f, 0.0f,  0.0f, -1.0f),
				new Matrix4f().lookAt(0.0f, 0.0f, 0.0f, 0.0f,  0.0f,  1.0f, 0.0f, -1.0f,  0.0f),
				new Matrix4f().lookAt(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f,  0.0f, -1.0f, 0.0f)
		};

		// Convert HDR equirectangular environment map to cubemap equivalent
		shader.use();
		shader.setInt("equirectangularMap", 0);
		shader.setMat4("projection", captureProj);
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, hdrTexture);

		glViewport(0, 0, 512, 512); // Don't forget to configure the viewport to the capture dimensions.
		glBindFramebuffer(GL_FRAMEBUFFER, captureFBO);
		for(int i = 0;i < 6;i++)
		{
			shader.setMat4("view", captureViews[i]);
			glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, environmentMap, 0);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			renderCube(cubeVAO);
		}
		glBindFramebuffer(GL_FRAMEBUFFER, 0);

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

	private static int createEnvironmentCubemap(int hdrTexture) {

		final int envCubemap = glGenTextures();

		glBindTexture(GL_TEXTURE_CUBE_MAP, envCubemap);

		for(int i = 0;i < 6;i++){
			nglTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL_RGB16F, 512, 512, 0, GL_RGB, GL_FLOAT, NULL);
		}

		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR); 
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

		return envCubemap;
	}

	private static int loadHDRTexture(String filename) {

		final int texture = glGenTextures();

		try(MemoryStack stack = MemoryStack.stackPush()) {

			IntBuffer width = stack.mallocInt(1);
			IntBuffer height = stack.mallocInt(1);
			IntBuffer channels = stack.mallocInt(1);

			stbi_set_flip_vertically_on_load(true);
			
			FloatBuffer data = stbi_loadf(filename, width, height, channels, 0);

			if(data == null) {
				throw new RuntimeException("Could not load image: " + filename);
			}

			glBindTexture(GL_TEXTURE_2D, texture);
			// Note how we specify the texture's data value to be float
			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB16F, width.get(0), height.get(0), 0, GL_RGB, GL_FLOAT, data); 
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

			stbi_image_free(data);
		}

		return texture;
	}

	private static void setUpCaptureFramebuffer(int captureFBO, int captureRBO) {
		glBindFramebuffer(GL_FRAMEBUFFER, captureFBO);
		glBindRenderbuffer(GL_RENDERBUFFER, captureRBO);
		glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, 512, 512);
		glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, captureRBO);
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


}
