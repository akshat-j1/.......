import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
                File exeFile = getFile(command);
                if (exeFile != null) {
                    List<String> commandList = new ArrayList<>();
                    // Fix: Running via a localized relative name forces Arg #0 to drop the absolute path prefix
                    commandList.add("/" + command);
                    commandList.addAll(Arrays.asList(parts).subList(1, parts.length));

                    ProcessBuilder pb = new ProcessBuilder(commandList);
                    // Switch the process running context to the directory where the target file lives
                    pb.directory(exeFile.getParentFile());
                    pb.inheritIO();
                    Process process = pb.start();
                    process.waitFor();
                } else {
                    System.out.println(input + ": command not found");
                }
            }
        }
    }

    private static File getFile(String command) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null || pathEnv.isEmpty()) {
            return null;
        }
        String[] directories = pathEnv.split(File.pathSeparator);
        for (String directory : directories) {
            File file = new File(directory, command);
            if (file.exists() && file.canExecute()) {
                return file;
            }
        }
        return null;
    }

    private static String getPath(String command) {
        File file = getFile(command);
        return file != null ? file.getAbsolutePath() : null;
    }
}