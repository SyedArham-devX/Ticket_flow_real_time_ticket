package com.shivam.bookMyShow.service;
import com.shivam.bookMyShow.dto.request.*;
import com.shivam.bookMyShow.dto.response.*;
import org.springframework.data.domain.*;
import java.time.LocalDate;
import java.util.List;
public interface ShowService {
    ShowDto createShow(CreateShowRequest request);
    ShowDto getShowById(Long id);
    Page<ShowDto> searchShows(String city, Long movieId, LocalDate date, Pageable pageable);
    List<ShowSeatDto> getSeatMap(Long showId);
    ShowDto updateShow(Long id, CreateShowRequest request);
    void deleteShow(Long id);
    ShowDto updatePrices(Long id, UpdateShowPriceRequest request);
}
