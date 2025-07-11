package ru.practicum.ewm.request.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.EventRepository;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.DuplicatedDataException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.request.EventRequestRepository;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.mapper.EventRequestMapper;
import ru.practicum.ewm.request.model.EventRequest;
import ru.practicum.ewm.user.UserRepository;
import ru.practicum.ewm.user.dto.UserDto;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.utils.enums.EventState;
import ru.practicum.ewm.utils.enums.RequestStatus;
import ru.practicum.ewm.utils.enums.StateAction;

import java.time.LocalDateTime;
import java.util.Collection;
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
    EventRequestMapper eventRequestMapper;

        @Override
        public ParticipationRequestDto create(Long userId, Long eventId) {
            userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("Пользователь c ID " + userId + " не найден"));
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new NotFoundException("Событие с id= " + eventId + " не найдено"));

            validateNewRequest(event,userId,eventId);
            EventRequest request = new EventRequest();
            request.setCreated(LocalDateTime.now());
            request.setRequesterId(userId);
            request.setEventId(eventId);

            if (event.getParticipantLimit() == 0) {
                request.setStatus(RequestStatus.CONFIRMED);
                event.setConfirmedRequests(event.getConfirmedRequests() + 1);
                eventRepository.save(event);
            } else if (event.getRequestModeration()) {
                request.setStatus(RequestStatus.PENDING);
            } else {
                request.setStatus(RequestStatus.CONFIRMED);
                event.setConfirmedRequests(event.getConfirmedRequests() + 1);
                eventRepository.save(event);
            }

            EventRequest savedRequest = eventRequestRepository.save(request);
            return eventRequestMapper.toEventRequest(savedRequest);
        }

        @Transactional(readOnly = true)
        @Override
        public List<ParticipationRequestDto> getRequestsByUserId(Long userId) {
            userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("Пользователь c ID " + userId + " не найден"));

            List<EventRequest> result = eventRequestRepository.findAllByRequesterId(userId);
            return result.stream().map(eventRequestMapper::toEventRequest).collect(Collectors.toList());
        }

        @Override
        public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
            EventRequest request = eventRequestRepository.findById(requestId)
                    .orElseThrow(() -> new NotFoundException("Запрос с id= " + requestId + " не найден"));

            if (!request.getRequesterId().equals(userId)) {
                throw new ConflictException("Запрос не принадлежит пользователю");
            }

            if (request.getStatus() == RequestStatus.CANCELED || request.getStatus() == RequestStatus.REJECTED) {
                throw new ConflictException("Запрос уже отменен или отклонен");
            }

            request.setStatus(RequestStatus.CANCELED);
            EventRequest savedRequest = eventRequestRepository.save(request);
            return eventRequestMapper.toEventRequest(savedRequest);
        }

        private void validateNewRequest(Event event, Long userId, Long eventId) {
            if (event.getInitiator().getId().equals(userId)) {
                throw new ConflictException("Инициатор события не может добавить запрос на участие в своём событии");
            }

            if (!event.getState().equals(EventState.PUBLISHED)) { // Предполагается, что EventState - это enum
                throw new ConflictException("Нельзя участвовать в неопубликованном событии");
            }

            if (eventRequestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
                throw new ConflictException("Нельзя добавить повторный запрос");
            }

            if (event.getParticipantLimit() != 0 && event.getConfirmedRequests() >= event.getParticipantLimit()) {
                throw new ConflictException("Превышен лимит участников события");
            }
        }
    }
