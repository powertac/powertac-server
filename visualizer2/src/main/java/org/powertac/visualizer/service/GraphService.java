package org.powertac.visualizer.service;

import org.powertac.visualizer.domain.Graph;
import org.powertac.visualizer.repository.GraphRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service Implementation for managing Graph.
 */
@Service
@Transactional
public class GraphService {

    private final Logger log = LoggerFactory.getLogger(GraphService.class);

    private final GraphRepository graphRepository;

    public GraphService(GraphRepository graphRepository) {
        this.graphRepository = graphRepository;
    }

    /**
     * Save a graph.
     *
     * @param graph the entity to save
     * @return the persisted entity
     */
    public Graph save(Graph graph) {
        log.debug("Request to save Graph : {}", graph);
        Graph result = graphRepository.save(graph);
        return result;
    }

    /**
     *  Get all the graphs.
     *
     *  @param pageable the pagination information
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    public Page<Graph> findAll(Pageable pageable) {
        log.debug("Request to get all Graphs");
        Page<Graph> result = graphRepository.findAll(pageable);
        return result;
    }

    /**
     *  Get all the graphs owned by this user, plus all shared graphs.
     *
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    public List<Graph> findByOwnerIsCurrentUserOrShared(String login) {
        log.debug("Request to get all owned and shared Graphs");
        List<Graph> result = graphRepository.findByOwnerIsCurrentUserOrShared(login);
        return result;
    }

    /**
     *  Get one graph by id.
     *
     *  @param id the id of the entity
     *  @return the entity
     */
    @Transactional(readOnly = true)
    public Optional<Graph> findOne(Long id) {
        log.debug("Request to get Graph : {}", id);
        return graphRepository.findById(id);
    }

    /**
     *  Delete the graph.
     *
     *  @param graph the Graph to delete
     */
    public void delete(Graph graph) {
        log.debug("Request to delete Graph : {}", graph);
        graphRepository.delete(graph);
    }
}
