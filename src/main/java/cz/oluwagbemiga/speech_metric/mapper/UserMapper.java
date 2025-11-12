package cz.oluwagbemiga.speech_metric.mapper;

import cz.oluwagbemiga.speech_metric.dto.UserDTO;
import cz.oluwagbemiga.speech_metric.entity.User;
import org.mapstruct.Mapper;

/**
 * UserMapper interface for converting between User entity and UserDTO.
 * It extends GenericMapper to provide basic mapping functionality.
 */
@Mapper(componentModel = "spring")
public interface UserMapper extends GenericMapper<User, UserDTO> {

}