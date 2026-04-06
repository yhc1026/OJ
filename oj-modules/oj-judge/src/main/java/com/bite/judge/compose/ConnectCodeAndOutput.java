package com.bite.judge.compose;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

@Component
public class ConnectCodeAndOutput {
    public String connectCode(String userCode, String mainMethod){
        return userCode + "\n\n" +mainMethod;
    }

    public void outputJava(Path path, String finalCode) throws IOException {
        path = path.resolve("Main.java");
        Files.writeString(path, finalCode, StandardCharsets.UTF_8);
    }

    public void outputTest(Path path, String testInput) throws IOException {
        path = path.resolve("input.txt");
        Files.writeString(path, testInput, StandardCharsets.UTF_8);
    }

    public void outputExpectedResult(Path path, String testInput) throws IOException {
        path = path.resolve("expected.txt");
        Files.writeString(path, testInput, StandardCharsets.UTF_8);
    }


    public static void main(String[] args) throws IOException {
        ConnectCodeAndOutput connectCodeAndOutput = new ConnectCodeAndOutput();
        Path path=Paths.get("D:/Desktop/new");
        connectCodeAndOutput.outputExpectedResult(path,"4");
    }
}
