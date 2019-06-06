import java.util.Scanner;

class cachedFile {

    protected String name;
    protected String accessMode;
    protected boolean owner;
    protected int state;

    private String[] states = {"cached", "shared", "owned"};

    public cachedFile(String name, String accessMode, boolean ownership, int state) {
        this.name = name;
        this.accessMode = accessMode;
        this.owner = ownership;
        this.state = state;
    }

}

public class FileClient {


    //methods: download, upload, invalidate, writeback
    public static void main(String[] args){

        FileClient client = new FileClient();

        System.out.println("FileClient: Next file to open");

        System.out.println("File name:");

        Scanner keyborad = new Scanner(System.in);

        String fileName = keyborad.next();

        System.out.println("How(r/w): ");

        String accessMode = keyborad.next();

        String downloadedFile  = client.download(fileName, accessMode);




    }

    public FileClient(){

    }

    public String download(String fileName, String accessMode){

        //check if file cashed locally

        //call server.download


        return "";
    }


}
