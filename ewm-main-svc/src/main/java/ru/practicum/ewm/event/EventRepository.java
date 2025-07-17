package ru.practicum.ewm.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.enums.EventState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    @Query("""
                SELECT e
                FROM Event AS e
                WHERE (?1 IS NULL or e.initiator.id IN ?1)
                    AND (?2 IS NULL or e.state IN ?2)
                    AND (?3 IS NULL or e.category.id in ?3)
                    AND (CAST(?4 AS timestamp) IS NULL or e.eventDate >= ?4)
                    AND (CAST(?5 AS timestamp) IS NULL or e.eventDate < ?5)
            """)
    List<Event> findAllByAdmin(
            List<Long> users,
            List<EventState> states,
            List<Long> categories,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            Pageable pageable
    );

    @Query("""
            SELECT e
            FROM Event e
            WHERE e.state = 'PUBLISHED'
            AND (?1 IS NULL OR lower(e.annotation) LIKE lower(concat('%', ?1, '%')) OR lower(e.description) LIKE lower(concat('%', ?1, '%')))
            AND (?2 IS NULL OR e.category.id IN ?2)
            AND ((?3 IS NULL AND (e.paid = true OR e.paid = false)) OR e.paid = ?3)
            AND (?6 = FALSE OR e.participantLimit = 0 OR e.confirmedRequests < e.participantLimit)
            AND (
                (?4 IS NULL AND ?5 IS NULL) OR
                (e.eventDate BETWEEN ?4 AND ?5)
            )
            """)
    Page<Event> findAllByPublic(
            String text,
            List<Long> categories,
            Boolean paid,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            Boolean onlyAvailable,
            Pageable pageable
    );

    List<Event> findAllByInitiatorId(Long initiatorId, Pageable pageable);

    List<Event> findAllByIdIn(List<Long> eventIds);

    boolean existsByCategoryId(Long id);

    Optional<Event> findByIdAndInitiatorId(Long eventId, Long userId);

}