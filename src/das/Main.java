package das;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class Main {
	private static Registry registry;
	public static int port = 1103;
	private static List<Thread> threads;
	
	public static void main(String[] args) {		
		setRegistry();
		
		// Initialize
		threads = new ArrayList<Thread>();
		// Read from input
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String input;
		String[] sep;
		try {
			while (true) {
				input = br.readLine().trim();
				sep = input.split(" ");
				try {
					// Process input
					if(sep[0].equals("start")) {
						int id = Integer.parseInt(sep[2]);
						if (sep[1].equals("client") || sep[1].equals("server")) {
							if (isIdActive(sep[1], id))
								startThread(sep[1], id);
							else
								System.out.println(sep[1] + ", id " + id +" already taken.");
						} 
					} else if (sep[0].equals("kill")) {
						int id = Integer.parseInt(sep[2]);
						if (isIdActive(sep[1], id))
							killThread(sep[1], id);
					} else if (sep[0].equals("exit")) {
						System.out.println("System exiting..");
						killRunningThreads();
						break;
					} else {
						throw new Exception();
					}
				} catch (Exception e) {
					// If array out of bounds or other exceptions happen
					System.out.println("Error on input: '" + input + "'");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Checks if a thread id is already running
	 */
	private static boolean isIdActive(String type, int id) {
		String threadName = type + "_" + id;
		for (Thread t : threads) {
			if (t.getName().equals(threadName))
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Launch a thread
	 * @throws RemoteException 
	 */
	private static void startThread(String type, int id) throws RemoteException {
		Thread t;
		if (type.equals("client")) {
			t = new Thread(new Client(id));
		} else { // Server
			t = new Thread(new Server(id));
		}
		t.setName(type + "_" + id);
		threads.add(t);
		t.start();
	}
	
	/**
	 * Kill a thread
	 */
	private static void killThread(String type, int id) {
		String threadName = type + "_" + id;
		for (Thread t : threads) {
			if (t.getName().equals(threadName))
			{
				t.interrupt();
				threads.remove(t);
				break;
			}
		}
	}
	
	/**
	 * Kill all running threads
	 */
	private static void killRunningThreads() {
		threads.forEach(t -> {
			t.interrupt();
		});
		threads.clear();
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
