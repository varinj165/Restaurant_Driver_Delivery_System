import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.Calendar;
import java.util.Vector;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;

public class ServerThread extends Thread {
	private Socket serverConnection;
	private Server mainServer;
	private PrintWriter pw;
	private BufferedReader br;
	private boolean ready;
	private int threadNumber;
	private double currLat;
	private double currLong;
	
	
	private Lock lock = new ReentrantLock();
	
	private Condition allDriversConnected = lock.newCondition();
	
	private Condition orderReceived = lock.newCondition();
	
	public ServerThread(Socket s, Server mainServer, int numDriversLeft, int threadNumber, double currLat, double currLong) {
		this.currLat = currLat;
		this.currLong = currLong;
		this.threadNumber = threadNumber;
		// Driver is ready to accept orders
		ready = true;
		try {
			this.serverConnection = s;
			this.mainServer = mainServer;
			
			pw = new PrintWriter(s.getOutputStream());
			br = new BufferedReader(new InputStreamReader(s.getInputStream()));
			
			pw.println("" + numDriversLeft);
			pw.flush();
			
			this.start();
		}
		
		catch (IOException ioe) {
			System.out.println("ioe in ServerThread constructor: " + ioe.getMessage());
		}
	}
	
	// Signal Drivers to start
	public void startDelivery() {
		lock.lock();
		allDriversConnected.signal();
		lock.unlock();
	}
	
	// Signal driver that an order has been received
	public void dispatchOrder() {
		lock.lock();
		orderReceived.signal();
		lock.unlock();
	}
	
	// Signal clients to start
	public void startSignal() {
		pw.println("start");
		pw.flush();
	}
	
	// Tells Server if this driver is free to take orders or not
	public boolean isReady() {
		return ready;
	}
	
	// To calculate the distance
	private static double calcDistance(double lat1, double long1, double lat2, double long2) {
		double result = 3963.0 * Math.acos((Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2))) 
				+ (Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(long2 - long1))));
		return result;
	}
	
	public void run() {
		ObjectInputStream ois = null;
		Calendar cal; // Calendar to get date and time
		String datetime = "";
		
		boolean first = true;
		try {
			lock.lock();
			// Wait till all drivers are connected
			allDriversConnected.await();
			
			// Send signal to clients to start receiving and printing messages
			pw.println("start");
			pw.flush();
			
			// Complete every order that is assigned
			while (true) {
				first = false;
				// Wait till we receive an order
				orderReceived.await();
				// Driver is on an order
				ready = false;
				// Get order for current driver
				ois = new ObjectInputStream(new FileInputStream("Intermediate" + threadNumber + ".txt"));
				
				Vector<String[]> order = (Vector<String[]>) ois.readObject();
				
				// To store api search results for each order
				Vector<double[]> locations = new Vector<double[]>();
				
				// Get and store all information about Restaurants
				for (int i = 0; i < order.size(); ++i) {
					// Get restaurant info from API calls
					String url = "https://api.yelp.com/v3/businesses/search?term=" + order.get(i)[1].replaceAll("\\s+", "-").toLowerCase() + "&latitude=" + currLat + "&longitude=" + currLong;
					URL obj = new URL(url);
					
					HttpURLConnection con = (HttpURLConnection) obj.openConnection();
					
					con.setRequestMethod("GET");
					con.setRequestProperty("Authorization", "Bearer aDWqt7fvVZQr1V8ZBMIEtSX4UdlDOutVWK5ZBWgOk1r9qo5HVL3uKKC-2uCgrPMBA_6WBP8Y91M2VhonSLO9sfiZ2mRyxfOVZf0Q5M6CkfrNJIEdDKXkE_PBVGaDXnYx");
					
					con.connect();
					
					BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
					
					String inputLine;
					StringBuffer response = new StringBuffer();
					
					while ((inputLine = in.readLine()) != null) {
						response.append(inputLine);
					}
					
					in.close();
					
					Gson gson = new Gson();
					AllRestaurants myResponse = gson.fromJson(response.toString(), AllRestaurants.class);
					
					double[] location1 = new double[2];
					location1[0] = myResponse.getBusinesses().get(0).getCoordinates().getLatitude(); // Latitude
					location1[1] = myResponse.getBusinesses().get(0).getCoordinates().getLongitude(); // Longitude
					
					locations.add(location1);
				}
				
				if (first) {
					// Initial delay
					Thread.sleep(1000 * Integer.parseInt(order.get(0)[0]));
					
					while (order.size() > 0) {
						// Calculate min distance
						double minDist = calcDistance(currLat, currLong, locations.get(0)[0], locations.get(0)[1]);
						int minDistIndex = 0;
						
						for (int i = 0; i < order.size(); ++i) {
							double dist = calcDistance(currLat, currLong, locations.get(i)[0], locations.get(i)[1]);
							
							if (dist < minDist) {
								minDist = dist;
								minDistIndex = i;
							}
						}
						
						cal = Calendar.getInstance();
						
						// Update time
						datetime = "[" + cal.get(Calendar.HOUR_OF_DAY);
						datetime += ":" + cal.get(Calendar.MINUTE);
						datetime += ":" + cal.get(Calendar.SECOND);
						datetime += "." + cal.get(Calendar.MILLISECOND) + "]";
						
						System.out.println(datetime + " Starting delivery of " + order.get(minDistIndex)[2] + " to " + order.get(minDistIndex)[1] + ".");
						
						int minDistInt = (int) Math.round(minDist);
						
						Thread.sleep(1000 * minDistInt);
						
						cal = Calendar.getInstance();
						
						// Update time
						datetime = "[" + cal.get(Calendar.HOUR_OF_DAY);
						datetime += ":" + cal.get(Calendar.MINUTE);
						datetime += ":" + cal.get(Calendar.SECOND);
						datetime += "." + cal.get(Calendar.MILLISECOND) + "]";
						
						System.out.println(datetime + " Starting delivery of " + order.get(minDistIndex)[2] + " to " + order.get(minDistIndex)[1] + ".");
					}
					
					
				}
			}
		}
		
		catch(ClassNotFoundException cnfe) {
			System.out.println("Vector was not found");
		}
		
		catch(FileNotFoundException fnfe) {
			System.out.println("Reading Error in opening vector file");
		}
		
		catch(IOException ioe) {
			
		}
 		
		catch(InterruptedException ie) {
			
		}
		
		finally {
			try {
				ois.close();
			}
			
			catch (IOException ioe) {
				System.out.println("IOException in closing input stream");
			}
			
			lock.unlock();
		}
	}
}
