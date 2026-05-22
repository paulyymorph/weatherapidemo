package dev.paulymorph.weatherdemo.service;

import dev.paulymorph.weatherdemo.dto.WeatherResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class ExternalWeatherServiceImpl implements WeatherService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String providerName;
    private final String urlTemplate;

    private final String temperaturePointer;

    private final String windSpeedPointer;

    public ExternalWeatherServiceImpl(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            String providerName,
            String urlTemplate,
            String temperaturePointer,
            String windSpeedPointer) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.providerName = providerName;
        this.urlTemplate = urlTemplate;
        this.temperaturePointer = temperaturePointer;
        this.windSpeedPointer = windSpeedPointer;
    }

    private String getUrl(String city) {
        String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
        return String.format(urlTemplate, encodedCity);
    }

    @Override
    public WeatherResponse getWeatherForCity(String city) {
        String url = getUrl(city);

        try {
            String responseBody = restTemplate.getForObject(url, String.class);
            if (responseBody == null || responseBody.isBlank()) {
                throw new WeatherProviderUnavailableException(
                        "Provider " + providerName + " returned an empty response");
            }

            JsonNode response = parseResponse(responseBody);

            return new WeatherResponse(
                    extractInteger(response, windSpeedPointer, "wind speed"),
                    extractInteger(response, temperaturePointer, "temperature"));
        } catch (RestClientException e) {
            throw new WeatherProviderUnavailableException(
                    "Provider " + providerName + " failed for city: " + city,
                    e);
        }
    }

    private JsonNode parseResponse(String responseBody) {
        try {
            return objectMapper.readTree(responseBody);
        } catch (Exception e) {
            throw new WeatherProviderUnavailableException(
                    "Provider " + providerName + " returned invalid JSON",
                    e);
        }
    }

    private Integer extractInteger(JsonNode response, String pointer, String fieldName) {
        JsonNode valueNode = response.at(pointer);

        if (valueNode.isMissingNode() || valueNode.isNull()) {
            throw new WeatherProviderUnavailableException(
                    "Provider " + providerName + " response is missing " + fieldName + " at mapping " + pointer);
        }

        try {
            return (int) Math.round(Double.parseDouble(valueNode.asText()));
        } catch (NumberFormatException e) {
            throw new WeatherProviderUnavailableException(
                    "Provider " + providerName + " returned invalid " + fieldName + " at mapping " + pointer,
                    e);
        }
    }

    public String getProviderName() {
        return providerName;
    }
}
