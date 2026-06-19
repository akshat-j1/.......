import java.io.File;
import java.util.ArrayList;
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

            List<String> parts = parseArguments(input);
            if (parts.isEmpty()) {
                continue;
            }
            
            String command = parts.get(0);

            if (command.equals("exit")) {
                System.exit(0);
            } else if (command.equals("echo")) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < parts.size(); i++) {
                    sb.append(parts.get(i));
                    if (i < parts.size() - 1) {
                        sb.append(" ");
                    }
                }
                System.out.println(sb.toString());
            } else if (command.equals("pwd")) {
                System.out.println(System.getProperty("user.dir"));
            } else if (command.equals("cd")) {
                if (parts.size() < 2) {
                    continue;
                }
                String targetDir = parts.get(1);
                java.nio.file.Path targetPath;

                if (targetDir.equals("~")) {
                    targetPath = java.nio.file.Paths.get(System.getenv("HOME"));
                } else {
                    java.nio.file.Path currentPath = java.nio.file.Paths.get(System.getProperty("user.dir"));
                    targetPath = currentPath.resolve(targetDir).normalize();
                }

                if (java.nio.file.Files.exists(targetPath) && java.nio.file.Files.isDirectory(targetPath)) {
                    System.setProperty("user.dir", targetPath.toAbsolutePath().toString());
                } else {
                    System.out.println("cd: " + targetDir + ": No such file or directory");
                }
            } else if (command.equals("type")) {
                if (parts.size() < 2) {
                    continue;
                }
                String arg = parts.get(1);
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
                    List<String> execArgs = new ArrayList<>();
                    execArgs.add("sh");
                    execArgs.add("-c");
                    
                    StringBuilder commandLine = new StringBuilder();
                    commandLine.append("'").append(executablePath.replace("'", "'\\''")).append("'");
                    for (int i = 1; i < parts.size(); i++) {
                        commandLine.append(" '").append(parts.get(i).replace("'", "'\\''")).append("'");
                    }
                    execArgs.add(commandLine.toString());

                    ProcessBuilder pb = new ProcessBuilder(execArgs);
                    pb.directory(new File(System.getProperty("user.dir")));
                    pb.inheritIO();
                    Process process = pb.start();
                    process.waitFor();
                } else {
                    System.out.println(command + ": command not found");
                }
            }
        }
    }

    private static List<String> parseArguments(String input) {
        List<String> args = new ArrayList<>();
        StringBuilder currentArg = new StringBuilder();
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;
        boolean hasContent = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '\\' && !inSingleQuotes) {
                if (inDoubleQuotes) {
                    if (i + 1 < input.length()) {
                        char nextChar = input.charAt(i + 1);
                        if (nextChar == '"' || nextChar == '\\' || nextChar == '$' || nextChar == '`') {
                            currentArg.append(nextChar);
                            i++;
                        } else {
                            currentArg.append(c);
                        }
                        hasContent = true;
                    } else {
                        currentArg.append(c);
                        hasContent = true;
                    }
                } else {
                    if (i + 1 < input.length()) {
                        currentArg.append(input.charAt(++i));
                        hasContent = true;
                    }
                }
            } else if (c == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
                hasContent = true;
            } else if (c == '"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
                hasContent = true;
            } else if (Character.isWhitespace(c) && !inSingleQuotes && !inDoubleQuotes) {
                if (currentArg.length() > 0 || hasContent) {
                    args.add(currentArg.toString());
                    currentArg.setLength(0);
                    hasContent = false;
                }
            } else {
                currentArg.append(c);
            }
        }

        if (currentArg.length() > 0 || hasContent) {
            args.add(currentArg.toString());
        }

        return args;
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