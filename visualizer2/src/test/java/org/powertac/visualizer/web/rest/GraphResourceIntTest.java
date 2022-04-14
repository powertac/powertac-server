package org.powertac.visualizer.web.rest;

import org.powertac.visualizer.Visualizer2App;

import org.powertac.visualizer.domain.Graph;
import org.powertac.visualizer.repository.GraphRepository;
import org.powertac.visualizer.repository.UserRepository;
import org.powertac.visualizer.service.GraphService;
import org.powertac.visualizer.web.rest.errors.ExceptionTranslator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.powertac.visualizer.domain.enumeration.GraphType;

/**
 * Test class for the GraphResource REST controller.
 *
 * @see GraphResource
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Visualizer2App.class)
public class GraphResourceIntTest {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final GraphType DEFAULT_TYPE = GraphType.LINE;
    private static final GraphType UPDATED_TYPE = GraphType.BAR;

    private static final Boolean DEFAULT_SHARED = false;
    private static final Boolean UPDATED_SHARED = true;

    @Autowired
    private GraphRepository graphRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GraphService graphService;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    private MockMvc restGraphMockMvc;

    private Graph graph;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        GraphResource graphResource = new GraphResource(graphService, userRepository);
        this.restGraphMockMvc = MockMvcBuilders.standaloneSetup(graphResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Graph createEntity(EntityManager em) {
        Graph graph = new Graph();
        graph.setName(DEFAULT_NAME);
        graph.setType(DEFAULT_TYPE);
        graph.setShared(DEFAULT_SHARED);
        return graph;
    }

    @BeforeEach
    public void initTest() {
        graph = createEntity(em);
    }

    @Test
    @Transactional
    public void createGraph() throws Exception {
        int databaseSizeBeforeCreate = graphRepository.findAll().size();

        // Create the Graph

        restGraphMockMvc.perform(post("/api/graphs")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(graph)))
            .andExpect(status().isCreated());

        // Validate the Graph in the database
        List<Graph> graphList = graphRepository.findAll();
        assertThat(graphList).hasSize(databaseSizeBeforeCreate + 1);
        Graph testGraph = graphList.get(graphList.size() - 1);
        assertThat(testGraph.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testGraph.getType()).isEqualTo(DEFAULT_TYPE);
        assertThat(testGraph.isShared()).isEqualTo(DEFAULT_SHARED);
    }

    @Test
    @Transactional
    public void createGraphWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = graphRepository.findAll().size();

        // Create the Graph with an existing ID
        Graph existingGraph = new Graph();
        existingGraph.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restGraphMockMvc.perform(post("/api/graphs")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(existingGraph)))
            .andExpect(status().isBadRequest());

        // Validate the Alice in the database
        List<Graph> graphList = graphRepository.findAll();
        assertThat(graphList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkNameIsRequired() throws Exception {
        int databaseSizeBeforeTest = graphRepository.findAll().size();
        // set the field null
        graph.setName(null);

        // Create the Graph, which fails.

        restGraphMockMvc.perform(post("/api/graphs")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(graph)))
            .andExpect(status().isBadRequest());

        List<Graph> graphList = graphRepository.findAll();
        assertThat(graphList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkTypeIsRequired() throws Exception {
        int databaseSizeBeforeTest = graphRepository.findAll().size();
        // set the field null
        graph.setType(null);

        // Create the Graph, which fails.

        restGraphMockMvc.perform(post("/api/graphs")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(graph)))
            .andExpect(status().isBadRequest());

        List<Graph> graphList = graphRepository.findAll();
        assertThat(graphList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkSharedIsRequired() throws Exception {
        int databaseSizeBeforeTest = graphRepository.findAll().size();
        // set the field null
        graph.setShared(null);

        // Create the Graph, which fails.

        restGraphMockMvc.perform(post("/api/graphs")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(graph)))
            .andExpect(status().isBadRequest());

        List<Graph> graphList = graphRepository.findAll();
        assertThat(graphList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllGraphs() throws Exception {
        // Initialize the database
        graphRepository.saveAndFlush(graph);

        // Get all the graphList
        restGraphMockMvc.perform(get("/api/graphs?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(graph.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME.toString())))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE.toString())))
            .andExpect(jsonPath("$.[*].shared").value(hasItem(DEFAULT_SHARED.booleanValue())));
    }

    @Test
    @Transactional
    public void getGraph() throws Exception {
        // Initialize the database
        graphRepository.saveAndFlush(graph);

        // Get the graph
        restGraphMockMvc.perform(get("/api/graphs/{id}", graph.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(graph.getId().intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME.toString()))
            .andExpect(jsonPath("$.type").value(DEFAULT_TYPE.toString()))
            .andExpect(jsonPath("$.shared").value(DEFAULT_SHARED.booleanValue()));
    }

    @Test
    @Transactional
    public void getNonExistingGraph() throws Exception {
        // Get the graph
        restGraphMockMvc.perform(get("/api/graphs/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateGraph() throws Exception {
        // Initialize the database
        graphService.save(graph);

        int databaseSizeBeforeUpdate = graphRepository.findAll().size();

        // Update the graph
        Graph updatedGraph = graphRepository.getOne(graph.getId());
        updatedGraph.setName(UPDATED_NAME);
        updatedGraph.setType(UPDATED_TYPE);
        updatedGraph.setShared(UPDATED_SHARED);

        restGraphMockMvc.perform(put("/api/graphs")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedGraph)))
            .andExpect(status().isOk());

        // Validate the Graph in the database
        List<Graph> graphList = graphRepository.findAll();
        assertThat(graphList).hasSize(databaseSizeBeforeUpdate);
        Graph testGraph = graphList.get(graphList.size() - 1);
        assertThat(testGraph.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testGraph.getType()).isEqualTo(UPDATED_TYPE);
        assertThat(testGraph.isShared()).isEqualTo(UPDATED_SHARED);
    }

    @Test
    @Transactional
    public void updateNonExistingGraph() throws Exception {
        int databaseSizeBeforeUpdate = graphRepository.findAll().size();

        // Create the Graph

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restGraphMockMvc.perform(put("/api/graphs")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(graph)))
            .andExpect(status().isCreated());

        // Validate the Graph in the database
        List<Graph> graphList = graphRepository.findAll();
        assertThat(graphList).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test
    @Transactional
    public void deleteGraph() throws Exception {
        // Initialize the database
        graphService.save(graph);

        int databaseSizeBeforeDelete = graphRepository.findAll().size();

        // Get the graph
        restGraphMockMvc.perform(delete("/api/graphs/{id}", graph.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Graph> graphList = graphRepository.findAll();
        assertThat(graphList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Graph.class);
    }
}
