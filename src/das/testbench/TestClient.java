package das.testbench;

import java.rmi.RemoteException;
import java.util.Set;

import das.Battlefield;
import das.Client;
import das.Unit;

public class TestClient {

	public static void main(String[] args) {
		
		// Initialize battlefield
		Battlefield bf = Battlefield.getBattlefield();
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
		client = new Client();
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
		
		new Thread(client).start();
		
	}

}
