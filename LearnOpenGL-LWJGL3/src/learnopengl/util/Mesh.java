package learnopengl.util;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.logging.Logger;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

public class Mesh {

	private static Logger logger = Logger.getAnonymousLogger();

	public static class Vertex {

		public static int sizeof() {
			return (3 + 3 + 2 + 3 + 3) * Float.BYTES;
		}

		public Vector3f position;
		public Vector3f normal;
		public Vector2f texCoords;
		public Vector3f tangent;
		public Vector3f bitangent;

	}

	public static class Texture {

		public int id;
		public String type;
		public String path;

	}

	/* Mesh Data */
	public List<Vertex> vertices;
	public List<Integer> indices;
	public List<Texture> textures;
	public int vao;
	/* Render Data */
	private int vbo;
	private int ebo;

	/* Functions */

	/**
	 * Constructs a new Mesh with the specified parameters 
	 * */
	public Mesh(List<Vertex> vertices, List<Integer> indices, List<Texture> textures) {
		this.vertices = vertices;
		this.indices = indices;
		this.textures = textures;

		// Now that we have all the required data, set the vertex buffers and its attribute pointers.
		setupMesh();
	}

	/**
	 * Render the mesh
	 * */
	public void draw(Shader shader) {

		// Bind appropiate textures
		int diffuseNr = 1;
		int specularNr = 1;
		int normalNr = 1;
		int heightNr = 1;

		for(int i = 0;i < textures.size();i++) {

			glActiveTexture(GL_TEXTURE0 + i); // Active proper texture unit before binding

			// Retrieve the texture number (the N in diffuse_textureN)
			int number = 0;
			final String name = textures.get(i).type;

			switch(name) {

			case "texture_diffuse":
				number = diffuseNr++; // Transfer int to stream
				break;
			case "texture_specular":
				number = specularNr++; // Transfer int to stream
				break;
			case "texture_normal":
				number = normalNr++; // Transfer int to stream
				break;
			case "texture_height":
				number = heightNr++; // Transfer int to stream
				break;
			default:
				logger.severe("Unknown texture type: " + name);
			}

			// Now set the sampler to the correct texture unit
			shader.setInt(name + number, i);
			// And finally bind the texture
			glBindTexture(GL_TEXTURE_2D, textures.get(i).id);
		}

		// Draw mesh
		glBindVertexArray(vao);
		glDrawElements(GL_TRIANGLES, indices.size(), GL_UNSIGNED_INT, 0L);
		glBindVertexArray(0);

		// Always good practice to set everything back to defaults once configured.
		glActiveTexture(GL_TEXTURE0);
	}

	/**
	 * Deletes the mesh's vertex array and buffers
	 * */
	public void delete() {
		glDeleteVertexArrays(vao);
		glDeleteBuffers(vbo);
		glDeleteBuffers(ebo);	
	}

	/**
	 * Initializes all the buffer objects/arrays
	 * */
	private void setupMesh() {

		// Create buffers/arrays
		vao = glGenVertexArrays();
		vbo = glGenBuffers();
		ebo = glGenBuffers();

		glBindVertexArray(vao);
		
		// Load data into vertex buffers
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		// Since we are programming in Java, we have to manually convert our vertices list into a FloatBuffer
		FloatBuffer vertexData = getVertexData();
		glBufferData(GL_ARRAY_BUFFER, vertexData, GL_STATIC_DRAW);  

		final int sizeofVertex = Vertex.sizeof();

		// Set the vertex attribute pointers
		// Vertex Positions
		glEnableVertexAttribArray(0);	
		glVertexAttribPointer(0, 3, GL_FLOAT, false, sizeofVertex, 0);
		//Vertex normals
		glEnableVertexAttribArray(1);	
		glVertexAttribPointer(1, 3, GL_FLOAT, false, sizeofVertex, 3 * Float.BYTES);
		// Vertex texture coords
		glEnableVertexAttribArray(2);	
		glVertexAttribPointer(2, 2, GL_FLOAT, false, sizeofVertex, 6 * Float.BYTES);
		// Vertex tangent
		glEnableVertexAttribArray(3);
		glVertexAttribPointer(3, 3, GL_FLOAT, false, sizeofVertex, 8 * Float.BYTES);
		// Vertex bitangent
		glEnableVertexAttribArray(4);
		glVertexAttribPointer(4, 3, GL_FLOAT, false, sizeofVertex, 11 * Float.BYTES);
		
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
		// Same with the indices, we have to convert it to a IntBuffer
		IntBuffer indicesData = getIndicesData(); 
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesData, GL_STATIC_DRAW);

		glBindVertexArray(0);

		// Free the dynamically allocated buffers
		MemoryUtil.memFree(vertexData);
		MemoryUtil.memFree(indicesData);
	}

	private FloatBuffer getVertexData() {

		final int offset = Vertex.sizeof() / Float.BYTES; // How many floats per vertex
		// We are natively allocating, so we have to free this memory afterwards!
		FloatBuffer buffer = MemoryUtil.memAllocFloat(vertices.size() * offset);		

		for(int i = 0;i < vertices.size();i++) {

			final Vertex vertex = vertices.get(i);
			final int index = i * offset;
			
			// Fill the buffer with the vertex data
			vertex.position.get(index, buffer);
			vertex.normal.get(index + 3, buffer);
			vertex.texCoords.get(index + 6, buffer);
			vertex.tangent.get(index + 8, buffer);
			vertex.bitangent.get(index + 11, buffer);
		}

		return buffer;
	}

	private IntBuffer getIndicesData() {

		final int size = indices.size();
		// Again, we are natively allocating, so we have to free this memory afterwards!
		IntBuffer buffer = MemoryUtil.memAllocInt(size);

		for(int i = 0;i < size;i++) {
			buffer.put(i, indices.get(i));
		}

		return buffer;
	}

}
