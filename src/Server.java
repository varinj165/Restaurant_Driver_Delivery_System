import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.Vector;

public class Server {
	private Vector<ServerThread> serverThreads;
	private Vector<String[]> orders;
	private int numDrivers;
	private int port;
	private double currLat;
	private double currLong;
	
	public Server(int port, int numDrivers, Vector<String[]> orders, double currLat, double currLong) {
		this.orders = orders;
		this.numDrivers = numDrivers;
		this.port = port;
		this.currLat = currLat;
		this.currLong = currLong;
		
		try {
			ServerSocket ss = new ServerSocket(port);
			serverThreads = new Vector<ServerThread>();
			int currDrivers = 0;
			
			System.out.println("Listening on port " + port + ".");
			System.out.println("Waiting for drivers...");
			
			while (currDrivers < this.numDrivers) {
				Socket s = ss.accept(); // Will wait here till there is a connection
				System.out.println("Connection from: " + s.getInetAddress());
				++currDrivers;
				if (this.numDrivers - currDrivers > 0) {
					System.out.println("Waiting for " + (numDrivers - currDrivers) + " more driver(s)...");
				}
				ServerThread st = new ServerThread(s, this, (numDrivers - currDrivers), currDrivers, currLat, currLong);
				serverThreads.add(st);
			}
			
			// All drivers have connected
			System.out.println("Starting service.");
			// Signal all drivers to start
			for (int i = 0; i < serverThreads.size(); ++i) {
				serverThreads.get(i).startDelivery();
				// serverThreads.get(i).startSignal();
			}
			
			// List of times the orders start
			Vector<Integer> orderTimes = new Vector<Integer>();
			
			// Store all times required for all orders
			for (int i = 0; i <  orders.size(); ++i) {
				orderTimes.add(Integer.parseInt(orders.get(i)[0]));
			}
			
			int time = 0;
			int orderIndex = 0; // To keep track of complete orders
			
			try {
				// Main timer
				while (orderIndex < orders.size()) {
					Vector<String[]> ordersForCurrDriver = new Vector<String[]>();
					for (int i = orderIndex; i < orders.size(); ++i) {
						if (orderTimes.get(i) == time) {
							ordersForCurrDriver.add(orders.get(i));
							++orderIndex;
						}
					}
					
					// We have orders to dispatch
					if (ordersForCurrDriver.size() > 0) {
						boolean driverChosen = false;
						
						while (!driverChosen) {
							// Search for an available driver
							for (int i = 0; i < serverThreads.size(); ++i) {
								if (serverThreads.get(i).isReady()) {
									// Send the vector of orders to the server thread class
									ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("Intermediate" + (i + 1) + ".txt"));
									// Write vector of orders to output stream
									oos.writeObject(ordersForCurrDriver);
									// Dispatch order
									serverThreads.get(i).dispatchOrder();
									driverChosen = true;
									break;
								}
							}
						}
					}
				}
			}
			
			finally {
				
			}
		}
		
		catch (IOException ioe) {
			System.out.println("ioe in Server constructor: " + ioe.getMessage());
		}
	}
	
	public static void main (String[] args) {
		Scanner in = new Scanner(System.in);
		String scheduleName = "";
		FileReader scheduleRead;
		BufferedReader scheduleBuffer;
		Vector<String[]> orders = new Vector<String[]>();
		
		while (true) {
			System.out.print("What is the name of the file containing the schedule information? ");
			try {
				// Get name of schedule file
				scheduleName = in.nextLine();
				
				scheduleRead = new FileReader(scheduleName);
				
				scheduleBuffer = new BufferedReader(scheduleRead);
				
				String line = scheduleBuffer.readLine(); // To parse each line in schedule.txt
				
				while (line != null) {
					String[] newOrder = line.split(",");
					
					for (int i = 0; i < newOrder.length; ++i) {
						newOrder[i] = newOrder[i].trim();
					}
					
					orders.add(newOrder); // Store each order while parsing
					
					line = scheduleBuffer.readLine();
				}
				
				break;
			}
			
			// If file name is not valid
			catch (FileNotFoundException fnfe) {
				System.out.println("The file " + scheduleName + " could not be found.");
				continue;
			}
			
			catch(IOException ioe) {
				System.out.println("There are missing data parameters.");
				continue;
			}
			
			finally {
				System.out.println("");
			}
		}
		
		// To store user's co-ordinates
		double currLat;
		double currLong;
		
		System.out.print("What is your latitude? ");
		currLat = in.nextDouble();
		System.out.println("");
		
		System.out.print("What is your longitude? ");
		currLong = in.nextDouble();
		System.out.println("");
		
		// To store number of drivers that will be in service
		int numDrivers;
		
		System.out.print("How many drivers will be in service today? ");
		numDrivers = in.nextInt();
		System.out.println("");
		
		Server mainServer = new Server(3456, numDrivers, orders, currLat, currLong); // Start listening on port 3456
		
		in.close();
	}
}
