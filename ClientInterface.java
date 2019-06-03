import java.rmi.*;
import java.util.Vector;

public interface ClientInterface extends Remote {
	public void receiveMessage(String command) throws RemoteException;
}
