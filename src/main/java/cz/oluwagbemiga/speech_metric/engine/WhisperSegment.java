package cz.oluwagbemiga.speech_metric.engine;


import lombok.Getter;
import lombok.Setter;

/**
 * Represents a single temporal segment produced during Whisper transcription.
 * <p>
 * Each segment covers an inclusive start time and exclusive end time (in milliseconds
 * as emitted by the underlying library) and the recognized sentence text for that span.
 * Instances are simple data carriers used for downstream formatting or analytics.
 * </p>
 */
@Setter
@Getter
public class WhisperSegment {
    /**
     * Start timestamp of the segment in milliseconds.
     */
    private long start;
    /**
     * End timestamp of the segment in milliseconds.
     */
    private long end;
    /**
     * Recognized sentence text for the time span.
     */
    private String sentence;

    /**
     * Default no-arg constructor.
     */
    public WhisperSegment() {
    }

    /**
     * Constructs a segment with explicit timing and text.
     *
     * @param start    start timestamp (ms)
     * @param end      end timestamp (ms)
     * @param sentence recognized text
     */
    public WhisperSegment(long start, long end, String sentence) {
        this.start = start;
        this.end = end;
        this.sentence = sentence;
    }

    /**
     * Human readable representation including timing and text.
     *
     * @return formatted string
     */
    @Override
    public String toString() {
        return "[" + start + " --> " + end + "]:" + sentence;
    }
}
