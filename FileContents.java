import java.io.*;
import java.util.*;


public class FileContents implements Serializable {
    private byte[] contents; // file contents
    public FileContents( byte[] contents ) {
        this.contents = contents;
    }
    public void print( ) throws IOException {
        System.out.println( "FileContents = " + contents );
    }
    public byte[] get( ) {
        return contents;
    }
}