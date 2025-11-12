package cz.oluwagbemiga.speech_metric.mapper;

import java.util.List;

/**
 * GenericMapper interface for converting between entity and DTO objects.
 *
 * @param <E> the type of the entity
 * @param <D> the type of the DTO
 */
public interface GenericMapper<E, D> {

    E toEntity(D dto);

    List<E> toEntity(List<D> dtoList);

    D toDto(E entity);

    List<D> toDto(List<E> entityList);
}
