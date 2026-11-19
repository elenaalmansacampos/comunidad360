package com.comunidad360.incidents;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IncidentStatusHistoryRepository extends JpaRepository<IncidentStatusHistory, UUID> {

    List<IncidentStatusHistory> findByCommunityIdAndIncidentIdOrderByChangedAtAsc(UUID communityId,
                                                                                  UUID incidentId);
}
