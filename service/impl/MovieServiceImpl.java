package com.shivam.bookMyShow.service.impl;

import com.shivam.bookMyShow.dto.request.CreateMovieRequest;
import com.shivam.bookMyShow.dto.response.MovieDto;
import com.shivam.bookMyShow.entity.Movie;
import com.shivam.bookMyShow.exception.ResourceNotFoundException;
import com.shivam.bookMyShow.repository.MovieRepository;
import com.shivam.bookMyShow.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private final ModelMapper modelMapper;

    @Override
    public MovieDto createMovie(CreateMovieRequest request) {
        Movie movie = modelMapper.map(request, Movie.class);
        movie.setActive(true);
        return modelMapper.map(movieRepository.save(movie), MovieDto.class);
    }

    @Override
    @Cacheable(value = "movies", key = "#id")
    public MovieDto getMovieById(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + id));
        return modelMapper.map(movie, MovieDto.class);
    }

    @Override
    
    public Page<MovieDto> getAllMovies(String city, String genre, Pageable pageable) {
        Page<Movie> movies;
        if (city != null && genre != null) {
            movies = movieRepository.findMoviesInCityByGenre(city, genre, pageable);
        } else if (city != null) {
            movies = movieRepository.findMoviesInCity(city, pageable);
        } else {
            movies = movieRepository.findByActiveTrue(pageable);
        }
        return movies.map(m -> modelMapper.map(m, MovieDto.class));
    }

    @Override
    @CacheEvict(value = {"movies", "moviesList"}, allEntries = true)
    public MovieDto updateMovie(Long id, CreateMovieRequest request) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + id));
        modelMapper.map(request, movie);
        return modelMapper.map(movieRepository.save(movie), MovieDto.class);
    }

    @Override
    @CacheEvict(value = {"movies", "moviesList"}, allEntries = true)
    public void deleteMovie(Long id) {
        if (!movieRepository.existsById(id))
            throw new ResourceNotFoundException("Movie not found with id: " + id);
        movieRepository.deleteById(id);
    }

    @Override
    @CacheEvict(value = {"movies", "moviesList"}, allEntries = true)
    public MovieDto toggleActive(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + id));
        movie.setActive(!movie.getActive());
        return modelMapper.map(movieRepository.save(movie), MovieDto.class);
    }
}
