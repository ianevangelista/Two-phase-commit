import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;

class KlientTraad extends Thread {
    DataInputStream is = null;
    String linje;
    PrintStream os = null;
    Socket klientSocket = null;
    String klientIdentitet;
    Tjener tjener;
    int saldo = 0;
    Loggforer logg;

    public KlientTraad(Tjener tjener, Socket klientSocket) {
        this.klientSocket = klientSocket;
        this.tjener = tjener;
    }

    @SuppressWarnings("deprecation")
    public void run() {
        try {
            // Simulerer avgjørelse
            is = new DataInputStream(klientSocket.getInputStream());
            os = new PrintStream(klientSocket.getOutputStream());
            os.println("Skriv inn navnet ditt: ");
            klientIdentitet = is.readLine();
            this.logg = new Loggforer(klientIdentitet);
            os.println("Hva er din saldo? (Dette er for eksemplets betyding, ikke slik i virkeligheten)");
            saldo = Integer.parseInt(is.readLine());
            os.println("Velkommen " + klientIdentitet + " til denne 2-fase applikasjonen.\nDu vil motta en VOTE_REQUEST...");
            os.println("VOTE_REQUEST:\nTrekker fra 5kr om du har raad. Skriv ok om du vil gå videre");
            is.readLine();
            for (int i = 0; i < (tjener.traadListe).size(); i++) {
                if ((tjener.traadListe).get(i) != this) {
                    ((tjener.traadListe).get(i)).os.println("---En ny bruker ved navn " + klientIdentitet + " har blitt med i applikasjonen---");
                }
            }
            while (true) {
                linje = saldo >= 5 ? "COMMIT":"ABORT";

                if (linje.equalsIgnoreCase("ABORT")) {
                    System.out.println("\nFra '" + klientIdentitet
                            + "' : ABORT\n\nSiden det ble skrevet ABORT, vil vi ikke vente paa flere input fra andre klienter.");
                    System.out.println("\nAborted...");

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
                    logg.loggfor(saldo, this.tjener.belop);
                    saldo += this.tjener.belop;

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
                            for(int i = 0; i < tjener.traadListe.size(); i++) {
                                ((tjener.traadListe).get(i)).os.println("GLOBAL_COMMIT");
                                ((tjener.traadListe).get(i)).os.close();
                                ((tjener.traadListe).get(i)).is.close();
                                tjener.data.remove(tjener.traadListe.indexOf(tjener.traadListe.get(0)));
                                //tjener.traadListe.remove(0); //fjerner senere etter ack
                            }

                            (tjener.ack).set((tjener.traadListe).indexOf(this), "ACKNOWLEDGEMENT");
                            for (int j = 0; j < (tjener.ack).size(); j++) {
                                if (!(((tjener.ack).get(j)).equalsIgnoreCase("NOT_SENT"))) {
                                    tjener.ackFraAlle = true;
                                    continue;
                                } else {
                                    tjener.ackFraAlle = false;
                                    System.out.println("\nVenter paa acknowledgement fra andre klienter.");
                                    break;
                                }
                            }

                            if (tjener.ackFraAlle) {
                                System.out.println("\nFra '" + klientIdentitet + "' : ACKNOWLEDGEMENT");
                            }
                            break;
                        }
                        break;
                    } // if traadListe.contains
                } // commit
            } // while
            // tjener.lukket = true;
            klientSocket.close();
        } catch (IOException e) { }
    }
}// end class KlientTraad