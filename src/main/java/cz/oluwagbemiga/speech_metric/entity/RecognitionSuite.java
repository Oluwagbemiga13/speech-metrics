package cz.oluwagbemiga.speech_metric.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Entity
@Data
public class RecognitionSuite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToMany(mappedBy = "recognitionSuite", cascade = CascadeType.ALL)
    private List<RecognitionResult> recognitionResults;
}
