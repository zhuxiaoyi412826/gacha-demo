package com.example.quizcard.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 聊天入参
 */
@Data
public class ChatRequestDTO {

    /** 本次用户输入 */
    private String message;

    /** 历史上下文（按设置里的「上下文轮数」裁剪后传入，可为空） */
    private List<ChatMessageDTO> history;
}
