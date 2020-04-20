import java.io.*;
import java.util.Calendar;
import java.util.Date;

/**
 * Loggforer-klasse som logger alt klienten foretar seg.
 * Logger klientens navn, saldo, transaksjoner, COMMIT og ABORT.
 * @author Nikolai Dokken
 * @author Ian Evangelista
 * @author Kasper Gundersen
 */
public class Loggforer {
    private File loggFil;
    private String filnavn;
    private BufferedReader leseForbindelse;
    private BufferedWriter skriveForbindelse;

    /**
     * Oppretter en txt-fil i klientens navn.
     * @param navn navnet til klienten. Loggen lagres som navnet til klienten som en txt-fil
     */
    public Loggforer(String navn) {
        this.filnavn = "logger/" + navn.toLowerCase() + ".txt";
        try {
            this.loggFil = new File(filnavn);
            this.loggFil.createNewFile();
            } catch(IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Skriver til fil.
     * @param loggforing tekst som sendes inn og skrives til fil.
     * @return true hvis den får skrevet til fil, false ellers.
     */
    public boolean loggfor(String loggforing) {
        Calendar kalender = Calendar.getInstance();
        Date dato = kalender.getTime();
        try {
            skriveForbindelse = new BufferedWriter(new FileWriter(filnavn, true));
            skriveForbindelse.write(dato + "," + loggforing + "\n");
            skriveForbindelse.close();
            return true;
        } catch(IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    /**
     * Rollback-metode som gjør at man kan finne tilbake til tidligere saldo hvis man allerede har gjort endringer.
     * Leter etter siste linje med SAVE for å finne saldoen.
     * @return heltall hvis den finner den tidligere saldoen. Returnerer -1 hvis den ikke finner.
     */
    public int getRollbackSaldo() {
        try {
            leseForbindelse = new BufferedReader(new FileReader(filnavn));
            String currentLine;
            String lagretSaldo = "";
            while ((currentLine = leseForbindelse.readLine()) != null) {
                if (currentLine.indexOf("SAVE") != -1) lagretSaldo = currentLine;
            }
            leseForbindelse.close();
            return Integer.parseInt(lagretSaldo.split(":")[4].trim());
        } catch(IOException e) {
            e.printStackTrace();
        }
        return -1;
    }
    /**
     * Lukker skrive-og-leseforbindelsen.
     */
    public void close() {
        try {
            if (skriveForbindelse != null) {
            skriveForbindelse.close();
            }
            if (leseForbindelse != null) {
                leseForbindelse.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
