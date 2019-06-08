import java.util.Scanner;
import java.io.*;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.Vector;


public class FileClient extends UnicastRemoteObject implements ClientInterface {


    String locallyCachedFile = "/tmp/--.txt";
    static ServerInterface server = null;
    String myIpName;
    File file;
    BufferedReader input;


    class File {
        //public String name;
        private byte[] bytes = null;

        public File() {

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

        private boolean execUnixCommnad(String command) {

            try {
                Runtime runtime = Runtime.getRuntime( );
                Process process = runtime.exec( command );
                int retval = process.waitFor();

            } catch ( Exception e ) {
                e.printStackTrace( );
            }

            return true;

        }
        public boolean launchEmacs(String mode){
            /*if(){

            }*/
            return true;
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
