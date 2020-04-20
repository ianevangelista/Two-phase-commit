import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;

/**
 * KlienTraad fungerer som en trådklasse knyttet til en klient eller en deltaker i two-phase commit.
 * Klassen leser responsen den får fra Klient-klassen og sender informasjon til både klient og tjener.
 * @author Nikolai Dokken
 * @author Ian Evangelista
 * @author Kasper Gundersen
 */
class KlientTraad extends Thread {
    DataInputStream is = null;
    PrintStream os = null;
    String linje;
    Socket klientSocket = null;
    String klientIdentitet;
    Tjener tjener;
    Loggforer logg;
    int antallAck = 0;

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
            for (int i = 0; i < (tjener.traadListe).size(); i++) {
                if ((tjener.traadListe).get(i) != this) {
                    ((tjener.traadListe).get(i)).os.println("---En ny bruker ved navn " + klientIdentitet + " har blitt med i applikasjonen---");
                }
            }
            while (true) {
                linje = is.readLine();
                if (linje.equalsIgnoreCase("ABORT")) {
                    System.out.println("\nFra '" + klientIdentitet
                            + "' : ABORT\n\nSiden det ble skrevet ABORT, vil vi ikke vente paa flere input fra andre klienter.");
                    System.out.println("\nAborted...");

                    while(tjener.traadListe.size() > 0) {
                        ((tjener.traadListe).get(0)).os.println("GLOBAL_ABORT");
                        tjener.data.remove(tjener.traadListe.indexOf(tjener.traadListe.get(0)));
                        tjener.traadListe.remove(0);
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
                            System.out.println("\n\nSending GLOBAL_COMMIT to all....");
                            while(tjener.traadListe.size() > 0) {
                                ((tjener.traadListe).get(0)).os.println("GLOBAL_COMMIT");
                                tjener.data.remove(tjener.traadListe.indexOf(tjener.traadListe.get(0)));
                                tjener.traadListe.remove(0); //fjerner senere etter ack
                            }
                            linje = is.readLine();
                            for (int i = 0; i < tjener.traadListe.size(); i++){
                                if (linje.equalsIgnoreCase("ACKNOWLEDGEMENT")) antallAck++;
                                System.out.println(antallAck);
                            }

                            if (antallAck == tjener.traadListe.size()) {
                                System.out.println("MOTTAT ACK FRA ALLE KLIENTER, TWO PHASE COMMIT ER NAA OVER");
                                tjener.data = new ArrayList<String>();
                                tjener.traadListe = new ArrayList<KlientTraad>();
                                break;
                            } else {
                                System.out.println("\nVenter paa acknowledgement fra andre klienter.");
                            }
                            break;
                        }
                    } // if traadListe.contains
                }
            } // while
            is.close();
            os.close();
            klientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}// end class KlientTraad