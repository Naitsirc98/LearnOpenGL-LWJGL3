package learnopengl.util;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.logging.Logger;

import org.lwjgl.system.MemoryStack;

/*
 * First Shader class approach
 * 
 * */
public class Shader1 {
	
	private static Logger logger = Logger.getAnonymousLogger();
	
	public final int id;
	
	public Shader1(String vertexPath, String fragmentPath) {
		final int vertex = createShader(GL_VERTEX_SHADER, readFile(vertexPath));
		final int fragment = createShader(GL_FRAGMENT_SHADER, readFile(fragmentPath));

		id = createShaderProgram(vertex, fragment);

		glDeleteShader(vertex);
		glDeleteShader(fragment);
	}
	
	public void use() {
		glUseProgram(id);
	}
	
	public void setBool(String name, boolean value) {
		setInt(name, value ? 1 : 0);
	}
	
	public void setInt(String name, int value) {
		glUniform1i(glGetUniformLocation(id, name), value);
	}
	
	public void setFloat(String name, float value) {
		glUniform1f(glGetUniformLocation(id, name), value);
	}
	
	public void delete() {
		glDeleteProgram(id);
	}
	
	
	// ========== Utility functions ===========
	
	private String readFile(String path) {
		
		StringBuilder builder = new StringBuilder();
		
		try(BufferedReader reader = new BufferedReader(new FileReader(path))) {
			
			String line;
			
			while((line = reader.readLine()) != null) {
				builder.append(line).append('\n');
			}
			
		} catch(IOException e) {
			logger.severe(e.getMessage());
		}
		
		return builder.toString();
		
	}

	private int createShaderProgram(int vertexShader, int fragmentShader) {

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

	private int createShader(int shaderType, String src) {

		final int shader = glCreateShader(shaderType);

		glShaderSource(shader, src);

		glCompileShader(shader);

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

}
