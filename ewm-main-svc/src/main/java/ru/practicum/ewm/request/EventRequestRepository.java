package ru.practicum.ewm.request;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.ewm.request.model.EventRequest;
import ru.practicum.ewm.utils.enums.RequestStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EventRequestRepository extends JpaRepository<EventRequest, Long> {

    int countByEventIdAndStatus(Long eventId, RequestStatus status);

    Boolean existsByEventIdAndRequesterId(Long eventId, Long userId);

    Optional<EventRequest> findByIdAndRequesterId(Long id, Long requesterId);

    List<EventRequest> findAllByRequesterId(Long userId);
    List<EventRequest> findAllByEventId(Long eventId);
    List<EventRequest> findAllByIdIn(Collection<Long> ids);
}
