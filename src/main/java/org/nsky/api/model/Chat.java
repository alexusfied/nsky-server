package org.nsky.api.model;

import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class Chat {
    @Id
    private Long id;
    private String name;
}
