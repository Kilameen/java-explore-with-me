package ru.practicum.stat.base;

import org.springframework.http.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

public class BaseClient {

    protected final RestTemplate rest;

    public BaseClient(RestTemplate rest) {
        this.rest = rest;
    }

    protected ResponseEntity<Object> get(String path) {
        return makeAndSendRequest(path);
    }

    protected ResponseEntity<Object> post(Object body) {
        HttpEntity<Object> requestEntity = new HttpEntity<>(body);
        return rest.postForEntity("/hit", requestEntity, Object.class);
    }

    private <T> ResponseEntity<Object> makeAndSendRequest(String path) {
        HttpEntity<T> requestEntity = new HttpEntity<>(null, defaultHeaders());

        ResponseEntity<Object> responseEntity;
        try {
            responseEntity = rest.exchange(path, HttpMethod.GET, requestEntity, Object.class);
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsByteArray());
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