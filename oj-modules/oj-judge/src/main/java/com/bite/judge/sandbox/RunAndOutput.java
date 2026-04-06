package com.bite.judge.sandbox;


import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class RunAndOutput {
    public boolean compile(Path javaFilePath, Path outputPath) throws Exception {
        javaFilePath=javaFilePath.resolve("/Main.java");
        // 生成命令行，挂载在docker
        String command = String.format(
                "docker run --rm -v \"%s:/workspace\" -w /workspace eclipse-temurin:17-jdk-alpine javac -d /workspace/classes /workspace/%s",
                outputPath.toAbsolutePath().toString().replace("\\", "/"),
                javaFilePath.getFileName().toString()
        );

        // 编译
        Process process = Runtime.getRuntime().exec(command);
        StringBuilder errorOutput = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();

        if (exitCode == 0) {
            System.out.println("编译成功！");
            return true;
        } else {
            System.err.println("编译失败：\n" + errorOutput);
            return false;
        }
    }

    public boolean run(Path javaClassPath, Path outputPath, Path inputPath) throws IOException, InterruptedException {
        //这个参数没被用到是因为java自动找class文件
        javaClassPath=javaClassPath.resolve("/Main.class");
        String command = String.format(
                "docker run --rm -v \"%s:/workspace\" -w /workspace eclipse-temurin:17-jdk-alpine sh -c \"java -cp classes Main < /workspace/input.txt\"",
                outputPath.toAbsolutePath().toString().replace("\\", "/")
        );

        Process process = Runtime.getRuntime().exec(command);

        // 读取输出
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();

        if (exitCode == 0) {
            // ✅ 输出到文件
            Path resultFile = outputPath.resolve("output.txt");
            Files.writeString(resultFile, output.toString(), StandardCharsets.UTF_8);
            System.out.println("运行成功！结果已保存到: " + resultFile);
            return true;
        } else {
            // 读取错误
            StringBuilder error = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    error.append(line).append("\n");
                }
            }
            System.err.println("运行失败: " + error);
            return false;
        }
    }

    public static void main(String[] args) throws Exception {
        Path javaFilePath = Paths.get("D:/Desktop/new");
        Path outputPath = Paths.get("D:/Desktop/new");
        Path javaClassPath = Paths.get("D:/Desktop/new");
        Path inputPath = Paths.get("D:/Desktop/new");
        RunAndOutput runAndOutput = new RunAndOutput();
        boolean success1=runAndOutput.compile(javaFilePath, outputPath);
        boolean success2=runAndOutput.run(javaClassPath, outputPath, inputPath);
        System.out.println(success1);
        System.out.println(success2);
    }
}

