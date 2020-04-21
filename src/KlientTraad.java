import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * KlienTraad fungerer som en trådklasse knyttet til en klient eller en deltaker i two-phase commit.
 * Klassen leser responsen den får fra Klient-klassen og sender informasjon til både klient og tjener.
 * @author Nikolai Dokken
 * @author Ian Evangelista
 * @author Kasper Gundersen
 */
public class KlientTraad extends Thread {
    DataInputStream is = null;
    PrintStream os = null;
    String linje;
    Socket klientSocket = null;
    String klientIdentitet;
    Tjener tjener;
    String data;

    public KlientTraad(Tjener tjener, Socket klientSocket) {
        this.klientSocket = klientSocket;
        this.tjener = tjener;
    }
    /**
     * Run-metode som fungerer som en trådklasse knyttet til en klient eller en deltaker i two-phase commit.
     * Klassen leser responsen den får fra Klient-klassen og sender informasjon til både klient og tjener.
     * Sender en melding til hver klient hver gang en ny klient kobler seg til tjeneren.
     * Sender en VOTE_REQUEST til klient.
     * Leser responsen fra klient. Hvis det er ABORT, vil den sende GLOBAL_ABORT til alle. Hvis det er COMMIT, vil den sende GLOBAL_COMMIT til alle
     * Vil hele tiden vente på alle klienter så langt en klient ikke sender ABORT.
     * Hvis alle klienter svarer ACKNOWLEDGEMENT etter at dem svarte med COMMIT, vil two-phase commit være over.
     * Forbindelsen mellom tjener og klient avsluttes.
     */
    @SuppressWarnings("deprecation")
    public void run() {
        try {
            is = new DataInputStream(klientSocket.getInputStream());
            os = new PrintStream(klientSocket.getOutputStream());
            klientIdentitet = is.readLine();
            os.println("VOTE_REQUEST: Ber om trekke fra folgende belop(kr):5");

            while (true) {
                linje = is.readLine();
                if (linje == null) break;
                if (linje.equalsIgnoreCase("ABORT")) {
                    System.out.println("\nFra '" + klientIdentitet
                            + "' : ABORT\n\nSiden det ble skrevet ABORT, vil vi ikke vente paa flere input fra andre klienter.");
                    System.out.println("\nAborted...");

                    while(tjener.traadListe.size() > 0) {
                        ((tjener.traadListe).get(0)).os.println("GLOBAL_ABORT");
                        tjener.traadListe.remove(0);
                    }
                    break;
                }
                if (linje.equalsIgnoreCase("COMMIT")) {
                    System.out.println("\nFra '" + klientIdentitet + "' : COMMIT");
                    if ((tjener.traadListe).contains(this)) {
                        this.data = "COMMIT";
                        for (int j = 0; j < (tjener.traadListe).size(); j++) {
                            if (!(tjener.traadListe.get(j).data.equalsIgnoreCase("NOT_SENT"))) {
                                tjener.inputFraAlle = true;
                                continue;
                            } else {
                                tjener.inputFraAlle = false;
                                System.out.println("\nVenter paa input fra andre klienter.");
                                break;
                            }
                        }

                        if (tjener.inputFraAlle) {
                            System.out.println("\n\nSending GLOBAL_COMMIT to all....");
                            for(int i = 0; i < tjener.traadListe.size(); i++) {
                                ((tjener.traadListe).get(i)).os.println("GLOBAL_COMMIT");
                            }
                            //break;
                        }
                    } // if traadListe.contains
                }
                if (linje.equalsIgnoreCase("ACKNOWLEDGEMENT")) {
                    tjener.traadListe.remove(tjener.traadListe.indexOf(this));

                    // Dersom alle har sendt acknowledge og koblet fra
                    if (tjener.traadListe.size() == 0) {
                        System.out.println("MOTTAT ACK FRA ALLE KLIENTER, TWO PHASE COMMIT ER NAA OVER");
                        break;
                    } else {
                        System.out.println("\nVenter paa acknowledgement fra andre klienter.");
                        break;
                    }
                }
            } // while
            is.close();
            os.close();
            klientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
            if ((tjener.traadListe).contains(this)) tjener.traadListe.remove(tjener.traadListe.indexOf(this));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }
}// end class KlientTraad