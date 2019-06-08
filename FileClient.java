import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;



public class FileClient extends UnicastRemoteObject implements ClientInterface {


    String locallyCachedFile = "/tmp/--.txt";
    static ServerInterface server = null;
    String myIpName;
    File file;
    BufferedReader input;


    class File {
        private byte[] bytes;
        private String name;
        private int state;

        private static final int state_invalid = 0;
        private static final int state_readshared = 1;
        private static final int state_writeowned = 2;
        private static final int state_back2readshared = 3;

        private boolean inEdit;

        private boolean ownership;

        private String myIpName;

        public File() {
            this.state = 0;
            this.inEdit = false;
            this.name = "";
            this.ownership = false;
            this.bytes = null;
            this.myIpName = null;

            try {
                InetAddress myaddress = InetAddress.getLocalHost();
                this.myIpName = myaddress.getHostName();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }


        }

        public boolean download(String filename, String mode){
            FileContents contents = server.download(myIpName, filename, mode);
            bytes = contents.get();
            return true;
        }

        public boolean upload(){
            FileContents contents = new FileContents(bytes);
            server.upload(myIpName, name, contents);
            return true;
        }

        public boolean invalidate( ) throws RemoteException{
            return true;
        }

        private boolean execUnixCommand(String command, String mode, String path) {
            try {
                Runtime runtime = Runtime.getRuntime( );
                Process process = runtime.exec( command );
                int retval = process.waitFor();

            } catch ( Exception e ) {
                e.printStackTrace( );
                return false;
            }

            return true;
        }

        public boolean launchEmacs(String mode) {
            if (!execUnixCommand("chmod", "600", "/tmp/"+ myIpName +".txt")) {
                return false;
            } else {

                try {

                    FileOutputStream fileOutput = new FileOutputStream("/tmp/\"+ myIpName +\".txt");
                    fileOutput.write(this.bytes);
                    fileOutput.flush();
                    fileOutput.close();

                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }

                if (!this.execUnixCommand("chmod", mode.equals("r") ? "400" : "600", "/tmp/"+ myIpName +".txt")) {
                    return false;
                } else {

                    boolean result = this.execUnixCommand("emacs", "/tmp/"+ myIpName +".txt", "");

                    if (result && mode.equals("w")) {


                        try {
                            FileInputStream fileInput = new FileInputStream("/tmp/"+ myIpName +".txt");
                            this.bytes = new byte[fileInput.available()];
                            fileInput.read(this.bytes);
                            fileInput.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                            return false;
                        }


                    }

                    return true;
                }

            }
        }


    }


    //methods: download, upload, invalidate, writeback
    public static void main(String[] args) throws Exception {

        if(args.length != 2){
            System.err.println("usage: java FileClient server_ip port#");
            System.exit(-1);
        }


        FileClient client = new FileClient(args[0],args[1]);

        Naming.rebind("rmi://localhost:" + args[1] + "/fileclient"
                + " invokded", client);

        client.loop();

    }

    public FileClient(String serverName, String port) throws  Exception{

        server = (ServerInterface)Naming.lookup("rmi://"+ serverName +":" + port + "/fileserver");

        file = new File();

        input = new BufferedReader(new InputStreamReader(System.in));

        try {
            InetAddress myaddress = InetAddress.getLocalHost();
            this.myIpName = myaddress.getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }

    public  void loop(){


        while (true){
            System.out.println("FileClient: Next file to open");

            System.out.print("\tFile name:");
            String fileName = keyborad.next();
            System.out.println();


            System.out.print("\tHow(r/w): ");
            String accessMode = keyborad.next();
            System.out.println();



        }


    }

}
