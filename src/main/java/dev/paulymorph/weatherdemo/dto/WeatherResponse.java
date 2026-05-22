package dev.paulymorph.weatherdemo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WeatherResponse {
    @JsonProperty("wind_speed")
    private Integer windSpeed;

    @JsonProperty("temperature_degrees")
    private Integer temperatureDegrees;

    public WeatherResponse() {
    }

    public WeatherResponse(Integer windSpeed, Integer temperatureDegrees) {
        this.windSpeed = windSpeed;
        this.temperatureDegrees = temperatureDegrees;
    }

    public Integer getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(Integer windSpeed) {
        this.windSpeed = windSpeed;
    }

    public Integer getTemperatureDegrees() {
        return temperatureDegrees;
    }

    public void setTemperatureDegrees(Integer temperatureDegrees) {
        this.temperatureDegrees = temperatureDegrees;
    }
}
