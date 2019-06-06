import java.rmi.*; // needed to extend the Remote class
import java.util.*; // needed to use the Vector class

public interface ServerInterface extends Remote {
	public Vector execute(String command) throws RemoteException;
	// exception needed for error detection
}