package ru.practicum.ewm.subscription.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.ewm.enums.FriendshipsStatus;

import java.time.LocalDateTime;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriberData {
    Long userId;
    String ownerName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime subscribeTime;
    FriendshipsStatus friendshipsStatus;
}
