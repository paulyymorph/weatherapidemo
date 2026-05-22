package dev.paulymorph.weatherdemo.service;

import dev.paulymorph.weatherdemo.dto.WeatherResponse;

public interface WeatherService {
    /**
     * Fetch weather information for the given city.
     *
     * @param city the city name
     * @return WeatherResponse containing wind_speed and temperature_degrees
     */
    WeatherResponse getWeatherForCity(String city);
}
