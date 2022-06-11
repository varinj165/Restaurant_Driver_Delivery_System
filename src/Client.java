import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client extends Thread {
	private BufferedReader br;
	private PrintWriter pw;
	private boolean started;
	
	public Client(String hostname, int port) {
		started = false;
		try {
			Socket s = new Socket(hostname, port);
			br = new BufferedReader(new InputStreamReader(s.getInputStream()));
			pw = new PrintWriter(s.getOutputStream());
			// Will receive how many drivers are left
			String line = br.readLine();
			
			int numDriversLeft = Integer.parseInt(line);
			
			if (numDriversLeft == 0) {
				System.out.println("Starting service.");
				started = true;
			} 
			
			else {
				System.out.println("" + numDriversLeft + " more driver(s) is needed before the service can begin.");
				System.out.println("Waiting...");
			}
			
			this.start();
		}
		
		catch(IOException ioe) {
			System.out.println("ioe in ChatClient constructor: " + ioe.getMessage());
		}
	}
	
	public void run() {
		if (started) {
			try {
				while(true) {
					String line = br.readLine();
					if (!line.equalsIgnoreCase("start")) {
						System.out.println(line);
					}
				}
			} 
			
			catch (IOException ioe) {
				System.out.println("ioe in Client.run(): " + ioe.getMessage());
			}
		}
		
		else {
			try {
				while(true) {
					String line = br.readLine();
					if (line.equalsIgnoreCase("start")) {
						System.out.println("Starting service.");
						started = true;
					}
					
					else {
						System.out.println(line);
					}
				}
			}
			
			catch (IOException ioe) {
				System.out.println("ioe in Client.run(): " + ioe.getMessage());
			}
		}
	}
	
	public static void main(String[] args) {
		Scanner in = new Scanner(System.in);
		String hostname;
		int port;
		
		System.out.println("Welcome to SalEats v2.0!");
		
		System.out.print("Enter the server hostname: ");
		hostname = in.nextLine();
		System.out.println("");
		
		System.out.print("Enter the server port: ");
		port = in.nextInt();
		
		Client c = new Client(hostname, port);
		
		in.close();
	}
}
