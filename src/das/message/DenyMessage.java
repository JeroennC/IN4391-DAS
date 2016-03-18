package das.message;
import java.rmi.RemoteException;

import das.Client;
import das.Node_RMI;
import das.Server;


public class DenyMessage extends Message {
	private static final long serialVersionUID = 2271377239141868250L;
	private int deniedMessage_id;

	public DenyMessage(Server from, String to_id, int deniedMessage_id) throws RemoteException {
		super(from, to_id);
		this.setDeniedMessage_id(deniedMessage_id);
	}

	@Override
	public void receive(Node_RMI node) {
		((Client) node).receiveDenial(this);

	}

	public int getDeniedMessage_id() {
		return deniedMessage_id;
	}

	public void setDeniedMessage_id(int deniedMessage_id) {
		this.deniedMessage_id = deniedMessage_id;
	}

}
