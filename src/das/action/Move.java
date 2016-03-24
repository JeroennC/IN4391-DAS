package das.action;

public class Move extends Action {
	private static final long serialVersionUID = 1605421687552048640L;
	private MoveType move;
	
	public Move (int executer_id, MoveType move) {
		super(executer_id);
		this.move = move;
	}
	
	public MoveType getMoveType() {
		return move;
	}
}
