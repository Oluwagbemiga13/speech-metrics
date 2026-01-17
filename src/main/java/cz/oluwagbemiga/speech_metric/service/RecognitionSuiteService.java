package cz.oluwagbemiga.speech_metric.service;


import cz.oluwagbemiga.speech_metric.dto.RecognitionSuiteDTO;
import cz.oluwagbemiga.speech_metric.entity.RecognitionResult;
import cz.oluwagbemiga.speech_metric.entity.RecognitionSuite;
import cz.oluwagbemiga.speech_metric.entity.Role;
import cz.oluwagbemiga.speech_metric.exception.NotAuthorizedException;
import cz.oluwagbemiga.speech_metric.repository.RecognitionSuiteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Service handling persistence and aggregation of {@link cz.oluwagbemiga.speech_metric.entity.RecognitionSuite}.
 * A suite groups multiple {@link cz.oluwagbemiga.speech_metric.entity.RecognitionResult} instances produced
 * across engines or batch runs for later comparative analysis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecognitionSuiteService {

    private final RecognitionSuiteRepository recognitionSuiteRepository;


    /**
     * Get suite by id.
     *
     * @param id suite UUID
     * @return found suite
     * @throws RuntimeException if not found (consider custom exception later)
     */
    public RecognitionSuite getById(UUID id) {
        if(SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(Role.ADMIN.getAuthority())){
            log.trace("Fetch RecognitionSuite id={}", id);
            return recognitionSuiteRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("RecognitionSuite not found with id: " + id));
        }
        String principal = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        RecognitionSuite recognitionSuite = recognitionSuiteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("RecognitionSuite not found with id: " + id));
        if(recognitionSuite.getOwner().getId().toString().equals(principal)){
            log.trace("Fetch RecognitionSuite id={}", id);
            return recognitionSuite;
        }
        throw new NotAuthorizedException("Not authorized to access RecognitionSuite with id: " + id);
    }

    /**
     * Create and persist a new suite populated with initial results.
     *
     * @param results recognition results to associate
     * @return DTO representation of persisted suite
     */
    public RecognitionSuiteDTO save(List<RecognitionResult> results) {
        log.debug("Create new RecognitionSuite resultsCount={}", results == null ? 0 : results.size());
        RecognitionSuite suite = new RecognitionSuite();
        suite.setRecognitionResults(results);
        return new RecognitionSuiteDTO(recognitionSuiteRepository.save(suite));
    }


    /**
     * Retrieve all RecognitionSuites in the database.
     *
     * @return list of suite DTOs
     */
    public List<RecognitionSuiteDTO> getAllSuites() {
        if(SecurityContextHolder.getContext().getAuthentication().getAuthorities().toString().contains(Role.ADMIN.getAuthority())){
            log.trace("Fetch all RecognitionSuites");
            return recognitionSuiteRepository.findAll().stream()
                    .map(RecognitionSuiteDTO::new)
                    .toList();
        }
        else{
            return getSuitesByOwner();
        }
    }

    /**
     * Retrieve all RecognitionSuites owned by a specific user.
     *
     *
     * @return list of suite DTOs for the owner
     */
    public List<RecognitionSuiteDTO> getSuitesByOwner() {
        String principal = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.trace("Fetch RecognitionSuites ownerId={}", principal);
        return recognitionSuiteRepository.findAllByOwner_Id(UUID.fromString(principal)).stream()
                .map(RecognitionSuiteDTO::new)
                .toList();

    }

    public RecognitionSuiteDTO getSuiteDTOById(UUID id) {
        return new RecognitionSuiteDTO(getById(id));
    }

    /**
     * Retrieve all RecognitionSuites owned by a specific user (admin access).
     *
     * @param userId the user's UUID
     * @return list of suite DTOs for the specified user
     */
    public List<RecognitionSuiteDTO> getSuitesByUserId(UUID userId) {
        if (userId == null) {
            log.warn("getSuitesByUserId called with null userId");
            return List.of();
        }
        log.trace("Fetch RecognitionSuites for userId={}", userId);
        return recognitionSuiteRepository.findAllByOwner_Id(userId).stream()
                .map(RecognitionSuiteDTO::new)
                .toList();
    }
}
