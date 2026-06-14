package com.shivam.bookMyShow.service;
import com.shivam.bookMyShow.dto.request.*;
import com.shivam.bookMyShow.dto.response.*;
import java.util.List;
public interface VenueService {
    VenueDto createVenue(CreateVenueRequest request);
    VenueDto getVenueById(Long id);
    List<VenueDto> getVenuesByCity(String cityName);
    VenueDto updateVenue(Long id, CreateVenueRequest request);
    void deleteVenue(Long id);
    ScreenDto addScreen(Long venueId, CreateScreenRequest request);
    List<ScreenDto> getScreensByVenue(Long venueId);
    ScreenDto updateScreen(Long screenId, CreateScreenRequest request);
    void deleteScreen(Long screenId);
    List<SeatDto> addSeats(Long screenId, List<CreateSeatRequest> request);
    List<SeatDto> getSeatsByScreen(Long screenId);
}
