package com.example.quizcard;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 题目抽卡项目启动类
 */
@SpringBootApplication
@MapperScan("com.example.quizcard.mapper")
public class QuizCardApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuizCardApplication.class, args);
    }
}
