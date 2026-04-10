package org.nsky.api.repository;

import org.nsky.api.model.Chat;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface ChatRepository extends ReactiveCrudRepository<Chat, Long> {
    @Modifying
    @Query("UPDATE chat SET name = :name WHERE id = :id")
    Mono<Void> updateNameById(Long id, String name);
}
