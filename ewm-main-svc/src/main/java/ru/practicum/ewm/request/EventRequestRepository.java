package ru.practicum.ewm.request;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.request.model.EventRequest;
import ru.practicum.ewm.enums.RequestStatus;

import java.util.List;

public interface EventRequestRepository extends JpaRepository<EventRequest, Long> {

    boolean existsByRequesterIdAndEventId(Long userId, Long eventId);

    List<EventRequest> findByRequesterId(Long userId);

    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    List<EventRequest> findByEvent(Event event);

}
