package getting_started.hello_triangle;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.nio.IntBuffer;
import java.util.logging.Logger;

import org.lwjgl.glfw.GLFWFramebufferSizeCallbackI;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;

public class HelloTriangle {

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

	// Vertex shader
	private static final String VERT_SRC = "#version 330 core\n"
			+ "layout(location = 0) in vec3 aPos;\n"
			+ "void main()\n"
			+ "{\n"
			+ "    gl_Position = vec4(aPos, 1.0f);\n"
			+ "}";

	// Fragment shader
	private static final String FRAG_SRC = "#version 330 core\n"
			+ "out vec4 FragColor;\n"
			+ "void main()\n"
			+ "{\n"
			+ "    FragColor = vec4(1.0f, 0.5f, 0.2f, 1.0f);\n"
			+ "}";

	private static final float[] VERTICES = {
			-0.5f, -0.5f, 0.0f, // left  
			0.5f, -0.5f, 0.0f, // right 
			0.0f,  0.5f, 0.0f  // top   
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

		// Create shaders
		final int vertex = createShader(GL_VERTEX_SHADER, VERT_SRC);
		final int fragment = createShader(GL_FRAGMENT_SHADER, FRAG_SRC);
		// Build and compile the shader program
		final int shaderProgram = createShaderProgram(vertex, fragment);
		// Delete shaders
		glDeleteShader(vertex);
		glDeleteShader(fragment);

		// Set up vertex data, the Vertex Buffer Object (VBO) and the Vertex Array Object (VAO)
		final int vao = glGenVertexArrays();
		final int vbo = glGenBuffers();
		setUpVertexData(vao, vbo);

		// Uncomment this call to draw in wireframe polygons
		// glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);


		// Render loop
		while(!glfwWindowShouldClose(window)) {

			// Input
			processInput(window);

			// Clear the screen
			glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
			glClear(GL_COLOR_BUFFER_BIT);

			// Draw triangle
			glUseProgram(shaderProgram);
			// Seeing as we only have a single VAO there's no need to bind it every time, but we'll do so to keep things a bit more organized
			glBindVertexArray(vao);
			glDrawArrays(GL_TRIANGLES, 0, 3);
			// glBindVertexArray(0); // No need to unbind it every time

			// Swap buffers and poll IO events (key/mouse events)
			glfwSwapBuffers(window);
			glfwPollEvents();

		}

		// Deallocate all resources when no longer necessary
		glDeleteVertexArrays(vao);
		glDeleteBuffers(vbo);
		glDeleteProgram(shaderProgram);

		// Clear all allocated resources by GLFW
		glfwTerminate();

	}

	private static int createShaderProgram(int vertexShader, int fragmentShader) {
		
		final int program = glCreateProgram();

		glAttachShader(program, vertexShader);
		glAttachShader(program, fragmentShader);

		glLinkProgram(program);

		// Check for linking errors

		try(MemoryStack stack = MemoryStack.stackPush()) {

			IntBuffer success = stack.mallocInt(1);

			glGetProgramiv(program, GL_LINK_STATUS, success);

			if(success.get(0) == GL_FALSE) {

				final String infoLog = glGetProgramInfoLog(program);

				logger.severe("Shader Linking Error: " + infoLog);				
			}

		}

		return program;
	}

	private static int createShader(int shaderType, String src) {

		final int shader = glCreateShader(shaderType);

		glShaderSource(shader, src);

		glCompileShader(shader);

		// Since in Java is not possible to pass pointers to primitive data, 
		// we need to simulate it by using LWJGL's MemoryStack class
		// It will allow us to create 'stack allocated' variables wrapped in Java NIO
		// Buffer Objects
		// Try with resources will clear all the stack allocated variables for us
		try(MemoryStack stack = MemoryStack.stackPush()) {

			IntBuffer success = stack.mallocInt(1);

			glGetShaderiv(shader, GL_COMPILE_STATUS, success);

			if(success.get(0) == GL_FALSE) {

				final String infoLog = glGetShaderInfoLog(shader);

				logger.severe("Shader Compilation failed: " + infoLog);

			}

		}

		return shader;
	}
	
	private static void setUpVertexData(int vao, int vbo) {
		// Bind the Vertex Array Object first, then bind and set vertex buffer(s), and then configure vertex attributes(s).
		glBindVertexArray(vao);

		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		glBufferData(GL_ARRAY_BUFFER, VERTICES, GL_STATIC_DRAW);

		glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, NULL);
		glEnableVertexAttribArray(0);

		// note that this is allowed, the call to glVertexAttribPointer registered VBO as the vertex attribute's bound vertex buffer object so afterwards we can safely unbind
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

	}

}
