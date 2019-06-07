import java.io.*;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;

public class FileClient extends UnicastRemoteObject implements ClientInterface {
    String locallyCachedFile = "/tmp/samreenm.txt";
    ServerInterface server = null;

    public FileClient(String servernode, int port) throws RemoteException {
        try {
            server = ( ServerInterface )
                    Naming.lookup("rmi://" + servernode + ":" + port + "/unixserver");
        } catch (Exception e) {}
        
    }
}
