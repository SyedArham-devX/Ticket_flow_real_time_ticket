package com.shivam.bookMyShow.config;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.*;
@Configuration
public class MapperConfig {
    @Bean
    public ModelMapper modelMapper() { return new ModelMapper(); }
}
