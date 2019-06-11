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
        synchronized (cache) {
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
        return (file == null) ? false : file.upload(client, contents);
    }

    public FileContents download( String client, String filename, String mode ){
        File file = null;
        synchronized (cache) {
            for (int i = 0; i < cache.size(); i++) {
                file = (File) cache.elementAt(i);
                if (file.hit(filename)) {
                    break;
                } else {
                    file = null;
                    continue;
                }
            }
            if (file = null) {
                file = new File(filename, port);
                cache.add(file);
            }
        }
        return file.download(client, mode);
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("usage: java FileServer port#");
            System.exit(-1);
        }

        FileServer server = new FileServer(args[0]);
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
            this.port = port;
            state = 0;
        }

        public boolean hit(String filename) {

            for (int i = 0; i < cache.size(); i++) {
                if (cache.get(i).name.equalsIgnoreCase(filename)) {
                    return true;
                }
            }
            return false;
        }

        private byte[] fileRead() {
            bytes[] b = null;
            FileInputStream file = new FileInputStream(name);
            b = new byte[file.available()];
            file.read(b);
            file.close();
            return b;
        }

        private boolean fileWrite() {
            FileOutputStream file = new FileOutputStream(name);
            file.write(bytes);
            file.flush();
            file.close();
            return true;
        }

        public synchronized FileContents download(String client, String mode) {
            boolean becomesWriteShared = false;
            boolean becomesOwnershipChanged = false;

            while (this.state == ownershipchange) {
                synchronized (this.waitingState) {
                    try {
                        waitingState.wait();
                    } catch (Exception e) {}
                }
            }

            readers.remove(client);
            if (mode.equalsIgnoreCase("r")) {
                readers.addElement(client);
                this.state = readshared;     // read shared
            } else {
                this.state = writeshared;     // write shared
            }
            if (this.state == readshared) {
                if (mode.equalsIgnoreCase("w")) {
                    this.owner = client;
                    becomesWriteShared = true;
                }
            }
            if (this.state == writeshared) {
                if (mode.equalsIgnoreCase("w")) {
                    becomesOwnershipChanged = true;
                    ClientInterface c;
                    try {
                        c = (ClientInterface)Naming.lookup("rmi://" + this.owner + ":" + this.port + "/fileclient");
                    } catch (Exception e) {}
                    if (c != null) {
                        try {
                            var5.writeback();
                        } catch (Exception e) {}
                    }
                    try {
                        this.wait();
                    } catch (Exception e) {}
                    this.owner = client;
                }
            }
            if (becomesWriteShared) {
                this.state = writeshared;
            }
            if (becomesOwnershipChanged) {
                this.state = ownershipchange;
            }

            FileContents cont = new FileContents(bytes);
            synchronized(this.waitingState) {
                this.waitingState.notifyAll();
            }

            return cont;
        }

        public synchronized boolean upload(String client, FileContents cont) {
            int error = 0;

            switch (this.state) {
                case writeshared:
                    this.state = notshared;
                    break;
                case ownershipchange:
                    this.state = writeshared;
            }
        }
    }

}
