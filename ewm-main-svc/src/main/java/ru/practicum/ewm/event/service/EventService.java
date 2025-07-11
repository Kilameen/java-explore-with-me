package ru.practicum.ewm.event.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.utils.enums.EventState;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface EventService {
    EventFullDto create(Long userId, NewEventDto newEventDto);

    EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest adminRequest);

    EventFullDto updateEventByPrivate(Long userId, Long eventId, UpdateEventUserRequest eventUserRequest);

    EventFullDto getEventOfUser(Long userId, Long eventId);

    Collection<EventShortDto> findAllByPublic(String text, List<Long> categories, Boolean paid, LocalDateTime rangeStart, LocalDateTime rangeEnd, Boolean onlyAvailable, String sort, Integer from, Integer size, HttpServletRequest request);

    Collection<EventShortDto> findAllByPrivate(Long userId, Integer from, Integer size);

    Collection<EventFullDto> findAllByAdmin(List<Long> users, List<EventState> states, List<Long> categories, LocalDateTime rangeStart, LocalDateTime rangeEnd, Integer from, Integer size);

EventFullDto findEventById(Long eventId, HttpServletRequest request);

    List<ParticipationRequestDto> getByEventId(Long eventInitiatorId, Long eventId);

    EventRequestStatusUpdateResult updateStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest requestsToUpdate);

    void updateEventConfirmedRequests(Long eventId, int confirmedRequests);
}
