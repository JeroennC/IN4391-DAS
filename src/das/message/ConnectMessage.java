package das.message;
import java.rmi.RemoteException;

import das.Client;
import das.Node_RMI;


public class ConnectMessage extends Message {
	private static final long serialVersionUID = 5205567105382587637L;

	public ConnectMessage(Client from, int to_id) throws RemoteException {
		super(from, to_id);
	}

	@Override
	public void receive(Node_RMI node) {
		// TODO Auto-generated method stub

	}

}
