package das.message;
import java.rmi.RemoteException;

import das.Action;
import das.Client;
import das.Node_RMI;


public class ActionMessage extends Message {
	private static final long serialVersionUID = 3743633525735683045L;
	private Action action;
	
	public ActionMessage(Client from, int to, Action a) throws RemoteException {
		super(from, to);
		this.action = a;
	}

	@Override
	public void receive(Node_RMI node) {
		//((Server) node).doSomething(action);
	}



}
