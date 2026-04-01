package com.rental.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.util.HashMap;
import java.util.Map;

@Service
public class FptAiService {

    @Value("${fpt.ai.apiKey:pyMNld3jHWF3CZLsaFRT1N5fbdGDEblt}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> scanCccd(MultipartFile file) throws Exception {
        return scanDocument(file, "https://api.fpt.ai/vision/idr/vnm");
    }

    public Map<String, Object> scanDriverLicense(MultipartFile file) throws Exception {
        String url = "https://api.fpt.ai/vision/dlr/vnm";
        JsonNode root = sendRequest(file, url);
        
        if (root.has("errorCode") && root.get("errorCode").asInt() != 0) {
            throw new RuntimeException("Lỗi từ FPT AI: " + root.get("errorMessage").asText());
        }

        JsonNode data = root.get("data").isArray() ? root.get("data").get(0) : root.get("data");
        Map<String, Object> result = new HashMap<>();

        if (data != null) {
            result.put("driverLicense", data.has("id") ? data.get("id").asText() : "");
            result.put("licenseClass", data.has("class") ? data.get("class").asText() : "");
            result.put("fullName", data.has("name") ? data.get("name").asText() : "");
            result.put("dateOfBirth", data.has("dob") ? data.get("dob").asText() : "");
            
            // FPT AI returns 'date' for issue date and 'due_date' for expiry
            String issueDate = data.has("date") ? data.get("date").asText() : "";
            result.put("issueDate", issueDate);
            
            String dueDate = data.has("due_date") ? data.get("due_date").asText() : "";
            boolean isUnlimited = "Không thời hạn".equalsIgnoreCase(dueDate);
            result.put("expiryDate", isUnlimited ? "" : dueDate);
            result.put("isUnlimited", isUnlimited ? "true" : "false");
        }
        
        return result;
    }

    private Map<String, Object> scanDocument(MultipartFile file, String url) throws Exception {
        JsonNode root = sendRequest(file, url);
        
        if (root.has("errorCode") && root.get("errorCode").asInt() != 0) {
            throw new RuntimeException("Lỗi từ FPT AI: " + root.get("errorMessage").asText());
        }

        JsonNode data = root.get("data").isArray() ? root.get("data").get(0) : root.get("data");
        Map<String, Object> result = new HashMap<>();

        if (data != null) {
            result.put("identityCard", data.has("id") ? data.get("id").asText() : "");
            result.put("fullName", data.has("name") ? data.get("name").asText() : "");
            result.put("dateOfBirth", data.has("dob") ? data.get("dob").asText() : "");
            // Some versions use 'address', others use 'address_entities'
            result.put("address", data.has("address") ? data.get("address").asText() : "");
            
            // Issue date is usually 'issue_date' or 'date', expiry is 'expiry' or 'due_date'
            result.put("identityCardIssueDate", data.has("issue_date") ? data.get("issue_date").asText() : "");
            result.put("identityCardExpiry", data.has("expiry") ? data.get("expiry").asText() : "");
        }
        return result;
    }

    private JsonNode sendRequest(MultipartFile file, String url) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("api-key", apiKey);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        });

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
        return objectMapper.readTree(response.getBody());
    }
}
