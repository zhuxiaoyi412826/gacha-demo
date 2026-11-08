package com.example.quizcard.dto;

import lombok.Data;

/**
 * 聊天历史消息（前端上下文轮数裁剪后回传）
 */
@Data
public class ChatMessageDTO {

    /** user / assistant */
    private String role;

    private String content;
}
