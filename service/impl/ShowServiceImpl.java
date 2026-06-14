package com.shivam.bookMyShow.service.impl;

import com.shivam.bookMyShow.dto.request.*;
import com.shivam.bookMyShow.dto.response.*;
import com.shivam.bookMyShow.entity.*;
import com.shivam.bookMyShow.entity.enums.*;
import com.shivam.bookMyShow.exception.*;
import com.shivam.bookMyShow.repository.*;
import com.shivam.bookMyShow.service.ShowService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShowServiceImpl implements ShowService {

    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final ShowSeatRepository showSeatRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public ShowDto createShow(CreateShowRequest request) {
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found: " + request.getMovieId()));
        Screen screen = screenRepository.findById(request.getScreenId())
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found: " + request.getScreenId()));

        LocalTime endTime = request.getStartTime().plusMinutes(movie.getDurationMins());

        Show show = Show.builder()
                .movie(movie)
                .screen(screen)
                .showDate(request.getShowDate())
                .startTime(request.getStartTime())
                .endTime(endTime)
                .language(request.getLanguage())
                .priceRegular(request.getPriceRegular())
                .pricePremium(request.getPricePremium())
                .priceVip(request.getPriceVip())
                .status(ShowStatus.SCHEDULED)
                .build();
        show = showRepository.save(show);

        // Auto-generate ShowSeat rows for every seat in the screen
        final Show savedShow = show;
        List<Seat> seats = seatRepository.findByScreen(screen);
        List<ShowSeat> showSeats = seats.stream().map(seat -> {
            BigDecimal price = switch (seat.getType()) {
                case PREMIUM -> request.getPricePremium();
                case VIP, RECLINER -> request.getPriceVip();
                default -> request.getPriceRegular();
            };
            return ShowSeat.builder()
                    .show(savedShow)
                    .seat(seat)
                    .status(SeatStatus.AVAILABLE)
                    .price(price)
                    .build();
        }).collect(Collectors.toList());
        showSeatRepository.saveAll(showSeats);

        return toShowDto(show);
    }

    @Override
    @Cacheable(value = "shows", key = "#id")
    public ShowDto getShowById(Long id) {
        Show show = showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + id));
        return toShowDto(show);
    }

    @Override
    @Cacheable(value = "showSearch", key = "#city + '_' + #movieId + '_' + #date + '_' + #pageable.pageNumber")
    public Page<ShowDto> searchShows(String city, Long movieId, LocalDate date, Pageable pageable) {
        LocalDate searchDate = date != null ? date : LocalDate.now();
        Page<Show> shows;
        if (city != null) {
            shows = showRepository.findShowsByCityAndDate(city, searchDate, pageable);
        } else {
            shows = showRepository.findAll(pageable);
        }
        return shows.map(this::toShowDto);
    }

    @Override
    public List<ShowSeatDto> getSeatMap(Long showId) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + showId));
        return showSeatRepository.findByShow(show).stream()
                .map(ss -> {
                    ShowSeatDto dto = new ShowSeatDto();
                    dto.setId(ss.getId());
                    dto.setRowNumber(ss.getSeat().getRowNumber());
                    dto.setSeatNumber(ss.getSeat().getSeatNumber());
                    dto.setSeatType(ss.getSeat().getType());
                    dto.setStatus(ss.getStatus());
                    dto.setPrice(ss.getPrice());
                    return dto;
                }).collect(Collectors.toList());
    }

    @Override
    @CacheEvict(value = {"shows", "showSearch"}, allEntries = true)
    public ShowDto updateShow(Long id, CreateShowRequest request) {
        Show show = showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + id));
        show.setShowDate(request.getShowDate());
        show.setStartTime(request.getStartTime());
        show.setLanguage(request.getLanguage());
        show.setPriceRegular(request.getPriceRegular());
        show.setPricePremium(request.getPricePremium());
        show.setPriceVip(request.getPriceVip());
        return toShowDto(showRepository.save(show));
    }

    @Override
    @CacheEvict(value = {"shows", "showSearch"}, allEntries = true)
    public void deleteShow(Long id) {
        if (!showRepository.existsById(id))
            throw new ResourceNotFoundException("Show not found: " + id);
        showRepository.deleteById(id);
    }

    @Override
    @CacheEvict(value = {"shows", "showSearch"}, allEntries = true)
    public ShowDto updatePrices(Long id, UpdateShowPriceRequest request) {
        Show show = showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + id));
        show.setPriceRegular(request.getPriceRegular());
        show.setPricePremium(request.getPricePremium());
        show.setPriceVip(request.getPriceVip());
        return toShowDto(showRepository.save(show));
    }

    private ShowDto toShowDto(Show show) {
        ShowDto dto = modelMapper.map(show, ShowDto.class);
        dto.setAvailableSeats(showSeatRepository.countAvailableSeats(show.getId()));
        return dto;
    }
}
