package ru.practicum.ewm.event.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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

    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    static int MIN_HOURS_BEFORE_EVENT = 2;
    static int MIN_HOURS_BEFORE_PUBLISH = 1;
    static long DEFAULT_VIEWS = 0L;
    static long DEFAULT_CONFIRMED_REQUESTS = 0L;

    EventRepository eventRepository;
    CategoryRepository categoryRepository;
    LocationRepository locationRepository;
    UserRepository userRepository;
    EventRequestRepository eventRequestRepository;

    StatisticsClient statClient;

    ObjectMapper mapper;
    EventMapper eventMapper;
    CategoryMapper categoryMapper;
    LocationMapper locationMapper;
    UserMapper userMapper;

    @Override
    public EventFullDto create(Long userId, NewEventDto newEventDto) {
        validateEventDate(newEventDto.getEventDate());
        LocalDateTime createdOn = LocalDateTime.now();

        User user = getUserById(userId);
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
        Event event = getEventById(eventId);

        LocalDateTime eventDate = (adminRequest.getEventDate() != null) ? adminRequest.getEventDate() : event.getEventDate();
        validateEventDateForAdmin(eventDate, adminRequest.getStateAction());
        validateStatusForAdmin(event.getState(), adminRequest.getStateAction());

        updateEventFields(event, Optional.ofNullable(adminRequest.getAnnotation()), Optional.ofNullable(adminRequest.getDescription()),
                eventDate, Optional.ofNullable(adminRequest.getPaid()), Optional.ofNullable(adminRequest.getParticipantLimit()),
                Optional.ofNullable(adminRequest.getRequestModeration()), Optional.ofNullable(adminRequest.getTitle()),
                adminRequest.getCategory());

        processStateAction(event, adminRequest.getStateAction());

        Event updatedEvent = eventRepository.save(event);
        return eventMapper.toEventFullDto(updatedEvent);
    }

    @Override
    public EventFullDto updateEventByPrivate(Long userId, Long eventId, UpdateEventUserRequest eventUserRequest) {
        User user = getUserById(userId);
        Event event = getEventById(eventId);

        validateUser(event.getInitiator(), user);

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Нельзя изменить опубликованное событие");
        }
        LocalDateTime eventDate = (eventUserRequest.getEventDate() != null) ? eventUserRequest.getEventDate() : event.getEventDate();
        validateEventDate(eventDate);

        updateEventFields(event, Optional.ofNullable(eventUserRequest.getAnnotation()), Optional.ofNullable(eventUserRequest.getDescription()),
                eventDate, Optional.ofNullable(eventUserRequest.getPaid()), Optional.ofNullable(eventUserRequest.getParticipantLimit()),
                Optional.ofNullable(eventUserRequest.getRequestModeration()), Optional.ofNullable(eventUserRequest.getTitle()),
                eventUserRequest.getCategory());

        processStateAction(event, eventUserRequest.getStateAction());

        Event updatedEvent = eventRepository.save(event);
        return eventMapper.toEventFullDto(updatedEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getEventOfUser(Long userId, Long eventId) {
        log.info("Получение события от пользователя {}", userId);
        User user = getUserById(userId);
        Event event = getEventById(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ValidationException("Пользователь не является инициатором события");
        }
        return eventMapper.toEventFullDto(event);
    }

    @Override
    public Collection<EventShortDto> findAllByPublic(String text, List<Long> categories, Boolean paid,
                                                     LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                                     Boolean onlyAvailable, String sort,
                                                     Integer from, Integer size,
                                                     HttpServletRequest request) {

        // Проверка временных параметров на корректность
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new IllegalArgumentException("rangeStart должен быть раньше rangeEnd");
        }

        // Проверка на допустимый тип сортировки
        if (sort != null && !List.of("EVENT_DATE", "VIEWS").contains(sort.toUpperCase())) {
            throw new IncorrectRequestException("Unknown sort type");
        }

        // Отправка статистики о запросе
        sendStats(request);

        // Получение списка событий из репозитория с пагинацией
        Pageable pageable = PageRequest.of(from / size, size);
        Page<Event> eventPage = eventRepository.findAllByPublic(text, categories, paid,
                rangeStart, rangeEnd, onlyAvailable, pageable);

        List<EventShortDto> eventShortDtos = eventPage.getContent().stream()
                .map(event -> {
                    EventShortDto eventDto = eventMapper.toEventShortDto(event);
                    eventDto.setViews(getViews(event.getId(), event.getCreatedOn(), request));
                    return eventDto;
                })
                .collect(Collectors.toList());

        // Сортировка по заданному критерию
        if ("EVENT_DATE".equalsIgnoreCase(sort)) {
            eventShortDtos.sort(Comparator.comparing(EventShortDto::getEventDate));
        } else if ("VIEWS".equalsIgnoreCase(sort)) {
            eventShortDtos.sort(Comparator.comparing(EventShortDto::getViews));
        }

        return eventShortDtos;
    }

    @Override
    public Collection<EventShortDto> findAllByPrivate(Long userId, Integer from, Integer size, HttpServletRequest request) {

        User user = getUserById(userId);

        Pageable pageable = PageRequest.of(from, size);

        List<Event> events = eventRepository.findAllByInitiatorId(user.getId(), pageable);
        return events.stream()
                .map(event -> {

                    EventShortDto eventShortDto = eventMapper.toEventShortDto(event);

                    eventShortDto.setCategory(categoryMapper.toCategoryDto(event.getCategory()));
                    eventShortDto.setInitiator(userMapper.toUserShortDto(event.getInitiator()));
                    eventShortDto.setViews(getViews(event.getId(), event.getCreatedOn(), request));
                    eventShortDto.setConfirmedRequests(eventRequestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED));

                    return eventShortDto;
                })
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public Collection<EventFullDto> findAllByAdmin(List<Long> users, List<EventState> states, List<Long> categories, LocalDateTime rangeStart, LocalDateTime rangeEnd, Integer from, Integer size, HttpServletRequest request) {

        long newHits = getHits(request);
        Pageable pageable = PageRequest.of(from, size);

        List<Event> eventList;
        try {
            eventList = eventRepository.findAllByAdmin(users, states, categories, rangeStart, rangeEnd, pageable);
        } catch (Exception e) {
            log.error("Ошибка при выполнении запроса к БД: ", e);
            throw new RuntimeException("Ошибка при получении данных из базы данных", e);
        }

        return eventList.stream()
                .map(event -> {
                    EventFullDto dto = eventMapper.toEventFullDto(event);
                    dto.setViews(newHits);
                    dto.setConfirmedRequests(eventRequestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto findEventById(Long eventId, HttpServletRequest request) {
        Event event = getEventById(eventId);

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

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь c ID " + userId + " не найден"));
    }

    private Event getEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие c ID " + eventId + " не найдено"));
    }

    private void updateEventFields(Event event, Optional<String> annotation, Optional<String> description,
                                   LocalDateTime eventDate, Optional<Boolean> paid, Optional<Integer> participantLimit,
                                   Optional<Boolean> requestModeration, Optional<String> title, Long categoryId) {

        annotation.ifPresent(event::setAnnotation);
        description.ifPresent(event::setDescription);
        event.setEventDate(eventDate);
        paid.ifPresent(event::setPaid);
        participantLimit.ifPresent(event::setParticipantLimit);
        requestModeration.ifPresent(event::setRequestModeration);
        title.ifPresent(event::setTitle);

        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new NotFoundException("Категория с ID " + categoryId + " не найдена"));
            event.setCategory(category);
        }
    }

    private void processStateAction(Event event, StateAction stateAction) {
        if (stateAction != null) {
            switch (stateAction) {
                case PUBLISH_EVENT:
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;
                case REJECT_EVENT:
                case CANCEL_REVIEW:
                    event.setState(EventState.CANCELED);
                    break;
                case SEND_TO_REVIEW:
                    event.setState(EventState.PENDING);
                    break;
                default:
                    throw new IllegalArgumentException("Недопустимое действие над событием: " + stateAction);
            }
        }
    }

    private void sendStats(HttpServletRequest request) {
        try {
            statClient.create(request);
        } catch (Exception e) {
            log.error("Ошибка при отправке статистики: {}", e.getMessage());

        }
    }

    private Long getViews(Long eventId, LocalDateTime createdOn, HttpServletRequest request) {
        LocalDateTime end = LocalDateTime.now();
        String uri = request.getRequestURI();
        Long defaultViews = DEFAULT_VIEWS;

        try {
            ResponseEntity<Object> statsResponse = statClient.getStats(createdOn, end, List.of(uri), true);
            if (statsResponse.getStatusCode().is2xxSuccessful() && statsResponse.hasBody()) {
                ViewStatsDto[] statsArray = mapper.convertValue(statsResponse.getBody(), ViewStatsDto[].class);
                if (statsArray.length > 0) {
                    return statsArray[0].getHits();
                }
            }
        } catch (Exception e) {
            log.error("Ошибка при получении статистики для события {}: {}", eventId, e.getMessage());
        }
        return defaultViews;
    }

    public Long getHits(HttpServletRequest request) {
        try {
            ResponseEntity<Object> response = statClient.getStats(
                    LocalDateTime.now().minusYears(100),
                    LocalDateTime.now(),
                    List.of(request.getRequestURI()),
                    true
            );

            if (response.getStatusCode().is2xxSuccessful() && response.hasBody()) {
                List<ViewStatsDto> viewStatsList = Arrays.asList(mapper.convertValue(response.getBody(), ViewStatsDto[].class));
                if (!viewStatsList.isEmpty()) {
                    return viewStatsList.get(0).getHits();
                }
            }
            return DEFAULT_VIEWS;
        } catch (Exception e) {
            return DEFAULT_VIEWS;
        }
    }
}