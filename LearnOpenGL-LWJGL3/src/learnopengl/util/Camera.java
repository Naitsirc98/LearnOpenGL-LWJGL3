package learnopengl.util;

import static org.lwjgl.glfw.GLFW.*;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/** An abstract camera class that processes input and calculates the corresponding Euler Angles, 
 	Vectors and Matrices for use in OpenGL
 */
public class Camera {
	
	/** Defines several possible options for camera movement. Used as abstraction to stay away
		from window-system specific input methods
	*/
	public enum CameraMovement {
		FORWARD,
		BACKWARD,
		LEFT,
		RIGHT
	}
	
	// Default camera values
	private static final float YAW         = -90.0f;
	private static final float PITCH       =  0.0f;
	private static final float SPEED       =  2.5f;
	private static final float SENSITIVITY = 0.1f;
	private static final float ZOOM      = 45.0f;
	
	
	// Camera attributes
	public Vector3f position;
	public Vector3f front = new Vector3f(0.0f, 0.0f, -1.0f);
	public Vector3f up = new Vector3f();
	public Vector3f right = new Vector3f();
	public Vector3f worldUp;
	// Euler angles
	public float yaw;
	public float pitch;
	// Camera options
	public float movementSpeed = SPEED;
	public float mouseSensitivity = SENSITIVITY;
	public float zoom = ZOOM;
	
	// Constructor with vectors
	public Camera(Vector3f position, Vector3f up, float yaw, float pitch) {
		this.position = position;
		worldUp = up;
		this.yaw = yaw;
		this.pitch = pitch;
		updateCameraVectors();
	}
	
	// Constructor with scalar values 
	public Camera(float posX, float posY, float posZ, float upX, float upY, float upZ, float yaw, float pitch) {
		this(new Vector3f(posX, posY, posZ), new Vector3f(upX, upY, upZ), yaw, pitch);
	}
	
	// Default constructor
	public Camera() {
		this(new Vector3f(0.0f), new Vector3f(0.0f, 1.0f, 0.0f), YAW, PITCH);
	}
	
	/**
	 * Returns the view matrix calculated using Euler Angles and the LookAt Matrix
	 * */
	public Matrix4f getViewMatrix() {
		return new Matrix4f().lookAt(position, position.add(front, new Vector3f()), up);
	}
	
	/**
	 * Processes input received from any keyboard-like input system. Accepts input parameter 
	 * in the form of camera defined ENUM (to abstract it from windowing systems)
	 * */
	public void processKeyboard(CameraMovement direction, float deltaTime) {
		
		final float velocity = movementSpeed * deltaTime;
		
		Vector3f pos;
		
		switch(direction) {
		case FORWARD:
			position.add(front.mul(velocity, new Vector3f()));
			break;
		case BACKWARD:
			position.sub(front.mul(velocity, new Vector3f()));
			break;
		case LEFT:
			pos = front.cross(up, new Vector3f()).normalize();
			position.sub(pos.mul(velocity));
			break;
		case RIGHT:
			pos = front.cross(up, new Vector3f()).normalize();
			position.add(pos.mul(velocity));
			break;
		}
		
	}
	
	/**
	 * Processes input received from a mouse input system. Expects the offset value in both the x and y direction.
	 * */
	public void processMouseMovement(float xoffset, float yoffset, boolean constrainPitch) {
		
		yaw += xoffset * mouseSensitivity;
		pitch += yoffset * mouseSensitivity;
		
		// Make sure that when pitch is out of bounds, screen doesn't get flipped
		if(constrainPitch) {
			// I prefer to use a clamp function, since it is more readable than 2 if statements
			pitch = (float)Math.min(Math.max(pitch, -89.0f), 89.0f);
		}
		
        // Update front, right and up Vectors using the updated Euler angles
		updateCameraVectors();	
	}
	
	/**
	 * Processes input received from a mouse scroll-wheel event. Only requires input on the vertical wheel-axis
	 * */
	public void processMouseScroll(float yoffset) {
		// I prefer to use a clamp function, since it is more readable than 2 if statements
		zoom = (float)Math.min(Math.max(zoom - yoffset, 1.0f), 45.0f);
	}

	/**
	 * Calculates the front vector from the Camera's (updated) Euler Angles
	 * */
	public void updateCameraVectors() {
		
		// Calculate the new front vector
		Vector3f front = new Vector3f();
		front.x = (float)Math.cos(Math.toRadians(yaw)) * (float)Math.cos(Math.toRadians(pitch));
		front.y = (float)Math.sin(Math.toRadians(pitch));
		front.z = (float)Math.sin(Math.toRadians(yaw)) * (float)Math.cos(Math.toRadians(pitch));
		
		this.front = front.normalize();
		
		// Also re-calculate the Right and Up vector
		// Normalize the vectors, because their length gets closer to 0 the more you look up or down 
		// which results in slower movement.
		right = front.cross(worldUp, right).normalize();
		up = right.cross(front, up).normalize();
	}

}
