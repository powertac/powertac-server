package org.powertac.visualizer.service;

import org.powertac.visualizer.domain.File;
import org.powertac.visualizer.domain.enumeration.FileType;
import org.powertac.visualizer.repository.FileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

import javax.inject.Inject;

/**
 * Service Implementation for managing File.
 */
@Service
@Transactional
public class FileService {

    private final Logger log = LoggerFactory.getLogger(FileService.class);
    
    @Inject
    private FileRepository fileRepository;
    
    /**
     * Save a file.
     * 
     * @param file the entity to save
     * @return the persisted entity
     */
    public File save(File file) {
        log.debug("Request to save File : {}", file);
        File result = fileRepository.save(file);
        return result;
    }

    /**
     *  Get all the files.
     *  
     *  @param pageable the pagination information
     *  @return the list of entities
     */
    @Transactional(readOnly = true) 
    public Page<File> findAll(Pageable pageable) {
        log.debug("Request to get all Files");
        Page<File> result = fileRepository.findAll(pageable); 
        return result;
    }

    /**
     *  Get all the files owned by this user, plus all shared files.
     *  
     *  @return the list of entities
     */
    @Transactional(readOnly = true) 
    public List<File> findByOwnerIsCurrentUserOrShared(String login, FileType type) {
        log.debug("Request to get all owned and shared Files");
        if (type != null && type.equals(FileType.ANY)) {
            type = null;
        }
        List<File> result = fileRepository.findByOwnerIsCurrentUserOrShared(login, type);
        return result;
    }

    /**
     *  Get one file by id.
     *
     *  @param id the id of the entity
     *  @return the entity
     */
    @Transactional(readOnly = true) 
    public File findOne(Long id) {
        log.debug("Request to get File : {}", id);
        File file = fileRepository.findOne(id);
        return file;
    }

    /**
     *  Delete the  file by id.
     *  
     *  @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete File : {}", id);
        File file = fileRepository.findOne(id);
        fileRepository.delete(id);
        file.delete();
    }
}
