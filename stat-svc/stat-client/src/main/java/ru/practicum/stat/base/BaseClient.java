package ru.practicum.stat.base;

import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.Map;

public class BaseClient {

    protected final RestTemplate rest;

    public BaseClient(RestTemplate rest) {
        this.rest = rest;
    }

    // Метод для выполнения GET запросов
    protected ResponseEntity<Object> get(String path, Map<String, Object> parameters) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(path);
        if (parameters != null) {
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                builder.queryParam(entry.getKey(), entry.getValue());
            }
        }
        return rest.getForEntity(builder.build().toUriString(), Object.class);
    }

    // Метод для выполнения POST запросов
    protected ResponseEntity<Object> post(String path, Object body) {
        return rest.postForEntity(path, body, Object.class);
    }
}