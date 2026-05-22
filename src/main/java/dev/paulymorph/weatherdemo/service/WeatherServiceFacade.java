package dev.paulymorph.weatherdemo.service;

import dev.paulymorph.weatherdemo.config.WeatherProvidersProperties;
import dev.paulymorph.weatherdemo.dto.WeatherResponse;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Primary
public class WeatherServiceFacade implements WeatherService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WeatherServiceFacade.class);

    private final List<ExternalWeatherServiceImpl> providers;

    public WeatherServiceFacade(
            RestTemplate restTemplate,
            tools.jackson.databind.ObjectMapper objectMapper,
            WeatherProvidersProperties weatherProvidersProperties) {
        this.providers = buildProviders(restTemplate, objectMapper, weatherProvidersProperties);
    }

    @Override
    public WeatherResponse getWeatherForCity(String city) {
        WeatherProviderUnavailableException lastException = null;

        for (ExternalWeatherServiceImpl provider : providers) {
            try {
                WeatherResponse response = provider.getWeatherForCity(city);
                LOGGER.info("Weather served by provider={} for city={}", provider.getProviderName(), city);
                return response;
            } catch (WeatherProviderUnavailableException exception) {
                LOGGER.warn("Provider={} failed for city={}, trying next provider", provider.getProviderName(), city, exception);
                lastException = exception;
            }
        }

        LOGGER.error("All configured weather providers failed for city={}", city, lastException);
        throw new WeatherProviderUnavailableException("All configured weather providers are unavailable", lastException);
    }

    private List<ExternalWeatherServiceImpl> buildProviders(
            RestTemplate restTemplate,
            tools.jackson.databind.ObjectMapper objectMapper,
            WeatherProvidersProperties weatherProvidersProperties) {
        List<ExternalWeatherServiceImpl> configuredProviders = new ArrayList<>();

        if (weatherProvidersProperties.getProviders() == null
                || weatherProvidersProperties.getProviders().isEmpty()) {
            throw new IllegalArgumentException("At least one weather provider must be configured");
        }

        for (WeatherProvidersProperties.Provider provider : weatherProvidersProperties.getProviders()) {
            validateProvider(provider);
            configuredProviders.add(new ExternalWeatherServiceImpl(
                    restTemplate,
                    objectMapper,
                    provider.getName(),
                    provider.getUrlTemplate(),
                    provider.getMapping().getTemperature(),
                    provider.getMapping().getWindSpeed()));
        }

        return configuredProviders;
    }

    private void validateProvider(WeatherProvidersProperties.Provider provider) {
        if (provider.getName() == null || provider.getName().isBlank()) {
            throw new IllegalArgumentException("Provider name must not be blank");
        }

        if (provider.getUrlTemplate() == null || provider.getUrlTemplate().isBlank()) {
            throw new IllegalArgumentException("Provider '" + provider.getName() + "' urlTemplate must not be blank");
        }

        if (!provider.getUrlTemplate().contains("%s")) {
            throw new IllegalArgumentException(
                    "Provider '" + provider.getName() + "' urlTemplate must include %s placeholder for city");
        }

        if (provider.getMapping() == null) {
            throw new IllegalArgumentException("Provider '" + provider.getName() + "' mapping must not be null");
        }

        validatePointer(provider.getName(), "temperature", provider.getMapping().getTemperature());
        validatePointer(provider.getName(), "windSpeed", provider.getMapping().getWindSpeed());
    }

    private void validatePointer(String providerName, String fieldName, String pointer) {
        if (pointer == null || pointer.isBlank()) {
            throw new IllegalArgumentException(
                    "Provider '" + providerName + "' mapping." + fieldName + " must not be blank");
        }

        if (!pointer.startsWith("/")) {
            throw new IllegalArgumentException(
                    "Provider '" + providerName + "' mapping." + fieldName + " must start with /");
        }
    }
}
