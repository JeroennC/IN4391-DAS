package das;

import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

public class Main {
	private static Registry registry;
	private static int port = 1103;
	
	public static void main(String[] args) {		
		setRegistry();
		//TODO spawn objects
	}
	

	/**
	 * Creates a new registry for remote method invocation, or retrieves it
	 */
	public static void setRegistry(){
		//Create and install a security manager
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new RMISecurityManager());
		}
		
		//Try to create a new registry, if it fails, it is assumed that it is already initialized. 
		try {	
			registry = java.rmi.registry.LocateRegistry.createRegistry(port);
		} catch (RemoteException e) {
			try{
				registry = java.rmi.registry.LocateRegistry.getRegistry(port);
			} catch (RemoteException e2){
				e.printStackTrace();
				e2.printStackTrace();
				return;
			}
		}
	}
	
	/**
	 * Get the registry object for remote method invocation.
	 * @return the registry object.
	 */
	public static Registry getRegistry(){
		return registry;
	}
	
	public static Node registerObject(Node n) {
		try {
			getRegistry().rebind(n.getName(), n);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return n;
	}
	

}
