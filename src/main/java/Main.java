import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        // Print the prompt with a trailing space, using print instead of println
        System.out.print("$ ");

        // Wait for user input so the program doesn't exit immediately
        Scanner scanner = new Scanner(System.util.in);
        scanner.nextLine();
    }
}