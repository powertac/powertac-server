package org.powertac.visualizer.web.rest;

import com.codahale.metrics.annotation.Timed;

import org.powertac.visualizer.domain.User;
import org.powertac.visualizer.domain.View;
import org.powertac.visualizer.repository.UserRepository;
import org.powertac.visualizer.security.SecurityUtils;
import org.powertac.visualizer.service.ViewService;
import org.powertac.visualizer.web.rest.util.HeaderUtil;
import org.powertac.visualizer.web.rest.util.PaginationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.Valid;
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
        
    @Inject
    private ViewService viewService;

    @Inject
    private UserRepository userRepository;

    /**
     * POST  /views : Create a new view.
     *
     * @param view the view to create
     * @return the ResponseEntity with status 201 (Created) and with body the new view, or with status 400 (Bad Request) if the view has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/views",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<View> createView(@Valid @RequestBody View view) throws URISyntaxException {
        log.debug("REST request to save View : {}", view);
        if (view.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("view", "idexists", "A new view cannot already have an ID")).body(null);
        }

        String login = SecurityUtils.getCurrentUserLogin();
        User user = userRepository.findOneByLogin(login).orElse(null);

        view.setOwner(user);

        View result = viewService.save(view);
        return ResponseEntity.created(new URI("/api/views/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("view", result.getId().toString()))
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
    @RequestMapping(value = "/views",
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<View> updateView(@Valid @RequestBody View view) throws URISyntaxException {
        log.debug("REST request to update View : {}", view);
        if (view.getId() == null) {
            return createView(view);
        }
        View result = viewService.save(view);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("view", view.getId().toString()))
            .body(result);
    }

    /**
     * GET  /views : get all the views.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of views in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @RequestMapping(value = "/views",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
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
    @RequestMapping(value = "/views/{id}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<View> getView(@PathVariable Long id) {
        log.debug("REST request to get View : {}", id);
        View view = viewService.findOne(id);
        return Optional.ofNullable(view)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /views/:id : delete the "id" view.
     *
     * @param id the id of the view to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @RequestMapping(value = "/views/{id}",
        method = RequestMethod.DELETE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Void> deleteView(@PathVariable Long id) {
        log.debug("REST request to delete View : {}", id);
        viewService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("view", id.toString())).build();
    }

    /**
     * Get all views owned by logged in user, plus all shared views.
     */
    @RequestMapping(value = "/myviews",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<List<View>> getMyViews() throws URISyntaxException {
        log.debug("REST request to get owned and shared views");
        String login = SecurityUtils.getCurrentUserLogin();
        List<View> list = viewService.findByOwnerIsCurrentUserOrShared(login);
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

}
