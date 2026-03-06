package org.nsky.api.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class Chat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(
        mappedBy = "message",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<Message> messages;
}
