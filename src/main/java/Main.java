import java.io.File;
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
            if (input.isEmpty()) {
                continue;
            }

            String[] parts = input.split("\\s+");
            String command = parts[0];

            if (command.equals("exit")) {
                System.exit(0);
            } else if (command.equals("echo")) {
                String content = input.substring(5);
                System.out.println(content);
            } else if (command.equals("pwd")) {
                System.out.println(System.getProperty("user.dir"));
            } else if (command.equals("type")) {
                if (parts.length < 2) {
                    continue;
                }
                String arg = parts[1];
                if (arg.equals("echo") || arg.equals("exit") || arg.equals("type") || arg.equals("pwd")) {
                    System.out.println(arg + " is a shell builtin");
                } else {
                    String executablePath = getPath(arg);
                    if (executablePath != null) {
                        System.out.println(arg + " is " + executablePath);
                    } else {
                        System.out.println(arg + ": not found");
                    }
                }
            } else {
                // If it's not a builtin, check if it exists in the PATH environment
                String executablePath = getPath(command);
                if (executablePath != null) {
                    // Pass the raw parts directly. The OS handles PATH lookups 
                    // and preserves the naked command name as Arg #0 perfectly.
                    ProcessBuilder pb = new ProcessBuilder(parts);
                    pb.inheritIO();
                    Process process = pb.start();
                    process.waitFor();
                } else {
                    System.out.println(input + ": command not found");
                }
            }
        }
    }

    private static String getPath(String command) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null || pathEnv.isEmpty()) {
            return null;
        }

        String[] directories = pathEnv.split(File.pathSeparator);
        for (String directory : directories) {
            File file = new File(directory, command);
            if (file.exists() && file.canExecute()) {
                return file.getAbsolutePath();
            }
        }
        return null;
    }
}