package dev.paulymorph.weatherdemo.controller;

import dev.paulymorph.weatherdemo.dto.WeatherResponse;
import dev.paulymorph.weatherdemo.service.WeatherService;
import dev.paulymorph.weatherdemo.service.WeatherProviderUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public class WeatherController {

    @Autowired
    private WeatherService weatherService;

    /**
     * Get weather information for a specified city.
     *
     * @param city the city name (required)
     * @return WeatherResponse with wind_speed and temperature_degrees
     */
    @GetMapping("/weather")
    public ResponseEntity<?> getWeather(@RequestParam(required = true) String city) {
        if (city == null || city.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("City parameter is required and cannot be empty"));
        }

        try {
            WeatherResponse weather = weatherService.getWeatherForCity(city);
            return ResponseEntity.ok(weather);
        } catch (WeatherProviderUnavailableException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorResponse("Weather providers unavailable"));
        }
    }

    // Simple error response class
    public static class ErrorResponse {
        private String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
