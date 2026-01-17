package cz.oluwagbemiga.speech_metric.mapper;

import cz.oluwagbemiga.speech_metric.dto.UserDTO;
import cz.oluwagbemiga.speech_metric.entity.AudioFile;
import cz.oluwagbemiga.speech_metric.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.UUID;

/**
 * UserMapper interface for converting between User entity and UserDTO.
 * It extends GenericMapper to provide basic mapping functionality.
 */
@Mapper(componentModel = "spring")
public interface UserMapper extends GenericMapper<User, UserDTO> {

    // Explicitly map id and audioFiles -> audioFileIds using helper to avoid any implicit conversions
    @Override
    @Mapping(target = "id", source = "id")
    @Mapping(target = "audioFileIds", expression = "java(mapAudioIds(entity))")
    UserDTO toDto(User entity);

    // Helper to extract audio file IDs
    default List<UUID> mapAudioIds(User user) {
        if (user == null || user.getAudioFiles() == null) return List.of();
        return user.getAudioFiles().stream()
                .map(AudioFile::getId)
                .toList();
    }
}