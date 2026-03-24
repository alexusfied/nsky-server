package org.nsky.api.repository;

import org.nsky.api.model.Message;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface MessageRepository extends ReactiveCrudRepository<Message, Long> {
    Flux<Message> findAllByChatId(Long chatId);
}
