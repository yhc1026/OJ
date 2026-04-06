package com.bite.judge.service.impl;

import com.bite.common.core.enums.JudgeVerdict;
import com.bite.domain.Result;
import com.bite.judge.domain.dto.friend.JudgeRunRequest;
import com.bite.judge.domain.dto.friend.JudgeSingleCaseResponse;
import com.bite.judge.compose.ConnectCodeAndOutput;
import com.bite.judge.sandbox.JudgeAndOutput;
import com.bite.judge.sandbox.RunAndOutput;
import com.bite.judge.service.JudgeRunService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class JudgeRunServiceImpl implements JudgeRunService {

    @Autowired
    private ConnectCodeAndOutput connectCodeAndOutput;

    @Autowired
    private RunAndOutput runAndOutput;

    @Autowired
    private JudgeAndOutput judgeAndOutput;

    @Override
    public Result<JudgeSingleCaseResponse> run(JudgeRunRequest request) {
        System.out.println("JudgeRunService 收到请求: " + request);

        if (request == null) {
            return Result.fail("请求体不能为空");
        }
        if (request.getLanguage() != 0) {
            return Result.fail("暂仅支持 Java（language=0）");
        }
        if (!StringUtils.hasText(request.getUserCode())) {
            return Result.fail("userCode 不能为空");
        }
        if (!StringUtils.hasText(request.getMainMethod())) {
            return Result.fail("mainMethod 不能为空");
        }

        long tl = request.getTimeLimitMs() <= 0 ? 5000L : request.getTimeLimitMs();
        long kb = request.getSpaceLimitKb() <= 0 ? 262144L : request.getSpaceLimitKb();

        Path workDir = null;
        try {
            Path projectTmp = Path.of(System.getProperty("user.dir"), "tmp-judge");
            Files.createDirectories(projectTmp);
            workDir = Files.createTempDirectory(projectTmp, "oj-judge-");
            System.out.println("工作目录: " + workDir);

            JudgeSingleCaseResponse resp = new JudgeSingleCaseResponse();

            // 1. 拼接 + 写入 Java 源码
            System.out.println("=== 步骤1: 拼接并写入 Java 源码 ===");
            String code = connectCodeAndOutput.connectCode(request.getUserCode(), request.getMainMethod());
            connectCodeAndOutput.outputJava(workDir, code);

            // 2. 写入测试用例
            System.out.println("=== 步骤2: 写入测试用例 ===");
            connectCodeAndOutput.outputTest(workDir, request.getTestInput());

            // 3. 写入预期结果
            System.out.println("=== 步骤3: 写入预期结果 ===");
            System.out.println("expectedOutput = " + request.getExpectedOutput());
            connectCodeAndOutput.outputExpectedResult(workDir, request.getExpectedOutput());

            // 4. 编译
            System.out.println("=== 步骤4: 编译 ===");
            boolean compileRes = runAndOutput.compile(workDir, workDir);
            if (!compileRes) {
                resp.setVerdict(JudgeVerdict.COMPILE_ERROR.getCode());
                resp.setMessage("编译失败");
                return Result.ok("编译失败", resp);
            }

            // 5. 运行
            System.out.println("=== 步骤5: 运行 ===");
            boolean runRes = runAndOutput.run(workDir, workDir, workDir);
            if (!runRes) {
                resp.setVerdict(JudgeVerdict.RUNTIME_ERROR.getCode());
                resp.setMessage("运行错误");
                return Result.ok("运行错误", resp);
            }

            // 6. 比较结果
            System.out.println("=== 步骤6: 比较结果 ===");
            boolean result = judgeAndOutput.judgeCode(workDir);

            if (result) {
                resp.setVerdict(JudgeVerdict.ACCEPTED.getCode());
                resp.setMessage("通过");
            } else {
                resp.setVerdict(JudgeVerdict.WRONG_ANSWER.getCode());
                resp.setMessage("答案错误");
            }
            resp.setActualOutput(String.valueOf(result));

            System.out.println("判题完成: verdict=" + resp.getVerdict() + ", message=" + resp.getMessage());
            return Result.ok("ok", resp);

        } catch (Exception e) {
            System.err.println("=== JudgeRunService 异常 ===");
            e.printStackTrace();

            JudgeSingleCaseResponse resp = new JudgeSingleCaseResponse();
            resp.setVerdict(JudgeVerdict.INTERNAL_ERROR.getCode());
            resp.setMessage("判题机异常: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return Result.ok("判题机异常", resp);
        }
    }
}