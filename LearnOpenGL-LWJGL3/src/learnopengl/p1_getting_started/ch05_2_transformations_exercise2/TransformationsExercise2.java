package learnopengl.p1_getting_started.ch05_2_transformations_exercise2;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.logging.Logger;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWFramebufferSizeCallbackI;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;

import learnopengl.util.Shader1;

public class TransformationsExercise2 {
	
	private static Logger logger = Logger.getAnonymousLogger();

	// Callbacks
	private static final GLFWFramebufferSizeCallbackI FRAMEBUFFER_SIZE_CALLBACK = (window, width, height) -> {
		// make sure the viewport matches the new window dimensions; note that width and 
		// height will be significantly larger than specified on retina displays.
		glViewport(0, 0, width, height);
	};

	// Window size
	private static final int SRC_WIDTH = 800;
	private static final int SRC_HEIGHT = 600;


	private static final float[] VERTICES = {
	        // positions           // texture coords
	         0.5f,  0.5f, 0.0f,    1.0f, 1.0f, // top right
	         0.5f, -0.5f, 0.0f,    1.0f, 0.0f, // bottom right
	        -0.5f, -0.5f, 0.0f,    0.0f, 0.0f, // bottom left
	        -0.5f,  0.5f, 0.0f,    0.0f, 1.0f  // top left
	}; 

	private static final int[] INDICES = {
			0, 1, 3, // first triangle
			1, 2, 3  // second triangle
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

		final long window = glfwCreateWindow(SRC_WIDTH, SRC_HEIGHT, "LearnOpenGL", NULL, NULL);

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
		final String dir = TransformationsExercise2.class.getResource(".").getFile();
		Shader1 ourShader = new Shader1(dir+"ch52_transform.vs", dir+"ch52_transform.fs");

		// Set up vertex data, the Vertex Buffer Object (VBO) and the Vertex Array Object (VAO)
		final int vao = glGenVertexArrays();
		final int vbo = glGenBuffers();
		final int ebo = glGenBuffers();
		setUpVertexData(vao, vbo, ebo);

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

		// Render loop
		while(!glfwWindowShouldClose(window)) {

			// Input
			processInput(window);

			// Clear the screen
			glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
			glClear(GL_COLOR_BUFFER_BIT);

			// Bind textures
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, texture1);
			glActiveTexture(GL_TEXTURE1);
			glBindTexture(GL_TEXTURE_2D, texture2);

			ourShader.use();
			glBindVertexArray(vao);
			
			final int transformLocation = glGetUniformLocation(ourShader.id, "transform");
			
			// First container
			Matrix4f transform = new Matrix4f(); // Identity matrix
			transform.translate(0.5f, -0.5f, 0.0f);
			// JOML needs the rotation vector to be normalized
			transform.rotate((float)glfwGetTime(), new Vector3f(0.0f, 0.0f, 1.0f).normalize());
			
			try(MemoryStack stack = MemoryStack.stackPush()) {
				FloatBuffer transformData = stack.mallocFloat(16);
				// Since LWJGL needs the data in array/Buffer form, we have to
				// fill a Buffer with the matrix data
				transform.get(transformData);
				glUniformMatrix4fv(transformLocation, false, transformData);
			}
			
			glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);

			
			// Second container
			transform.translation(-0.5f, 0.5f, 0.0f); // Calling translation instead of translate cause the matrix to be treat as if it is an identity matrix
			final float scaleAmount = (float)Math.sin(glfwGetTime());
			transform.scale(scaleAmount);
			
			try(MemoryStack stack = MemoryStack.stackPush()) {
				FloatBuffer transformData = stack.mallocFloat(16);
				transform.get(transformData);
				glUniformMatrix4fv(transformLocation, false, transformData);
			}
			
			glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
			
			// Swap buffers and poll IO events (key/mouse events)
			glfwSwapBuffers(window);
			glfwPollEvents();

		}

		// Deallocate all resources when no longer necessary
		glDeleteVertexArrays(vao);
		glDeleteBuffers(vbo);
		glDeleteBuffers(ebo);
		glDeleteTextures(texture1);
		glDeleteTextures(texture2);
		ourShader.delete();

		// Clear all allocated resources by GLFW
		glfwTerminate();

	}

	private static void setUpVertexData(int vao, int vbo, int ebo) {
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


		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, INDICES, GL_STATIC_DRAW);

		// Note that this is allowed, the call to glVertexAttribPointer registered VBO as the vertex attribute's bound vertex buffer object so afterwards we can safely unbind
		glBindBuffer(GL_ARRAY_BUFFER, 0); 

		// Remember: do NOT unbind the EBO while a VAO is active as the bound element buffer object IS stored in the VAO; keep the EBO bound.
		// glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

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

	}

}
