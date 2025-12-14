package cz.oluwagbemiga.speech_metric.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String secretKey;
}