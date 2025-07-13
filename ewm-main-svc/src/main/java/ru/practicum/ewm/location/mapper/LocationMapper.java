package ru.practicum.ewm.location.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.location.dto.LocationDto;
import ru.practicum.ewm.location.model.Location;

@Component
public class LocationMapper {

    public Location toLocationFromDto(LocationDto locationDto){
        return Location.builder()
                .lat(locationDto.getLat())
                .lon(locationDto.getLon())
                .build();
    }

    public LocationDto toLocationDto(Location location){
        return LocationDto.builder()
                .lat(location.getLat())
                .lon(location.getLon())
                .build();
    }
}