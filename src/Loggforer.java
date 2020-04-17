import java.io.*;
import java.util.Calendar;
import java.util.Date;

public class Loggforer {
    private File loggFil;
    private String filnavn;
    private BufferedReader leseForbindelse;
    private BufferedWriter skriveForbindelse;

    public Loggforer(String navn) {
        this.filnavn = navn.toLowerCase() + ".txt";
        String attributter = "dato,saldoFor,transaksjon,saldoEtter\n";
        try {
            this.loggFil = new File(filnavn);
            if (this.loggFil.createNewFile()) {
                this.skriveForbindelse = new BufferedWriter(new FileWriter(filnavn));
                this.skriveForbindelse.write(attributter);
                this.skriveForbindelse.close();
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public boolean loggfor(int saldoFor, int belop) {
        Calendar kalender = Calendar.getInstance();
        Date dato = kalender.getTime();
        try {
            skriveForbindelse = new BufferedWriter(new FileWriter(filnavn, true));
            skriveForbindelse.write(dato + "," + saldoFor + "," + belop + "," + (saldoFor+belop) + "\n");
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
            String sCurrentLine;
            String lastLine = "";
            while ((sCurrentLine = leseForbindelse.readLine()) != null) {
                lastLine = sCurrentLine;
            }
            String[] data = lastLine.split(",");
            leseForbindelse.close();
            return Integer.parseInt(data[1]);
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
        logg.loggfor(10, -5);
        System.out.println(logg.getRollbackSaldo());
        logg.close();
    }
}
