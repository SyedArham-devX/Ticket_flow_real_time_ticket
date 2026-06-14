package com.shivam.bookMyShow.service;
import com.shivam.bookMyShow.dto.request.CreateMovieRequest;
import com.shivam.bookMyShow.dto.response.MovieDto;
import org.springframework.data.domain.*;
public interface MovieService {
    MovieDto createMovie(CreateMovieRequest request);
    MovieDto getMovieById(Long id);
    Page<MovieDto> getAllMovies(String city, String genre, Pageable pageable);
    MovieDto updateMovie(Long id, CreateMovieRequest request);
    void deleteMovie(Long id);
    MovieDto toggleActive(Long id);
}
