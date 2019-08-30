package learnopengl.util;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.logging.Logger;

import org.joml.Matrix3fc;
import org.joml.Matrix4fc;
import org.joml.Vector2fc;
import org.joml.Vector3fc;
import org.joml.Vector4fc;
import org.lwjgl.system.MemoryStack;

/*
 * Second Shader class approach
 * 
 * */
public class Shader2 {
	
	private static Logger logger = Logger.getAnonymousLogger();
	
	public final int id;
	
	public Shader2(String vertexPath, String fragmentPath) {
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
	
	public void setVec2(String name, Vector2fc value) {
		setVec2(name, value.x(), value.y());
	}
	
	public void setVec2(String name, float x, float y) {
		glUniform2f(glGetUniformLocation(id, name), x, y);
	}
	
	public void setVec3(String name, Vector3fc value) {
		setVec3(name, value.x(), value.y(), value.z());
	}
	
	public void setVec3(String name, float x, float y, float z) {
		glUniform3f(glGetUniformLocation(id, name), x, y, z);
	}
	
	public void setVec4(String name, Vector4fc value) {
		setVec4(name, value.x(), value.y(), value.z(), value.w());
	}
	
	public void setVec4(String name, float x, float y, float z, float w) {
		glUniform4f(glGetUniformLocation(id, name), x, y, z, w);
	}
	
	public void setMat3(String name, Matrix3fc matrix) {
		try(MemoryStack stack = MemoryStack.stackPush()) {
			glUniformMatrix3fv(glGetUniformLocation(id, name), false, matrix.get(stack.mallocFloat(9)));
		}
	}
	
	public void setMat4(String name, Matrix4fc matrix) {
		try(MemoryStack stack = MemoryStack.stackPush()) {
			glUniformMatrix4fv(glGetUniformLocation(id, name), false, matrix.get(stack.mallocFloat(16)));
		}
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
