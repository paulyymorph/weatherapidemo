package dev.paulymorph.weatherdemo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.paulymorph.weatherdemo.config.WeatherProvidersProperties;
import dev.paulymorph.weatherdemo.dto.WeatherResponse;
import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

class WeatherServiceFacadeCacheTest {

    @Test
    void shouldReturnCachedValueWithinFiveSecondsForSameNormalizedCity() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn("{\"current\":{\"temperature\":25,\"wind_speed\":12}}")
                .thenReturn("{\"current\":{\"temperature\":99,\"wind_speed\":99}}")
;

        WeatherServiceFacade facade = new WeatherServiceFacade(
                restTemplate,
                new ObjectMapper(),
                singleProviderProperties());

        WeatherResponse first = facade.getWeatherForCity(" London ");
        WeatherResponse second = facade.getWeatherForCity("london");

        assertEquals(12, first.getWindSpeed());
        assertEquals(25, first.getTemperatureDegrees());
        assertEquals(12, second.getWindSpeed());
        assertEquals(25, second.getTemperatureDegrees());
        verify(restTemplate, times(1)).getForObject(anyString(), eq(String.class));
    }

    @Test
    void shouldExpireCacheAfterFiveSeconds() throws InterruptedException {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn("{\"current\":{\"temperature\":20,\"wind_speed\":10}}")
                .thenReturn("{\"current\":{\"temperature\":30,\"wind_speed\":15}}")
;

        WeatherServiceFacade facade = new WeatherServiceFacade(
                restTemplate,
                new ObjectMapper(),
                singleProviderProperties());

        WeatherResponse first = facade.getWeatherForCity("tokyo");
        Thread.sleep(5200);
        WeatherResponse second = facade.getWeatherForCity("tokyo");

        assertEquals(10, first.getWindSpeed());
        assertEquals(20, first.getTemperatureDegrees());
        assertEquals(15, second.getWindSpeed());
        assertEquals(30, second.getTemperatureDegrees());
        verify(restTemplate, times(2)).getForObject(anyString(), eq(String.class));
    }

    @Test
    void shouldNotCacheFailures() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenThrow(new RestClientException("provider down"));

        WeatherServiceFacade facade = new WeatherServiceFacade(
                restTemplate,
                new ObjectMapper(),
                singleProviderProperties());

        assertThrows(WeatherProviderUnavailableException.class, () -> facade.getWeatherForCity("manila"));
        assertThrows(WeatherProviderUnavailableException.class, () -> facade.getWeatherForCity("manila"));

        verify(restTemplate, times(2)).getForObject(anyString(), eq(String.class));
    }

    private WeatherProvidersProperties singleProviderProperties() {
        WeatherProvidersProperties properties = new WeatherProvidersProperties();

        WeatherProvidersProperties.Mapping mapping = new WeatherProvidersProperties.Mapping();
        mapping.setTemperature("/current/temperature");
        mapping.setWindSpeed("/current/wind_speed");

        WeatherProvidersProperties.Provider provider = new WeatherProvidersProperties.Provider();
        provider.setName("provider-1");
        provider.setUrlTemplate("https://example.test/current?city={city}&key={apiKey}");
        provider.setApiKey("dummy");
        provider.setMapping(mapping);

        properties.setProviders(java.util.List.of(provider));
        return properties;
    }
}
