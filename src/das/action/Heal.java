package das.action;

/**
 * Action for healing a player on the battlefield
 */
public class Heal extends Action {
	private static final long serialVersionUID = -382625101848738567L;
	
	private int receiver_id;
	
	public Heal (int executer_id, int receiver_id) {
		super(executer_id);
		this.receiver_id = receiver_id;
	}
	
	public int getReceiverId() {
		return receiver_id;
	}
	
	@Override
	public String toString() {
		return "[Heal [executer_id=" + getExecuterId() + " receiver_id=" + getReceiverId() +"]]";
	}
}

