package ru.practicum.stat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.stat.model.EndpointHit;
import ru.practicum.stat.model.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

public interface EndpointHitRepository extends JpaRepository<EndpointHit, Long> {

    @Query("SELECT new ru.practicum.stat.model.ViewStats(e.app, e.uri, COUNT(DISTINCT e.ip)) " +
            "FROM EndpointHit e " +
            "WHERE e.timestamp BETWEEN ?1 AND ?2 " +
            "AND e.uri IN (?3) " +
            "GROUP BY e.app, e.uri " +
            "ORDER BY COUNT(DISTINCT e.ip) DESC")
    List<ViewStats> findStatsUniqueIp(LocalDateTime start, LocalDateTime end, List<String> uris);

    @Query("SELECT new ru.practicum.stat.model.ViewStats(e.app, e.uri, COUNT(DISTINCT e.ip)) " +
            "FROM EndpointHit e " +
            "WHERE e.timestamp BETWEEN ?1 AND ?2 " +
            "GROUP BY e.app, e.uri " +
            "ORDER BY COUNT(DISTINCT e.ip) DESC")
    List<ViewStats> findStatsUniqueIpAllUris(LocalDateTime start, LocalDateTime end);

    @Query("SELECT new ru.practicum.stat.model.ViewStats(e.app, e.uri, COUNT(e.ip)) " +
            "FROM EndpointHit e " +
            "WHERE e.timestamp BETWEEN ?1 AND ?2 " +
            "AND e.uri IN (?3) " +
            "GROUP BY e.app, e.uri " +
            "ORDER BY COUNT(e.ip) DESC")
    List<ViewStats> findStats(LocalDateTime start, LocalDateTime end, List<String> uris);

    @Query("SELECT new ru.practicum.stat.model.ViewStats(e.app, e.uri, COUNT(e.ip)) " +
            "FROM EndpointHit e " +
            "WHERE e.timestamp BETWEEN ?1 AND ?2 " +
            "GROUP BY e.app, e.uri " +
            "ORDER BY COUNT(e.ip) DESC")
    List<ViewStats> findStatsAllUris(LocalDateTime start, LocalDateTime end);
}