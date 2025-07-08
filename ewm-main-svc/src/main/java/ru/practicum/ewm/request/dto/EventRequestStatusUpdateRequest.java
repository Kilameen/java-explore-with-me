package ru.practicum.ewm.request.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.ewm.utils.enums.RequestStatus;

import java.util.Collection;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestStatusUpdateRequest {
    @NotEmpty(message = "Have not requests to update")
    Collection<Long> requestIds;

    @NotBlank(message = "Have not new status for requests to update")
    RequestStatus status;
}
