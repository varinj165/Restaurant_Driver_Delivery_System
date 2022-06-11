import java.util.Vector;

public class AllRestaurants {
	private Vector<Restaurant> businesses = null;
	
	public void setBusinesses(Vector<Restaurant> businesses) {
		this.businesses = businesses;
	}
	
	public Vector<Restaurant> getBusinesses() {
		return this.businesses;
	}
	
	public void addToData(Restaurant rest1) {
		this.businesses.add(rest1);
	}
}
