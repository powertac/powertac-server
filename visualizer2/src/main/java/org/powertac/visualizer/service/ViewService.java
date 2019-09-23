package org.powertac.visualizer.service;

import org.powertac.visualizer.domain.View;
import org.powertac.visualizer.repository.ViewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service Implementation for managing View.
 */
@Service
@Transactional
public class ViewService {

    private final Logger log = LoggerFactory.getLogger(ViewService.class);

    private final ViewRepository viewRepository;

    public ViewService(ViewRepository viewRepository) {
        this.viewRepository = viewRepository;
    }

    /**
     * Save a view.
     *
     * @param view the entity to save
     * @return the persisted entity
     */
    public View save(View view) {
        log.debug("Request to save View : {}", view);
        View result = viewRepository.save(view);
        return result;
    }

    /**
     *  Get all the views.
     *
     *  @param pageable the pagination information
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    public Page<View> findAll(Pageable pageable) {
        log.debug("Request to get all Views");
        Page<View> result = viewRepository.findAll(pageable);
        return result;
    }

    /**
     *  Get all the views owned by this user, plus all shared views.
     *
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    public List<View> findByOwnerIsCurrentUserOrShared(String login) {
        log.debug("Request to get all owned and shared Graphs");
        List<View> result = viewRepository.findByOwnerIsCurrentUserOrShared(login);
        return result;
    }

    /**
     *  Get one view by id.
     *
     *  @param id the id of the entity
     *  @return the entity
     */
    @Transactional(readOnly = true)
    public Optional<View> findOne(Long id) {
        log.debug("Request to get View : {}", id);
        return viewRepository.findById(id);
    }

    /**
     *  Delete the view.
     *
     *  @param view the View to delete
     */
    public void delete(View view) {
        log.debug("Request to delete View : {}", view);
        viewRepository.delete(view);
    }
}
