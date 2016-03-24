package das.message;

public class Address {
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
