import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.io.*;

//Potentially send the list of other servers to each server so that they can "gossip"
public class FEServer implements ServerInterface {
	private List<RepServer> replicationServers = new ArrayList<>();
	private List<Status> repStatus = new ArrayList<>();
	public FEServer() {
		for (int i = 0; i<3; i++) {
			replicationServers.add(new RepServer(i));
			repStatus.add(Status.ACTIVE);
		}
		for (int j = 0; j<replicationServers.size(); j++) {
			List<RepServer> repServerCopy = new ArrayList<>(replicationServers);
			repServerCopy.remove(j);
			replicationServers.get(j).gossipServers(repServerCopy);
		}
	}
	public void updateStatus(){
		repStatus = new ArrayList<>();
		for (int i = 0; i<replicationServers.size(); i++){
			repStatus.add(replicationServers.get(i).getStatus());
		}
	}
	public String getServers(){
		String output = "";
		for(int i = 0; i < replicationServers.size(); i++){
			output = output + "Server" + i + " - " + repStatus.get(i); 
		}
		return output;
	}
	public void setServerStatus(int serverNo, Status status){
		System.out.println("Server " + serverNo + " set to status: " + status + ".");
		replicationServers.get(serverNo).setServerStatus(serverNo, status);
	}
	public String sayHello() {
		return "Hello, World!";
	}
	public float getRating(String mName){
		updateStatus();
		int chosenServer = -1;
		for (int i = 0; i < replicationServers.size(); i++) {
			if (repStatus.get(i) == Status.ACTIVE) {
				chosenServer = i;
				break;
			} 
		}
		if (chosenServer == -1) {
			replicationServers.add(new RepServer(replicationServers.size()-1));
			repStatus.add(Status.ACTIVE);
		}
		System.out.println("Chosen Server: " + chosenServer);
		return replicationServers.get(chosenServer).getRating(mName);
	}
	public Boolean submitRating(String mName, int rating) {
		updateStatus();
		int chosenServer = -1;
		for (int i = 0; i < replicationServers.size(); i++) {
			if (repStatus.get(i) == Status.ACTIVE) {
				chosenServer = i;
				break;
			} 
		}
		if (chosenServer == -1) {
			replicationServers.add(new RepServer(replicationServers.size()));
			repStatus.add(Status.ACTIVE);
			chosenServer = replicationServers.size()-1;
			for (int j = 0; j<replicationServers.size(); j++) {
				List<RepServer> repServerCopy = new ArrayList<>(replicationServers);
				repServerCopy.remove(j);
				replicationServers.get(j).gossipServers(repServerCopy);
			}
		}
		System.out.println("Chosen Server: " + chosenServer);
		return replicationServers.get(chosenServer).submitRating(mName,rating);
	}
	public static void main(String args[]){
		try{
			FEServer obj = new FEServer();
			ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(obj,0);
			Registry registry = LocateRegistry.getRegistry("localhost", 37029);
			registry.bind("rServer", stub);
		} catch (Exception e) {
			System.err.println("Server Exception: " + e.toString());
			e.printStackTrace();
		}
	}
}