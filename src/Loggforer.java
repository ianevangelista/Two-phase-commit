import java.io.*;
import java.util.Calendar;
import java.util.Date;

public class Loggforer {
    private File loggFil;
    private String filnavn;
    private BufferedReader leseForbindelse;
    private BufferedWriter skriveForbindelse;

    public Loggforer(String navn) {
        this.filnavn = "logger/" + navn.toLowerCase() + ".txt";
        String attributter = "dato,saldoFor,transaksjon,saldoEtter\n";
        try {
            this.loggFil = new File(filnavn);
            this.loggFil.createNewFile();
            } catch(IOException e) {
            e.printStackTrace();
        }
    }

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

    public static void main(String[] args) {
        Loggforer logg = new Loggforer("niko");
        logg.loggfor("10,-5,5");
        System.out.println(logg.getRollbackSaldo());
        logg.close();
    }
}
