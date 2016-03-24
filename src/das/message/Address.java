package das.message;

import java.io.Serializable;

public class Address implements Serializable {
	private static final long serialVersionUID = 3983271924100172349L;
	
	String address;
	int port;
	
	public Address(String address, int port) {
		super();
		this.address = address;
		this.port = port;
	}
	
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
}
