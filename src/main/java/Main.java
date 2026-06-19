import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
            } else if (command.equals("cd")) {
                if (parts.length < 2) {
                    continue;
                }
                String targetDir = parts[1];
                Path currentPath = Paths.get(System.getProperty("user.dir"));
                Path targetPath = currentPath.resolve(targetDir).normalize();

                if (Files.exists(targetPath) && Files.isDirectory(targetPath)) {
                    System.setProperty("user.dir", targetPath.toAbsolutePath().toString());
                } else {
                    System.out.println("cd: " + targetDir + ": No such file or directory");
                }
            } else if (command.equals("type")) {
                if (parts.length < 2) {
                    continue;
                }
                String arg = parts[1];
                if (arg.equals("echo") || arg.equals("exit") || arg.equals("type") || arg.equals("pwd") || arg.equals("cd")) {
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
                String executablePath = getPath(command);
                if (executablePath != null) {
                    ProcessBuilder pb = new ProcessBuilder(parts);
                    pb.directory(new File(System.getProperty("user.dir")));
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