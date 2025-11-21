package cz.oluwagbemiga.speech_metric.service;


import cz.oluwagbemiga.speech_metric.engine.SpeechEngine;
import cz.oluwagbemiga.speech_metric.exception.EngineNotFound;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service acting as a registry / factory for available {@link cz.oluwagbemiga.speech_metric.engine.SpeechEngine} implementations.
 * <p>Provides lookup by name and exposes lists of engines / engine names used for batch recognition.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EngineService {

    private final AudioFileService audioFileService;

    private final SpeechEngine voskLargeEngine;
    private final SpeechEngine voskSmallEngine;
    private final SpeechEngine whisperBaseEngine;
    //    private final SpeechEngine whisperLargeV3TurboQ5Engine; // Problematic engine - severe halucinations unable  to finish properly
    private final SpeechEngine whisperMediumEnQ5Engine;
    private final SpeechEngine whisperSmallQ51Engine;
    private final SpeechEngine whisperSmallQ8Engine;

    /**
     * Returns an engine by its externalized name.
     *
     * @param engineName configured engine key (e.g. "vosk-large")
     * @return matching {@link cz.oluwagbemiga.speech_metric.engine.SpeechEngine}
     * @throws cz.oluwagbemiga.speech_metric.exception.EngineNotFound if no engine mapped to name
     */
    public SpeechEngine getEngineByName(String engineName) {
        log.debug("Engine lookup requested name={}", engineName);
        try {
            return switch (engineName) {
                case "vosk-large" -> voskLargeEngine;
                case "vosk-small" -> voskSmallEngine;
                case "whisper-base" -> whisperBaseEngine;
                case "whisper-medium-en-q5" -> whisperMediumEnQ5Engine;
                case "whisper-small-q51" -> whisperSmallQ51Engine;
                case "whisper-small-q8" -> whisperSmallQ8Engine;
                default -> throw new EngineNotFound(engineName);
            };
        } catch (EngineNotFound ex) {
            log.warn("Engine not found name={}", engineName);
            throw ex;
        }
    }

    /**
     * All active engines available for recognition.
     *
     * @return immutable list of engines
     */
    public List<SpeechEngine> getAllEngines() {
        var list = List.of(
                voskLargeEngine,
                voskSmallEngine,
                whisperBaseEngine,
                whisperMediumEnQ5Engine,
                whisperSmallQ51Engine,
                whisperSmallQ8Engine
        );
        log.debug("Returning all engines count={}", list.size());
        return list;
    }

    /**
     * Names of all active engines (keys accepted by {@link #getEngineByName(String)}).
     *
     * @return immutable list of engine names
     */
    public List<String> getAllEngineNames() {
        var names = List.of(
                "vosk-large",
                "vosk-small",
                "whisper-base",
                "whisper-medium-en-q5",
                "whisper-small-q51",
                "whisper-small-q8"
        );
        log.trace("Returning engine names count={}", names.size());
        return names;
    }


}
