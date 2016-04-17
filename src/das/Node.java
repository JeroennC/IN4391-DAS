package das;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import das.message.Address;

/**
 * Base class for remote objects Server & Client
 */
public abstract class Node extends UnicastRemoteObject implements Node_RMI, Runnable {
	private static final long serialVersionUID = -3510258906366116527L;
	
	protected int id;
	protected String name;
	private boolean doesPrint;
	
	public enum State { Disconnected, Initialization, Running, Inconsistent, Exit };
	protected volatile State state;
	
	protected Node(int id, String name) throws RemoteException {
		super();
		this.id = id;
		this.name = name;
		doesPrint = true;
		Main.registerObject(this);
	}
	
	/**
	 * Unbind this node from the RMI registry
	 */
	protected void close() {	
		Main.unregisterObject(this);
	}
	
	/**
	 * Change node state
	 */
	public void changeState(State newState) {
		if (state != State.Exit) {
			state = newState;
			Print("changed state to "+newState);
		}
	}
	
	/**
	 * Change node state to EXIT
	 */
	public void stop() {
		changeState(State.Exit);
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}	
	
	/**
	 * Gets local address
	 */
	public Address getAddress() {
		try {
			return new Address (InetAddress.getLocalHost().getHostAddress(), Main.port);
		} catch (UnknownHostException e) {
			return null;
		}
	}
	
	/**
	 * Print a message to the console
	 */
	public void Print(String msg) {
		if (doesPrint)
			System.out.println(getName() + ": " + msg);
	}

	/**
	 * Print a message to the console
	 */
	public void Printf(String msg, Object... arg1) {
		if (doesPrint)
			System.out.printf(getName() + ": " + msg + "\n", arg1);
	}
	
	/**
	 * Returns true if state is not EXIT
	 */
	public boolean isRunning() {
		return state != State.Exit;
	}
	
	/**
	 * Enable/disable console printing of this node
	 */
	public void setPrinting(boolean print) {
		doesPrint = print;
	}
	
	public boolean isPrinting() {
		return doesPrint;
	}
	
	public boolean exists() {
		return true;
	}
}
