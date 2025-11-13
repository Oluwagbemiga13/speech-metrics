package cz.oluwagbemiga.speech_metric.mapper;

import cz.oluwagbemiga.speech_metric.dto.AudioFileDto;
import cz.oluwagbemiga.speech_metric.entity.AudioFile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AudioFileMapper extends GenericMapper<AudioFile, AudioFileDto> {

    @Override
    @Mapping(target = "data", expression = "java(entity.getData() == null ? null : entity.getData().clone())")
    AudioFileDto toDto(AudioFile entity);
}

