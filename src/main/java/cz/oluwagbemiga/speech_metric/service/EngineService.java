package cz.oluwagbemiga.speech_metric.service;


import cz.oluwagbemiga.speech_metric.engine.SpeechEngine;
import cz.oluwagbemiga.speech_metric.exception.EngineNotFound;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EngineService {

    private final AudioFileService audioFileService;

    private final SpeechEngine voskLargeEngine;
    private final SpeechEngine voskSmallEngine;
    private final SpeechEngine whisperBaseEngine;
    //    private final SpeechEngine whisperLargeV3TurboQ5Engine; // Problematic engine - severe halucinations unable  to finish properly
    private final SpeechEngine whisperMediumEnQ5Engine;
    private final SpeechEngine whisperSmallQ51Engine;
    private final SpeechEngine whisperSmallQ8Engine;

    public SpeechEngine getEngineByName(String engineName) {
        return switch (engineName) {
            case "vosk-large" -> voskLargeEngine;
            case "vosk-small" -> voskSmallEngine;
            case "whisper-base" -> whisperBaseEngine;
//            case "whisper-large-v3-turbo-q5" -> whisperLargeV3TurboQ5Engine;
            case "whisper-medium-en-q5" -> whisperMediumEnQ5Engine;
            case "whisper-small-q51" -> whisperSmallQ51Engine;
            case "whisper-small-q8" -> whisperSmallQ8Engine;
            default -> throw new EngineNotFound(engineName);
        };
    }

    public List<SpeechEngine> getAllEngines() {
        return List.of(
                voskLargeEngine,
                voskSmallEngine,
                whisperBaseEngine,
//                whisperLargeV3TurboQ5Engine,
                whisperMediumEnQ5Engine,
                whisperSmallQ51Engine,
                whisperSmallQ8Engine
        );
    }

    public List<String> getAllEngineNames() {
        return List.of(
                "vosk-large",
                "vosk-small",
                "whisper-base",
//                "whisper-large-v3-turbo-q5",
                "whisper-medium-en-q5",
                "whisper-small-q51",
                "whisper-small-q8"
        );
    }


}
