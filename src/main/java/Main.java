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
            
            if (input.equals("exit") || input.startsWith("exit ")) {
                System.exit(0);
            } else if (input.startsWith("echo ")) {
                String content = input.substring(5);
                System.out.println(content);
            } else if (input.startsWith("type ")) {
                String arg = input.substring(5).trim();
                if (arg.equals("echo") || arg.equals("exit") || arg.equals("type")) {
                    System.out.println(arg + " is a shell builtin");
                } else {
                    System.out.println(arg + ": not found");
                }
            } else {
                System.out.println(input + ": command not found");
            }
        }
    }
}