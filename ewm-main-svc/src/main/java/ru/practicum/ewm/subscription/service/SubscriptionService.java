package ru.practicum.ewm.subscription.service;

import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.subscription.dto.NewRequestSubscription;
import ru.practicum.ewm.subscription.dto.SubscriberData;
import ru.practicum.ewm.subscription.dto.SubscriptionDto;

import java.util.List;

public interface SubscriptionService {
    SubscriptionDto subscribe(Long userId, NewRequestSubscription requestSubscription);

    SubscriptionDto unSubscribe(Long userId, NewRequestSubscription requestUnSubscription);

    List<EventShortDto> getEventsFromSubscriptions(Long userId, int from, int size);

    Long getSubscriberCount(Long userId);

    @Transactional(readOnly = true)
    List<SubscriberData> getAllSubscribers(Long userId, int from, int size);
}
