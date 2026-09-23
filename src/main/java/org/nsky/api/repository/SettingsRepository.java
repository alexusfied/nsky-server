package org.nsky.api.repository;

import org.nsky.api.model.Setting;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface SettingsRepository extends ReactiveCrudRepository<Setting, Long> {}
