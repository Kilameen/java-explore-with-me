package ru.practicum.ewm.request.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.service.EventRequestService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users/{userId}")
@RequiredArgsConstructor
public class EventRequestPrivateController {

    private final EventRequestService eventRequestService;

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto create(@PathVariable @Positive Long userId,
                                          @RequestParam @Positive Long eventId) {
        log.info("POST запрос /users/{}/requests?eventId={}", userId, eventId);
        ParticipationRequestDto createdRequest = eventRequestService.create(userId, eventId);
        log.info("Создан запрос на участие: {}", createdRequest);
        return createdRequest;
    }

    @PatchMapping("/requests/{requestId}/cancel")
    public ParticipationRequestDto cancel(@PathVariable @Positive Long userId,
                                          @PathVariable @Positive Long requestId) {
        log.info("PATCH запрос /users/{}/requests/{}/cancel", userId, requestId);
        ParticipationRequestDto cancelledRequest = eventRequestService.cancelRequest(userId, requestId);
        log.info("Запрос отменен: {}", cancelledRequest);
        return cancelledRequest;
    }

    @GetMapping("/requests")
    public List<ParticipationRequestDto> getParticipationRequests(@PathVariable @Positive Long userId) {
        log.info("GET запрос /users/{}/requests", userId);
        List<ParticipationRequestDto> requests = eventRequestService.getParticipationRequests(userId);
        log.info("Получены запросы на участие: {}", requests);
        return requests;
    }

    @GetMapping("/events/{eventId}/requests")
    public List<ParticipationRequestDto> getParticipationRequestsForUserEvent(@PathVariable @Positive Long userId,
                                                                              @PathVariable @Positive Long eventId) {
        log.info("GET запрос /users/{}/events/{}/requests", userId, eventId);
        List<ParticipationRequestDto> requests = eventRequestService.getParticipationRequestsForUserEvent(userId, eventId);
        log.info("Получены запросы на участие в событии: {}", requests);
        return requests;
    }

    @PatchMapping("/events/{eventId}/requests")
    public EventRequestStatusUpdateResult updateStatus(@PathVariable @Positive Long userId,
                                                       @PathVariable @Positive Long eventId,
                                                       @RequestBody EventRequestStatusUpdateRequest requestDto) {
        log.info("PATCH запрос /users/{}/events/{}/requests с телом: {}", userId, eventId, requestDto);
        EventRequestStatusUpdateResult updateResult = eventRequestService.updateStatus(userId, eventId, requestDto);
        log.info("Обновлен статус запросов: {}", updateResult);
        return updateResult;
    }
}