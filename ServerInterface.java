import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.*;
import java.io.*;

public interface ServerInterface extends Remote{
	String sayHello() throws RemoteException;
	float getRating(String mName) throws RemoteException;
	Boolean submitRating(String mName, int rating) throws RemoteException;
	String getServers() throws RemoteException;
	void setServerStatus(int serverNo, Status st) throws RemoteException;
	Status getStatus() throws RemoteException;
	void gossipServers(List<ServerInterface> servers) throws RemoteException;
	List<List<String>> getLogsList() throws RemoteException;
	List<List<String>> getRatingsList() throws RemoteException;
	void gossip() throws RemoteException;
}
