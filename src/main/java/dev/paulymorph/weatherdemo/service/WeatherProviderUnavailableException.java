package dev.paulymorph.weatherdemo.service;

public class WeatherProviderUnavailableException extends RuntimeException {

    public WeatherProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public WeatherProviderUnavailableException(String message) {
        super(message);
    }
}
