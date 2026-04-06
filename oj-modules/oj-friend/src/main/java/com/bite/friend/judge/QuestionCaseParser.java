package com.bite.friend.judge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 解析 {@code tb_question.question_case}，约定为 JSON 数组：
 * {@code [{"input":"...","output":"..."}]}，字段别名支持 in/out、expected。
 */
public final class QuestionCaseParser {

    private static final ObjectMapper M = new ObjectMapper();

    private QuestionCaseParser() {
    }

    public static List<QuestionTestCase> parse(String questionCaseJson) throws Exception {
        if (!StringUtils.hasText(questionCaseJson)) {
            return List.of();
        }
        JsonNode root = M.readTree(questionCaseJson.trim());
        if (!root.isArray()) {
            return List.of();
        }
        List<QuestionTestCase> list = new ArrayList<>();
        for (JsonNode n : root) {
            if (n == null || !n.isObject()) {
                continue;
            }
            QuestionTestCase t = new QuestionTestCase();
            t.setInput(firstText(n, "input", "in"));
            t.setOutput(firstText(n, "output", "out", "expected"));
            if (StringUtils.hasText(t.getInput()) || StringUtils.hasText(t.getOutput())) {
                list.add(t);
            }
        }
        return list;
    }

    private static String firstText(JsonNode obj, String... keys) {
        for (String k : keys) {
            JsonNode v = obj.get(k);
            if (v != null && v.isTextual()) {
                return v.asText();
            }
            if (v != null && v.isNumber()) {
                return v.asText();
            }
        }
        Iterator<String> it = obj.fieldNames();
        while (it.hasNext()) {
            String name = it.next();
            for (String k : keys) {
                if (name.equalsIgnoreCase(k)) {
                    JsonNode v = obj.get(name);
                    if (v != null && v.isTextual()) {
                        return v.asText();
                    }
                    if (v != null && v.isNumber()) {
                        return v.asText();
                    }
                }
            }
        }
        return "";
    }
}
