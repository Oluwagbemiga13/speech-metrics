package cz.oluwagbemiga.speech_metric.mapper;

import cz.oluwagbemiga.speech_metric.dto.AudioFileDTO;
import cz.oluwagbemiga.speech_metric.entity.AudioFile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AudioFileMapper extends GenericMapper<AudioFile, AudioFileDTO> {

    @Override
    @Mapping(target = "data", expression = "java(entity.getData() == null ? null : entity.getData().clone())")
    AudioFileDTO toDto(AudioFile entity);
}

