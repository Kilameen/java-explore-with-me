package ru.practicum.ewm.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventSearchParams;
import ru.practicum.ewm.event.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.event.service.EventService;

import java.util.Collection;

@Slf4j
@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
public class AdminEventController {
    private final EventService eventService;

    @GetMapping
    public Collection<EventFullDto> findAllByAdmin(@Valid EventSearchParams searchEventParams,
                                                   HttpServletRequest request) {
        log.info("GET запрос на получения событий с фильтром");
        Collection<EventFullDto> events = eventService.findAllByAdmin(searchEventParams, request);
        log.info("Отправлен ответ GET /admin/events с телом: {}", events);
        return events;
    }

    @PatchMapping("/{eventId}")
    public EventFullDto update(@PathVariable Long eventId, @RequestBody @Valid UpdateEventAdminRequest eventDto) {
        log.info("PATCH запрос /admin/events/{} с телом {}", eventId, eventDto);
        EventFullDto event = eventService.updateEventByAdmin(eventId, eventDto);
        log.info("Отправлен ответ PATCH /admin/events/{} с телом: {}", eventId, event);
        return event;
    }
}