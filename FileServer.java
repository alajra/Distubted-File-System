import java.io.*;
import java.util.*;
import java.rmi.*;
import java.rmi.server.*;


public class FileServer extends UnicastRemoteObject implements ServerInterface {
    private Vector<File> cache = new Vector<File>();
    private String port;

    public FileServer(String port) throws  RemoteException{
        this.port = port;
    }

    public boolean upload( String client, String filename, FileContents contents ){

        File file = null;

        //cache vector must be synchronized to avoid collision
        synchronized (cache) {

            //look for file in cache vector
            for (int i = 0; i < cache.size(); i++) {
                file = (File) cache.elementAt(i);
                if (file.hit(filename)) {
                    break;
                } else {
                    file = null;
                    continue;
                }
            }

        }

        //if file does not exist return false,
        //otherwise, upload the file
        return (file == null) ? false : file.upload(client, contents);
    }

    public FileContents download( String client, String filename, String mode ){

        File file = null;

        //cache vector must be synchronized to avoid collision
        synchronized (cache) {

            //check if the file exist in the server
            for (int i = 0; i < cache.size(); i++) {
                file = (File) cache.elementAt(i);
                if (file.hit(filename)) {
                    break;
                } else {
                    file = null;
                    continue;
                }
            }

            //if file does not exist
            if (file == null) {

                //create new file
                file = new File(filename, port);

                //add the file to the cache vector
                cache.add(file);
            }
        }

        //return the file content to the client
        return file.download(client, mode);
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("usage: java FileServer port#");
            System.exit(-1);
        }

        try{
            FileServer server = new FileServer(args[0]);
            Naming.rebind("rmi://localhost:" + args[0] + "/fileserver", server);
            System.out.println("Server is up and read");

            System.out.println("Enter QUIT to exit: ");
            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

            boolean keepGoing = true;
            while(keepGoing){

                if(input.readLine().equals("QUIT"))
                    keepGoing = false;

                System.out.println("Enter QUIT to exit: ");
            }

            Naming.unbind("rmi://localhost:" + args[0] + "/fileserver");

            for (int i = 0; i < server.cache.size(); i++) {
                System.out.println("Writing File("+ server.cache.elementAt(i).name +") back to disk");
                server.cache.elementAt(i).fileWrite();
            }

            System.out.println("All files written to local disk. GoodBye");


        }catch (Exception e){
            System.out.println(e.getMessage());
            System.exit(-1);
        }
    }



    private class File {
        public String name;
        private byte[] bytes = null;
        private Vector<String> readers = null;

        private static final int notshared = 0;
        private static final int readshared = 1;
        private static final int writeshared = 2;
        private static final int ownershipchange = 3;

        public int state;

        public Object waitingState;

        private String owner = null;
        private String port;

        public File(String name, String port) {
            this.name = name;
            readers = new Vector<String>();
            bytes = fileRead();
            if(bytes == null){
                cache.remove(this);
            }
            this.port = port;
            state = 0;
            waitingState = new Object();
        }

        public synchronized boolean hit(String filename) {
            //return true if this is file has the same filename
            return filename.equals(name);
        }

        private byte[] fileRead() {
            byte[] b = null;

            try {
                //read from physical file to bytes
                //and create a new file if does not exist
                FileInputStream file = new FileInputStream(name);
                b = new byte[file.available()];
                file.read(b);
                file.close();
            }catch (Exception e){
                System.out.println(e.getMessage());
            }

            return b;
        }

        private boolean fileWrite() {

            try {
                //write bytes to physical file
                FileOutputStream file = new FileOutputStream(name);
                file.write(bytes);
                System.out.println("File("+ name +") new content is: " + new String(bytes) );
                file.flush();
                file.close();
            }catch (Exception e){
                System.out.println(e.getMessage());
                return false;
            }

            return true;
        }

        public synchronized FileContents download(String client, String mode) {

            if(bytes == null){
                return null;
            }

            boolean becomesWriteShared = false;
            boolean becomesOwnershipChanged = false;


            //wait if this file is owned by different client
            while (this.state == ownershipchange) {
                synchronized (this.waitingState) {
                    try {
                        System.out.println(client + " is waiting for File("+ name +")'s  ownership change");
                        waitingState.wait();
                        System.out.println(client + " is done waiting for File("+ name +")'s  ownership change");
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println(e.getMessage());
                    }
                }
            }


            //remove this client from the file reader
            readers.remove(client);


            //if file state is readshared
            if (this.state == readshared) {
                //if the access mode is write
                if (mode.equalsIgnoreCase("w")) {
                    //change the owner ship to this client
                    this.owner = client;
                    becomesWriteShared = true;
                } else {
                    readers.add(client);
                }
            }

            //if file state is readshared
            if (this.state == notshared) {
                //if the access mode is write
                if (mode.equalsIgnoreCase("w")) {
                    //change the owner ship to this client
                    this.owner = client;
                    becomesWriteShared = true;
                } else {
                    readers.add(client);
                    state = readshared;
                }
            }



            //if the file is writeshared
            if (this.state == writeshared) {
                //if the access mode write
                if (mode.equalsIgnoreCase("w")) {

                    becomesOwnershipChanged = true;

                    ClientInterface c = null;

                    try {
                        //look up the current owner
                        c = (ClientInterface)Naming.lookup("rmi://" + this.owner + ":" + this.port + "/fileclient");
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }

                    //if owner found
                    if (c != null) {
                        try {

                            this.state = ownershipchange;
                            System.out.println("writeback request for File("+ name +") has been sent to " + this.owner);
                            //ask the current owner to write back
                            c.writeback();

                            System.out.println("Downloading file(" + this.name + " ): waits for writeback in " + client);
                            this.wait();

                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }

                } else {
                    this.readers.add(client);
                }
            }

            //change the file state
            if (becomesWriteShared) {
                this.state = writeshared;
            } if (becomesOwnershipChanged) {
                owner = client;

                synchronized(this.waitingState) {
                    System.out.println("resume all clients");
                    this.waitingState.notifyAll();
                }

            }


            //wait till the upload finishes
            FileContents cont = new FileContents(bytes);
            System.out.println("File("+ name +") was downloaded to " + client);


            return cont;
        }


        public synchronized boolean upload(String client, FileContents cont) {

            if(!client.equals(owner))
                return false;

            //check file state
            switch (this.state) {

                //if the file is writeshared
                case writeshared:
                    //change the file state to not shared
                    this.state = notshared;
                    break;

                //if the file is ownership change
                case ownershipchange:
                    //change the file state to writeshared
                    this.state = writeshared;
                    break;

                //if file is notshared or readshared
                default:
                    return false;

            }

            //loop through all readers
            for(String reader : readers){

                ClientInterface c = null;

                try {

                    //look up the reader
                    c = (ClientInterface)Naming.lookup("rmi://" + reader + ":" + this.port + "/fileclient");

                    System.out.println("File("+ name +")'s  cached copy has been invalidated in " + reader);
                    //invalidate cached file
                    c.invalidate();

                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }

            }

            //get the new content
            bytes = cont.get();

            //remove all readers
            this.readers.removeAllElements();

            //write the new content to physical file
            this.fileWrite();


            //if file was ownerchange
            if(state == writeshared){
                //resume the file download to the new owner
                this.notify();
            }

            return true;
        }
    }






}
