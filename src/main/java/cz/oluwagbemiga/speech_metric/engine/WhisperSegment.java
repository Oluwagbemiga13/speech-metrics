package cz.oluwagbemiga.speech_metric.engine;


import lombok.Getter;
import lombok.Setter;

/**
 * Created by litonglinux@qq.com on 10/21/2023_7:48 AM
 */
@Setter
@Getter
public class WhisperSegment {
    private long start, end;
    private String sentence;

    public WhisperSegment() {
    }

    public WhisperSegment(long start, long end, String sentence) {
        this.start = start;
        this.end = end;
        this.sentence = sentence;
    }

    @Override
    public String toString() {
        return "[" + start + " --> " + end + "]:" + sentence;
    }
}
