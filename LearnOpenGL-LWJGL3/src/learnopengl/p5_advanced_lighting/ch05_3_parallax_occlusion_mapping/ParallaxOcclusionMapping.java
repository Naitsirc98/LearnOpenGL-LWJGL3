package learnopengl.p5_advanced_lighting.ch05_3_parallax_occlusion_mapping;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.logging.Logger;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector2fc;
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
import learnopengl.util.Shader2;

public class ParallaxOcclusionMapping {

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

	// Parallax Mapping
	private static float heightScale = 0.1f;


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

		// Build and compile our shader programs
		final String dir = ParallaxOcclusionMapping.class.getResource(".").getFile();
		Shader2 shader = new Shader2(dir+"parallax_mapping.vs", dir+"parallax_mapping.fs");

		final int quadVAO = glGenVertexArrays();
		final int quadVBO = glGenBuffers();
		setUpQuadVertexData(quadVAO, quadVBO);

		// Load textures
		final int diffuseMap = loadTexture("resources/textures/bricks2.jpg");
		final int normalMap = loadTexture("resources/textures/bricks2_normal.jpg");
		final int heightMap = loadTexture("resources/textures/bricks2_disp.jpg");
		// final int diffuseMap = loadTexture("resources/textures/toy_box_diffuse.png");
		// final int normalMap = loadTexture("resources/textures/toy_box_normal.png");
		// final int heightMap = loadTexture("resources/textures/toy_box_disp.png");

		shader.use();
		shader.setInt("diffuseMap", 0);
		shader.setInt("normalMap", 1);
		shader.setInt("depthMap", 2);

		// Lighting info
		final Vector3f lightPos = new Vector3f(0.5f, 1.0f, 0.3f);

		// "Pass projection matrix to shader (as projection matrix rarely changes there's no need to do this per frame)"
		// ** This is true as long as you don't change the window size!
		// That's why I check every frame if the projection matrix has to be changed
		Matrix4f projection = new Matrix4f();

		// Create the model matrix before enter the loop to avoid calling new every frame
		Matrix4f model = new Matrix4f();

		Vector3f rotationVector = new Vector3f(1.0f, 0.0f, 1.0f).normalize();

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
			final Matrix4f view = camera.getViewMatrix();
			shader.setMat4("view", view);

			// Render normal mapped quad
			model.rotation((float)Math.toRadians(glfwGetTime() * -10.0), rotationVector);
			shader.setMat4("model", model);
			shader.setVec3("viewPos", camera.position);
			shader.setVec3("lightPos", lightPos);
			shader.setFloat("heightScale", heightScale); // Adjust with Q and E keys
			System.out.println(heightScale);
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, diffuseMap);
			glActiveTexture(GL_TEXTURE1);
			glBindTexture(GL_TEXTURE_2D, normalMap);
			glActiveTexture(GL_TEXTURE2);
			glBindTexture(GL_TEXTURE_2D, heightMap);
			renderQuad(quadVAO);

			// Render light source (simply re-renders a smaller plane at the light's position for debugging/visualization)
			model.translation(lightPos);
			model.scale(0.1f);
			shader.setMat4("model", model);
			renderQuad(quadVAO);

			// Swap buffers and poll IO events (key/mouse events)
			glfwSwapBuffers(window);
			glfwPollEvents();

		}

		// Deallocate all resources when no longer necessary
		glDeleteVertexArrays(quadVAO);
		glDeleteBuffers(quadVBO);
		glDeleteTextures(diffuseMap);
		glDeleteTextures(normalMap);
		glDeleteTextures(heightMap);
		shader.delete();

		// Clear all allocated resources by GLFW
		glfwTerminate();

	}

	/**
	 * Renders a 1x1 quad in NDC with manually calculated tangent vectors
	 * 
	 * */
	private static void renderQuad(int quadVAO) {
		glBindVertexArray(quadVAO);
		glDrawArrays(GL_TRIANGLES, 0, 6);
		glBindVertexArray(0);
	}

	private static int loadTexture(String path) {

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

	private static void setUpQuadVertexData(int vao, int vbo) {

		float[] quadVertices = getQuadVertices();

		// Configure quad VAO
		glBindVertexArray(vao);
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		glBufferData(GL_ARRAY_BUFFER, quadVertices, GL_STATIC_DRAW);
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 14 * 4, 0L);
		glEnableVertexAttribArray(1);
		glVertexAttribPointer(1, 3, GL_FLOAT, false, 14 * Float.BYTES, (3 * Float.BYTES));
		glEnableVertexAttribArray(2);
		glVertexAttribPointer(2, 2, GL_FLOAT, false, 14 * Float.BYTES, (6 * Float.BYTES));
		glEnableVertexAttribArray(3);
		glVertexAttribPointer(3, 3, GL_FLOAT, false, 14 * Float.BYTES, (8 * Float.BYTES));
		glEnableVertexAttribArray(4);
		glVertexAttribPointer(4, 3, GL_FLOAT, false, 14 * Float.BYTES, (11 * Float.BYTES));

	}

	private static float[] getQuadVertices() {

		float[] quadVertices = new float[84];

		// Positions
		Vector3fc pos1 = new Vector3f(-1.0f, 1.0f, 0.0f);
		Vector3fc pos2 = new Vector3f(-1.0f, -1.0f, 0.0f);
		Vector3fc pos3 = new Vector3f(1.0f, -1.0f, 0.0f);
		Vector3fc pos4 = new Vector3f(1.0f, 1.0f, 0.0f);
		// Texture coordinates
		Vector2fc uv1 = new Vector2f(0.0f, 1.0f);
		Vector2fc uv2 = new Vector2f(0.0f, 0.0f);
		Vector2fc uv3 = new Vector2f(1.0f, 0.0f); 
		Vector2fc uv4 = new Vector2f(1.0f, 1.0f);
		// Normal vector
		Vector3fc normal = new Vector3f(0, 0, 1);

		// Calculate tangent/bitangent vectors of both triangles
		Vector3f tangent = new Vector3f();
		Vector3f bitangent = new Vector3f();
		Vector3f edge1 = new Vector3f();
		Vector3f edge2 = new Vector3f();
		Vector2f deltaUV1 = new Vector2f();
		Vector2f deltaUV2 = new Vector2f();

		// Triangle 1
		edge1 = pos2.sub(pos1, edge1);
		edge2 = pos3.sub(pos1, edge2);
		deltaUV1 = uv2.sub(uv1, deltaUV1);
		deltaUV2 = uv3.sub(uv1, deltaUV2);

		float f = 1.0f / (deltaUV1.x * deltaUV2.y - deltaUV2.x * deltaUV1.y); // Matrix 2x2 determinant

		tangent = setTangent(tangent, f, edge1, edge2, deltaUV1, deltaUV2);
		bitangent = setBitangent(bitangent, f, edge1, edge2, deltaUV1, deltaUV2);

		// Set triangle 1 vertices
		addVertex(quadVertices, 0, pos1, normal, uv1, tangent, bitangent);
		addVertex(quadVertices, 1, pos2, normal, uv2, tangent, bitangent);
		addVertex(quadVertices, 2, pos3, normal, uv3, tangent, bitangent);

		// Triangle 2
		edge1 = pos3.sub(pos1, edge1);
		edge2 = pos4.sub(pos1, edge2);
		deltaUV1 = uv3.sub(uv1, deltaUV1);
		deltaUV2 = uv4.sub(uv1, deltaUV2);

		f = 1.0f / (deltaUV1.x * deltaUV2.y - deltaUV2.x * deltaUV1.y);

		tangent = setTangent(tangent, f, edge1, edge2, deltaUV1, deltaUV2);
		bitangent = setBitangent(bitangent, f, edge1, edge2, deltaUV1, deltaUV2);

		// Set triangle 2 vertices
		addVertex(quadVertices, 3, pos1, normal, uv1, tangent, bitangent);
		addVertex(quadVertices, 4, pos3, normal, uv3, tangent, bitangent);
		addVertex(quadVertices, 5, pos4, normal, uv4, tangent, bitangent);

		return quadVertices;
	}

	private static Vector3f setTangent(Vector3f tangent, float f, Vector3fc edge1, Vector3fc edge2, 
			Vector2fc deltaUV1, Vector2fc deltaUV2) {

		tangent.x = f * (deltaUV2.y() * edge1.x() - deltaUV1.y() * edge2.x());
		tangent.y = f * (deltaUV2.y() * edge1.y() - deltaUV1.y() * edge2.y());
		tangent.z = f * (deltaUV2.y() * edge1.z() - deltaUV1.y() * edge2.z());

		return tangent.normalize();
	}

	private static Vector3f setBitangent(Vector3f bitangent, float f, Vector3fc edge1, Vector3fc edge2, 
			Vector2fc deltaUV1, Vector2fc deltaUV2) {

		bitangent.x = f * (-deltaUV2.x() * edge1.x() + deltaUV1.x() * edge2.x());
		bitangent.y = f * (-deltaUV2.x() * edge1.y() + deltaUV1.x() * edge2.y());
		bitangent.z = f * (-deltaUV2.x() * edge1.z() + deltaUV1.x() * edge2.z());

		return bitangent.normalize();
	}

	private static void addVertex(float[] vertices, int index, Vector3fc pos, Vector3fc normal, 
			Vector2fc uv, Vector3fc tangent, Vector3fc bitangent) {

		final int offset = index * 14;

		vertices[offset +0] = pos.x();
		vertices[offset +1] = pos.y();
		vertices[offset +2] = pos.z();

		vertices[offset +3] = normal.x();
		vertices[offset +4] = normal.y();
		vertices[offset +5] = normal.z();

		vertices[offset +6] = uv.x();
		vertices[offset +7] = uv.y();

		vertices[offset +8] = tangent.x();
		vertices[offset +9] = tangent.y();
		vertices[offset+10] = tangent.z();

		vertices[offset+11] = bitangent.x();
		vertices[offset+12] = bitangent.y();
		vertices[offset+13] = bitangent.z();
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

		if(glfwGetKey(window, GLFW_KEY_Q) == GLFW_PRESS) {
			if(heightScale > 0.0f) {
				heightScale -= 0.0005f;
			} else {
				heightScale = 0.0f;
			}

		} else if(glfwGetKey(window, GLFW_KEY_E) == GLFW_PRESS) {
			if(heightScale < 1.0f) {
				heightScale += 0.0005f;
			} else {
				heightScale = 1.0f;
			}
		}

		camera.movementSpeed = speed;
	}

}
