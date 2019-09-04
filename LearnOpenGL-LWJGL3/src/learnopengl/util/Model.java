package learnopengl.util;

import static org.lwjgl.assimp.Assimp.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AINode;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIString;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import learnopengl.util.Mesh.Texture;
import learnopengl.util.Mesh.Vertex;

public class Model {

	private static Logger logger = Logger.getAnonymousLogger();

	private static int textureFromFile(String path, String directory, boolean gamma) {

		final String filename = directory + '/' + path;

		final int textureID = glGenTextures();

		try(MemoryStack stack = MemoryStack.stackPush()) {

			IntBuffer width = stack.ints(0);
			IntBuffer height = stack.ints(0);
			IntBuffer nrComponents = stack.ints(0);
			
			final ByteBuffer data = STBImage.stbi_load(filename, width, height, nrComponents, 0);

			if(data == null) {
				logger.severe("Texture failed to load at path: " + filename);
				return textureID;
			}

			int format = 0;

			switch(nrComponents.get(0)) {

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
			}
			
			glBindTexture(GL_TEXTURE_2D, textureID);
			glTexImage2D(GL_TEXTURE_2D, 0, format, width.get(0), height.get(0), 0, format, GL_UNSIGNED_BYTE, data);
			glGenerateMipmap(GL_TEXTURE_2D);

			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			
			STBImage.stbi_image_free(data);

			return textureID;
		}

	}

	/* Model Data */
	// Stores all the textures loaded so far, optimization to make sure textures aren't loaded more than once.
	private Map<String, Texture> texturesLoaded; 
	private List<Mesh> meshes;
	private String directory;
	private boolean gammaCorrection;

	/* Functions */

	/**
	 * Constructor, expects a filepath to a 3D model
	 * */
	public Model(String path, boolean gamma) {
		gammaCorrection = gamma;
		texturesLoaded = new HashMap<>();
		loadModel(path);
	}

	/**
	 * Constructs a mesh with no gamma correction
	 * */
	public Model(String path) {
		this(path, false);
	}

	/**
	 * Draws the model, and thus all its meshes
	 * */
	public void draw(Shader shader) {
		for(final Mesh mesh : meshes) {
			mesh.draw(shader);
		}
	}

	/**
	 * Deletes the model, thus all its meshes and textures
	 * */
	public void delete() {
		for(final Mesh mesh : meshes) {
			mesh.delete();
		}

		for(final Texture texture : texturesLoaded.values()) {
			if(texture != null) {
				glDeleteTextures(texture.id);
			}
		}

		meshes = null;
		texturesLoaded = null;
	}


	/** 
	 * Loads a model with supported ASSIMP extensions from file and stores the resulting meshes in the meshes vector.
	 */
	private void loadModel(String path) {

		// Read file via ASSIMP
		final AIScene scene = aiImportFile(path, 
				  aiProcess_Triangulate
				| aiProcess_FlipUVs 
				| aiProcess_CalcTangentSpace);

		// Check for errors
		if(scene == null || (scene.mFlags() & AI_SCENE_FLAGS_INCOMPLETE) == AI_SCENE_FLAGS_INCOMPLETE
				|| scene.mRootNode() == null) { // If is Not Zero

			logger.severe("ERROR::ASSIMP:: " + aiGetErrorString());
			return;
		}

		// Retrieve the directory path of the filepath
		directory = path.substring(0, path.lastIndexOf('/'));
		
		meshes = new ArrayList<>(scene.mNumMeshes());

		// Process ASSIMP's root node recursively
		processNode(scene.mRootNode(), scene);

		// Release all ASSIMP allocated resources
		aiReleaseImport(scene);
	}

	/**
	 * Processes a node in a recursive fashion. Processes each individual mesh located 
	 * at the node and repeats this process on its children nodes (if any).
	 * */
	private void processNode(AINode node, AIScene scene) {
		
		// Process each mesh located at the current node
		final int numMeshes = node.mNumMeshes();
		PointerBuffer meshesBuffer = scene.mMeshes();
		IntBuffer meshesIndicesBuffer = node.mMeshes();
		for(int i = 0;i < numMeshes;i++) {
			// The node object only contains indices to index the actual objects in the scene. 
			// The scene contains all the data, node is just to keep stuff organized (like relations between nodes).
			AIMesh mesh = AIMesh.createSafe(meshesBuffer.get(meshesIndicesBuffer.get(i)));
			meshes.add(processMesh(mesh, scene));
		}

		// After we've processed all of the meshes (if any) we then recursively process each of the children nodes
		final int numChildren = node.mNumChildren();
		PointerBuffer childrenBuffer = node.mChildren();
		for(int i = 0;i < numChildren;i++) {
			AINode child = AINode.createSafe(childrenBuffer.get(i));
			processNode(child, scene);
		}

	}
	
	private Mesh processMesh(AIMesh mesh, AIScene scene) {

		// Data to fill
		ArrayList<Vertex> vertices = new ArrayList<>();
		ArrayList<Integer> indices = new ArrayList<>();
		ArrayList<Texture> textures = new ArrayList<>();

		final int numVertices = mesh.mNumVertices();

		AIVector3D.Buffer verticesBuffer = mesh.mVertices();
		AIVector3D.Buffer normalsBuffer = mesh.mNormals();
		// A vertex can contain up to 8 different texture coordinates. We thus make the assumption that we won't 
		// use models where a vertex can have multiple texture coordinates so we always take the first set (0).
		AIVector3D.Buffer texCoordsBuffer = mesh.mTextureCoords(0);
		AIVector3D.Buffer tangentBuffer = mesh.mTangents();
		AIVector3D.Buffer bitangentBuffer = mesh.mBitangents();

		// Walk through each of the mesh's vertices
		for(int i = 0;i < numVertices;i++) {

			Vertex vertex = new Vertex();

			// Position
			AIVector3D pos = verticesBuffer.get(i);
			vertex.position = new Vector3f(pos.x(), pos.y(), pos.z());

			// Normal
			AIVector3D normal = normalsBuffer.get(i);
			vertex.normal = new Vector3f(normal.x(), normal.y(), normal.z());

			// Texture coordinates
			if(texCoordsBuffer != null) { // Does the mesh contain texture coordinates?
				AIVector3D texCoords = texCoordsBuffer.get(i);
				vertex.texCoords = new Vector2f(texCoords.x(), texCoords.y());
			} else {
				vertex.texCoords = new Vector2f(0.0f);
			}
			
			// Tangent
			AIVector3D tangent = tangentBuffer.get(i);
			vertex.tangent = new Vector3f(tangent.x(), tangent.y(), tangent.z());

			// Bitangent
			AIVector3D bitangent = bitangentBuffer.get(i);
			vertex.bitangent = new Vector3f(bitangent.x(), bitangent.y(), bitangent.z());

			vertices.add(vertex);
		}

		// Now walk through each of the mesh's faces (a face is a mesh its triangle) and 
		// retrieve the corresponding vertex indices.
		final int numFaces = mesh.mNumFaces();
		AIFace.Buffer facesBuffer = mesh.mFaces();
		for(int i = 0;i < numFaces;i++) {
			AIFace face = facesBuffer.get(i);
			// Retrieve all indices of the face and store them into the indices list
			final int numIndices = face.mNumIndices();
			IntBuffer indicesBuffer = face.mIndices();
			for(int j = 0;j < numIndices;j++) {
				indices.add(indicesBuffer.get(j));
			}
		}

		// Process materials
		final int matIndex = mesh.mMaterialIndex();
		if(matIndex >= 0) {
			PointerBuffer materialsBuffer = scene.mMaterials();
			AIMaterial material = AIMaterial.createSafe(materialsBuffer.get(matIndex));
			// We assume a convention for sampler names in the shaders. Each diffuse texture should be named
			// as 'texture_diffuseN' where N is a sequential number ranging from 1 to MAX_SAMPLER_NUMBER. 
			// Same applies to other texture as the following list summarizes:
			// diffuse: texture_diffuseN
			// specular: texture_specularN
			// normal: texture_normalN

			// We pass in our textures list to avoid calling new every time

			// 1. Diffuse maps
			loadMaterialTextures(material, aiTextureType_DIFFUSE, "texture_diffuse", textures);
			// 2. Specular maps
			loadMaterialTextures(material, aiTextureType_SPECULAR, "texture_specular", textures);
			// 3. Normal maps
			loadMaterialTextures(material, aiTextureType_NORMALS, "texture_normal", textures);
			// 4. Height maps
			loadMaterialTextures(material, aiTextureType_HEIGHT, "texture_height", textures);
		}

		// Shrink the lists to save memory
		vertices.trimToSize();
		indices.trimToSize();
		textures.trimToSize();
		
		// Return a mesh object created from the extracted mesh data
		return new Mesh(vertices, indices, textures);
	}

	/**
	 * Checks all material textures of a given type and loads the textures if they're not loaded yet.
	 * The required info is returned as a Texture object.
	 * */
	private void loadMaterialTextures(AIMaterial mat, int textureType, String typeName, List<Texture> textures) {

		final int numTextures = aiGetMaterialTextureCount(mat, textureType);

		for(int i = 0;i < numTextures;i++) {

			try(MemoryStack stack = MemoryStack.stackPush()) {

				AIString str = AIString.mallocStack(stack);

				aiGetMaterialTexture(mat, textureType, i, str, (int[])null, null, null, null, null, null);

				final String path = str.dataString();
				
				if(path == null || path.length() <= 0) {
					continue;
				}

				// Check if texture was loaded before and if so, continue to next iteration: skip loading a new texture
				if(texturesLoaded.containsKey(path)) {
					textures.add(texturesLoaded.get(path));

				} else { // If texture hasn't been loaded already, load it
					Texture texture = new Texture();
					texture.id = textureFromFile(path, directory, false);
					texture.type = typeName;
					texture.path = path;
					textures.add(texture);
					// Store it as texture loaded for entire model, to ensure we won't unnecesary load duplicate textures.
					texturesLoaded.put(path, texture);
				}

			}

		}

	}

}
