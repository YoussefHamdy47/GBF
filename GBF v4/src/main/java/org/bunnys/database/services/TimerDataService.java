package org.bunnys.database.services;

import org.bunnys.database.models.timer.TimerData;
import org.bunnys.database.repositories.TimerDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@SuppressWarnings("unused")
public class TimerDataService {

    private static final Logger logger = LoggerFactory.getLogger(TimerDataService.class);
    private final TimerDataRepository timerDataRepository;

    @Autowired
    public TimerDataService(TimerDataRepository timerDataRepository) {
        this.timerDataRepository = timerDataRepository;
    }

    @Transactional
    @CacheEvict(value = "timerData", key = "#timerData.id")
    public TimerData save(TimerData timerData) {
        if (timerData == null)
            throw new IllegalArgumentException("Timer data cannot be null");

        try {
            logger.debug("Saving timer data with ID: {}. Semester subjects: {}",
                    timerData.getId(),
                    timerData.getCurrentSemester().getSemesterSubjects());

            TimerData savedData = timerDataRepository.save(timerData);
            logger.debug("Successfully saved timer data with ID: {}. Updated subjects: {}",
                    savedData.getId(),
                    savedData.getCurrentSemester().getSemesterSubjects());
            return savedData;
        } catch (Exception e) {
            logger.error("Error saving timer data", e);
            throw new RuntimeException("Failed to save timer data", e);
        }
    }

    @Cacheable(value = "timerData", key = "#id")
    @Transactional(readOnly = true)
    public Optional<TimerData> findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            return timerDataRepository.findById(id);
        } catch (Exception e) {
            logger.error("Error finding timer data with ID: {}", id, e);
            return Optional.empty();
        }
    }

    @Transactional(readOnly = true)
    public List<TimerData> findAll() {
        try {
            return timerDataRepository.findAll();
        } catch (Exception e) {
            logger.error("Error retrieving all timer data", e);
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public boolean existsById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }

        try {
            return timerDataRepository.existsById(id);
        } catch (Exception e) {
            logger.error("Error checking existence of timer data with ID: {}", id, e);
            return false;
        }
    }

    @Transactional
    @CacheEvict(value = "timerData", key = "#id")
    public void deleteById(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID cannot be null or empty");
        }

        try {
            if (timerDataRepository.existsById(id)) {
                timerDataRepository.deleteById(id);
                logger.info("Successfully deleted timer data with ID: {}", id);
            } else {
                logger.warn("Attempted to delete non-existent timer data with ID: {}", id);
            }
        } catch (Exception e) {
            logger.error("Error deleting timer data with ID: {}", id, e);
            throw new RuntimeException("Failed to delete timer data", e);
        }
    }

    @Transactional
    @CacheEvict(value = "timerData", allEntries = true)
    public void deleteAll() {
        try {
            timerDataRepository.deleteAll();
            logger.info("Successfully deleted all timer data");
        } catch (Exception e) {
            logger.error("Error deleting all timer data", e);
            throw new RuntimeException("Failed to delete all timer data", e);
        }
    }
}