import java.rmi.Remote;
import java.rmi.RemoteException;

public interface FEServerInterface extends Remote{
	String sayHello() throws RemoteException;
	float getRating(String mName) throws RemoteException;
	Boolean submitRating(String mName, int rating) throws RemoteException;
	String getServers() throws RemoteException;
	void setServerStatus(int serverNo, Status st) throws RemoteException;
}
