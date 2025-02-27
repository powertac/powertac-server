package org.powertac.visualizer.web.rest;

import com.codahale.metrics.annotation.Timed;

import org.powertac.visualizer.domain.User;
import org.powertac.visualizer.domain.View;
import org.powertac.visualizer.repository.UserRepository;
import org.powertac.visualizer.security.SecurityUtils;
import org.powertac.visualizer.service.ViewService;
import org.powertac.visualizer.web.rest.util.HeaderUtil;
import org.powertac.visualizer.web.rest.util.PaginationUtil;

import io.github.jhipster.web.util.ResponseUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing View.
 */
@RestController
@RequestMapping("/api")
public class ViewResource {

    private final Logger log = LoggerFactory.getLogger(ViewResource.class);

    private static final String ENTITY_NAME = "view";

    private final ViewService viewService;
    private final UserRepository userRepository;

    public ViewResource(ViewService viewService, UserRepository userRepository) {
        this.viewService = viewService;
        this.userRepository = userRepository;

    }

    /**
     * POST  /views : Create a new view.
     *
     * @param view the view to create
     * @return the ResponseEntity with status 201 (Created) and with body the new view, or with status 400 (Bad Request) if the view has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/views")
    @Timed
    public ResponseEntity<View> createView(@Valid @RequestBody View view) throws URISyntaxException {
        log.debug("REST request to save View : {}", view);
        if (view.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert(ENTITY_NAME, "idexists", "A new view cannot already have an ID")).body(null);
        }

        String login = SecurityUtils.getCurrentUserLogin();
        User user = userRepository.findOneByLogin(login).orElse(null);

        view.setOwner(user);

        View result = viewService.save(view);
        return ResponseEntity.created(new URI("/api/views/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /views : Updates an existing view.
     *
     * @param view the view to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated view,
     * or with status 400 (Bad Request) if the view is not valid,
     * or with status 500 (Internal Server Error) if the view couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/views")
    @Timed
    public ResponseEntity<View> updateView(@Valid @RequestBody View view) throws URISyntaxException {
        log.debug("REST request to update View : {}", view);
        if (view.getId() == null) {
            return createView(view);
        }
        View result = viewService.save(view);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, view.getId().toString()))
            .body(result);
    }

    /**
     * GET  /views : get all the views.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of views in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @GetMapping("/views")
    @Timed
    public ResponseEntity<List<View>> getAllViews(Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of Views");
        Page<View> page = viewService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/views");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /views/:id : get the "id" view.
     *
     * @param id the id of the view to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the view, or with status 404 (Not Found)
     */
    @GetMapping("/views/{id}")
    @Timed
    public ResponseEntity<View> getView(@PathVariable Long id) {
        log.debug("REST request to get View : {}", id);
        Optional<View> view = viewService.findOne(id);
        return ResponseUtil.wrapOrNotFound(view);
    }

    /**
     * DELETE  /views/:id : delete the "id" view.
     *
     * @param id the id of the view to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/views/{id}")
    @Timed
    public ResponseEntity<Void> deleteView(@PathVariable Long id) {
        log.debug("REST request to delete View : {}", id);
        Optional<View> view = viewService.findOne(id);
        view.ifPresent(viewService::delete);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

    /**
     * Get all views owned by logged in user, plus all shared views.
     */
    @GetMapping(value = "/myviews")
    @Timed
    public ResponseEntity<List<View>> getMyViews() throws URISyntaxException {
        log.debug("REST request to get owned and shared views");
        String login = SecurityUtils.getCurrentUserLogin();
        List<View> list = viewService.findByOwnerIsCurrentUserOrShared(login);
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

}
