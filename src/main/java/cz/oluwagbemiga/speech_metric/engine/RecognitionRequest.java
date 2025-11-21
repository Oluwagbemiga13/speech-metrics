package cz.oluwagbemiga.speech_metric.engine;

import cz.oluwagbemiga.speech_metric.entity.AudioFile;

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
public record RecognitionRequest(AudioFile audioFile, String expectedText) {
}
