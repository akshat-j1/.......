import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    static class Job {
        int id;
        Process process;
        String commandString;
        String status;

        public Job(int id, Process process, String commandString, String status) {
            this.id = id;
            this.process = process;
            this.commandString = commandString;
            this.status = status;
        }
    }

    private static final List<Job> backgroundJobs = new ArrayList<>();
    private static int nextJobId = 1;

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            reapBeforePrompt();

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

            boolean isBackground = false;
            if (parts.get(parts.size() - 1).equals("&")) {
                isBackground = true;
                parts.remove(parts.size() - 1);
            }

            if (parts.isEmpty()) {
                continue;
            }

            String stdoutFile = null;
            String stderrFile = null;
            boolean isAppendStdout = false;
            boolean isAppendStderr = false;
            int redirectIndex = -1;

            for (int i = 0; i < parts.size(); i++) {
                String token = parts.get(i);
                if (token.equals(">") || token.equals("1>")) {
                    redirectIndex = i;
                    if (i + 1 < parts.size()) {
                        stdoutFile = parts.get(i + 1);
                    }
                    break;
                } else if (token.equals(">>") || token.equals("1>>")) {
                    redirectIndex = i;
                    isAppendStdout = true;
                    if (i + 1 < parts.size()) {
                        stdoutFile = parts.get(i + 1);
                    }
                    break;
                } else if (token.equals("2>")) {
                    redirectIndex = i;
                    if (i + 1 < parts.size()) {
                        stderrFile = parts.get(i + 1);
                    }
                    break;
                } else if (token.equals("2>>")) {
                    redirectIndex = i;
                    isAppendStderr = true;
                    if (i + 1 < parts.size()) {
                        stderrFile = parts.get(i + 1);
                    }
                    break;
                }
            }

            List<String> execParts;
            if (redirectIndex != -1) {
                execParts = parts.subList(0, redirectIndex);
            } else {
                execParts = parts;
            }

            if (execParts.isEmpty()) {
                continue;
            }
            
            String command = execParts.get(0);

            if (command.equals("exit")) {
                System.exit(0);
            } else if (command.equals("echo")) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < execParts.size(); i++) {
                    sb.append(execParts.get(i));
                    if (i < execParts.size() - 1) {
                        sb.append(" ");
                    }
                }
                if (stdoutFile != null) {
                    File file = new File(stdoutFile);
                    if (file.getParentFile() != null) file.getParentFile().mkdirs();
                    if (isAppendStdout) {
                        Files.writeString(file.toPath(), sb.toString() + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } else {
                        Files.writeString(file.toPath(), sb.toString() + "\n");
                    }
                } else {
                    System.out.println(sb.toString());
                }
                if (stderrFile != null) {
                    File file = new File(stderrFile);
                    if (file.getParentFile() != null) file.getParentFile().mkdirs();
                    if (!file.exists()) file.createNewFile();
                }
            } else if (command.equals("pwd")) {
                String currentDir = System.getProperty("user.dir");
                if (stdoutFile != null) {
                    File file = new File(stdoutFile);
                    if (file.getParentFile() != null) file.getParentFile().mkdirs();
                    if (isAppendStdout) {
                        Files.writeString(file.toPath(), currentDir + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } else {
                        Files.writeString(file.toPath(), currentDir + "\n");
                    }
                } else {
                    System.out.println(currentDir);
                }
                if (stderrFile != null) {
                    File file = new File(stderrFile);
                    if (file.getParentFile() != null) file.getParentFile().mkdirs();
                    if (!file.exists()) file.createNewFile();
                }
            } else if (command.equals("jobs")) {
                // 1. Update all statuses first
                for (Job job : backgroundJobs) {
                    if (job.status.equals("Running") && !job.process.isAlive()) {
                        job.status = "Done";
                        if (job.commandString.endsWith(" &")) {
                            job.commandString = job.commandString.substring(0, job.commandString.length() - 2);
                        }
                    }
                }

                // 2. Print all jobs in exact sequential order
                StringBuilder jobsOutput = new StringBuilder();
                int numJobs = backgroundJobs.size();
                List<Job> jobsToRemove = new ArrayList<>();

                for (int i = 0; i < numJobs; i++) {
                    Job job = backgroundJobs.get(i);
                    char marker = ' ';
                    if (i == numJobs - 1) {
                        marker = '+';
                    } else if (i == numJobs - 2) {
                        marker = '-';
                    }

                    String statusField = String.format("%-24s", job.status);
                    jobsOutput.append(String.format("[%d]%c  %s%s\n", job.id, marker, statusField, job.commandString));

                    if (job.status.equals("Done")) {
                        jobsToRemove.add(job);
                    }
                }

                // 3. Clean up reaped jobs after sequential display output is formed
                backgroundJobs.removeAll(jobsToRemove);

                if (stdoutFile != null) {
                    File file = new File(stdoutFile);
                    if (file.getParentFile() != null) file.getParentFile().mkdirs();
                    if (isAppendStdout) {
                        Files.writeString(file.toPath(), jobsOutput.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } else {
                        Files.writeString(file.toPath(), jobsOutput.toString());
                    }
                } else {
                    System.out.print(jobsOutput.toString());
                }

                if (stderrFile != null) {
                    File file = new File(stderrFile);
                    if (file.getParentFile() != null) file.getParentFile().mkdirs();
                    if (!file.exists()) file.createNewFile();
                }
            } else if (command.equals("cd")) {
                if (execParts.size() < 2) {
                    continue;
                }
                String targetDir = execParts.get(1);
                java.nio.file.Path targetPath;

                if (targetDir.equals("~")) {
                    targetPath = java.nio.file.Paths.get(System.getenv("HOME"));
                } else {
                    java.nio.file.Path currentPath = java.nio.file.Paths.get(System.getProperty("user.dir"));
                    targetPath = currentPath.resolve(targetDir).normalize();
                }

                if (java.nio.file.Files.exists(targetPath) && java.nio.file.Files.isDirectory(targetPath)) {
                    System.setProperty("user.dir", targetPath.toAbsolutePath().toString());
                    if (stdoutFile != null) {
                        File file = new File(stdoutFile);
                        if (file.getParentFile() != null) file.getParentFile().mkdirs();
                        if (!file.exists()) file.createNewFile();
                    }
                } else {
                    String errMsg = "cd: " + targetDir + ": No such file or directory";
                    if (stderrFile != null) {
                        File file = new File(stderrFile);
                        if (file.getParentFile() != null) file.getParentFile().mkdirs();
                        if (isAppendStderr) {
                            Files.writeString(file.toPath(), errMsg + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        } else {
                            Files.writeString(file.toPath(), errMsg + "\n");
                        }
                    } else {
                        System.out.println(errMsg);
                    }
                    if (stdoutFile != null) {
                        File file = new File(stdoutFile);
                        if (file.getParentFile() != null) file.getParentFile().mkdirs();
                        if (!file.exists()) file.createNewFile();
                    }
                }
            } else if (command.equals("type")) {
                if (execParts.size() < 2) {
                    continue;
                }
                String arg = execParts.get(1);
                String resultMessage;
                if (arg.equals("echo") || arg.equals("exit") || arg.equals("type") || arg.equals("pwd") || arg.equals("cd") || arg.equals("jobs")) {
                    resultMessage = arg + " is a shell builtin";
                } else {
                    String executablePath = getPath(arg);
                    if (executablePath != null) {
                        resultMessage = arg + " is " + executablePath;
                    } else {
                        resultMessage = arg + ": not found";
                    }
                }

                if (stdoutFile != null) {
                    File file = new File(stdoutFile);
                    if (file.getParentFile() != null) file.getParentFile().mkdirs();
                    if (isAppendStdout) {
                        Files.writeString(file.toPath(), resultMessage + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } else {
                        Files.writeString(file.toPath(), resultMessage + "\n");
                    }
                } else {
                    System.out.println(resultMessage);
                }
                if (stderrFile != null) {
                    File file = new File(stderrFile);
                    if (file.getParentFile() != null) file.getParentFile().mkdirs();
                    if (!file.exists()) file.createNewFile();
                }
            } else {
                String executablePath = getPath(command);
                if (executablePath != null) {
                    ProcessBuilder pb = new ProcessBuilder(execParts);
                    pb.directory(new File(System.getProperty("user.dir")));
                    
                    if (stdoutFile != null) {
                        File file = new File(stdoutFile);
                        if (file.getParentFile() != null) file.getParentFile().mkdirs();
                        if (isAppendStdout) {
                            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(file));
                        } else {
                            pb.redirectOutput(ProcessBuilder.Redirect.to(file));
                        }
                    } else {
                        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                    }

                    if (stderrFile != null) {
                        File file = new File(stderrFile);
                        if (file.getParentFile() != null) file.getParentFile().mkdirs();
                        if (isAppendStderr) {
                            pb.redirectError(ProcessBuilder.Redirect.appendTo(file));
                        } else {
                            pb.redirectError(ProcessBuilder.Redirect.to(file));
                        }
                    } else {
                        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                    }

                    Process process = pb.start();
                    
                    if (isBackground) {
                        long pid = process.pid();
                        System.out.println("[" + nextJobId + "] " + pid);
                        backgroundJobs.add(new Job(nextJobId++, process, input, "Running"));
                    } else {
                        process.waitFor();
                    }
                } else {
                    String errMsg = command + ": command not found";
                    if (stderrFile != null) {
                        File file = new File(stderrFile);
                        if (file.getParentFile() != null) file.getParentFile().mkdirs();
                        if (isAppendStderr) {
                            Files.writeString(file.toPath(), errMsg + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        } else {
                            Files.writeString(file.toPath(), errMsg + "\n");
                        }
                    } else {
                        System.out.println(errMsg);
                    }
                }
            }
        }
    }

    private static void reapBeforePrompt() {
        int numJobs = backgroundJobs.size();
        List<Job> jobsToRemove = new ArrayList<>();

        for (int i = 0; i < numJobs; i++) {
            Job job = backgroundJobs.get(i);
            if (job.status.equals("Running") && !job.process.isAlive()) {
                job.status = "Done";
                if (job.commandString.endsWith(" &")) {
                    job.commandString = job.commandString.substring(0, job.commandString.length() - 2);
                }

                char marker = ' ';
                if (i == numJobs - 1) {
                    marker = '+';
                } else if (i == numJobs - 2) {
                    marker = '-';
                }

                String statusField = String.format("%-24s", job.status);
                System.out.printf("[%d]%c  %s%s\n", job.id, marker, statusField, job.commandString);
                jobsToRemove.add(job);
            }
        }
        backgroundJobs.removeAll(jobsToRemove);
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