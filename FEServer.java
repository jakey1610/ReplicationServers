import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.io.*;
import java.util.concurrent.TimeUnit;
//Create registry from the main function using the function in GC
//Try to run RepServer from here
public class FEServer implements FEServerInterface {
	private static List<ServerInterface> replicationServers = new ArrayList<>();
	private static List<Status> repStatus = new ArrayList<>();
	private static int curServer = 0;
	private static Boolean changeSRandom = false;
	public FEServer() {}
	public void updateStatus(){
		repStatus = new ArrayList<>();
		try{
			for (int i = 0; i<replicationServers.size(); i++){
				repStatus.add(replicationServers.get(i).getStatus());
			}
		} catch(RemoteException e){
			e.printStackTrace();
		}
	}
	public String getServers(){
		String output = "";
		try{
			for(int i = 0; i < replicationServers.size(); i++){
				output = output + "Server" + i + " - " + replicationServers.get(i).getStatus() + "\n";
			}
		} catch(RemoteException e){
			e.printStackTrace();
		}
		return output;
	}
	public void setServerStatus(int serverNo, Status status){
		try{
			//Make printthis method in client to print this on the client.
			System.out.println("Server " + serverNo + " set to status: " + status + ".\n");
			replicationServers.get(serverNo).setServerStatus(serverNo, status);
		} catch(RemoteException e){
			e.printStackTrace();
		}
	}
	//This needs to be tested before committing.
	public static void randomServerStatus(){
		//select the random server
		double RD = Math.random();
		RD = RD * 3;
		int randomServer = (int) RD;
		//select the random Status
		List<Status> statuses = new ArrayList<Status>(Arrays.asList(Status.ACTIVE, Status.OFFLINE, Status.OVERLOADED));
		RD = Math.random();
		RD = RD * 3;
		int Rd = (int) RD;
		Status randomStatus = statuses.get(Rd);
		//apply the status to the server
		try{
			replicationServers.get(randomServer).setStatus(randomStatus);
		} catch (RemoteException e){
			System.err.println(e);
		}
	}

	public void setRandomStatus(Boolean s){
		changeSRandom = s;
	}
	public String sayHello() {
		return "Hello, World!";
	}
	public float getRating(String mName){
		updateStatus();
		float rate = 0;
		int chosenServer = curServer;
		try{
			while (replicationServers.get(chosenServer).getStatus() != Status.ACTIVE){
				chosenServer += 1;
				chosenServer %= 3;
			}
			if(replicationServers.get(curServer).getStatus() != Status.OVERLOADED){
				curServer = chosenServer;
			}
		} catch(RemoteException e) {
			System.err.println(e);
		}

		// if (chosenServer == -1) {
		// 	System.out.println("Server currently unavailable...");
		// }
		// if (chosenServer == -1) {
		// 	replicationServers.add(new RepServer(replicationServers.size()-1));
		// 	repStatus.add(Status.ACTIVE);
		// }
		System.out.println("Chosen Server: " + chosenServer);
		try{
			rate = replicationServers.get(chosenServer).getRating(mName);
		} catch(RemoteException e){
			e.printStackTrace();
		}
		return rate;
	}
	public Boolean submitRating(String mName, int rating) {
		updateStatus();
		Boolean rated = false;
		int chosenServer = curServer;
		try{
			while (replicationServers.get(chosenServer).getStatus() != Status.ACTIVE){
				chosenServer += 1;
				chosenServer %= 3;
			}
			if(replicationServers.get(curServer).getStatus() != Status.OVERLOADED){
				curServer = chosenServer;
			}
		} catch(RemoteException e) {
			System.err.println(e);
		}
		// if (chosenServer == -1) {
		// 	replicationServers.add(new RepServer(replicationServers.size()));
		// 	repStatus.add(Status.ACTIVE);
		// 	chosenServer = replicationServers.size()-1;
		// 	for (int j = 0; j<replicationServers.size(); j++) {
		// 		List<RepServer> repServerCopy = new ArrayList<>(replicationServers);
		// 		repServerCopy.remove(j);
		// 		replicationServers.get(j).gossipServers(repServerCopy);
		// 	}
		// }
		System.out.println("Chosen Server: " + chosenServer);
		try{
			rated = replicationServers.get(chosenServer).submitRating(mName,rating);
		} catch(RemoteException e){
			e.printStackTrace();
		}
		return rated;
	}
	public static void main(String args[]){
		try{
			//createRegistry(37029);
			LocateRegistry.createRegistry(37029);
			Registry registry = LocateRegistry.getRegistry("localhost", 37029);
			FEServer obj = new FEServer();
			FEServerInterface Fstub = (FEServerInterface) UnicastRemoteObject.exportObject(obj,0);
			registry.bind("FEServer", Fstub);
			Thread thread = new Thread(new Runnable() {

			     public void run() {
						 try{
							 RepServer RS = new RepServer(4);
				 			 RS.main(new String[0]);
							 while(true){
								 if(changeSRandom){
									 randomServerStatus();
								 }
								 TimeUnit.SECONDS.sleep(15);
							 }
						 } catch(Exception e) {
							 System.err.println(e);
						 }
			     }

			});
			thread.start();

			//Lookup is not working here
			//Check this; try to find a way not completely dependent on sleep
			TimeUnit.SECONDS.sleep(3);
			ServerInterface RStub1 = (ServerInterface) registry.lookup("RServer1");
			ServerInterface RStub2 = (ServerInterface) registry.lookup("RServer2");
			ServerInterface RStub3 = (ServerInterface) registry.lookup("RServer3");
			replicationServers.add(RStub1);
			replicationServers.add(RStub2);
			replicationServers.add(RStub3);
			repStatus.add(Status.ACTIVE);
			repStatus.add(Status.ACTIVE);
			repStatus.add(Status.ACTIVE);
			for (int j = 0; j<replicationServers.size(); j++) {
				List<ServerInterface> repServerCopy = new ArrayList<>(replicationServers);
				repServerCopy.remove(j);
				replicationServers.get(j).gossipServers(repServerCopy);
			}

		} catch (Exception e) {
			System.err.println("Server Exception: " + e.toString());
			e.printStackTrace();
		}
	}
}
