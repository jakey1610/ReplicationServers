import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.io.*;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;
import java.sql.Timestamp;
import java.math.*;

public class RepServer implements ServerInterface{
	private List<List<String>> movies = new ArrayList<>();
	private List<List<String>> ratings = new ArrayList<>();
	private List<ServerInterface> servers = new ArrayList<>();
	private List<List<String>> logs = new ArrayList<>();
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
	public List<List<String>> getLogsList(){
		return logs;
	}
	public List<List<String>> getRatingsList(){
		return ratings;
	}
	public String getServers(){
		String output = "";
		try{
			for(int i = 0; i < servers.size(); i++){
				output = output + "Server " + i + " - " + servers.get(i).getStatus();
			}
		} catch(RemoteException e) {
			e.printStackTrace();
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
	public void gossipServers(List<ServerInterface> servers) {
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

		logs.add(new ArrayList<String>(Arrays.asList(Integer.toString(id), "GR-"+movieID, Long.toString(new Timestamp(System.currentTimeMillis()).getTime()))));
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
		logs.add(new ArrayList<String>(Arrays.asList(Integer.toString(id), "SR-"+newRating.get(0), Long.toString(new Timestamp(System.currentTimeMillis()).getTime()), newRating.get(1))));
    return true;
	}
	//data being added repeatedly. Logs are correct
	public void gossip(){
		try{
			int lOld = 0;
			for (int i = 0; i<servers.size(); i++) {
				List<List<String>> other = servers.get(i).getLogsList();
				int o = 0;
				int l = 0;
				List<List<String>> combLogs = new ArrayList<>();
				while(true){
					//Need to check for both being equal to size
					if (l == logs.size() && o < other.size()){
						combLogs.addAll(other.subList(o, other.size()));
						logs = combLogs;
						break;
					} else if (o == other.size() && l < logs.size()) {
						combLogs.addAll(logs.subList(l, logs.size()));
						logs = combLogs;
						break;
					} else if (o < other.size() && l < logs.size()) {
						//attempt to interleave the logs on this server and others here to create joint dataset
						if ((new BigInteger(logs.get(l).get(2))).compareTo((new BigInteger(other.get(o).get(2)))) == 1){
							combLogs.add(other.get(o));
							o+=1;
						} else if ((new BigInteger(logs.get(l).get(2))).compareTo((new BigInteger(other.get(o).get(2)))) == -1){
							combLogs.add(logs.get(l));
							l+=1;
						} else {
							combLogs.add(logs.get(l));
							o+=1;
							l+=1;
						}
					} else {
						break;
					}
					
				}
				logs = combLogs;
				//Now we have the logs in the right order need to edit data to fit the logs.
				//data being added repeatedly because same logs are being viewed again and again
				for(int j = 0; j < logs.size(); j++){
					if(Integer.parseInt(logs.get(j).get(0)) != id && logs.get(j).get(1).contains("SR")){
						String movieIDGiven = logs.get(j).get(1).split("-")[1];
						String ratingGiven = logs.get(j).get(3);
						String ts = logs.get(j).get(2);
						//Check the list equality as below and this should work.
						if ((new ArrayList<>(Arrays.asList(movieIDGiven, ratingGiven, ts))).equals(ratings.get(ratings.size()-1))){
							continue;
						} else {
							ratings.add(new ArrayList<>(Arrays.asList(movieIDGiven, ratingGiven, ts)));
						}
					}
				}
				System.out.println(ratings.subList(ratings.size()-3, ratings.size()));
			}
			//System.out.println(ratings.get(ratings.size()-1));
		} catch(RemoteException e){
			e.printStackTrace();
		}
	}
	public static void main(String args[]){
		try{
			Registry registry = LocateRegistry.getRegistry("localhost", 37029);
			RepServer obj1 = new RepServer(1);
			ServerInterface RStub1 = (ServerInterface) UnicastRemoteObject.exportObject(obj1,0);
			registry.bind("RServer1", RStub1);
			RepServer obj2 = new RepServer(2);
			ServerInterface RStub2 = (ServerInterface) UnicastRemoteObject.exportObject(obj2,0);
			registry.bind("RServer2", RStub2);
			RepServer obj3 = new RepServer(3);
			ServerInterface RStub3 = (ServerInterface) UnicastRemoteObject.exportObject(obj3,0);
			registry.bind("RServer3", RStub3);
			while(true){
				try{
					TimeUnit.SECONDS.sleep(5);
				} catch (Exception e) {
					System.out.println("exception :" + e.getMessage());
				}
				RStub1.gossip();
				RStub2.gossip();
				RStub3.gossip();
			}
			//Create a new stub to interact with the Replication Servers
		} catch (Exception e) {
			System.err.println("Server Exception: " + e.toString());
			e.printStackTrace();
		}
	}
}
