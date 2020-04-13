//Two Phase Commit Protocol TJENER
import java.io.*;
import java.net.*;
import java.util.*;

public class Tjener {
    boolean lukket = false, inputFraAlle = false;
    List<KlientTraad> traadListe;
    List<String> data;
    String tjenerNavn = "Per";
    String tjenerNavnGammel = "";

    Tjener() {
        traadListe = new ArrayList<KlientTraad>();
        data = new ArrayList<String>();
    }

    public static void main(String args[]) {
        Socket klientSocket = null;
        ServerSocket tjenerSocket = null;
        int port_number = 1111;
        Tjener tjener = new Tjener();
        try {
            tjenerSocket = new ServerSocket(port_number);
            System.out.println("Navnet mitt er " + tjener.tjenerNavn);
        } catch (IOException e) {
            System.out.println(e);
        }
        while (!tjener.lukket) {
            try {
                klientSocket = tjenerSocket.accept();
                KlientTraad klientTraad = new KlientTraad(tjener, klientSocket);
                (tjener.traadListe).add(klientTraad);
                System.out.println("\nAntall klienter er oppdatert til: " + (tjener.traadListe).size());

                (tjener.data).add("NOT_SENT");
                klientTraad.start();
            } catch (IOException e) { }
        }
        try {

            tjenerSocket.close();
        } catch (Exception e1) { }
    } // end main
} // end class Server

class KlientTraad extends Thread {
    DataInputStream is = null;
    String linje;
    String navn;
    PrintStream os = null;
    Socket klientSocket = null;
    String klientIdentitet;
    Tjener tjener;

    public KlientTraad(Tjener tjener, Socket klientSocket) {
        this.klientSocket = klientSocket;
        this.tjener = tjener;
    }

    @SuppressWarnings("deprecation")
    public void run() {
        try {
            is = new DataInputStream(klientSocket.getInputStream());
            os = new PrintStream(klientSocket.getOutputStream());
            os.println("Skriv inn navnet ditt: ");
            navn = is.readLine();
            klientIdentitet = navn;
            os.println("Velkommen " + navn + " til denne 2-fase applikasjonen.\nDu vil motta en VOTE_REQUEST...");
            // Simulerer avgjørelse
            if (tjener.traadListe.size() == 1) {
                tjener.tjenerNavnGammel = tjener.tjenerNavn;
                if (tjener.tjenerNavn.equals("Ola")) {
                    tjener.tjenerNavn = "Per"; // dette skjedde egt før
                } else {
                    tjener.tjenerNavn = "Ola"; // dette skjedde egt før
                }
            }
            os.println("Tjenerens navn er endret til " + tjener.tjenerNavn + ". Beholde endringen?");
            os.println("VOTE_REQUEST\nVennligst skriv inn COMMIT eller ABORT: ");
            for (int i = 0; i < (tjener.traadListe).size(); i++) {
                if ((tjener.traadListe).get(i) != this) {
                    ((tjener.traadListe).get(i)).os.println("---En ny bruker ved navn " + navn + " har blitt med i applikasjonen---");
                }
            }
            while (true) {
                linje = is.readLine();
                if (linje.equalsIgnoreCase("ABORT")) {
                    System.out.println("\nFra '" + klientIdentitet
                            + "' : ABORT\n\nSiden det ble skrevet ABORT, vil vi ikke vente paa flere input fra andre klienter.");
                    tjener.tjenerNavn = tjener.tjenerNavnGammel;
                    System.out.println("\nAborted....Navnet mitt er fortsatt " + tjener.tjenerNavn);

                    for (int i = 0; i < (tjener.traadListe).size(); i++) {
                        ((tjener.traadListe).get(i)).os.println("GLOBAL_ABORT");
                        ((tjener.traadListe).get(i)).os.close();
                        ((tjener.traadListe).get(i)).is.close();
                        tjener.data.remove(tjener.traadListe.indexOf(tjener.traadListe.get(i)));
                        tjener.traadListe.remove(i);

                    }
                    break;
                }
                if (linje.equalsIgnoreCase("COMMIT")) {
                    System.out.println("\nFra '" + klientIdentitet + "' : COMMIT");
                    if ((tjener.traadListe).contains(this)) {
                        (tjener.data).set((tjener.traadListe).indexOf(this), "COMMIT");
                        for (int j = 0; j < (tjener.data).size(); j++) {
                            if (!(((tjener.data).get(j)).equalsIgnoreCase("NOT_SENT"))) {
                                tjener.inputFraAlle = true;
                                continue;
                            } else {
                                tjener.inputFraAlle = false;
                                System.out.println("\nVenter paa input fra andre klienter.");
                                break;
                            }
                        }
                        if (tjener.inputFraAlle) {
                            tjener.tjenerNavnGammel = tjener.tjenerNavn;
                            System.out.println("\n\nCommited.... Navnet mitt er naa " + tjener.tjenerNavn);
                            for (int i = 0; i < (tjener.traadListe).size(); i++) {
                                ((tjener.traadListe).get(i)).os.println("GLOBAL_COMMIT");
                                ((tjener.traadListe).get(i)).os.close();
                                ((tjener.traadListe).get(i)).is.close();
                                tjener.data.remove(tjener.traadListe.indexOf(tjener.traadListe.get(i)));
                                tjener.traadListe.remove(i);
                            }
                            break;
                        }
                    } // if traadListe.contains
                } // commit
            } // while
            // tjener.lukket = true;
            klientSocket.close();
        } catch (IOException e) { }
    }
}// end class KlientTraad

/*
Coordinator                                          Cohorts
                            QUERY TO COMMIT
                -------------------------------->
                              VOTE YES/NO           prepare/abort
                <-------------------------------
commit/abort                 COMMIT/ROLLBACK
                -------------------------------->
                              ACKNOWLEDGMENT        commit/abort
                <--------------------------------
end

 Two Phases :
 1.Prepare and Vote Phase
 2. Commit or Abort Phase

 "Either All Commit Or All RollBack."
 */
