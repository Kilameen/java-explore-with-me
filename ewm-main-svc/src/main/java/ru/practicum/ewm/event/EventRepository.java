package ru.practicum.ewm.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.enums.EventState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("SELECT e FROM Event e WHERE " +
            "(:users is null or e.initiator.id in :users) AND " +
            "(:states is null or e.state in :states) AND " +
            "(:categories is null or e.category.id in :categories) AND " +
            "(e.eventDate BETWEEN :rangeStart AND :rangeEnd)")
    List<Event> findAllByAdmin(@Param("users") List<Long> users,
                               @Param("states") List<EventState> states,
                               @Param("categories") List<Long> categories,
                               @Param("rangeStart") LocalDateTime rangeStart,
                               @Param("rangeEnd") LocalDateTime rangeEnd,
                               Pageable pageable);

    List<Event> findAllByInitiatorId(Long initiatorId, Pageable pageable);

    List<Event> findAllByIdIn(List<Long> eventIds);

    boolean existsByCategoryId(Long id);

    Optional<Event> findByIdAndInitiatorId(Long eventId, Long userId);

    Page<Event> findAll(Specification<Event> spec, Pageable pageable);
}