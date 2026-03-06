package org.nsky.api.repository;

import org.nsky.api.model.Chat;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface ChatRepository extends ReactiveCrudRepository<Chat, Long> {
}
