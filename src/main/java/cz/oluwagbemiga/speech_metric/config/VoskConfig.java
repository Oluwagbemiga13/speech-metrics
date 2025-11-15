package cz.oluwagbemiga.speech_metric.config;

import cz.oluwagbemiga.speech_metric.engine.VoskEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class VoskConfig {

    @Value("${speech.vosk.large-model-path:/app/models/vosk-model-en-us-0.22-lgraph}")
    private String largeModelPathCfg;

    @Value("${speech.vosk.small-model-path:/app/models/vosk-model-small-en-us-0.15}")
    private String smallModelPathCfg;

    private String resolvePath(String configured, String fallbackRelative) {
        Path cfg = Path.of(configured);
        if (Files.isDirectory(cfg)) {
            return cfg.toString();
        }
        // fallback to source tree under user.dir (IDE/dev convenience)
        Path fallback = Path.of(System.getProperty("user.dir"), fallbackRelative);
        if (Files.isDirectory(fallback)) {
            return fallback.toString();
        }
        // last resort: return configured (VoskEngine will throw a clear error)
        return configured;
    }

    @Bean(name = "voskLargeEngine")
    public VoskEngine voskLargeEngine() {
        String resolved = resolvePath(largeModelPathCfg, "src/main/resources/model/vosk-model-en-us-0.22-lgraph");
        return new VoskEngine("vosk-large", resolved);
    }

    @Bean(name = "voskSmallEngine")
    public VoskEngine voskSmallEngine() {
        String resolved = resolvePath(smallModelPathCfg, "src/main/resources/model/vosk-model-small-en-us-0.15");
        return new VoskEngine("vosk-small", resolved);
    }
}
