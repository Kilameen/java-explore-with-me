package ru.practicum.ewm.request;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.request.model.EventRequest;
import ru.practicum.ewm.enums.RequestStatus;

import java.util.List;

public interface EventRequestRepository extends JpaRepository<EventRequest, Long> {

    boolean existsByRequesterIdAndEventId(Long userId, Long eventId);

    List<EventRequest> findByRequesterId(Long userId);

    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    @Query("SELECT r.event.id, COUNT(r) FROM EventRequest r " +
            "WHERE r.event.id IN :eventIds AND r.status = :status " +
            "GROUP BY r.event.id")
    List<Object[]> countByEventIdInAndStatus(@Param("eventIds") List<Long> eventIds, @Param("status") RequestStatus status);

    List<EventRequest> findByEvent(Event event);

}
