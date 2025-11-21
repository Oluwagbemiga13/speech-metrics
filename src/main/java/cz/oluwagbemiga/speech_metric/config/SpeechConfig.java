package cz.oluwagbemiga.speech_metric.config;

import cz.oluwagbemiga.speech_metric.engine.VoskEngine;
import cz.oluwagbemiga.speech_metric.engine.WhisperEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class SpeechConfig {

    @Value("${speech.vosk.large-model-path:/app/models/vosk-model-en-us-0.22-lgraph}")
    private String largeModelPathCfg;

    @Value("${speech.vosk.small-model-path:/app/models/vosk-model-small-en-us-0.15}")
    private String smallModelPathCfg;

    // Whisper base model path (default container path, fallback to source tree)
    @Value("${speech.whisper.base-model-path:/app/models/ggml-base.en.bin}")
    private String whisperBaseModelPathCfg;

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

    private String resolveFile(String configured, String fallbackRelative) {
        Path cfg = Path.of(configured);
        if (Files.isRegularFile(cfg)) {
            return cfg.toString();
        }
        // fallback to source tree under user.dir (IDE/dev convenience)
        Path fallback = Path.of(System.getProperty("user.dir"), fallbackRelative);
        if (Files.isRegularFile(fallback)) {
            return fallback.toString();
        }
        // last resort: return configured (WhisperEngine will throw a clear error)
        return fallbackRelative; // TODO: FIX it should not return fallbackRelative but configured'
    }


    @Bean(name = "voskLargeEngine")
    public VoskEngine voskLargeEngine() {
        String resolved = resolvePath(largeModelPathCfg, "src/main/resources/model/vosk-model-en-us-0.22-lgraph");
        return new VoskEngine(resolved);
    }

    @Bean(name = "voskSmallEngine")
    public VoskEngine voskSmallEngine() {
        String resolved = resolvePath(smallModelPathCfg, "src/main/resources/model/vosk-model-small-en-us-0.15");
        return new VoskEngine(resolved);
    }

    @Bean(name = "whisperBaseEngine")
    public WhisperEngine whisperBaseEngine() {
        String resolved = resolveFile(whisperBaseModelPathCfg, "src/main/resources/model/ggml-base.en.bin");
        return new WhisperEngine(resolved);
    }

    @Bean(name = "whisperLargeV3TurboQ5Engine")
    public WhisperEngine whisperLargeV3TurboQ5Engine() {
        String resolved = resolveFile(whisperBaseModelPathCfg, "src/main/resources/model/ggml-large-v3-turbo-q5_0.bin");
        return new WhisperEngine(resolved);
    }

    @Bean(name = "whisperMediumEnQ5Engine")
    public WhisperEngine whisperMediumEnQ5Engine() {
        String resolved = resolveFile(whisperBaseModelPathCfg, "src/main/resources/model/ggml-medium.en-q5_0.bin");
        return new WhisperEngine(resolved);
    }

    @Bean(name = "whisperSmallQ51Engine")
    public WhisperEngine whisperSmallQ51Engine() {
        String resolved = resolveFile(whisperBaseModelPathCfg, "src/main/resources/model/ggml-small.en-q5_1.bin");
        return new WhisperEngine(resolved);
    }

    @Bean(name = "whisperSmallQ8Engine")
    public WhisperEngine whisperSmallQ8Engine() {
        String resolved = resolveFile(whisperBaseModelPathCfg, "src/main/resources/model/ggml-small.en-q8_0.bin");
        return new WhisperEngine(resolved);
    }

}


