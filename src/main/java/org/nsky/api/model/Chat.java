package org.nsky.api.model;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.List;

@Data
public class Chat {
    @Id
    private Long id;

    private String name;
    private List<Message> messages;
}
