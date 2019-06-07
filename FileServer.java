public class FileServer {

    boolean upload( String myIpName, String filename, FileContents contents ){

        return true;
    }

    FileContents download( String myIpName, String filename, String mode ){


        return new FileContents(new byte[2]);
    }

}
