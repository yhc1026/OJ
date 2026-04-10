package com.bite.judge.sandbox;


import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class RunAndOutput {
    private final ManageDockerPool dockerPool;
    private final ThreadLocal<String> borrowedContainer = new ThreadLocal<>();

    public RunAndOutput(ManageDockerPool dockerPool) {
        this.dockerPool = dockerPool;
    }

    public boolean compile(Path javaFilePath) throws Exception {
        Path sourceFile = javaFilePath.resolve("Main.java").toAbsolutePath();
        System.out.println("sourceFile: " + sourceFile.toString());
        String containerName = borrowedContainer.get();
        if (containerName == null) {
            containerName = dockerPool.borrowContainer(3000);
            if (containerName != null) {
                borrowedContainer.set(containerName);
            }
        }
        if (containerName == null) {
            throw new IllegalStateException("当前无可用判题容器，请稍后重试");
        }

        try {
            String copyCommand = String.format(
                    "docker cp \"%s\" %s:/tmp/Main.java",
                    sourceFile.toString().replace("\\", "/"),
                    containerName
            );
            ExecResult copyResult = exec(copyCommand);
            if (copyResult.exitCode != 0) {
                System.err.println("复制源码到容器失败：\n" + copyResult.stderr);
                return false;
            }

            String compileCommand = String.format(
                    "docker exec %s sh -c \"mkdir -p /tmp/classes && javac -d /tmp/classes /tmp/Main.java\"",
                    containerName
            );
            ExecResult compileResult = exec(compileCommand);
            if (compileResult.exitCode == 0) {
                System.out.println("编译成功！");
                return true;
            }
            System.err.println("编译失败：\n" + compileResult.stderr);
            dockerPool.returnContainer(containerName);
            borrowedContainer.remove();
            return false;
        }catch (Exception e){
            e.printStackTrace();
            System.out.println(e.getMessage());
            return false;
        }
    }

    public boolean run(Path javaClassPath, Path outputPath, Path inputPath) throws IOException, InterruptedException {
        Path inputFile = inputPath.resolve("input.txt").toAbsolutePath();
        String containerName = borrowedContainer.get();
        if (containerName == null) {
            throw new IllegalStateException("未找到已编译容器，请先执行compile");
        }

        try {
            ExecResult copyInputResult = exec(String.format(
                    "docker cp \"%s\" %s:/tmp/input.txt",
                    inputFile.toString().replace("\\", "/"),
                    containerName
            ));
            if (copyInputResult.exitCode != 0) {
                System.err.println("复制输入文件到容器失败：\n" + copyInputResult.stderr);
                return false;
            }

            ExecResult runResult = exec(String.format(
                    "docker exec %s sh -c \"java -cp /tmp/classes Main < /tmp/input.txt\"",
                    containerName
            ));
            if (runResult.exitCode == 0) {
                Path resultFile = outputPath.resolve("output.txt");
                Files.writeString(resultFile, runResult.stdout, StandardCharsets.UTF_8);
                System.out.println("运行成功！结果已保存到: " + resultFile);
                return true;
            }
            System.err.println("运行失败: " + runResult.stderr);
            return false;
        } finally {
            dockerPool.returnContainer(containerName);
            borrowedContainer.remove();
        }
    }

    private ExecResult exec(String command) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(command);
        String stdout = readStream(process.getInputStream());
        String stderr = readStream(process.getErrorStream());
        int exitCode = process.waitFor();
        return new ExecResult(exitCode, stdout, stderr);
    }

    private String readStream(java.io.InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
        }
        return output.toString();
    }

    private static class ExecResult {
        private final int exitCode;
        private final String stdout;
        private final String stderr;

        private ExecResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }
    }
}

