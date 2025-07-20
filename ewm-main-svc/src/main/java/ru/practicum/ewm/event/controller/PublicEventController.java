package ru.practicum.ewm.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventSearchParams;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.service.EventService;

import java.util.Collection;

@Slf4j
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class PublicEventController {

    private final EventService eventService;

    @GetMapping
    public Collection<EventShortDto> findAllByPublic(@Valid EventSearchParams searchEventParams,
                                                     HttpServletRequest request) {
        log.info("GET запрос на получения событий с фильтром");
        Collection<EventShortDto> events = eventService.findAllByPublic(searchEventParams, request);
        log.info("Отправлен ответ GET /events с телом: {}", events);
        return events;
    }

    @GetMapping("/{eventId}")
    public EventFullDto findEventById(@PathVariable Long eventId, HttpServletRequest request) {
        log.info("GET запрос /events/{}", eventId);
        EventFullDto event = eventService.findEventById(eventId, request);
        log.info("Отправлен ответ GET /events/{} с телом: {}", eventId, event);
        return event;
    }
}