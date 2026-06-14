import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("$ ");
            
            if (!scanner.hasNextLine()) {
                break;
            }
            
            String input = scanner.nextLine().trim();
            
            if (input.equals("exit 0")) {
                System.exit(0);
            } else {
                System.out.println(input + ": command not found");
            }
        }
    }
}