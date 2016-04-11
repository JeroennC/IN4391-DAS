package das;
import java.rmi.Remote;
import java.rmi.RemoteException;

import das.message.Message;

public interface Node_RMI extends Remote {
	public void receiveMessage(Message m) throws RemoteException;
	public Battlefield getBattlefield() throws RemoteException;
	public boolean exists() throws RemoteException;
}
