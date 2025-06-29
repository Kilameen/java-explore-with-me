package ru.practicum.stat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.DefaultUriBuilderFactory;
import ru.practicum.stat.base.BaseClient;
import java.util.List;
import java.util.Map;

@Service
public class StatisticsClient extends BaseClient {

    private static final String API_PREFIX = "/";

    @Autowired
    public StatisticsClient(@Value("${stat-server.url}") String serverUrl, RestTemplateBuilder builder) {
        super(
                builder
                        .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl + API_PREFIX))
                        .requestFactory(() -> new HttpComponentsClientHttpRequestFactory())
                        .build()
        );
    }

    // Отправка запроса на сохранение информации о просмотре endpoint
    public ResponseEntity<Object> createEndpointHit(EndpointHitCreateDto endpointHitCreateDto) {
        return post("/hit", endpointHitCreateDto);
    }

    // Получение статистики просмотров
    public ResponseEntity<Object> getStatistics(String start, String end, List<String> uris, Boolean unique) {
        Map<String, Object> parameters = Map.of(
                "start", start,
                "end", end,
                "uris", uris,
                "unique", unique
        );
        return get("/stats", parameters);
    }
}