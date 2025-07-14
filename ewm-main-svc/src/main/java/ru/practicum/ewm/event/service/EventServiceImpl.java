package ru.practicum.ewm.event.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
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
import ru.practicum.ewm.user.UserRepository;
import ru.practicum.ewm.user.mapper.UserMapper;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.enums.EventState;
import ru.practicum.ewm.enums.RequestStatus;
import ru.practicum.ewm.enums.StateAction;
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

    private final String appName = "ewm-main-service";
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

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие c ID " + eventId + " не найдено"));

        LocalDateTime eventDate = adminRequest.getEventDate() == null ? event.getEventDate() : adminRequest.getEventDate();

        validateEventDateForAdmin(eventDate, adminRequest.getStateAction());
        validateStatusForAdmin(event.getState(), adminRequest.getStateAction());

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

        if (adminRequest.getCategory() != null) {
            Category category = categoryRepository.findById(adminRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория с ID " + adminRequest.getCategory() + " не найдена"));
            event.setCategory(category);
        }

        if (adminRequest.getLocation() != null) {
            event.setLocation(adminRequest.getLocation());
        }

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
        return eventMapper.toEventFullDto(updatedEvent);
    }

    @Override
    public EventFullDto updateEventByPrivate(Long userId, Long eventId, UpdateEventUserRequest eventUserRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь c ID " + userId + " не найден"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие c ID " + eventId + " не найдено"));

        validateUser(event.getInitiator(), user);

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
        return eventMapper.toEventFullDto(updatedEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getEventOfUser(Long userId, Long eventId) {
        log.info("Получение события от пользователя {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь c ID " + userId + " не найден"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие c ID " + eventId + " не найдено"));

        if (!event.getInitiator().getId().equals(userId)) {
            log.warn("Пользователь с идентификатором {} не является инициатором события с идентификатором {}", userId, eventId);
            throw new ValidationException("Пользователь не является инициатором события");
        }
        return eventMapper.toEventFullDto(event);
    }


    @Transactional(readOnly = true)
    @Override
    public Collection<EventShortDto> findAllByPublic(String text, List<Long> categories, Boolean paid, LocalDateTime rangeStart, LocalDateTime rangeEnd, Boolean onlyAvailable, String sort, Integer from, Integer size, HttpServletRequest request) {

        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new IllegalArgumentException("rangeStart должен быть указан раньше rangeEnd");
        }
        sendStats(request);

        int page = (from == null || size == null || size <= 0) ? 0 : from / size;
        int pageSize = (size == null || size <= 0) ? 10 : size;

        Pageable pageable = PageRequest.of(page, pageSize);

        List<Event> events;
        try {
            events = eventRepository.findAllByPublic(text, categories, paid, rangeStart, rangeEnd, onlyAvailable, pageable);
        } catch (Exception e) {
            log.error("Ошибка при выполнении запроса к БД: ", e);
            throw new RuntimeException("Ошибка при получении данных из базы данных", e);
        }

        Map<Long, Long> views = getViewsAllEvents(events);
        List<EventShortDto> eventShortDtos = events.stream()
                .map(event -> {
                    EventShortDto eventShortDto = eventMapper.toEventShortDto(event);
                    eventShortDto.setViews(views.getOrDefault(event.getId(), DEFAULT_VIEWS));
                    eventShortDto.setConfirmedRequests(eventRequestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED));
                    return eventShortDto;
                })
                .collect(Collectors.toList());

        if ("VIEWS".equalsIgnoreCase(sort)) {
            eventShortDtos.sort(Comparator.comparing(EventShortDto::getViews));
        } else {
            eventShortDtos.sort(Comparator.comparing(EventShortDto::getEventDate));
        }

        return eventShortDtos;
    }

    @Transactional(readOnly = true)
    @Override
    public Collection<EventShortDto> findAllByPrivate(Long userId, Integer from, Integer size,HttpServletRequest request) {
        // Получаем пользователя по ID. Если не найден, выбрасываем исключение.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь c ID " + userId + " не найден"));

        // Создаем объект Pageable для пагинации результатов.
        Pageable pageable = PageRequest.of(from, size);

        // Получаем список событий, инициированных пользователем, с учетом пагинации.
        List<Event> events = eventRepository.findAllByInitiatorId(user.getId(), pageable);

        // Преобразуем список Event в список EventShortDto.
        return events.stream()
                .map(event -> {
                    // Преобразуем Event в EventShortDto с помощью mapper.
                    EventShortDto eventShortDto = eventMapper.toEventShortDto(event);

                    // Заполняем поля EventShortDto данными из связанных сущностей.
                    eventShortDto.setCategory(categoryMapper.toCategoryDto(event.getCategory()));
                    eventShortDto.setInitiator(userMapper.toUserShortDto(event.getInitiator()));

                    // Получаем количество просмотров события.
                    eventShortDto.setViews(getViews(event.getId(), event.getCreatedOn(), request));

                    // Получаем количество подтвержденных заявок на участие в событии.
                    eventShortDto.setConfirmedRequests(eventRequestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED));

                    return eventShortDto;
                })
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Transactional(readOnly = true)
    @Override
    public Collection<EventFullDto> findAllByAdmin(List<Long> users, List<EventState> states, List<Long> categories, LocalDateTime rangeStart, LocalDateTime rangeEnd, Integer from, Integer size) {

        int page = (from == null || size == null || size <= 0) ? 0 : from / size;
        int pageSize = (size == null || size <= 0) ? 10 : size;

        Pageable pageable = PageRequest.of(page, pageSize);

        List<Event> eventList;
        try {
            eventList = eventRepository.findAllByAdmin(users, states, categories, rangeStart, rangeEnd, pageable);
        } catch (Exception e) {
            log.error("Ошибка при выполнении запроса к БД: ", e);
            throw new RuntimeException("Ошибка при получении данных из базы данных", e);
        }

        Map<Long, Long> viewsMap = getViewsAllEvents(eventList);
        return eventList.stream()
                .map(event -> {
                    EventFullDto dto = eventMapper.toEventFullDto(event);
                    dto.setViews(viewsMap.getOrDefault(event.getId(), DEFAULT_VIEWS));
                    dto.setConfirmedRequests(eventRequestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public EventFullDto findEventById(Long eventId, HttpServletRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с ID = " + eventId + " не найдено"));

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new NotFoundException("Событие с ID = " + eventId + " не опубликовано");
        }

        sendStats(request);

        EventFullDto eventFullDto = eventMapper.toEventFullDto(event);
        eventFullDto.setViews(getViews(eventId, event.getCreatedOn(), request));
        eventFullDto.setConfirmedRequests(eventRequestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED));
        return eventFullDto;
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

    private Long getViews(Long eventId, LocalDateTime createdOn, HttpServletRequest request) {
        LocalDateTime end = LocalDateTime.now();
        String uri = request.getRequestURI();
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

    private Map<Long, Long> getViewsAllEvents(List<Event> events) {
        List<String> uris = events.stream()
                .map(event -> String.format("/events/%s", event.getId()))
                .collect(Collectors.toList());

        List<LocalDateTime> startDates = events.stream()
                .map(Event::getCreatedOn)
                .toList();
        LocalDateTime earliestDate = startDates.stream()
                .min(LocalDateTime::compareTo)
                .orElse(null);
        Map<Long, Long> viewStatsMap = new HashMap<>();

        if (earliestDate != null) {
            ResponseEntity<Object> response = statClient.getStats(earliestDate, LocalDateTime.now(),
                    uris, true);

            List<ViewStatsDto> viewStatsList = mapper.convertValue(response.getBody(), new TypeReference<>() {
            });

            viewStatsMap = viewStatsList.stream()
                    .filter(statsDto -> statsDto.getUri().startsWith("/events/"))
                    .collect(Collectors.toMap(
                            statsDto -> Long.parseLong(statsDto.getUri().substring("/events/".length())),
                            ViewStatsDto::getHits
                    ));
        }
        return viewStatsMap;
    }
}