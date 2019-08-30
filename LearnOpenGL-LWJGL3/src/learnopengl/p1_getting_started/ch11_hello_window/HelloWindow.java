package learnopengl.p1_getting_started.ch11_hello_window;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.util.logging.Logger;

import org.lwjgl.glfw.GLFWFramebufferSizeCallbackI;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.Platform;

public class HelloWindow {
	
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
		// With LWJGL we use long instead of GLFWwindow*
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
	    
	    // Render loop
	    while(!glfwWindowShouldClose(window)) {
	    	
	    	// Input
	    	processInput(window);
	    	
	    	// Swap buffers and poll IO events (key/mouse events)
	    	glfwSwapBuffers(window);
	    	glfwPollEvents();
	    	
	    }
	    
	    // Clear all allocated resources by GLFW
	    glfwTerminate();
	    
	}


	private static void processInput(long window) {
		// Close window when ESC key is pressed
		if(glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS) {
			glfwSetWindowShouldClose(window, true);
		}
		
	}

}
