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


        try {
            //create new client
            FileClient client = new FileClient(args[0],args[1]);
            Naming.rebind("rmi://localhost:" + args[1] + "/fileclient", client);
            System.out.println("rmi://localhost: " + args[0] + "/fileclient invokded");
            client.loop();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }




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
            System.out.println(e.getMessage());
        }

    }

    public  void loop(){

        while (true){

            FileClient.WritebackThread writingBackThread = new FileClient.WritebackThread();
            writingBackThread.start();

            System.out.println("FileClient: Next file to open");

            try {
                //ask the user for file name
                System.out.print("File name or enter QUIT to exit:");
                String fileName = input.readLine();

                if (fileName.equals("QUIT")) {

                    if(file.state == file.state_writeowned){
                        writeback();
                    }
                    System.exit(0);
                }

                while(fileName.equals("")){
                    System.out.println();
                    System.out.println("Please enter a valid name for the file.");
                    System.out.print("File name:");
                    fileName = input.readLine();
                }

                System.out.println();

                //ask the user for access mode
                System.out.print("How(r/w): ");
                String accessMode = input.readLine();

                while(!(accessMode.equals("r") || accessMode.equals("w"))){
                    System.out.println();
                    System.out.println("Please enter a valid access mode.");
                    System.out.print("How(r/w): ");
                    accessMode = input.readLine();
                }

                System.out.println();

                writingBackThread.kill();
                //if the file is a cache miss
                if(!file.hit(fileName, accessMode)){

                    System.out.println("File("+ fileName +") with access mode "+ accessMode +" is not cached");
                    //if the cached file is write owned, then write it back to the server
                    if(file != null && file.state == file.state_writeowned){
                        writeback();
                    }


                    System.out.println("Downloading File("+ fileName +") with access mode "+ accessMode +" from the server");
                    //download the file from the server with the right mode

                    if(!file.download(fileName, accessMode)){
                        System.out.println("File is not found. Please try another file");
                        writingBackThread.kill();
                        continue;
                    }


                } else {
                    System.out.println("File("+ fileName +") is already cached");
                }

                //launch emacs
                file.launchEmacs(accessMode);


            }catch (Exception e){
                System.out.println(e.getMessage());
            }


        }


    }

    public boolean invalidate( ) throws RemoteException{
        //change the file state to invalid
        System.out.println("File("+ file.name +")'s cached copy has been invalidated");
        return file.invalidate();
    }

    public synchronized boolean  writeback( ) throws 	RemoteException{

        System.out.println("Writing File("+ file.name +") back to the server");

        return file.writeback();
    }


    class File {
        private byte[] bytes;
        private String name;
        private int state;

        private static final int state_invalid = 0;
        private static final int state_readshared = 1;
        private static final int state_writeowned = 2;
        private static final int state_writeback = 3;

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
                System.out.println(e.getMessage());
            }


        }

        public synchronized boolean hit(String filename, String mode){

            //if the file is cached
            if(!name.equals(filename)){
                return false;
            }

            //if the file cached with the right mode
            if(mode.equals("r" ) && (state == state_readshared || state == state_writeowned )){
                return  true;
            } else if(mode.equals("w") && state == state_writeowned){
                return true;
            }

            return false;
        }

        public boolean download(String filename, String mode){

            synchronized(this) {
                state = (mode.startsWith("w")) ? state_writeowned : state_readshared;
            }

            this.name = filename;

            try {
                //download the filecontent from the server
                FileContents contents = server.download(myIpName, filename, mode);
                if(contents != null){
                    //get the bytes from the contents
                    bytes = contents.get();
                }else {
                    return false;
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

        }

        public synchronized boolean invalidate(){
            if(state == state_readshared){
                state = state_invalid;
                return false;
            }else {
                return false;
            }

        }

        public synchronized boolean writeback(){
            if(state == state_writeowned){
                state = state_writeback;
                return false;
            }else {
                return false;
            }

        }

        public boolean upload(){

            synchronized (this){
                //change the state to read
                state = state_readshared;
            }

            try {

                //create file contents from file bytes
                FileContents contents = new FileContents(bytes);

                //upload the file to the server
                server.upload(myIpName, name, contents);


            } catch (Exception e){
                System.out.println(e.getMessage());
                return false;
            }


            return true;
        }



        private boolean execUnixCommand(String command, String mode, String path) {

            String[] args = path.equals("") ? new String[2] : new String[3];
            args[0] = command;
            args[1] = mode;
            if (!path.equals("")) {
                args[2] = path;
            }

            try {
                Runtime runtime = Runtime.getRuntime();
                Process process = runtime.exec(args);
                int var7 = process.waitFor();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

        }

        public boolean launchEmacs(String mode) {
            System.out.println("Emacs should be working");
            //check if the file is accessible
            if (!execUnixCommand("chmod", "600", "/tmp/"+myIpName+".txt")) {
                return false;
            } else {

                try {
                    //write the bytes to the physical file
                    FileOutputStream fileOutput = new FileOutputStream("/tmp/"+myIpName+".txt");
                    fileOutput.write(this.bytes);
                    fileOutput.flush();
                    fileOutput.close();

                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    return false;
                }

                //execute the unix command on the file
                if (!this.execUnixCommand("chmod", mode.equals("r") ? "400" : "600", "/tmp/"+myIpName+".txt")) {
                    return false;
                } else {

                    //get the result from excuting the unix command
                    boolean result = this.execUnixCommand("emacs", "/tmp/"+myIpName+".txt", "");

                    // if the unix changed the file, and the access mode is write
                    if (result && mode.equals("w")) {

                        try {

                            //get the new file content from the physical file
                            FileInputStream fileInput = new FileInputStream("/tmp/"+ myIpName +".txt");
                            this.bytes = new byte[fileInput.available()];
                            fileInput.read(this.bytes);
                            fileInput.close();

                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                            return false;
                        }


                    }

                    return true;
                }

            }
        }


    }

    private class WritebackThread extends Thread {
        private boolean active = false;

        public WritebackThread() {
            this.active = true;
        }

        public void run() {
            while(this.isActive()) {
                if (FileClient.this.file.state == file.state_writeback) {
                    FileClient.this.file.upload();
                }
            }

        }

        synchronized void kill() {
            this.active = false;

            try {
                this.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        synchronized boolean isActive() {
            return this.active;
        }
    }

}
