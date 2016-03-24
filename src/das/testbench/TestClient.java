package das.testbench;

import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import das.Battlefield;
import das.Client;
import das.Main;
import das.Unit;
import das.message.Data;
import das.message.DataMessage;

public class TestClient {

	public static void main(String[] args) {
		Main.setRegistry();
		
		// Initialize battlefield
		Battlefield bf = new Battlefield();
		Unit dragon = new Unit();
		dragon.setX(5);
		dragon.setY(5);
		dragon.setHp(50);
		dragon.setMaxHp(50);
		dragon.setAp(3);
		dragon.setId(1);
		dragon.setType(false);
		bf.placeUnit(dragon);
		
		// Start the client
		Client client;
		try {
			client = new Client(0);
		} catch (RemoteException e) {
			e.printStackTrace();
			return;
		}
		Unit player = new Unit();
		player.setX(10);
		player.setY(10);
		player.setHp(10);
		player.setMaxHp(10);
		player.setAp(3);
		player.setId(0);
		player.setType(true);
		client.setPlayer(player);
		bf.placeUnit(player);
		
		List<Unit> updateUnits = new LinkedList<Unit>();
		updateUnits.add(dragon);
		updateUnits.add(dragon);
		Data data = new Data();
		data.setUpdatedUnits(updateUnits);
		data.setPlayer(player);
		DataMessage msg = new DataMessage(null, "Client_1",  data,  1, 1 );
		client.sendMessage(msg);
		
		new Thread(client).start();
		
	}

}
