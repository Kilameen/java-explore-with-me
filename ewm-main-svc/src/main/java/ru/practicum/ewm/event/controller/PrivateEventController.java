package ru.practicum.ewm.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.dto.NewEventDto;
import ru.practicum.ewm.event.dto.UpdateEventUserRequest;
import ru.practicum.ewm.event.service.EventService;

import java.util.Collection;

@Slf4j
@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
public class PrivateEventController {

    private final EventService eventService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public EventFullDto create(@PathVariable Long userId, @RequestBody @Valid NewEventDto eventDto) {
        log.info("POST запрос /users/{}/events с телом {}", userId, eventDto);
        EventFullDto event = eventService.create(userId, eventDto);
        log.info("Отправлен ответ POST /users/{}/events с телом: {}", userId, event);
        return event;
    }

    @PatchMapping("/{eventId}")
    public EventFullDto update(@PathVariable Long userId, @PathVariable Long eventId, @RequestBody @Valid UpdateEventUserRequest eventDto) {
        log.info("PATCH запрос /users/{}/events/{} с телом {}", userId, eventId, eventDto);
        EventFullDto event = eventService.updateEventByPrivate(userId, eventId, eventDto);
        log.info("Отправлен ответ PATCH /users/{}/events/{} с телом: {}", userId, eventId, event);
        return event;
    }

    @GetMapping("/{eventId}")
    public EventFullDto getEventOfUser(@PathVariable Long userId, @PathVariable Long eventId) {
        log.info("GET запрос на получение списка всех событий пользователя с id: {}", userId);
        return eventService.getEventOfUser(userId, eventId);
    }

    @GetMapping
    public Collection<EventShortDto> findAllByPrivate(
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(required = false, defaultValue = "10") @Positive Integer size, HttpServletRequest request
    ) {
        log.info("GET запрос /users/{}/events?from={}&size={}", userId, from, size);
        Collection<EventShortDto> events = eventService.findAllByPrivate(userId, from, size,request);
        log.info("Отправлен ответ GET /users/{}/events?from={}&size={} с телом: {}", userId, from, size, events);
        return events;
    }
}