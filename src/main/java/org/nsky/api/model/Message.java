package org.nsky.api.model;

import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class Message {
    @Id
    private Long id;
    private Long chatId;
    private String author;
    private String content;
}
