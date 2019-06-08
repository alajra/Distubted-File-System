import java.io.*;
import java.util.*;
import java.rmi.*;
import java.rmi.server.*;
public class FileServer extends UnicastRemoteObject implements ClientInterface {
    Vector<File> cache = new Vector<File>();
    static int port;

    class File {
        public String name;
        private byte[] bytes = null;
        private Vector<String> readers = null;

        private static final int state_notshared = 0;
        private static final int state_readshared = 1;
        private static final int state_writeshared = 2;
        private static final int state_back2writeshared = 3;
        private int state;

        private String owner;
        private String port;
        private Object inStateBack2WriteShared;


        public File(String name, String port) {
            this.name = name;
            readers = new Vector<String>();
            bytes = fileRead();
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
        port = Integer.parseInt(args[0]);
        FileServer server = new FileServer(args[0]);
    }

}
