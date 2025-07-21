package ru.practicum.ewm.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.ewm.location.dto.LocationDto;

import java.time.LocalDateTime;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewEventDto {
    @NotBlank
    @Size(min = 20, max = 2000, message = "Длина аннотации должна не больше 2000 символов и не меньше 20")
    String annotation;
    Long category;

    @NotBlank
    @Size(min = 20, max = 7000, message = "Длина описания должна не больше 7000 символов и не меньше 20")
    String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime eventDate;
    LocationDto location;
    Boolean paid = false;

    @Min(value = 0, message = "Лимит участников не может быть отрицательным")
    Integer participantLimit = 0;
    Boolean requestModeration = true;

    @Size(min = 3, max = 120, message = "Длина аннотации должна не больше 120 символов и не меньше 3")
    String title;
}