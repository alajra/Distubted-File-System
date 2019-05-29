
import UWAgent.UWAgent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Vector;

public class UnixAgent extends UWAgent implements Serializable{

    String[] machines;
    String[] commands;
    String orgPlace;
    Vector<Vector<String>> result;
    boolean print;
    int currentMachine;
    int numberOflines;
    Date startTime;


    public UnixAgent(){
        System.out.println("usage: UnixAgent P 1 cssmpi2 4 who ls ps df");
    }

    public UnixAgent(String[] args) {

        //set whether to print or not
        print = args[0].startsWith("P");

        //initialize the num of machines
        int numOfMachines = Integer.parseInt(args[1]);
        machines = new String[numOfMachines];

        //add the machines to the array machines
        for(int i = 0; i < numOfMachines;i++){
            machines[i] =  args[2 + i];
        }

        //initialize the num of commands
        int numOfCommands = Integer.parseInt(args[2 + numOfMachines]);
        commands = new String[numOfCommands];

        //add the commands to the array commands
        for(int i = 0; i< numOfCommands;i++){
           commands[i] = args[3 + numOfMachines + i];
        }

        //print out the proccess summary
        System.out.println("print/count = "+ args[0] +", nServers = "+ numOfMachines+", server1 = "+
                args[2] + ", command1 = "+ args[3 + numOfMachines] );

        //set the current machine to zero
        currentMachine = 0;

        //initialize the result vector
        result = new Vector<>();

        //number of line of the output
        numberOflines = 0;
    }

    public void init() {
        //start the timer
        startTime = new Date();

        if (machines.length == 0) {
            //if there's no machines
            System.out.println("DONE");
        } else {

            try {
                //set the orgPlace to the start node
                orgPlace = InetAddress.getLocalHost().getHostAddress();

                //hop to the current machines to strart excuteing
                hop(machines[currentMachine], "unix", (String[])null);

            } catch (Exception e) {
                e.printStackTrace( );
                System.exit( -1 );
            }

        }
    }

    public void PrintResult(){

        if(print){
            //if print is true print the output
            for(int i = 0; i < result.size(); i++)
                for(int n = 0; n < result.get(i).size(); n++)
                    System.out.println(result.get(i).get(n));

        } else {
            //print the number of lines
            System.out.println("count = " + numberOflines);
        }

        //end timer
        Date endTime = new Date();

        //print duration
        System.out.println("Execution Time = " + (endTime.getTime() - startTime.getTime()));


    }

    public void unix(){

        //start execute the commands on this node
        for(int i = 0; i < commands.length;i++){
            Vector output = execute(commands[i]);

            //add the output to the result
            result.add(output);

            //increament the number of lines
            numberOflines += output.size() - 1;
        }

        //go to the next machine
        currentMachine++;

        if(currentMachine >= machines.length){

            //we are done, go back to orgPlace to print result
            hop(orgPlace, "PrintResult", (String[])null);

        } else {

            //else hop to the next machines
            hop(machines[currentMachine], "unix", (String[])null);

        }

    }

    public Vector execute( String command ) {

        Vector<String> output = new Vector<String>( );

        output.addElement(machines[currentMachine] + " command("+ command +"):..............................");

        String line;
        try {
            Runtime runtime = Runtime.getRuntime( );
            Process process = runtime.exec( command );
            InputStream input = process.getInputStream();
            BufferedReader bufferedInput
                    = new BufferedReader( new InputStreamReader( input ) );
            while ( ( line = bufferedInput.readLine( ) ) != null ) {
                output.addElement( line );
            }

        } catch ( IOException e ) {
            e.printStackTrace( );
            return output;
        }

        return output;
    }

}
