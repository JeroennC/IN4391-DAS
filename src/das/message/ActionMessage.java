package das.message;

import das.Client;
import das.Node_RMI;
import das.action.Action;
import das.action.NewPlayer;
import das.server.Server;

/**
 * Message for sending an action to a Server
 */
public class ActionMessage extends Message {
	private static final long serialVersionUID = 3743633525735683045L;
	private Action action;
	private long responseTime;
	
	public ActionMessage(InitMessage m, int newPlayerId) {
		super(m);
		action = new NewPlayer(newPlayerId);
		this.responseTime = -2;
	}
	
	public ActionMessage(Client from, Address to, int to_id, Action a, long responseTime) {
		super(from, to, to_id);
		this.action = a;
		this.responseTime = responseTime;
	}
	
	public ActionMessage(Server from, Action a) {
		super(from);
		this.action = a;
		this.responseTime = -3;
	}

	public ActionMessage(Server from, Address to, String to_id, Action a) {
		super(from, to, to_id);
		this.action = a;
	}

	@Override
	public void receive(Node_RMI node) {
		((Server) node).receiveActionMessage(this);
	}

	public synchronized Action getAction() {
		return action;
	}

	public synchronized long getResponseTime() {
		return responseTime;
	}

	public synchronized void setResponseTime(long responseTime) {
		this.responseTime = responseTime;
	}

}
