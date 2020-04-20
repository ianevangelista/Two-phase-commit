import java.util.ArrayList;

public class Test {
    public static void main(String[] args) {
        ArrayList<String> test = new ArrayList<String>();
        test.add("niko");
        test.add("ian");
        test.remove(test.indexOf("ian"));
        test.remove(test.indexOf("niko"));
        System.out.println(test.size());
    }
}
