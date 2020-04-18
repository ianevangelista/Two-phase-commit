//Two Phase Commit Protocol KLIENT

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Klient implements Runnable {
    static Socket klientSocket = null;
    static PrintStream os = null;
    static DataInputStream is = null;
    static BufferedReader inputLinje = null;
    static boolean lukket = false;
    private int saldo = 10;
    private String klientIdentitet;
    private Loggforer logg;
    private boolean gjordeEndringer = false;

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
                new Thread(new Klient()).start();
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
        Scanner leser = new Scanner(System.in);
        int belop = 0;
        try {
            System.out.println("Hva er navnet ditt?");
            klientIdentitet = leser.nextLine();
            System.out.println("navn " + klientIdentitet);
            System.out.println("Hva er din saldo? (For eksempels skyld)");
            saldo = Integer.parseInt(leser.nextLine());
            logg = new Loggforer(klientIdentitet);
            logg.loggfor(klientIdentitet + " er tilkoblet. Saldo er: " + saldo + "kr.");
            System.out.println("Velkommen " + klientIdentitet + " til denne 2-fase applikasjonen.\nDu vil motta en VOTE_REQUEST...");
            while ((responseLinje = is.readLine()) != null) {
                System.out.println("\n"+responseLinje);
                if (responseLinje.indexOf("VOTE_REQUEST") != -1) {
                    belop = Integer.parseInt(responseLinje.split("\n")[1].split(":")[1]);
                    logg.loggfor("Fikk voterequest om å trekke " + belop + "kr.");
                    if (saldo >= belop) {
                        os.println("COMMIT");
                        logg.loggfor("SAVE: Lagrer gammel saldo(kr): " + saldo);
                        logg.loggfor("Sender COMMIT til tjener.");
                        saldo += belop;
                        gjordeEndringer = true;
                    } else {
                        os.print("ABORT");
                    }
                }
                if (responseLinje.equalsIgnoreCase("GLOBAL_ABORTED")) {
                    logg.loggfor("Fikk beskjed om ABORT fra tjener");
                    if (gjordeEndringer) {
                        logg.loggfor("Rollback. Saldo er nå " + logg.getRollbackSaldo() + "kr");
                    }
                    break;
                }
                if (responseLinje.equalsIgnoreCase("GLOBAL_COMMIT")) {
                    logg.loggfor("Fikk klarsignal(GLOBAL_COMMIT) fra tjener. Loggører transaksjon:");
                    logg.loggfor("Utførte transaksjon: [" + (saldo-belop) + "," + belop + "," + saldo + "]");
                    os.println("ACKNOWLEDGEMENT");
                    logg.loggfor("Sendte ACKNOWLEDGE til tjener. Klienten er nå frakoblet\n");
                    break;
                }
            }
            lukket=true;
        }
        catch (IOException e) {
            System.err.println("IOException:  " + e);
        }
    }
} //end klient
