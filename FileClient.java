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


    static ServerInterface server = null;
    String myIpName;
    File file;
    BufferedReader input;


    //methods: download, upload, invalidate, writeback
    public static void main(String[] args) throws Exception {

        if(args.length != 2){
            System.err.println("usage: java FileClient server_ip port#");
            System.exit(-1);
        }

        //create new client
        FileClient client = new FileClient(args[0],args[1]);

        //name binding
        Naming.rebind("rmi://localhost:" + args[1] + "/fileclient"
                + " invokded", client);

        //start the client loop
        client.loop();

    }

    public FileClient(String serverName, String port) throws  Exception{

        //look up the server
        server = (ServerInterface)Naming.lookup("rmi://"+ serverName +":" + port + "/fileserver");

        //create a new empty file
        file = new File();

        //initialize the buffer for user input
        input = new BufferedReader(new InputStreamReader(System.in));

        //get my ip name
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

            try {
                //ask the user for file name
                System.out.print("\tFile name:");
                String fileName = input.readLine();

                while(fileName.equals("")){
                    System.out.println();
                    System.out.println("Please enter a valid name for the file.");
                    System.out.print("\tFile name:");
                    fileName = input.readLine();
                }

                System.out.println();

                //ask the user for access mode
                System.out.print("\tHow(r/w): ");
                String accessMode = input.readLine();

                while(!(accessMode.equals("r") || accessMode.equals("w"))){
                    System.out.println();
                    System.out.println("Please enter a valid access mode.");
                    System.out.print("\tHow(r/w): ");
                    accessMode = input.readLine();
                }

                System.out.println();

                //if the file is a cache miss
                if(!file.hit(fileName, accessMode)){

                    //if the cached file is write owned, then write it back to the server
                    if(file.state == file.state_writeowned){
                        writeback();
                    }

                    //download the file from the server with the right mode
                    file.download(fileName, accessMode);
                }

                //launch emacs
                file.launchEmacs(accessMode);


            }catch (Exception e){
                e.printStackTrace();
            }


        }


    }

    public boolean invalidate( ) throws RemoteException{
        //change the file state to invalid
        file.state = file.state_invalid;
        return true;
    }

    public boolean writeback( ) throws 	RemoteException{
        //create a file content with the cached file
        FileContents contents = new FileContents(file.bytes);

        //upload it to the server
        server.upload(myIpName,file.name,contents);

        //change the state to read
        file.state = file.state_readshared;

        return true;
    }


    class File {
        private byte[] bytes;
        private String name;
        private int state;

        private static final int state_invalid = 0;
        private static final int state_readshared = 1;
        private static final int state_writeowned = 2;

        private String myIpName;

        public File() {
            this.state = 0;
            this.name = "";
            this.bytes = null;
            this.myIpName = null;

            try {
                InetAddress myaddress = InetAddress.getLocalHost();
                this.myIpName = myaddress.getHostName();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }


        }

        public boolean hit(String filename, String mode){

            //if the file is cached
            if(name !=  filename){
                return false;
            }

            //if the file cached with the right mode
            if(mode == "r" && (state == state_readshared || state == state_writeowned )){
                return  true;
            } else if(mode == "w" && state == state_writeowned){
                return true;
            }

            return false;
        }

        public boolean download(String filename, String mode){
            try {
                //download the filecotnet from the server
                FileContents contents = server.download(myIpName, filename, mode);

                //get the bytes from the contents
                bytes = contents.get();

            } catch (Exception e){
                e.printStackTrace();
                return false;
            }

            return true;
        }

        public boolean upload(){
            try {
                //create file contents from file bytes
                FileContents contents = new FileContents(bytes);
                //upload the file to the server
                server.upload(myIpName, name, contents);

            }catch (Exception e){
                e.printStackTrace();
                return false;
            }
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
            //check if the file is accessible
            if (!execUnixCommand("chmod", "600", "/tmp/"+ myIpName +".txt")) {
                return false;
            } else {

                try {
                    //write the bytes to the physical file
                    FileOutputStream fileOutput = new FileOutputStream("/tmp/"+ myIpName +".txt");
                    fileOutput.write(this.bytes);
                    fileOutput.flush();
                    fileOutput.close();

                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }

                //execute the unix command on the file
                if (!this.execUnixCommand("chmod", mode.equals("r") ? "400" : "600", "/tmp/"+ myIpName +".txt")) {
                    return false;
                } else {

                    //get the result from excuting the unix command
                    boolean result = this.execUnixCommand("emacs", "/tmp/"+ myIpName +".txt", "");

                    // if the unix changed the file, and the access mode is write
                    if (result && mode.equals("w")) {

                        try {

                            //get the new file content from the physical file
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

}
