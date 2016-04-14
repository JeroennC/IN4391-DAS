package das.message;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import das.Client;
import das.Node;
import das.Node_RMI;
import das.server.Server;

public abstract class Message implements Serializable {
	private static final long serialVersionUID = 5970408991964088527L;
	public static final int FWD_COUNT = 3;
	
	private static class NodeComponent {
		Address address;
		String name;
		
		NodeComponent(Address a, String n) {
			this.address = a;
			this.name = n;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((address == null) ? 0 : address.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof NodeComponent))
				return false;
			NodeComponent other = (NodeComponent) obj;
			if (address == null) {
				if (other.address != null)
					return false;
			} else if (!address.equals(other.address))
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}
	}
	
	private static Map<NodeComponent, Node_RMI> components = new ConcurrentHashMap<NodeComponent, Node_RMI>();;
	
	private int message_id;
	private transient final Node from;
	private Address fromAddress;
	private Address receiverAddress;
	private final Node_RMI receiver;
	private final String from_id;
	private final String receiver_id;
	private Message[] messageTail;
	private long timestamp = 0;
	private List<Integer> acks;	
	
	public Message(Client from, Address to, int to_id) {
		this(from, getComponent(to, "Server_"+to_id), "Server_"+to_id);
		setReceiverAddress(to);
	}
	
	public Message(Node from, Address to, String to_id)  {
		this(from, getComponent(to, to_id), to_id);
		setReceiverAddress(to);
	}
	
	public Message(Server from) {
		this.receiver = null;
		this.receiver_id = from.getName();
		this.from_id = from.getName();
		this.fromAddress = from.getAddress();
		this.from = from;
	}
	
	private Message(Node from, Node_RMI to, String id) {
		super();
		this.from = from;
		this.receiver = to;
		this.from_id = from.getName();
		this.fromAddress = from.getAddress();
		this.receiver_id = id;		
	}
	
	protected Message(Message m) {
		message_id = m.getID();
		fromAddress = m.getFromAddress();
		receiverAddress = m.getReceiverAddress();
		receiver = m.receiver;
		from_id = m.getFrom_id();
		from = m.getFrom();
		receiver_id = m.getReceiver_id();
		messageTail = m.messageTail;
		timestamp = m.getTimestamp();
		acks = m.getAcks();
	}
		
	public synchronized void send() {
		if(from instanceof Server && timestamp == 0)
			setTimestamp(((Server) from).getTime());
		from.Print("Sendmessage "+this);
		if(receiver == null)
			return;
		if(new Random(System.nanoTime()).nextInt(100) == 0) return;
		new Thread() {
			  public void run() { 
				  try {
					receiver.receiveMessage(Message.this);
				} catch (RemoteException e) { 
					e.printStackTrace();
				} //Do nothing (simulate Time-out)
			  }
		}.start();
	}
	
	public synchronized void setID(int id) {
		message_id = id;
	}
	
	public synchronized int getID() {
		return message_id;
	}
	
	public synchronized void addTail(List<Message> list) {
		messageTail = (Message[]) list.toArray(new Message[list.size()]);
	}
	
	public abstract void receive(Node_RMI node);
	
	public static Node_RMI getComponent(Address address, String name){
		NodeComponent nc = new NodeComponent(address, name);
		if (Message.components.containsKey(nc)) {
			boolean exists = true;
			Node_RMI nr = Message.components.get(nc);
			try {
				nr.exists();
			} catch (RemoteException e) {
				// Doesn't exist anymore
				Message.components.remove(nc);
				exists = false;
			}
			if (exists)
				return Message.components.get(nc);
		}
		try {
			Node_RMI nr = (Node_RMI) java.rmi.Naming.lookup("rmi://"+address.getAddress()+":"+address.getPort()+"/"+name);
			Message.components.put(nc, nr);
			return nr;
		} catch (MalformedURLException | RemoteException | NotBoundException e) {
			return null;
		}
	}

	public synchronized long getTimestamp() {
		return timestamp;
	}

	public synchronized void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public synchronized String getFrom_id() {
		return from_id;
	}
	
	public synchronized String getReceiver_id() {
		return receiver_id;
	}
	
	public synchronized Node getFrom() {
		return from;
	}

	public synchronized Address getFromAddress() {
		return fromAddress;
	}

	public synchronized List<Integer> getAcks() {
		return acks;
	}

	public synchronized void setAcks(List<Integer> acks) {
		this.acks = acks;
	}

	public synchronized Address getReceiverAddress() {
		return receiverAddress;
	}

	public synchronized void setReceiverAddress(Address receiverAddress) {
		this.receiverAddress = receiverAddress;
	}
	
	@Override
	public synchronized String toString() {
		return getClass().getName()+ "["+message_id+", " + from_id + " > " +receiver_id + "]";
	}

	@Override
	public synchronized int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((from_id == null) ? 0 : from_id.hashCode());
		result = prime * result + message_id;
		return result;
	}

	@Override
	public synchronized boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Message other = (Message) obj;
		if (from_id == null) {
			if (other.from_id != null)
				return false;
		} else if (!from_id.equals(other.from_id))
			return false;
		if (message_id != other.message_id)
			return false;
		return true;
	}

}
