import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.io.*;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

public class RepServer implements ServerInterface{
	private static List<List<String>> movies = new ArrayList<>();
	private static List<List<String>> ratings = new ArrayList<>();
	private static List<RepServer> servers = new ArrayList<>();
	private Status status = Status.OFFLINE;
	private int id;
	public RepServer(int id){
		this.id = id;
		status = Status.ACTIVE;
		try (BufferedReader br = new BufferedReader(new FileReader("./ml-latest-small/movies.csv"))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		        String[] values = line.split(",");
		        movies.add(Arrays.asList(values));
		    }
		} catch (Exception e) {
			System.err.println("Server Exception: " + e.toString());
			e.printStackTrace();
		}
		try (BufferedReader br = new BufferedReader(new FileReader("./ml-latest-small/ratings.csv"))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		        String[] values = line.split(",");
		        ratings.add(Arrays.asList(values));
		    }
		} catch (Exception e) {
			System.err.println("Server Exception: " + e.toString());
			e.printStackTrace();
		}
	}
	public String sayHello() {
		return "Hello, World!";
	}
	public List<List<String>> getRatingsList(){
		return ratings;
	}
	public String getServers(){
		String output = "";
		for(int i = 0; i < servers.size(); i++){
			output = output + "Server " + i + " - " + servers.get(i).getStatus(); 
		}
		return output;
	}
	public void setServerStatus(int serverNo, Status st) {
		setStatus(st);
	}
	public Status getStatus(){
		return status;
	}
	public void setStatus(Status s){
		status = s;
	}
	public void gossipServers(List<RepServer> servers) {
		this.servers = servers;
	}
	public float getRating(String mName){
		int movieID=-1;
		for(int i = 0; i < movies.size(); i++) {
			if (movies.get(i).get(1).contains(mName)) {
				movieID = Integer.parseInt(movies.get(i).get(0));
				break;
			}
		}
		if(movieID == -1){return 0;}
		int sum = 0;
		int count = 0;
		for(int j = 0; j < ratings.size(); j++) {
			if(Integer.parseInt(ratings.get(j).get(0)) == movieID) {
				sum += Float.parseFloat(ratings.get(j).get(1));
				count += 1;
			}
		}
		float average = (float)sum/(float)count;
		return average;
	}
	//Need to add in the feature to submit and update movie ratings;
	//Potentially, don't use the ratings.csv and instead use the movies.csv,
	//and add another column which holds the rating for the movie.
	public Boolean submitRating(String mName, int rating) {
		int movieID = -1;
		for(int i = 0; i < movies.size(); i++) {
			if (movies.get(i).get(1).contains(mName)) {
				movieID = Integer.parseInt(movies.get(i).get(0));
				break;
			}
		}
		if(movieID == -1){return false;}
		List<String> newRating = Arrays.asList(Integer.toString(movieID), Integer.toString(rating), Long.toString(ZonedDateTime.now().toInstant().toEpochMilli()));
		ratings.add(newRating);
		//System.out.println(ratings.get(ratings.size()-1));
        return true;
	}
	public static void gossip(){
		for (int i = 0; i<servers.size(); i++) {
			List<List<String>> other = servers.get(i).getRatingsList();
			if (ratings.size() < other.size()){
				ratings = other;
			}
		}
	}
	public static void main(String args[]){
		while(true){
			try{
				TimeUnit.SECONDS.sleep(1);
			} catch (Exception e) {
				System.out.println("exception :" + e.getMessage());
			}
			gossip();
		}
	}
}