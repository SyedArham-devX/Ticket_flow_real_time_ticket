package com.shivam.bookMyShow.service;

import com.shivam.bookMyShow.dto.response.CityDto;
import java.util.List;

public interface CityService {
    CityDto createCity(String name);
    List<CityDto> getAllCities();
}