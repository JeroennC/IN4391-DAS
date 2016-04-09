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
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((address == null) ? 0 : address.hashCode());
		result = prime * result + port;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Address))
			return false;
		Address other = (Address) obj;
		if (address == null) {
			if (other.address != null)
				return false;
		} else if (!address.equals(other.address))
			return false;
		if (port != other.port)
			return false;
		return true;
	}
	public String toString() {
		return "[Address address=" + this.address + ",port=" + this.port +"]";
	}
}
