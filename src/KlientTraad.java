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
            is = new DataInputStream(klientSocket.getInputStream());
            os = new PrintStream(klientSocket.getOutputStream());
            os.println("Skriv inn navnet ditt: ");
            klientIdentitet = is.readLine();
            this.logg = new Loggforer(klientIdentitet);
            os.println("Hva er din saldo? (Dette er for eksemplets betyding, ikke slik i virkeligheten)");
            saldo = Integer.parseInt(is.readLine());
            logg.loggfor(klientIdentitet + " er tilkoblet. Saldo er: " + saldo + "kr.");
            os.println("Velkommen " + klientIdentitet + " til denne 2-fase applikasjonen.\nDu vil motta en VOTE_REQUEST...");
            os.println("VOTE_REQUEST:\nTrekker fra " + this.tjener.belop + "kr om du har raad. Skriv ok om du vil gaa videre");
            logg.loggfor("Fikk voterequest om å trekke " + this.tjener.belop + "kr.");
            is.readLine();
            for (int i = 0; i < (tjener.traadListe).size(); i++) {
                if ((tjener.traadListe).get(i) != this) {
                    ((tjener.traadListe).get(i)).os.println("---En ny bruker ved navn " + klientIdentitet + " har blitt med i applikasjonen---");
                }
            }
            while (true) {
                linje = saldo >= 5 ? "COMMIT":"ABORT";

                if (linje.equalsIgnoreCase("ABORT")) {
                    logg.loggfor("Sender ABORT til tjener, har ikke raad.");
                    System.out.println("\nFra '" + klientIdentitet
                            + "' : ABORT\n\nSiden det ble skrevet ABORT, vil vi ikke vente paa flere input fra andre klienter.");
                    System.out.println("\nAborted...");

                    while(tjener.traadListe.size() > 0) {
                        ((tjener.traadListe).get(0)).os.println("ABORT");
                        if (tjener.traadListe.get(0) != this) { // TODO: rollback skjer bare om man har commited
                            ((tjener.traadListe).get(0)).logg.loggfor("Fikk beskjed om ABORT, ruller tilbake...");
                            ((tjener.traadListe).get(0)).saldo = tjener.traadListe.get(0).logg.getRollbackSaldo();
                            tjener.traadListe.get(0).logg.loggfor("Rollback ferdig. Saldo er nå " + tjener.traadListe.get(0).saldo + "kr");
                        }
                        ((tjener.traadListe).get(0)).os.close();
                        ((tjener.traadListe).get(0)).is.close();
                        tjener.data.remove(tjener.traadListe.indexOf(tjener.traadListe.get(0)));
                        tjener.traadListe.remove(0);
                    }
                    break;
                }
                if (linje.equalsIgnoreCase("COMMIT")) {
                    logg.loggfor("Lagrer gammel saldo: " + saldo + "kr. Sender COMMIT beskjed til tjener.");
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
                                ((tjener.traadListe).get(i)).logg.loggfor("Mottok GLOBAL_COMMIT fra tjener");
                                ((tjener.traadListe).get(i)).logg.loggfor("Committer transaksjon: " + (saldo-tjener.belop) + "," + tjener.belop + "," + saldo);
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