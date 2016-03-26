package das;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.sun.jmx.snmp.tasks.ThreadService;

import das.gui.ClientViewer;
import das.server.Server;

@SuppressWarnings("deprecation")
public class Main {
	private static class NodeRef {
		public Thread thread;
		public Node node;
		
		public NodeRef(Thread thread, Node node) {
			this.thread = thread;
			this.node = node;
		}
	}
	
	private static Registry registry;
	public static int port = 1103;
	private static List<NodeRef> nodes;
	
	public static void main(String[] args) {		
		setRegistry();
		
		// Initialize
		nodes = new ArrayList<NodeRef>();
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
						if (sep[1].equals("Client") || sep[1].equals("Server")) {
							if (!isIdActive(sep[1], id))
								startThread(sep[1], id);
							else
								System.out.println(sep[1] + ", id " + id +" already taken.");
						} 
					} else if (sep[0].equals("kill")) {
						int id = Integer.parseInt(sep[2]);
						if (isIdActive(sep[1], id))
							killThread(sep[1], id);
					} else if (sep[0].equals("sleep")){
						int time = Integer.parseInt(sep[1]);
						Thread.sleep(time);
					} else if (sep[0].equals("view")) {
						new ClientViewer();
					} else if (sep[0].equals("exit")) {
						System.out.println("System exiting..");
						killRunningThreads();
						//printRunningThreads();
						break;
					} else {
						throw new Exception();
					}
				} catch (Exception e) {
					// If array out of bounds or other exceptions happen
					System.out.println("Error on input: '" + input + "'");
					e.printStackTrace();
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
		for (NodeRef nr : nodes) {
			if (nr.thread.getName().equals(threadName))
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
		Node n;
		if (type.equals("Client")) {
			n = new Client(id);
		} else { // Server
			n = new Server(id);
		}
		Thread t = new Thread(n);
		t.setName(type + "_" + id);
		nodes.add(new NodeRef(t, n));
		t.start();
	}
	
	/**
	 * Kill a thread
	 */
	private static void killThread(String type, int id) {
		String threadName = type + "_" + id;
		for (NodeRef nr : nodes) {
			if (nr.thread.getName().equals(threadName))
			{
				nr.node.stop();
				//nr.thread.interrupt();
				nodes.remove(nr);
				break;
			}
		}
	}
	
	/**
	 * Kill all running threads
	 */
	private static void killRunningThreads() {
		nodes.forEach(nr -> {
			nr.node.stop();
			//nr.thread.interrupt();
		});
		nodes.clear();
	}
	
	/**
	 * Print all running threads
	 */
	private static void printRunningThreads() {
		Set<Thread> threads = Thread.getAllStackTraces().keySet();
		for (Thread t : threads) {
			System.out.println(t.getName());
		}
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
