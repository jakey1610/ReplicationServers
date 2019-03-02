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
	private int[] vecClock = new int[]{0,0,0};
	private Status status = Status.OFFLINE;
	private int id;
	public RepServer(int id){
		this.id = id;
		status = Status.ACTIVE;
		try (BufferedReader br = new BufferedReader(new FileReader("./ml-latest-small/movies.csv"))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		        String[] values = line.split(",");
						List<String> listValues = new ArrayList<String>(Arrays.asList(values));
						// var status = false;
						// while (!status){
						// 	System.out.println(listValues);
						// 	if (!(listValues.get(1).contains(")")) || listValues.get(2) == "(no genres listed)" || listValues.get(2) == "") {
						// 			 status = true;
						// 	 } else {
						// 			 status = false;
						// 			 String i = listValues.get(1);
						// 			 String j = listValues.get(2);
						// 			 i= i.concat(j);
						// 			 listValues.set(0,i);
						// 			 listValues.remove(1);
						// 	 }
						// }
		        movies.add(listValues);
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
		vecClock[id-1]+=1;
		return average;
	}

	public Boolean submitRating(String mName, int rating) {
		int movieID = -1;
		int largestMovieID = 1;
		for(int i = 0; i < movies.size(); i++) {
		  if (movies.get(i).get(1).contains(mName) && movieID == -1) {
		    movieID = Integer.parseInt(movies.get(i).get(0));
		  }
		  if(Integer.parseInt(movies.get(i).get(0))>largestMovieID){
		    largestMovieID = Integer.parseInt(movies.get(i).get(0));
		  }
		}
		if(movieID == -1){
		  movieID = largestMovieID + 1;
		  movies.add(Arrays.asList(Integer.toString(movieID), mName, ""));
			try{
				for(int i = 0; i < servers.size(); i++){
					servers.get(i).addToMoviesList(Arrays.asList(Integer.toString(movieID), mName, ""));
				}
			} catch(RemoteException e){
				System.err.println(e);
			}
		}
		List<String> newRating = Arrays.asList(Integer.toString(movieID), Integer.toString(rating), Long.toString(ZonedDateTime.now().toInstant().toEpochMilli()));
		ratings.add(newRating);
		//System.out.println(ratings.get(ratings.size()-1));
		logs.add(new ArrayList<String>(Arrays.asList(Integer.toString(id), "SR-"+newRating.get(0), Long.toString(new Timestamp(System.currentTimeMillis()).getTime()), newRating.get(1))));
		vecClock[id-1]+= 1;
    return true;
	}

	public List<List<String>> getMoviesList(){
	  return movies;
	}

	public void addToMoviesList(List<String> movie){
		movies.add(movie);
	}

	public int[] getVecClock(){
		return vecClock;
	}

	public void setVecClock(int[] vc){
		vecClock = vc;
	}

	//Need to increment the vector clock of each machine for the following:
	// * When sent communication between RepServer (check)
	// * For internal event (check)
	// * Receiving a message (check)
	// * Update to be max of vecClock and received vecClock. (check)
	// 		- Then need to make the data consistent by checking logs. (check)
	public void gossip(){
		try{
			for (int i = 0; i<servers.size(); i++) {
				// vecClock[id-1] += 1;
				// vecClock[i] += 1;
				int[] diff = new int[3];
				for(int j =0; j<3; j++){
		      diff[j] = servers.get(i).getVecClock()[j] - vecClock[j];
		    }
				//For all of the positive diffs go and get the logs for the appropriate servers and update the data accordingly.
				for(int l = 0; l < diff.length; l++){
					if(diff[l] > 0){
						//get the logs for server l
						List<List<String>> other = servers.get(i).getLogsList();
						logs.addAll(other.subList(other.size()-diff[l], other.size()));
						for (int k = logs.size()-1; k >= logs.size()-diff[l]; k--){
							if (logs.get(k).get(1).contains("SR")){
								String movieIDGiven = logs.get(k).get(1).split("-")[1];
								String ratingGiven = logs.get(k).get(3);
								String ts = logs.get(k).get(2);
								//Check the list equality as below and this should work.
								if ((new ArrayList<>(Arrays.asList(movieIDGiven, ratingGiven, ts))).equals(ratings.get(ratings.size()-1))){
									continue;
								} else {
									ratings.add(new ArrayList<>(Arrays.asList(movieIDGiven, ratingGiven, ts)));
								}
							}
						}
						vecClock[l]+=diff[l];
					}
				}
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
					TimeUnit.SECONDS.sleep(3);
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
