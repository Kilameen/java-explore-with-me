package ru.practicum.ewm.location.mapper;

import ru.practicum.ewm.location.dto.LocationDto;
import ru.practicum.ewm.location.model.Location;

public class LocationMapper {

    public static Location toLocationFromDto(LocationDto locationDto) {
        return Location.builder()
                .lat(locationDto.getLat())
                .lon(locationDto.getLon())
                .build();
    }

    public static LocationDto toLocationDto(Location location) {
        return LocationDto.builder()
                .lat(location.getLat())
                .lon(location.getLon())
                .build();
    }
}