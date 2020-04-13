//Two Phase Commit Protocol CLIENT

import java.io.*;
import java.net.*;

public class Client implements Runnable
{
    static Socket klientSocket = null;
    static PrintStream os = null;
    static DataInputStream is = null;
    static BufferedReader inputLinje = null;
    static boolean lukket = false;
    public static void main(String[] args) {
        int port=1111;
        String host="localhost";
        try {
            klientSocket = new Socket(host, port);
            inputLinje = new BufferedReader(new InputStreamReader(System.in));
            os = new PrintStream(klientSocket.getOutputStream());
            is = new DataInputStream(klientSocket.getInputStream());
        } catch (Exception e) {
            System.out.println("Exception occurred : " + e.getMessage());
        }

        if (klientSocket != null && os != null && is != null) {
            try {
                new Thread(new Client()).start();
                while (!lukket) {
                    os.println(inputLinje.readLine());
                }
                os.close();
                is.close();
                klientSocket.close();
            } catch (IOException e) {
                System.err.println("IOException:  " + e);
            }
        }
    }
    @SuppressWarnings("deprecation")
    public void run() {
        String responseLinje;
        try {
            while ((responseLinje = is.readLine()) != null) {
                System.out.println("\n"+responseLinje);
                if (responseLinje.equalsIgnoreCase("GLOBAL_COMMIT")==true || responseLinje.equalsIgnoreCase("GLOBAL_ABORT")==true ) {
                    break;
                }
            }
            lukket=true;
        }
        catch (IOException e) {
            System.err.println("IOException:  " + e);
        }
    }
} //end client
