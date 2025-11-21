package cz.oluwagbemiga.speech_metric.engine;

import cz.oluwagbemiga.speech_metric.entity.AudioFile;
import lombok.extern.slf4j.Slf4j;

/**
 * Immutable container for a single speech recognition operation.
 * <p>
 * It bundles the audio to be transcribed together with an optional
 * expected (ground-truth) transcript used to compute accuracy metrics
 * (e.g. Character Error Rate) in {@link SpeechEngine} implementations.
 * </p>
 *
 * @param audioFile    audio file entity providing WAV bytes and metadata
 * @param expectedText optional expected transcript; may be null or blank
 */
@Slf4j
public record RecognitionRequest(AudioFile audioFile, String expectedText) {
    public RecognitionRequest {
        if (audioFile != null) {
            log.debug("RecognitionRequest created for audioFile={} expectedLength={}", audioFile.getId(), expectedText == null ? 0 : expectedText.length());
        } else {
            log.debug("RecognitionRequest created with null audioFile expectedLength={}", expectedText == null ? 0 : expectedText.length());
        }
    }
}
