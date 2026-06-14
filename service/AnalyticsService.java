package com.shivam.bookMyShow.service;
import com.shivam.bookMyShow.dto.response.*;
import java.time.LocalDate;
import java.util.List;
public interface AnalyticsService {
    AnalyticsDto getRevenueReport(LocalDate start, LocalDate end);
    ShowReportDto getShowReport(Long showId);
    List<MovieDto> getTopMovies(LocalDate start, LocalDate end, int limit);
    AnalyticsDto getVenueReport(Long venueId);
    List<Object[]> getPeakBookingHours();
}
