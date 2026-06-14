package com.shivam.bookMyShow.service.impl;

import com.shivam.bookMyShow.dto.response.CityDto;
import com.shivam.bookMyShow.entity.City;
import com.shivam.bookMyShow.repository.CityRepository;
import com.shivam.bookMyShow.service.CityService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CityServiceImpl implements CityService {

    private final CityRepository cityRepository;
    private final ModelMapper modelMapper;

    @Override
    public CityDto createCity(String name) {
        City city = City.builder().name(name).build();
        return modelMapper.map(cityRepository.save(city), CityDto.class);
    }

    @Override
    public List<CityDto> getAllCities() {
        return cityRepository.findAll().stream()
                .map(city -> modelMapper.map(city, CityDto.class))
                .collect(Collectors.toList());
    }
}