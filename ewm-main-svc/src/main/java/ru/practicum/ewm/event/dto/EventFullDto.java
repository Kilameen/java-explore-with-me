package ru.practicum.ewm.event.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.location.dto.LocationDto;
import ru.practicum.ewm.location.model.Location;
import ru.practicum.ewm.user.dto.UserShortDto;
import ru.practicum.ewm.utils.enums.EventState;

import java.time.LocalDateTime;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventFullDto {
    Long id;
    String title;
    String annotation;
    String description;
    LocalDateTime eventDate;
    LocationDto location;
    Boolean paid;
    Integer participantLimit;
    Boolean requestModeration;
    EventState state;
    LocalDateTime createdOn;
    LocalDateTime publishedOn;
    UserShortDto initiator;
    CategoryDto category;
    int views;
    int confirmedRequests;
}
