//Two Phase Commit Protocol TJENER
import java.io.*;
import java.net.*;
import java.util.*;

public class Tjener {
    boolean lukket = false, inputFraAlle = false, ackFraAlle = false;
    List<KlientTraad> traadListe;
    List<String> data, ack;
    int belop;

    Tjener() {
        traadListe = new ArrayList<KlientTraad>();
        data = new ArrayList<String>();
        ack = new ArrayList<String>();
        belop = -5;
    }

    public static void main(String args[]) {
        Socket klientSocket = null;
        ServerSocket tjenerSocket = null;
        int port_number = 1111;
        Tjener tjener = new Tjener();
        try {
            tjenerSocket = new ServerSocket(port_number);
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
                (tjener.ack).add("NOT_SENT");
                klientTraad.start();
            } catch (IOException e) { }
        }
        try {

            tjenerSocket.close();
        } catch (Exception e1) { }
    } // end main
} // end class Server

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
