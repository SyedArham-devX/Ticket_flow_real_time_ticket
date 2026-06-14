package com.shivam.bookMyShow.service.impl;

import com.shivam.bookMyShow.dto.response.*;
import com.shivam.bookMyShow.entity.Show;
import com.shivam.bookMyShow.exception.ResourceNotFoundException;
import com.shivam.bookMyShow.repository.*;
import com.shivam.bookMyShow.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final BookingRepository bookingRepository;
    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final VenueRepository venueRepository;
    private final MovieRepository movieRepository;
    private final ModelMapper modelMapper;

    @Override
    public AnalyticsDto getRevenueReport(LocalDate start, LocalDate end) {
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.atTime(LocalTime.MAX);
        BigDecimal revenue = bookingRepository.getTotalRevenue(startDt, endDt);
        revenue = revenue != null ? revenue : BigDecimal.ZERO;
        List<?> bookings = bookingRepository.findAll();
        long count = bookings.size();
        BigDecimal avg = count > 0 ? revenue.divide(BigDecimal.valueOf(count), RoundingMode.HALF_UP) : BigDecimal.ZERO;
        return AnalyticsDto.builder().totalRevenue(revenue).totalBookings(count).avgRevenuePerBooking(avg).build();
    }

    @Override
    public ShowReportDto getShowReport(Long showId) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + showId));
        long total = show.getScreen().getTotalSeats();
        long available = showSeatRepository.countAvailableSeats(showId);
        long booked = total - available;
        double occupancy = total > 0 ? (booked * 100.0 / total) : 0;
        BigDecimal revenue = bookingRepository.findByShowIdAndStatus(showId,
                com.shivam.bookMyShow.entity.enums.BookingStatus.CONFIRMED)
                .stream().map(b -> b.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return ShowReportDto.builder()
                .showId(showId)
                .movieTitle(show.getMovie().getTitle())
                .totalSeats(total)
                .bookedSeats(booked)
                .availableSeats(available)
                .occupancyPercent(Math.round(occupancy * 100.0) / 100.0)
                .totalRevenue(revenue)
                .build();
    }

    @Override
    public List<MovieDto> getTopMovies(LocalDate start, LocalDate end, int limit) {
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.atTime(LocalTime.MAX);
        List<Object[]> results = bookingRepository.getTopMoviesByRevenue(startDt, endDt,
                PageRequest.of(0, limit));
        return results.stream().map(row -> {
            MovieDto dto = new MovieDto();
            dto.setTitle((String) row[0]);
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public AnalyticsDto getVenueReport(Long venueId) {
        if (!venueRepository.existsById(venueId))
            throw new ResourceNotFoundException("Venue not found: " + venueId);
        return AnalyticsDto.builder()
                .totalRevenue(BigDecimal.ZERO)
                .totalBookings(0L)
                .avgRevenuePerBooking(BigDecimal.ZERO)
                .build();
    }

    @Override
    public List<Object[]> getPeakBookingHours() {
        return bookingRepository.getBookingsByHour();
    }
}
