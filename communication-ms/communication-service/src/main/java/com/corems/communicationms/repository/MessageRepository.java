package com.corems.communicationms.repository;

import com.corems.communicationms.entity.MessageEntity;
import org.springframework.stereotype.Repository;
import com.corems.common.utils.db.repo.SearchableRepository;
import java.util.List;
import java.util.Map;

@Repository
public interface MessageRepository extends SearchableRepository<MessageEntity, Long> {

    @Override
    default List<String> getSearchFields() {
        return List.of("subject", "body", "recipient", "sender");
    }

    @Override
    default List<String> getAllowedFilterFields() {
        return List.of("userId", "type", "createdAt", "subject", "recipient", "sender");
    }

    @Override
    default List<String> getAllowedSortFields() {
        return List.of("createdAt", "type");
    }

    @Override
    default Map<String, String> getFieldAliases() {
        // API field -> JPA attribute path (dot-separated for nested)
        return Map.of(
                "userId", "userId",
                "createdAt", "createdAt",
                "subject", "subject",
                "recipient", "recipient",
                "sender", "sender",
                "type", "type"
        );
    }
}
