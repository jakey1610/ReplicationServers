import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.io.*;

//Potentially send the list of other servers to each server so that they can "gossip"
public class FEServer implements FEServerInterface {
	private static List<ServerInterface> replicationServers = new ArrayList<>();
	private static List<Status> repStatus = new ArrayList<>();
	public FEServer() {
		// for (int i = 0; i<3; i++) {
		// 	replicationServers.add(new RepServer(i));
		// 	repStatus.add(Status.ACTIVE);
		// }
		try{
			for (int j = 0; j<replicationServers.size(); j++) {
				List<ServerInterface> repServerCopy = new ArrayList<>(replicationServers);
				repServerCopy.remove(j);
				replicationServers.get(j).gossipServers(repServerCopy);
			}
		} catch(RemoteException e) {
			e.printStackTrace();
		}
	}
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
			System.out.println("Server " + serverNo + " set to status: " + status + ".\n");
			replicationServers.get(serverNo).setServerStatus(serverNo, status);
		} catch(RemoteException e){
			e.printStackTrace();
		}
	}
	public String sayHello() {
		return "Hello, World!";
	}
	public float getRating(String mName){
		updateStatus();
		float rate = 0;
		int chosenServer = -1;
		for (int i = 0; i < replicationServers.size(); i++) {
			if (repStatus.get(i) == Status.ACTIVE) {
				chosenServer = i;
				break;
			}
		}
		if (chosenServer == -1) {
			System.out.println("Server currently unavailable...");
		}
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
		int chosenServer = -1;
		for (int i = 0; i < replicationServers.size(); i++) {
			if (repStatus.get(i) == Status.ACTIVE) {
				chosenServer = i;
				break;
			}
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
			FEServer obj = new FEServer();
			FEServerInterface Fstub = (FEServerInterface) UnicastRemoteObject.exportObject(obj,0);
			Registry registry = LocateRegistry.getRegistry("localhost", 37029);
			registry.bind("FEServer", Fstub);
			ServerInterface RStub1 = (ServerInterface) registry.lookup("RServer1");
			ServerInterface RStub2 = (ServerInterface) registry.lookup("RServer2");
			ServerInterface RStub3 = (ServerInterface) registry.lookup("RServer3");
			replicationServers.add(RStub1);
			replicationServers.add(RStub2);
			replicationServers.add(RStub3);
			repStatus.add(Status.ACTIVE);
			repStatus.add(Status.ACTIVE);
			repStatus.add(Status.ACTIVE);
			//Create a new stub to interact with the Replication Servers
		} catch (Exception e) {
			System.err.println("Server Exception: " + e.toString());
			e.printStackTrace();
		}
	}
}
