package das.message;

import das.Node_RMI;
import das.Server;

public class NewServerMessage extends Message {
	private static final long serialVersionUID = -4587682238478282956L;

	public NewServerMessage(Server from, String to_id) {
		super(from, to_id);
	}

	@Override
	public void receive(Node_RMI node) {
		// TODO Auto-generated method stub

	}

}
