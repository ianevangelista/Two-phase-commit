//Two Phase Commit Protocol TJENER
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Tjener-klasse som fungerer som koordinator i two-phase commit.
 * Flere klienter kan koble seg opp til samme tjener. Tjeneren spør om alle er klare til å committe.
 * Tjeneren sender GLOBAL_COMMIT eller GLOBAL_ABORT avhengig av hva klientene svarer.
 * @author Nikolai Dokken
 * @author Ian Evangelista
 * @author Kasper Gundersen
 */
public class Tjener {
    boolean lukket = false, inputFraAlle = false;
    List<KlientTraad> traadListe;
    int belop;

    Tjener() {
        traadListe = new ArrayList<KlientTraad>();
        belop = -5;
    }
    /**
     * Main-metoden lager en tjener som kjører så lenge objektvariabelen lukket er false.
     * Åpner en serverSocket på for en gitt port, slik at klienter kan koble seg opp.
     * Den legger til klienter i traadListe når dem kobler seg opp mot tjeneren.
     */

    public static void main(String args[]) {
        Socket klientSocket = null;
        ServerSocket tjenerSocket = null;
        int port_number = 1111;
        Tjener tjener = new Tjener();
        try {
            tjenerSocket = new ServerSocket(port_number); // Localhost
            // tjenerSocket = new ServerSocket(); // server
            // tjenerSocket.bind(new InetSocketAddress("129.241.96.153", 1111)); // server
            System.out.println("Serveren er startet...");
        } catch (IOException e) {
            System.out.println(e);
        }
        while (!tjener.lukket) {
            try {
                klientSocket = tjenerSocket.accept();
                KlientTraad klientTraad = new KlientTraad(tjener, klientSocket);
                (tjener.traadListe).add(klientTraad);
                System.out.println("\nAntall klienter er oppdatert til: " + (tjener.traadListe).size());
                klientTraad.data = "NOT_SENT";
                klientTraad.start();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            tjenerSocket.close();
        } catch (Exception e1) { }
    } // end main
} // end class Server
