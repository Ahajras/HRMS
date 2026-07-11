package com.hrms.benefits.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrms.benefits.dto.TicketDtos;
import com.hrms.common.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class TicketFareProviderService {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String amadeusBaseUrl;
    private final String amadeusClientId;
    private final String amadeusClientSecret;

    public TicketFareProviderService(RestClient.Builder restClientBuilder,
                                     ObjectMapper objectMapper,
                                     @Value("${AMADEUS_API_BASE_URL:https://test.api.amadeus.com}") String amadeusBaseUrl,
                                     @Value("${AMADEUS_CLIENT_ID:}") String amadeusClientId,
                                     @Value("${AMADEUS_CLIENT_SECRET:}") String amadeusClientSecret) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.amadeusBaseUrl = amadeusBaseUrl;
        this.amadeusClientId = amadeusClientId;
        this.amadeusClientSecret = amadeusClientSecret;
    }

    public ProviderFare lookup(TicketDtos.FareLookupRequest request) {
        String provider = request.getProvider() == null ? "AMADEUS" : request.getProvider().trim().toUpperCase();
        if (!"AMADEUS".equals(provider)) {
            throw new BusinessRuleException("ticket.provider.unsupported", "Unsupported ticket fare provider: " + provider);
        }
        return lookupAmadeus(request);
    }

    private ProviderFare lookupAmadeus(TicketDtos.FareLookupRequest request) {
        if (amadeusClientId.isBlank() || amadeusClientSecret.isBlank()) {
            throw new BusinessRuleException("ticket.provider.credentials",
                    "Amadeus API credentials are not configured. Set AMADEUS_CLIENT_ID and AMADEUS_CLIENT_SECRET.");
        }
        String token = amadeusToken();
        LocalDate departureDate = request.getDepartureDate() == null ? LocalDate.now().plusMonths(1) : request.getDepartureDate();
        String body = restClient.get()
                .uri(amadeusBaseUrl + "/v2/shopping/flight-offers?originLocationCode={from}&destinationLocationCode={to}&departureDate={date}&adults=1&nonStop=false&max=20&currencyCode={currency}",
                        request.getFromAirportCode(), request.getToAirportCode(), departureDate, currency(request.getCurrencyCode()))
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(String.class);
        try {
            JsonNode data = objectMapper.readTree(body).path("data");
            if (!data.isArray() || data.isEmpty()) {
                throw new BusinessRuleException("ticket.provider.no_fare",
                        "No flight fare found for route " + request.getFromAirportCode() + " -> " + request.getToAirportCode() + ".");
            }
            JsonNode cheapest = null;
            for (JsonNode item : data) {
                if (cheapest == null || total(item).compareTo(total(cheapest)) < 0) {
                    cheapest = item;
                }
            }
            if (cheapest == null) {
                throw new BusinessRuleException("ticket.provider.no_fare",
                        "No flight fare found for route " + request.getFromAirportCode() + " -> " + request.getToAirportCode() + ".");
            }
            String offerId = cheapest.path("id").asText(null);
            JsonNode price = cheapest.path("price");
            return new ProviderFare(total(cheapest), price.path("currency").asText(currency(request.getCurrencyCode())), "AMADEUS", offerId);
        } catch (BusinessRuleException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessRuleException("ticket.provider.response", "Unable to parse ticket fare provider response.");
        }
    }

    private String amadeusToken() {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", amadeusClientId);
        form.add("client_secret", amadeusClientSecret);
        String body = restClient.post()
                .uri(amadeusBaseUrl + "/v1/security/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(String.class);
        try {
            String token = objectMapper.readTree(body).path("access_token").asText();
            if (token == null || token.isBlank()) {
                throw new BusinessRuleException("ticket.provider.auth", "Ticket fare provider did not return an access token.");
            }
            return token;
        } catch (BusinessRuleException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessRuleException("ticket.provider.auth", "Unable to authenticate with ticket fare provider.");
        }
    }

    private BigDecimal total(JsonNode offer) {
        String value = offer.path("price").path("grandTotal").asText(offer.path("price").path("total").asText("0"));
        return new BigDecimal(value);
    }

    private String currency(String value) {
        return value == null || value.isBlank() ? "QAR" : value.trim().toUpperCase();
    }

    public record ProviderFare(BigDecimal amount, String currencyCode, String provider, String providerOfferId) {}
}
