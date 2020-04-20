//Two Phase Commit Protocol KLIENT

import java.io.*;
import java.net.*;

/**
 * Klient-klasse som fungerer som en deltaker i two-phase commit.
 * Klienten  kobler seg opp til en tjener. Klienten må enten si at den er klar til å "committe" eller "aborte".
 * Hvis klienten er klar til å "committe" får den klarsignal av tjeneren, og klienten "committer".
 * Avslutter forbindelse hvis den "aborter", eller hvis "committen" ble gjennomført.
 * @author Nikolai Dokken
 * @author Ian Evangelista
 * @author Kasper Gundersen
 */
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

    /**
     * Main-metode som lager en socket for hver klient.
     * Kjører så lenge forbindelsen til tjeneren ikke avsluttes.
     */
    public static void main(String[] args) {
        int port=1111;
        try {
            //InetAddress host= InetAddress.getByName("129.241.96.153");
            klientSocket = new Socket("localhost", port);
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
                 //    os.println(inputLinje.readLine());
                }
                os.close();
                is.close();
                klientSocket.close();
            } catch (IOException e) {
                System.err.println("IOException:  " + e);
            }
        }
    }
    /**
     * Run-metode som tar seg av forespørsler fra tjeneren.
     * Håndterer når data skal loggføres og responsen den får fra tjeneren.
     * Her skrives navnet til klienten og saldo.
     * Loggfører navn og saldo
     * Saldoen bestemmer om man kan si at man er klart til å "committe" eller ikke.
     * Håndterer hva som skjer når responsen er GLOBAL_COMMIT eller GLOBAL_ABORT.
     * Hvis klienten får GLOBAL_COMMIT, vil den loggføre dette og utføre transaksjonen for så å loggføre den og deretter sender ACKNOWLEDGEMENT til tjeneren.
     * Hvis klienten får GLOBAL_ABORT, vil den loggføre dette og hvis den allerede har gjort endringer, vil den finne i loggen sin og starte en rollback.
     * Avslutter forbindelsen til tjener hvis den fikk GLOBAL_ABORT eller hvis den utførte transaksjonen etter GLOBAL_COMMIT.
     */
    @SuppressWarnings("deprecation")
    public void run() {
        String responseLinje;
        int belop = 0;
        try {
            System.out.println("Hva er navnet ditt?");
            klientIdentitet = inputLinje.readLine();
            os.println(klientIdentitet);
            System.out.println("Hva er din saldo? (For eksempels skyld)");
            boolean ugyldigSaldo = true;
            while(ugyldigSaldo){
                try
                {
                    saldo = Integer.parseInt(inputLinje.readLine());
                    if(saldo < 0)System.out.println("Saldo er ugyldig. Vennligst skriv inn et gyldig postivt heltall");
                    else ugyldigSaldo = false;
                }
                catch (NumberFormatException e)
                {
                    System.out.println("Saldo er ugyldig. Vennligst skriv inn et gyldig postivt heltall");
                }
            }
            logg = new Loggforer(klientIdentitet);
            logg.loggfor(klientIdentitet + " er tilkoblet. Saldo er: " + saldo + "kr.");
            System.out.println("Velkommen " + klientIdentitet + " til denne 2-fase applikasjonen.");

            while ((responseLinje = is.readLine()) != null && !lukket) {
                System.out.println("\n"+responseLinje);
                if (responseLinje.indexOf("VOTE_REQUEST") != -1) {
                    System.out.println("Trykk enter om du vil fortsette. (Nå er tiden til å kjøre flere klienter)");
                    inputLinje.readLine();
                    belop = Integer.parseInt(responseLinje.split(":")[2]);
                    logg.loggfor("Fikk VOTE_REQUEST om å trekke " + belop + "kr.");
                    if (saldo >= belop) {
                        os.println("COMMIT");
                        logg.loggfor("SAVE: Lagrer gammel saldo(kr): " + saldo);
                        logg.loggfor("Sender COMMIT til tjener.");
                        saldo -= belop;
                        gjordeEndringer = true;
                    } else {
                        os.println("ABORT");
                        logg.loggfor("Sender ABORT til tjener, har ikke raad.");
                    }
                }
                if (responseLinje.equalsIgnoreCase("GLOBAL_ABORT")) {
                    logg.loggfor("Fikk beskjed om ABORT fra tjener");
                    if (gjordeEndringer) {
                        saldo = logg.getRollbackSaldo();
                        logg.loggfor("Rollback. Saldo er nå " + saldo + "kr");
                    }
                    break;
                }
                if (responseLinje.equalsIgnoreCase("GLOBAL_COMMIT")) {
                    logg.loggfor("Fikk klarsignal(GLOBAL_COMMIT) fra tjener.");
                    logg.loggfor("Utførte transaksjon: [Opprinnelig beløp: " + (saldo+belop) + ", Transaksjonsbeløp: " + belop + ", Nytt beløp: " + saldo + "]");
                    System.out.println("Trykk enter om du vil acknowledge.");
                    inputLinje.readLine();
                    os.println("ACKNOWLEDGEMENT");
                    logg.loggfor("Sendte ACKNOWLEDGE til tjener.");
                    break;
                }
            }
            logg.loggfor("Klienten er nå frakoblet.\n");
            System.out.println("Two phase commit er nå ferdig. Ha det bra");
            logg.close();
            //System.exit(0);
        }
        catch (IOException e) {
            System.err.println("IOException:  " + e);
        }
    }
} //end klient
