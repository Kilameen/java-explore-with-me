package ru.practicum.ewm.event.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.CategoryRepository;
import ru.practicum.ewm.category.mapper.CategoryMapper;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.event.EventRepository;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.exception.*;
import ru.practicum.ewm.location.LocationRepository;
import ru.practicum.ewm.location.mapper.LocationMapper;
import ru.practicum.ewm.location.model.Location;
import ru.practicum.ewm.request.EventRequestRepository;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.mapper.EventRequestMapper;
import ru.practicum.ewm.request.model.EventRequest;
import ru.practicum.ewm.user.UserRepository;
import ru.practicum.ewm.user.mapper.UserMapper;
import ru.practicum.ewm.user.model.User;

import ru.practicum.ewm.utils.enums.EventState;
import ru.practicum.ewm.utils.enums.RequestStatus;
import ru.practicum.ewm.utils.enums.StateAction;
import ru.practicum.stat.StatisticsClient;
import ru.practicum.stat.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventServiceImpl implements EventService {

    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    static int MIN_HOURS_BEFORE_EVENT = 2;
    static int MIN_HOURS_BEFORE_PUBLISH = 1;
    static long DEFAULT_VIEWS = 0L;
    static int DEFAULT_CONFIRMED_REQUESTS = 0;

    EventRepository eventRepository;
    CategoryRepository categoryRepository;
    LocationRepository locationRepository;
    UserRepository userRepository;
    EventRequestRepository eventRequestRepository;

    StatisticsClient statClient;

    ObjectMapper mapper = new ObjectMapper();
    EventMapper eventMapper;
    CategoryMapper categoryMapper;
    LocationMapper locationMapper;
    UserMapper userMapper;
    EventRequestMapper eventRequestMapper;

    @Override
    public EventFullDto create(Long userId, NewEventDto newEventDto) {
        validateEventDate(newEventDto.getEventDate());
        LocalDateTime createdOn = LocalDateTime.now();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь c ID " + userId + " не найден"));
        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Категория с ID " + newEventDto.getCategory() + " не найдена"));

        Event event = eventMapper.toEvent(newEventDto);
        event.setCreatedOn(createdOn);
        event.setCategory(category);
        event.setInitiator(user);
        event.setState(EventState.PENDING);

        Location location = locationMapper.toLocationFromDto(newEventDto.getLocation());
        Location savedLocation = locationRepository.save(location);
        event.setLocation(savedLocation);

        Event eventSaved = eventRepository.save(event);

        EventFullDto eventFullDto = eventMapper.toEventFullDto(eventSaved);
        eventFullDto.setViews(DEFAULT_VIEWS);
        eventFullDto.setConfirmedRequests(DEFAULT_CONFIRMED_REQUESTS);
        return eventFullDto;
    }

    @Override
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest adminRequest) {
        // Находим событие по ID, если не найдено, выбрасываем исключение
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие c ID " + eventId + " не найдено"));

        // Получаем текущую дату события или новую из запроса
        LocalDateTime eventDate = adminRequest.getEventDate() == null ? event.getEventDate() : adminRequest.getEventDate();

        // Валидируем дату события и статус
        validateEventDateForAdmin(eventDate, adminRequest.getStateAction());
        validateStatusForAdmin(event.getState(), adminRequest.getStateAction());

        // Обновляем поля события, если они есть в запросе
        if (adminRequest.getAnnotation() != null) {
            event.setAnnotation(adminRequest.getAnnotation());
        }
        if (adminRequest.getDescription() != null) {
            event.setDescription(adminRequest.getDescription());
        }
        event.setEventDate(eventDate);
        if (adminRequest.getPaid() != null) {
            event.setPaid(adminRequest.getPaid());
        }
        if (adminRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(adminRequest.getParticipantLimit());
        }
        if (adminRequest.getRequestModeration() != null) {
            event.setRequestModeration(adminRequest.getRequestModeration());
        }
        if (adminRequest.getTitle() != null) {
            event.setTitle(adminRequest.getTitle());
        }

        // Обновляем категорию, если она есть в запросе
        if (adminRequest.getCategory() != null) {
            Category category = categoryRepository.findById(adminRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория с ID " + adminRequest.getCategory() + " не найдена"));
            event.setCategory(category);
        }

        // Обновляем location, если она есть в запросе.
        if (adminRequest.getLocation() != null) {
            event.setLocation(adminRequest.getLocation());
        }

        // Обновляем статус события в зависимости от действия
        if (adminRequest.getStateAction() != null) {
            switch (adminRequest.getStateAction()) {
                case PUBLISH_EVENT:
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;
                case REJECT_EVENT:
                    event.setState(EventState.CANCELED);
                    break;
            }
        }


        Event updatedEvent = eventRepository.save(event);

        updatedEvent.setViews(getViews(eventId, updatedEvent.getCreatedOn()));

        return eventMapper.toEventFullDto(updatedEvent);
    }


    @Override
    public EventFullDto updateEventByPrivate(Long userId, Long eventId, UpdateEventUserRequest eventUserRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь c ID " + userId + " не найден"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие c ID " + eventId + " не найдено"));

        validateUser(event.getInitiator(), user);
        // Проверяем, что текущий статус события не является опубликованным
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Нельзя изменить опубликованное событие");
        }

        LocalDateTime newEventDate = eventUserRequest.getEventDate() == null ? event.getEventDate() : eventUserRequest.getEventDate();

        validateEventDate(newEventDate);

        if (eventUserRequest.getAnnotation() != null) {
            event.setAnnotation(eventUserRequest.getAnnotation());
        }
        if (eventUserRequest.getDescription() != null) {
            event.setDescription(eventUserRequest.getDescription());
        }
        event.setEventDate(newEventDate);
        if (eventUserRequest.getPaid() != null) {
            event.setPaid(eventUserRequest.getPaid());
        }
        if (eventUserRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(eventUserRequest.getParticipantLimit());
        }
        if (eventUserRequest.getRequestModeration() != null) {
            event.setRequestModeration(eventUserRequest.getRequestModeration());
        }
        if (eventUserRequest.getTitle() != null) {
            event.setTitle(eventUserRequest.getTitle());
        }

        if (eventUserRequest.getStateAction() != null) {
            switch (eventUserRequest.getStateAction()) {
                case CANCEL_REVIEW:
                    event.setState(EventState.CANCELED);
                    break;
                case SEND_TO_REVIEW:
                    event.setState(EventState.PENDING);
                    break;
                default:
                    throw new IllegalArgumentException("Недопустимое действие над событием: " + eventUserRequest.getStateAction());
            }
        }

        if (eventUserRequest.getCategory() != null) {
            Category category = categoryRepository.findById(eventUserRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория с ID " + eventUserRequest.getCategory() + " не найдена"));
            event.setCategory(category);
        }

        if (eventUserRequest.getLocation() != null) {
            event.setLocation(eventUserRequest.getLocation());
        }

        Event updatedEvent = eventRepository.save(event);
        updatedEvent.setViews(getViews(eventId, updatedEvent.getCreatedOn())); // Предполагаю, что getViews принимает eventId
        return eventMapper.toEventFullDto(updatedEvent);
    }

    @Override
    public EventFullDto getEventOfUser(Long userId, Long eventId) {
        log.info("Getting event of user {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь c ID " + userId + " не найден"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие c ID " + eventId + " не найдено"));
        ;
        if (!event.getInitiator().getId().equals(userId)) {
            log.warn("User with id {} is not initiator of event with id {}", userId, eventId);
            throw new ValidationException("User is not initiator of event");
        }
        return eventMapper.toEventFullDto(event);
    }


    @Transactional(readOnly = true)
    @Override
    public Collection<EventShortDto> findAllByPublic(String text, List<Long> categories, Boolean paid, LocalDateTime rangeStart, LocalDateTime rangeEnd, Boolean onlyAvailable, String sort, Integer from, Integer size, HttpServletRequest request) {

        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new IllegalArgumentException("rangeStart must be before rangeEnd");
        }

        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findAllByPublic(text, categories, paid, rangeStart, rangeEnd, onlyAvailable, pageable);
        Map<Long, Long> views = getViewsAllEvents(events);

        List<EventShortDto> eventShortDtos = events.stream()
                .map(event -> {
                    EventShortDto eventShortDto = eventMapper.toEventShortDto(event);
                    eventShortDto.setViews(views.getOrDefault(event.getId(), 0L).intValue()); // Устанавливаем количество просмотров
                    eventShortDto.setConfirmedRequests(eventRequestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED)); // Устанавливаем количество подтвержденных заявок
                    return eventShortDto;
                })
                .collect(Collectors.toList());

        if ("VIEWS".equalsIgnoreCase(sort)) {
            eventShortDtos.sort(Comparator.comparing(EventShortDto::getViews)); // Сортируем по количеству просмотров
        } else {
            eventShortDtos.sort(Comparator.comparing(EventShortDto::getEventDate)); // Сортируем по дате события
        }
        sendStats(request);
        return eventShortDtos;
    }

    @Transactional(readOnly = true)
    @Override
    public Collection<EventShortDto> findAllByPrivate(Long userId, Integer from, Integer size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь c ID " + userId + " не найден"));

        Pageable pageable = PageRequest.of(from, size);
        List<Event> events = eventRepository.findAllByInitiatorId(user.getId(), pageable);

        return events.stream()
                .map(event -> {
                    EventShortDto eventShortDto = eventMapper.toEventShortDto(event);
                    eventShortDto.setCategory(categoryMapper.toCategoryDto(event.getCategory()));
                    eventShortDto.setInitiator(userMapper.toUserShortDto(event.getInitiator()));
                    eventShortDto.setViews(getViews(event.getId(), event.getCreatedOn()));//Получаем просмотры из статистики
                    eventShortDto.setConfirmedRequests(eventRequestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED));//Получаем подтвержденные запросы
                    return eventShortDto;
                })
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Transactional(readOnly = true)
    @Override
    public Collection<EventFullDto> findAllByAdmin(List<Long> users, List<EventState> states, List<Long> categories, LocalDateTime rangeStart, LocalDateTime rangeEnd, Integer from, Integer size) {

        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findAllByAdmin(users, states, categories, rangeStart, rangeEnd, pageable);
        Map<Long, Long> viewsMap = getViewsAllEvents(events);

        return events.stream()
                .map(event -> {
                    EventFullDto eventFullDto = eventMapper.toEventFullDto(event);
                    eventFullDto.setViews(viewsMap.getOrDefault(event.getId(), 0L).intValue());
                    eventFullDto.setConfirmedRequests(eventRequestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED));
                    return eventFullDto;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public EventFullDto findEventById(Long eventId, HttpServletRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие c ID " + eventId + " не найдено"));

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new NotFoundException("Событие с ID = " + eventId + " не опубликовано");
        }
        statClient.create(request);
        EventFullDto eventFullDto = eventMapper.toEventFullDto(event);
        Map<Long, Long> viewStatsMap = getViewsAllEvents(List.of(event));
        Long views = viewStatsMap.getOrDefault(event.getId(), 0L);
        eventFullDto.setViews(Math.toIntExact(views));
        return eventFullDto;
    }

    @Override
    public List<ParticipationRequestDto> getByEventId(Long eventInitiatorId, Long eventId) {

        userRepository.findById(eventInitiatorId)
                .orElseThrow(() -> new NotFoundException("Пользователь c ID " + eventInitiatorId + " не найден"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id= " + eventId + " не найдено"));

        if (!event.getInitiator().getId().equals(eventInitiatorId)) {
            throw new IllegalArgumentException("Пользователь не является инициатором события.");
        }

        List<EventRequest> requests = eventRequestRepository.findAllByEventId(eventId);

        return requests.stream()
                .map(eventRequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }


    @Override
    public EventRequestStatusUpdateResult updateStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest requestsToUpdate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь c ID " + userId + " не найден"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id= " + eventId + " не найдено"));

        if (!event.getInitiator().getId().equals(user.getId())) {
            throw new ConflictException("Только инициатор события может менять статус запросов");
        }

        validateEventForRequest(event);

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        List<EventRequest> requests = eventRequestRepository.findAllByIdIn(requestsToUpdate.getRequestIds());
        validateRequests(requests, event);

        switch (requestsToUpdate.getStatus()) {
            case REJECTED:
                rejectRequests(result, requests);
                break;
            case CONFIRMED:
                confirmRequests(result, event, requests);
                break;
            default:
                throw new IllegalArgumentException("Неизвестный статус для обновления");
        }

        return result;
    }


    @Override
    public void updateEventConfirmedRequests(Long eventId, int confirmedRequests) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие c ID " + eventId + " не найдено"));

        event.setConfirmedRequests(confirmedRequests);
        eventRepository.save(event);
    }

    private void rejectRequests(EventRequestStatusUpdateResult result, Collection<EventRequest> requests) {
        for (EventRequest request : requests) {
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Можно отклонить только заявки в статусе ожидания");
            }
            request.setStatus(RequestStatus.REJECTED);
            EventRequest updatedRequest = eventRequestRepository.save(request);
            result.getRejectedRequests().add(eventRequestMapper.toParticipationRequestDto(updatedRequest));
        }
    }

    private void confirmRequests(EventRequestStatusUpdateResult result, Event event, Collection<EventRequest> requests) {
        int limit = event.getParticipantLimit();
        int currentConfirmed = event.getConfirmedRequests();

        for (EventRequest request : requests) {
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Можно подтвердить только заявки в статусе ожидания");
            }
            if (limit != 0 && currentConfirmed >= limit) {
                request.setStatus(RequestStatus.REJECTED);
                EventRequest updatedRequest = eventRequestRepository.save(request);
                result.getRejectedRequests().add(eventRequestMapper.toParticipationRequestDto(updatedRequest));

            } else {
                request.setStatus(RequestStatus.CONFIRMED);
                EventRequest updatedRequest = eventRequestRepository.save(request);
                result.getConfirmedRequests().add(eventRequestMapper.toParticipationRequestDto(updatedRequest));
                currentConfirmed++;
            }
        }
        updateEventConfirmedRequests(event.getId(), currentConfirmed);
    }


    private void validateEventForRequest(Event event) {
        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Can't send request to unpublished event");
        }
        if (event.getParticipantLimit().equals(0)) {
            return;
        }
        if (event.getConfirmedRequests() >= event.getParticipantLimit()) {
            throw new ConflictException("Limit of event can't be full");
        }
    }

    private void validateRequests(List<EventRequest> requests, Event event) {
        for (EventRequest request : requests) {
            if (!request.getEventId().equals(event.getId())) {
                throw new ConflictException("Запрос относится к другому событию");
            }
        }
    }

    private void validateEventDate(LocalDateTime eventDate) {
        LocalDateTime nowPlusMinHours = LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_EVENT);
        if (eventDate.isBefore(nowPlusMinHours)) {
            String formattedEventDate = eventDate.format(formatter);
            String formattedMinHours = nowPlusMinHours.format(formatter);

            throw new ValidationException("Дата мероприятия должна быть не ранее, чем через " + MIN_HOURS_BEFORE_EVENT + " часа(ов) от текущего момента. " +
                    "Указанная дата: " + formattedEventDate + ", Минимальная допустимая дата: " + formattedMinHours);
        }
    }

    private void validateEventDateForAdmin(LocalDateTime eventDate, StateAction stateAction) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_EVENT))) {
            throw new ValidationException("Дата мероприятия должна быть на " + MIN_HOURS_BEFORE_EVENT + "часа раньше текущего момента");
        }
        if (stateAction != null && stateAction.equals(StateAction.PUBLISH_EVENT) && eventDate.isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_PUBLISH))) {
            throw new ValidationException("Дата события должна быть на " + MIN_HOURS_BEFORE_PUBLISH + " час раньше момента публикации");
        }
    }

    private void validateStatusForAdmin(EventState state, StateAction stateAction) {
        if (stateAction != null && !stateAction.equals(StateAction.REJECT_EVENT) && !stateAction.equals(StateAction.PUBLISH_EVENT)) {
            throw new ForbiddenException("Неизвестный state action");
        }
        if (!state.equals(EventState.PENDING) && stateAction.equals(StateAction.PUBLISH_EVENT)) {
            throw new ConflictException("\n" +
                    "Не удается опубликовать незавершенное событие");
        }
        if (state.equals(EventState.PUBLISHED) && stateAction.equals(StateAction.REJECT_EVENT)) {
            throw new ConflictException("Невозможно отклонить уже опубликованное событие");
        }
    }

    private void validateUser(User user, User initiator) {
        if (!initiator.getId().equals(user.getId())) {
            throw new NotFoundException("Попытка изменить информацию не от инициатора события");
        }
    }

    private void sendStats(HttpServletRequest request) {
        statClient.create(request);
    }

    private Long getViews(Long eventId, LocalDateTime createdOn) {
        LocalDateTime end = LocalDateTime.now();
        String uri = "/events/" + eventId;
        Boolean unique = true;

        try {
            ResponseEntity<Object> statsResponse = statClient.getStats(createdOn, end, List.of(uri), unique);

            if (statsResponse.getStatusCode().is2xxSuccessful() && statsResponse.hasBody()) {
                List<ViewStatsDto> stats = Arrays.asList(mapper.convertValue(statsResponse.getBody(), ViewStatsDto[].class));

                if (!stats.isEmpty()) {
                    return stats.getFirst().getHits();
                } else {
                    log.info("Нет данных статистики для события {}", eventId);
                }
            } else {
                log.warn("Неуспешный ответ от statClient для события {}: {}", eventId, statsResponse.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Ошибка при получении статистики для события {}: {}", eventId, e.getMessage());
        }
        return DEFAULT_VIEWS;
    }

// Основная проблема: статистика не передается в getViewsAllEvents, но работает в getViews
// Решение: проверить формат дат и обработку URI в запросе

    private Map<Long, Long> getViewsAllEvents(List<Event> events) {
        if (events.isEmpty()) return Collections.emptyMap();

        // 1. Исправляем диапазон дат (убираем минус 1 год как fallback)
        LocalDateTime start = events.stream()
                .map(Event::getCreatedOn)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now()); // Берем текущее время если событий нет

        LocalDateTime end = LocalDateTime.now();

        // 2. Форматируем URI с проверкой
        List<String> uris = events.stream()
                .map(event -> {
                    String uri = "/events/" + event.getId();
                    log.debug("Формируем URI для статистики: {}", uri);
                    return uri;
                })
                .collect(Collectors.toList());

        try {
            // 3. Добавляем логирование параметров запроса
            log.debug("Запрос статистики: start={}, end={}, uris={}", start, end, uris);

            ResponseEntity<Object> response = statClient.getStats(start, end, uris, true);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // 4. Добавляем проверку преобразования тела ответа
                ViewStatsDto[] statsArray = mapper.convertValue(response.getBody(), ViewStatsDto[].class);
                if (statsArray == null) {
                    log.warn("Не удалось преобразовать тело ответа в ViewStatsDto[]");
                    return Collections.emptyMap();
                }

                List<ViewStatsDto> viewStatsList = Arrays.asList(statsArray);
                log.debug("Получено {} записей статистики", viewStatsList.size());

                return viewStatsList.stream()
                        .collect(Collectors.toMap(
                                viewStats -> Long.parseLong(viewStats.getUri().replace("/events/", "")),
                                ViewStatsDto::getHits,
                                (existing, replacement) -> existing // обработка дубликатов
                        ));
            } else {
                log.warn("Ошибка запроса статистики. Код: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Ошибка при запросе статистики", e);
        }
        return Collections.emptyMap();
    }

}


