import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        // Step 1: Display the prompt (keep the code from the previous stage)
        System.out.print("$ ");

        // Step 2: Read the user's input command
        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine();

        // Step 3: Print the error message format: {command}: command not found
        System.out.println(input + ": command not found");
    }
}