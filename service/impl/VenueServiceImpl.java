package com.shivam.bookMyShow.service.impl;

import com.shivam.bookMyShow.dto.request.*;
import com.shivam.bookMyShow.dto.response.*;
import com.shivam.bookMyShow.entity.*;
import com.shivam.bookMyShow.entity.enums.SeatType;
import com.shivam.bookMyShow.exception.*;
import com.shivam.bookMyShow.repository.*;
import com.shivam.bookMyShow.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VenueServiceImpl implements VenueService {

    private final VenueRepository venueRepository;
    private final CityRepository cityRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final ModelMapper modelMapper;

    @Override
    public VenueDto createVenue(CreateVenueRequest request) {
        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("City not found: " + request.getCityId()));
        Venue venue = Venue.builder()
                .name(request.getName())
                .address(request.getAddress())
                .city(city)
                .amenities(request.getAmenities())
                .active(true)
                .build();
        return modelMapper.map(venueRepository.save(venue), VenueDto.class);
    }

    @Override
    public VenueDto getVenueById(Long id) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + id));
        return modelMapper.map(venue, VenueDto.class);
    }

    @Override
    public List<VenueDto> getVenuesByCity(String cityName) {
        City city = cityRepository.findByNameIgnoreCase(cityName)
                .orElseThrow(() -> new ResourceNotFoundException("City not found: " + cityName));
        return venueRepository.findByCityAndActiveTrue(city).stream()
                .map(v -> modelMapper.map(v, VenueDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public VenueDto updateVenue(Long id, CreateVenueRequest request) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + id));
        venue.setName(request.getName());
        venue.setAddress(request.getAddress());
        venue.setAmenities(request.getAmenities());
        return modelMapper.map(venueRepository.save(venue), VenueDto.class);
    }

    @Override
    public void deleteVenue(Long id) {
        if (!venueRepository.existsById(id))
            throw new ResourceNotFoundException("Venue not found: " + id);
        venueRepository.deleteById(id);
    }

    @Override
    @Transactional
    public ScreenDto addScreen(Long venueId, CreateScreenRequest request) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + venueId));
        Screen screen = Screen.builder()
                .venue(venue)
                .name(request.getName())
                .type(request.getType())
                .totalSeats(0)
                .build();
        return modelMapper.map(screenRepository.save(screen), ScreenDto.class);
    }

    @Override
    public List<ScreenDto> getScreensByVenue(Long venueId) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + venueId));
        return screenRepository.findByVenue(venue).stream()
                .map(s -> modelMapper.map(s, ScreenDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public ScreenDto updateScreen(Long screenId, CreateScreenRequest request) {
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found: " + screenId));
        screen.setName(request.getName());
        screen.setType(request.getType());
        return modelMapper.map(screenRepository.save(screen), ScreenDto.class);
    }

    @Override
    public void deleteScreen(Long screenId) {
        if (!screenRepository.existsById(screenId))
            throw new ResourceNotFoundException("Screen not found: " + screenId);
        screenRepository.deleteById(screenId);
    }

    @Override
    @Transactional
    public List<SeatDto> addSeats(Long screenId, List<CreateSeatRequest> requests) {
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found: " + screenId));
        List<Seat> seats = requests.stream().map(req -> Seat.builder()
                .screen(screen)
                .rowNumber(req.getRowNumber())
                .seatNumber(req.getSeatNumber())
                .type(req.getType())
                .build()).collect(Collectors.toList());
        List<Seat> saved = seatRepository.saveAll(seats);
        // Update totalSeats count on screen
        screen.setTotalSeats(seatRepository.countByScreen(screen));
        screenRepository.save(screen);
        return saved.stream().map(s -> modelMapper.map(s, SeatDto.class)).collect(Collectors.toList());
    }

    @Override
    public List<SeatDto> getSeatsByScreen(Long screenId) {
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found: " + screenId));
        return seatRepository.findByScreen(screen).stream()
                .map(s -> modelMapper.map(s, SeatDto.class))
                .collect(Collectors.toList());
    }
}
