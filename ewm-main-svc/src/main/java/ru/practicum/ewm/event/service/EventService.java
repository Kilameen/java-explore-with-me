package ru.practicum.ewm.event.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.dto.*;

import java.util.Collection;
import java.util.List;

public interface EventService {
    EventFullDto create(Long userId, NewEventDto newEventDto);

    EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest adminRequest);

    EventFullDto updateEventByPrivate(Long userId, Long eventId, UpdateEventUserRequest eventUserRequest);

    Collection<EventShortDto> findAllByPublic(String text, List<Long> categories, Boolean paid, String rangeStart, String rangeEnd, Boolean onlyAvailable, String sort, Integer from, Integer size, HttpServletRequest request);

    Collection<EventShortDto> findAllByPrivate(Long userId, Integer from, Integer size);

    @Transactional(readOnly = true)
    Collection<EventFullDto> findAllByAdmin(List<Long> users, List<String> states, List<Long> categories, String rangeStart, String rangeEnd, Integer from, Integer size);

    EventFullDto findEventById(Long eventId, HttpServletRequest request);

    void updateEventConfirmedRequests(Long eventId, int confirmedRequests);
}
