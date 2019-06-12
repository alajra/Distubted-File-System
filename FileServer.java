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

        private Object inStateBack2WriteShared = null;
        public int state;

        public Object waitingState;

        private String owner = null;
        private String port;

        public File(String name, String port) {
            this.name = name;
            readers = new Vector<String>();
            bytes = fileRead();
            System.out.println("finsihed file read");
            this.port = port;
            state = 0;
            this.inStateBack2WriteShared = new Object();
        }

        public boolean hit(String filename) {
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
                file.flush();
                file.close();
            }catch (Exception e){
                System.out.println(e.getMessage());
                return false;
            }

            return true;
        }

        /*public synchronized FileContents download(String client, String mode) {

            System.out.println("check");

            boolean becomesWriteShared = false;
            boolean becomesOwnershipChanged = false;

            //wait if this file is owned by different client
            while (this.state == ownershipchange) {
                synchronized (this.waitingState) {
                    try {
                        waitingState.wait();
                    } catch (Exception e) {}
                }
            }

            System.out.println("check");

            //remove this client from the file reader
            readers.remove(client);


            System.out.println("check" + state);


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


            System.out.println("check" + state);

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
                            //ask the current owner to write back
                            c.writeback();
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }

                    //wait while the current owner write back
                    try {
                        this.wait();
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }

                    //change the ownership
                    this.owner = client;
                } else {
                    this.readers.add(client);
                }
            }

            //change the file state
            if (becomesWriteShared) {
                this.state = writeshared;
            }
            if (becomesOwnershipChanged) {
                this.state = ownershipchange;
                /*synchronized(this.waitingState) {
                    this.waitingState.notifyAll();
                }*/
           /* }

            System.out.println("check");

            //wait till the upload finishes
            FileContents cont = new FileContents(bytes);


            System.out.println("check");

            return cont;
        }*/

        public synchronized FileContents download(String var1, String var2) {
            if (this.name.equals("")) {
                return null;
            } else {
                while(this.state == 3) {
                    synchronized(this.inStateBack2WriteShared) {
                        try {
                            System.out.println(var1 + "now wait on inStateBack2WriteShared");
                            this.inStateBack2WriteShared.wait();
                        } catch (InterruptedException var12) {
                            var12.printStackTrace();
                        }
                    }
                }

                int var3 = this.state;
                byte var4 = 0;
                switch(this.state) {
                    case 0:
                        if (var2.equals("r")) {
                            this.state = 1;
                            this.readers.add(var1);
                        } else if (var2.equals("w")) {
                            this.state = 2;
                            if (this.owner != null) {
                                var4 = 1;
                            } else {
                                this.owner = var1;
                            }
                        } else {
                            var4 = 2;
                        }
                        break;
                    case 1:
                        this.readers.remove(var1);
                        if (var2.equals("r")) {
                            this.readers.add(var1);
                        } else if (var2.equals("w")) {
                            this.state = 2;
                            if (this.owner != null) {
                                var4 = 3;
                            } else {
                                this.owner = var1;
                            }
                        } else {
                            var4 = 4;
                        }
                        break;
                    case 2:
                        this.readers.remove(var1);
                        if (var2.equals("r")) {
                            this.readers.add(var1);
                        } else if (var2.equals("w")) {
                            this.state = 3;
                            ClientInterface var5 = null;

                            try {
                                var5 = (ClientInterface)Naming.lookup("rmi://" + this.owner + ":" + this.port + "/fileclient");
                            } catch (Exception var11) {
                                var11.printStackTrace();
                                var4 = 5;
                            }

                            if (var5 != null) {
                                try {
                                    var5.writeback();
                                } catch (RemoteException var10) {
                                    var10.printStackTrace();
                                    var4 = 6;
                                }

                                System.out.println("download( " + this.name + " ): " + this.owner + "'s copy was invalidated");
                            }

                            if (var4 == 0) {
                                try {
                                    System.out.println("download " + this.name + " ): " + var1 + " waits for writeback");
                                    this.wait();
                                } catch (InterruptedException var9) {
                                    var9.printStackTrace();
                                    var4 = 7;
                                }

                                this.owner = var1;
                            }
                        } else {
                            var4 = 8;
                        }
                }

                if (var4 > 0) {
                    return null;
                } else {
                    FileContents var14 = new FileContents(this.bytes);
                    if (var3 == 3) {
                        synchronized(this.inStateBack2WriteShared) {
                            this.inStateBack2WriteShared.notifyAll();
                            System.out.println(var1 + " woke up all waiting on inStateBack2WriteShared");
                        }
                    }

                    return var14;
                }
            }
        }


        public synchronized boolean upload(String client, FileContents cont) {

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

                    //invalidate cached file
                    c.invalidate();

                    //get the new content
                    bytes = cont.get();

                    //remove all readers
                    this.readers.removeAllElements();

                    //write the new content to physical file
                    this.fileWrite();

                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }

            }

            //if file was ownerchange
            if(state == writeshared){
                //resume the file download to the new owner
                this.notify();
            }

            return true;
        }
    }

}
