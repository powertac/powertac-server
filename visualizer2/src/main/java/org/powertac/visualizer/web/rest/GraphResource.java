package org.powertac.visualizer.web.rest;

import com.codahale.metrics.annotation.Timed;
import org.powertac.visualizer.domain.Graph;
import org.powertac.visualizer.domain.User;
import org.powertac.visualizer.repository.UserRepository;
import org.powertac.visualizer.security.SecurityUtils;
import org.powertac.visualizer.service.GraphService;
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
 * REST controller for managing Graph.
 */
@RestController
@RequestMapping("/api")
public class GraphResource {

    private final Logger log = LoggerFactory.getLogger(GraphResource.class);

    private static final String ENTITY_NAME = "graph";

    private final GraphService graphService;
    private final UserRepository userRepository;

    public GraphResource(GraphService graphService, UserRepository userRepository) {
        this.graphService = graphService;
        this.userRepository = userRepository;
    }

    /**
     * POST  /graphs : Create a new graph.
     *
     * @param graph the graph to create
     * @return the ResponseEntity with status 201 (Created) and with body the new graph, or with status 400 (Bad Request) if the graph has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/graphs")
    @Timed
    public ResponseEntity<Graph> createGraph(@Valid @RequestBody Graph graph) throws URISyntaxException {
        log.debug("REST request to save Graph : {}", graph);
        if (graph.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert(ENTITY_NAME, "idexists", "A new graph cannot already have an ID")).body(null);
        }

        String login = SecurityUtils.getCurrentUserLogin();
        User user = userRepository.findOneByLogin(login).orElse(null);

        graph.setOwner(user);

        Graph result = graphService.save(graph);
        return ResponseEntity.created(new URI("/api/graphs/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /graphs : Updates an existing graph.
     *
     * @param graph the graph to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated graph,
     * or with status 400 (Bad Request) if the graph is not valid,
     * or with status 500 (Internal Server Error) if the graph couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/graphs")
    @Timed
    public ResponseEntity<Graph> updateGraph(@Valid @RequestBody Graph graph) throws URISyntaxException {
        log.debug("REST request to update Graph : {}", graph);
        if (graph.getId() == null) {
            return createGraph(graph);
        }

        Graph result = graphService.save(graph);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, graph.getId().toString()))
            .body(result);
    }

    /**
     * GET  /graphs : get all the graphs.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of graphs in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @GetMapping("/graphs")
    @Timed
    public ResponseEntity<List<Graph>> getAllGraphs(Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of Graphs");
        Page<Graph> page = graphService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/graphs");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /graphs/:id : get the "id" graph.
     *
     * @param id the id of the graph to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the graph, or with status 404 (Not Found)
     */
    @GetMapping("/graphs/{id}")
    @Timed
    public ResponseEntity<Graph> getGraph(@PathVariable Long id) {
        log.debug("REST request to get Graph : {}", id);
        Optional<Graph> graph = graphService.findOne(id);
        return ResponseUtil.wrapOrNotFound(graph);
    }

    /**
     * DELETE  /graphs/:id : delete the "id" graph.
     *
     * @param id the id of the graph to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/graphs/{id}")
    @Timed
    public ResponseEntity<Void> deleteGraph(@PathVariable Long id) {
        log.debug("REST request to delete Graph : {}", id);
        Optional<Graph> graph = graphService.findOne(id);
        graph.ifPresent(graphService::delete);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

    /**
     * Get all graphs owned by logged in user, plus all shared graphs.
     */
    @GetMapping(value = "/mygraphs")
    @Timed
    public ResponseEntity<List<Graph>> getMyGraphs() throws URISyntaxException {
        log.debug("REST request to get owned and shared graphs");
        String login = SecurityUtils.getCurrentUserLogin();
        List<Graph> list = graphService.findByOwnerIsCurrentUserOrShared(login);
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

}
