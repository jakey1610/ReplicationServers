import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.io.*;

public class Client {
	private static Boolean randomStatus = false;
	private Client() {}
	public static void main(String args[]){
		try{
			Registry registry = LocateRegistry.getRegistry("localhost", 37029);
			FEServerInterface stub = (FEServerInterface) registry.lookup("FEServer");
			System.out.println("  __  __            _        _____       _            ");
			System.out.println(" |  \\/  |          (_)      |  __ \\     | |           ");
			System.out.println(" | \\  / | _____   ___  ___  | |__) |__ _| |_ ___ _ __ ");
			System.out.println(" | |\\/| |/ _ \\ \\ / / |/ _ \\ |  _  // _` | __/ _ \\ '__|");
			System.out.println(" | |  | | (_) \\ V /| |  __/ | | \\ \\ (_| | ||  __/ |   ");
			System.out.println(" |_|  |_|\\___/ \\_/ |_|\\___| |_|  \\_\\__,_|\\__\\___|_|   ");
            System.out.println("\n");
            Boolean status = false;
            while(!status){
	            System.out.println("(1) Get Rating");
	            System.out.println("(2) Submit Rating");
	            System.out.println("(3) Alter Servers");
							System.out.println("(4) Toggle Automatic Server Alteration");
	            System.out.println("(5) Exit");
	            System.out.println("Please enter your choice: ");
	            Scanner a = new Scanner(System.in);
	            int choice = a.nextInt();
	            if (choice == 1){
	            	System.out.println("\n");
	            	System.out.println("For which movie? (type 'back' to go back)");
	            	a.nextLine();
	            	String movie = a.nextLine();
	            	if (movie == "back") {
	            		System.out.println("Going back to main menu...");
	            	} else {
	            		float res = stub.getRating(movie);
	            		if (res == 0){
	            			System.out.println("There have been no ratings for this movie.");
	            		} else if(res == -1.0) {
										System.out.println("Could not process request at the moment.");
									} else {
	            			System.out.println("The movie you searched for scored a " + res + " rating.");
	            		}
	            	}
	            } else if (choice == 2) {
	            	System.out.println("For which movie? (type 'back' to go back)");
	            	a.nextLine();
	            	String movie = a.nextLine();
	            	if (movie == "back") {
	            		System.out.println("Going back to main menu...");
	            	} else {
	            		System.out.println("What rating would you like to give? ");
	            		int rating = a.nextInt();
	            		Boolean r = stub.submitRating(movie, rating);
									if(r){
										System.out.println("Update submitted.");
									} else {
										System.out.println("Could not process request at the moment.");
									}
	            	}
	            } else if (choice == 3) {
	            	System.out.print(stub.getServers());
	            	System.out.println("Please enter the number of a server to edit: ");
	            	int server = a.nextInt();
	            	System.out.println("What would you like the status of this server to be?\n");
	            	a.nextLine();
	            	String s = a.nextLine();
	            	Status st = Status.ACTIVE;
	            	if (s.contains("active")) {
	            		st = Status.ACTIVE;
	            	} else if (s.contains("offline")) {
	            		st = Status.OFFLINE;
	            	} else if (s.contains("overloaded")) {
	            		st = Status.OVERLOADED;
	            	} else {
	            		System.out.println("This is not an option...");
	            	}
	            	stub.setServerStatus(server, st);
	            } else if(choice == 4){
								stub.setRandomStatus(!randomStatus);
								randomStatus = !randomStatus;
							} else if (choice == 5) {
	            	status = true;
	            } else {
	            	System.out.println("Sorry, this is not one of the choices.");
	            }

	        }
			//float res = stub.getRating("Toy Story");
			//System.out.println("Response: " + res);
		} catch (Exception e) {
			System.err.println("Client Exception: " + e.toString());
			e.printStackTrace();
		}
	}
}
