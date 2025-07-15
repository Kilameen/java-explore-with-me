package ru.practicum.stat.base;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import java.util.List;

@Slf4j
public class BaseClient {

    protected final RestTemplate rest;
    private final String statsUri;

    public BaseClient(RestTemplate rest,@Value("${stats-server.url}") String statsUri) {
        this.rest = rest;
        this.statsUri = statsUri;
    }

    protected ResponseEntity<Object> get(String path) {
        return makeAndSendRequest(statsUri + path);
    }

    protected ResponseEntity<Object> post(Object body) {
        HttpEntity<Object> requestEntity = new HttpEntity<>(body);
        try {
            log.info("Отправка POST запроса на URL: {}, тело: {}", statsUri + "/hit", body);
            ResponseEntity<Object> response = rest.postForEntity(statsUri + "/hit", requestEntity, Object.class);
            log.info("Получен ответ от сервиса статистики, статус: {}", response.getStatusCode());
            return response;
        } catch (HttpStatusCodeException e) {
            log.error("Ошибка при отправке POST запроса: {}, тело ответа: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    private <T> ResponseEntity<Object> makeAndSendRequest(String path) {
        HttpEntity<T> requestEntity = new HttpEntity<>(null, defaultHeaders());
        ResponseEntity<Object> responseEntity;
        try {
            log.info("Отправка GET запроса на URL: {}", path);
            responseEntity = rest.exchange(path, HttpMethod.GET, requestEntity, Object.class);
            log.info("Получен ответ от сервиса статистики, статус: {}", responseEntity.getStatusCode());
        } catch (HttpStatusCodeException e) {
            log.error("Ошибка при отправке GET запроса: {}, тело ответа: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
        return prepareResponse(responseEntity);
    }

    private HttpHeaders defaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private static ResponseEntity<Object> prepareResponse(ResponseEntity<Object> response) {
        if (response.getStatusCode().is2xxSuccessful()) {
            return response;
        }
        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(response.getStatusCode());
        if (response.hasBody()) {
            return responseBuilder.body(response.getBody());
        }
        return responseBuilder.build();
    }
}
