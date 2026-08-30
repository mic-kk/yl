package com.tcm.common.exception;

import com.tcm.common.base.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/business")
        public Result<String> business() {
            throw new BusinessException(ErrorCode.LOGIN_ERROR, "密码错误");
        }

        @GetMapping("/system")
        public Result<String> system() {
            throw new IllegalStateException("boom");
        }

        @PostMapping("/body")
        public Result<String> body(@RequestBody Map<String, Object> body) {
            return Result.ok(String.valueOf(body));
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void businessException_shouldReturnUnifiedResult() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2003))
                .andExpect(jsonPath("$.msg").value("密码错误"));
    }

    @Test
    void unknownException_shouldReturnSystemError() throws Exception {
        mockMvc.perform(get("/test/system"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.msg").value("系统繁忙，请稍后重试"));
    }

    @Test
    void badJsonBody_shouldReturnParamError() throws Exception {
        mockMvc.perform(post("/test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.msg").value("请求体格式错误"));
    }
}
