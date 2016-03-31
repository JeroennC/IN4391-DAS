package das.server;

import das.message.ActionMessage;

public class StateCommand {
	// Points to the equivalent command in other server states
	private StateCommand[] commands;
	private int myPosition;
	
	// Defines whether this Command should still be executed
	private boolean valid;
	
	// Defines the number of the command within it's own state
	private long command_nr; 
	
	// The message containing the action
	private ActionMessage message;
	
	public StateCommand(StateCommand[] commands, int myPosition, ActionMessage message) {
		this.commands = commands;
		this.myPosition = myPosition;
		this.message = message;
		valid = true;
	}
	
	public synchronized void Invalidate() {
		valid = false;
	}
	
	public void InvalidateUpwards() {
		for (int i = myPosition + 1; i < commands.length; i++ ){
			commands[i].Invalidate();
		}
	}
	
	public synchronized boolean isValid() {
		return valid;
	}
	
	public StateCommand getPreviousCommand() {
		return myPosition == 0 ? null : commands[myPosition - 1];
	}
	
	public StateCommand[] getCommands() {
		return commands;
	}
	
	public long getCommandNr() {
		return command_nr;
	}
	
	public void setCommandNr(long commandNr) {
		command_nr = commandNr;
	}
	
	public int getPosition() {
		return myPosition;
	}
	
	public ActionMessage getMessage() {
		return message;
	}
	
	public long getTimestamp() {
		return message.getTimestamp();
	}
	
	public boolean isConsistent() {
		// TODO could be more sophisticated, is now very strict
		return getPreviousCommand() == null || getCommandNr() == getPreviousCommand().getCommandNr();
	}
	
	public StateCommand reset() {
		this.valid = true;
		this.command_nr = 0;
		return this;
	}
}
