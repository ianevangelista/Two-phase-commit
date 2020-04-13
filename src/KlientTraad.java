import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;

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
            // Simulerer avgjørelse
            if (tjener.traadListe.size() == 1) {
                tjener.tjenerNavnGammel = tjener.tjenerNavn;
                if (tjener.tjenerNavn.equals("Ola")) {
                    tjener.tjenerNavn = "Per"; // dette skjedde egt før
                } else {
                    tjener.tjenerNavn = "Ola"; // dette skjedde egt før
                }
            }
            is = new DataInputStream(klientSocket.getInputStream());
            os = new PrintStream(klientSocket.getOutputStream());
            os.println("Skriv inn navnet ditt: ");
            navn = is.readLine();
            klientIdentitet = navn;
            os.println("Velkommen " + navn + " til denne 2-fase applikasjonen.\nDu vil motta en VOTE_REQUEST...");
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

                    while(tjener.traadListe.size() > 0) {
                        ((tjener.traadListe).get(0)).os.println("ABORT");
                        ((tjener.traadListe).get(0)).os.close();
                        ((tjener.traadListe).get(0)).is.close();
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
                            tjener.tjenerNavnGammel = tjener.tjenerNavn;
                            System.out.println("\n\nCommited.... Navnet mitt er naa " + tjener.tjenerNavn);
                            while(tjener.traadListe.size() > 0) {
                                ((tjener.traadListe).get(0)).os.println("GLOBAL_COMMIT");
                                ((tjener.traadListe).get(0)).os.close();
                                ((tjener.traadListe).get(0)).is.close();
                                tjener.data.remove(tjener.traadListe.indexOf(tjener.traadListe.get(0)));
                                tjener.traadListe.remove(0);
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