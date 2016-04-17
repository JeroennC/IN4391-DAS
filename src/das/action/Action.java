package das.action;

import java.io.Serializable;

/**
 * Abstract base class for all actions
 */
public abstract class Action implements Serializable {
	private static final long serialVersionUID = -8729632997383817951L;
	private int executer_id;
	
	public Action(int executer_id) {
		this.executer_id = executer_id;
	}
	
	public int getExecuterId() {
		return executer_id;
	}
	
	@Override
	public String toString() {
		return "[Action [executer_id=" + executer_id + "]]";
	}
}
