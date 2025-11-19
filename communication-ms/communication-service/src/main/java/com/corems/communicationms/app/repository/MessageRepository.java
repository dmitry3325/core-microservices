package com.corems.communicationms.app.repository;

import com.corems.communicationms.app.entity.MessageEntity;
import org.springframework.stereotype.Repository;
import com.corems.common.utils.db.repo.SearchableRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends SearchableRepository<MessageEntity, Long> {

    Optional<MessageEntity> findByUuid(UUID uuid);

    @Override
    default List<String> getSearchFields() {
        return List.of("recipient", "sender", "subject", "phoneNumber");
    }

    @Override
    default List<String> getAllowedFilterFields() {
        return List.of("userId", "type", "recipient", "sender");
    }

    @Override
    default List<String> getAllowedSortFields() {
        return List.of("createdAt", "type");
    }
}
