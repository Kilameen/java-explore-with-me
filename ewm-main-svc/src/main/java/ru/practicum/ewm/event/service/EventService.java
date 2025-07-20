package ru.practicum.ewm.event.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.ewm.event.dto.*;
import java.util.Collection;

public interface EventService {
    EventFullDto create(Long userId, NewEventDto newEventDto);

    EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest adminRequest);

    EventFullDto updateEventByPrivate(Long userId, Long eventId, UpdateEventUserRequest eventUserRequest);

    EventFullDto getEventOfUser(Long userId, Long eventId);

    Collection<EventShortDto> findAllByPublic(EventSearchParams params, HttpServletRequest request);

    Collection<EventShortDto> findAllByPrivate(Long userId, Integer from, Integer size, HttpServletRequest request);

    Collection<EventFullDto> findAllByAdmin(EventSearchParams params, HttpServletRequest request);

    EventFullDto findEventById(Long eventId, HttpServletRequest request);

}
