package das.message;
import java.rmi.RemoteException;

import das.Client;
import das.Node_RMI;


public class RetransmitMessage extends Message {
	private static final long serialVersionUID = 345879987762306922L;
	
	private int retransmit_id;
	
	public RetransmitMessage(Client from, int to_id, int message_id) throws RemoteException {
		super(from, to_id);
		this.retransmit_id = message_id;
	}

	@Override
	public void receive(Node_RMI node) {
		// TODO Auto-generated method stub

	}

}
