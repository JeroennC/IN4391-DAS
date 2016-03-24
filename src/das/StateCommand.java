package das;

import das.message.ActionMessage;

public class StateCommand {
	// Points to the equivalent command in earlier executed state
	private StateCommand previous;
	
	// Defines the number of the command within it's own state
	private long command_nr; 
	
	// The message containing the action
	private ActionMessage message;
	
	public StateCommand(StateCommand previous, long commandNr, ActionMessage message) {
		this.previous = previous;
		this.command_nr = commandNr;
		this.message = message;
	}
	
	public StateCommand getPreviousCommand() {
		return previous;
	}
	
	public long getCommandNr() {
		return command_nr;
	}
	
	public ActionMessage getMessage() {
		return message;
	}
}
