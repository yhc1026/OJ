package com.bite.judge.sandbox;


import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@Service
public class JudgeAndOutput {

    public boolean judgeCode(Path path) throws IOException {
        Path expectedResPath=path.resolve("expected.txt");
        Path outputResPath=path.resolve("output.txt");
        String expected = Files.readString(expectedResPath);
        String actual = Files.readString(outputResPath);

        return compare(expected, actual);
    }



    private boolean compare(String expected, String actual) {
        if (expected == null && actual == null) return true;
        if (expected == null || actual == null) return false;

        // 宽松比较：去空格、合并连续空格
        String normExpected = expected.trim().replaceAll("\\s+", " ");
        String normActual = actual.trim().replaceAll("\\s+", " ");

        return normExpected.equals(normActual);
    }

    public static void main(String[] args) throws IOException {
        JudgeAndOutput judgeAndOutput = new JudgeAndOutput();
        Path path= Paths.get("D:/Desktop/new");
        boolean result=judgeAndOutput.judgeCode(path);
        System.out.println(result);
    }
}
