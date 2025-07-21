package ru.practicum.ewm.request.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.EventRepository;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.DuplicatedDataException;
import ru.practicum.ewm.exception.ForbiddenException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.request.EventRequestRepository;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.mapper.EventRequestMapper;
import ru.practicum.ewm.request.model.EventRequest;
import ru.practicum.ewm.user.UserRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.enums.EventState;
import ru.practicum.ewm.enums.RequestStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventRequestServiceImpl implements EventRequestService {

    UserRepository userRepository;
    EventRepository eventRepository;
    EventRequestRepository eventRequestRepository;

    @Override
    public ParticipationRequestDto create(Long userId, Long eventId) {
        log.info("Создание запроса на участие: userId = {}, eventId = {}", userId, eventId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь c ID " + userId + " не найден"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие c ID " + eventId + " не найдено"));

        if (eventRequestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new DuplicatedDataException("Пользователь уже подал заявку на это событие.");
        }

        if (event.getInitiator().getId().equals(userId)) {
            throw new DuplicatedDataException("Инициатор не может подавать заявку на своё событие.");
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new DuplicatedDataException("Нельзя участвовать в неопубликованном событии.");
        }

        if (event.getParticipantLimit() != 0 &&
                eventRequestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED) >= event.getParticipantLimit()) {
            throw new DuplicatedDataException("Достигнут лимит участников.");
        }

        RequestStatus status = (!event.getRequestModeration() || event.getParticipantLimit() == 0)
                ? RequestStatus.CONFIRMED : RequestStatus.PENDING;

        EventRequest request = EventRequest.builder()
                .event(event)
                .requester(user)
                .created(LocalDateTime.now())
                .status(status)
                .build();

        EventRequest savedRequest = eventRequestRepository.save(request);
        log.info("Создан запрос на участие с ID: {}", savedRequest.getId());
        return EventRequestMapper.toRequestDto(savedRequest);
    }

    @Override
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.info("Отмена запроса на участие: userId = {}, requestId = {}", userId, requestId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь c ID " + userId + " не найден"));

        EventRequest request = eventRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос не найден."));

        if (!request.getRequester().getId().equals(userId)) {
            throw new ForbiddenException("Можно отменить только собственный запрос.");
        }

        request.setStatus(RequestStatus.CANCELED);
        EventRequest savedRequest = eventRequestRepository.save(request);
        return EventRequestMapper.toRequestDto(savedRequest);
    }

    @Override
    public List<ParticipationRequestDto> getParticipationRequests(Long userId) {
        log.info("Получение запросов пользователя: userId = {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь c ID " + userId + " не найден"));

        return eventRequestRepository.findByRequesterId(userId).stream()
                .map(EventRequestMapper::toRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ParticipationRequestDto> getParticipationRequestsForUserEvent(Long userId, Long eventId) {
        log.info("Получение заявок на своё событие: userId = {}, eventId = {}", userId, eventId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь c ID " + userId + " не найден"));

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new ForbiddenException("Пользователь не инициатор события."));

        return eventRequestRepository.findByEvent(event).stream()
                .map(EventRequestMapper::toRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventRequestStatusUpdateResult updateStatus(Long userId, Long eventId,
                                                       EventRequestStatusUpdateRequest dto) {
        log.info("Изменение статуса заявок: userId = {}, eventId = {}, status = {}", userId, eventId, dto.getStatus());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь c ID " + userId + " не найден"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие c ID " + eventId + " не найдено"));

        List<EventRequest> requests = eventRequestRepository.findAllById(dto.getRequestIds());
        EventRequestStatusUpdateResult result = EventRequestStatusUpdateResult.builder()
                .confirmedRequests(new ArrayList<>())
                .rejectedRequests(new ArrayList<>())
                .build();

        if (requests.isEmpty()) {
            return result;
        }

        RequestStatus targetStatus = RequestStatus.valueOf(String.valueOf(dto.getStatus()));

        int limit = event.getParticipantLimit();
        long confirmed = eventRequestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);

        if (targetStatus == RequestStatus.CONFIRMED) {
            if (limit != 0 && confirmed >= limit) {
                throw new ConflictException("Достигнут лимит участников.");
            }

            for (EventRequest request : requests) {
                if (!request.getEvent().getId().equals(eventId)) {
                    throw new NotFoundException("Запрос не относится к данному событию.");
                }
                if (request.getStatus() != RequestStatus.PENDING) {
                    throw new ConflictException("Изменять можно только заявки в статусе PENDING.");
                }
                request.setStatus(RequestStatus.CONFIRMED);
                result.getConfirmedRequests().add(EventRequestMapper.toRequestDto(request));
                confirmed++;
                if (confirmed == limit) {
                    break;
                }
            }

        } else if (targetStatus == RequestStatus.REJECTED) {
            for (EventRequest request : requests) {
                if (!request.getEvent().getId().equals(eventId)) {
                    throw new NotFoundException("Запрос не относится к данному событию.");
                }
                if (request.getStatus() != RequestStatus.PENDING) {
                    throw new ConflictException("Изменять можно только заявки в статусе PENDING.");
                }
                request.setStatus(RequestStatus.REJECTED);
                result.getRejectedRequests().add(EventRequestMapper.toRequestDto(request));
            }
        }

        eventRequestRepository.saveAll(requests);
        return result;
    }
}