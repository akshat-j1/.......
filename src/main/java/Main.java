import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        // Print the prompt with a trailing space
        System.out.print("$ ");

        // Fixed: Changed System.util.in to System.in
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
    }
}