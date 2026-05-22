package dev.paulymorph.weatherdemo.service;

import dev.paulymorph.weatherdemo.config.WeatherProvidersProperties;
import dev.paulymorph.weatherdemo.dto.WeatherResponse;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Primary
public class WeatherServiceFacade implements WeatherService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WeatherServiceFacade.class);
    private static final long CACHE_TTL_MILLIS = 5000;

    private final List<ExternalWeatherServiceImpl> providers;
    private final Map<String, CachedWeather> weatherCache = new ConcurrentHashMap<>();
    private final Map<String, Object> cityLocks = new ConcurrentHashMap<>();

    public WeatherServiceFacade(
            RestTemplate restTemplate,
            tools.jackson.databind.ObjectMapper objectMapper,
            WeatherProvidersProperties weatherProvidersProperties) {
        this.providers = buildProviders(restTemplate, objectMapper, weatherProvidersProperties);
    }

    @Override
    public WeatherResponse getWeatherForCity(String city) {
        String normalizedCity = normalizeCity(city);
        WeatherResponse cached = getCachedResponse(normalizedCity);
        if (cached != null) {
            LOGGER.info("Weather served from cache for city={}", city);
            return cached;
        }

        Object cityLock = cityLocks.computeIfAbsent(normalizedCity, key -> new Object());
        synchronized (cityLock) {
            try {
                WeatherResponse cachedAfterLock = getCachedResponse(normalizedCity);
                if (cachedAfterLock != null) {
                    LOGGER.info("Weather served from cache for city={} after lock", city);
                    return cachedAfterLock;
                }

                WeatherResponse freshResponse = fetchFromProviders(city);
                weatherCache.put(normalizedCity, new CachedWeather(copyResponse(freshResponse),
                        System.currentTimeMillis() + CACHE_TTL_MILLIS));
                return copyResponse(freshResponse);
            } finally {
                cityLocks.remove(normalizedCity, cityLock);
            }
        }
    }

    private WeatherResponse fetchFromProviders(String city) {
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

    private WeatherResponse getCachedResponse(String normalizedCity) {
        CachedWeather cachedWeather = weatherCache.get(normalizedCity);
        if (cachedWeather == null) {
            return null;
        }

        if (cachedWeather.expiresAtMillis() < System.currentTimeMillis()) {
            weatherCache.remove(normalizedCity, cachedWeather);
            return null;
        }

        return copyResponse(cachedWeather.response());
    }

    private String normalizeCity(String city) {
        return city == null ? "" : city.trim().toLowerCase(Locale.ROOT);
    }

    private WeatherResponse copyResponse(WeatherResponse response) {
        if (response == null) {
            return null;
        }

        return new WeatherResponse(response.getWindSpeed(), response.getTemperatureDegrees());
    }

    private record CachedWeather(WeatherResponse response, long expiresAtMillis) {
    }

    private List<ExternalWeatherServiceImpl> buildProviders(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
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
                    provider.getApiKey(),
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

        if (!provider.getUrlTemplate().contains("{city}") || !provider.getUrlTemplate().contains("{apiKey}")) {
            throw new IllegalArgumentException(
                    "Provider '" + provider.getName() + "' urlTemplate must include {city} and {apiKey} placeholders");
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
