package cz.oluwagbemiga.speech_metric.repository;

import cz.oluwagbemiga.speech_metric.entity.AudioFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AudioFileRepository extends JpaRepository<AudioFile, UUID> {

    @Modifying
    @Query("delete from AudioFile a where a.id = :id")
    int deleteByUUID(@Param("id") UUID id);

    // List all audio files owned by a given user
    List<AudioFile> findAllByOwner_Id(UUID ownerId);
}
