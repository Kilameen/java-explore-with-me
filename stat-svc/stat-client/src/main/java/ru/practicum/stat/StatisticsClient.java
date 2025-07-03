package ru.practicum.stat;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.stat.base.BaseClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class StatisticsClient extends BaseClient {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String appName;

    @Autowired
    public StatisticsClient(@Value("${stat-server.url}") String serverUrl,
                            @Value("${app.name}") String appName,
                            RestTemplateBuilder builder) {
        super(
                builder
                        .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl))
                        .requestFactory(() -> new HttpComponentsClientHttpRequestFactory())
                        .build()
        );
        this.appName = appName;
    }

    public ResponseEntity<Object> create(HttpServletRequest request) {
        EndpointHitCreateDto endpointHitCreateDto = EndpointHitCreateDto.builder()
                .app(appName)
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(LocalDateTime.now())
                .build();
        return post(endpointHitCreateDto);
    }

    public ResponseEntity<Object> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/stats")
                .queryParam("start", start.format(formatter))
                .queryParam("end", end.format(formatter))
                .queryParam("unique", unique);

        if (uris != null && !uris.isEmpty()) {
            builder.queryParam("uris", String.join(",", uris));
        }
        String url = builder.toUriString();
        return get(url);
    }
}