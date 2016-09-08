package org.powertac.visualizer.web.rest;

import org.powertac.visualizer.Visualizer2App;
import org.powertac.visualizer.domain.Graph;
import org.powertac.visualizer.repository.GraphRepository;
import org.powertac.visualizer.repository.UserRepository;
import org.powertac.visualizer.service.GraphService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.hamcrest.Matchers.hasItem;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.powertac.visualizer.domain.enumeration.GraphType;

/**
 * Test class for the GraphResource REST controller.
 *
 * @see GraphResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Visualizer2App.class)
public class GraphResourceIntTest {

    private static final String DEFAULT_NAME = "A";
    private static final String UPDATED_NAME = "B";

    private static final GraphType DEFAULT_TYPE = GraphType.LINE;
    private static final GraphType UPDATED_TYPE = GraphType.BAR;

    private static final Boolean DEFAULT_SHARED = false;
    private static final Boolean UPDATED_SHARED = true;

    @Inject
    private GraphRepository graphRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private GraphService graphService;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    private MockMvc restGraphMockMvc;

    private Graph graph;

    @PostConstruct
    public void setup() {
        MockitoAnnotations.initMocks(this);
        GraphResource graphResource = new GraphResource();
        ReflectionTestUtils.setField(graphResource, "graphService", graphService);
        ReflectionTestUtils.setField(graphResource, "userRepository", userRepository);
        this.restGraphMockMvc = MockMvcBuilders.standaloneSetup(graphResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    @Before
    public void initTest() {
        graph = new Graph();
        graph.setName(DEFAULT_NAME);
        graph.setType(DEFAULT_TYPE);
        graph.setShared(DEFAULT_SHARED);
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
        List<Graph> graphs = graphRepository.findAll();
        assertThat(graphs).hasSize(databaseSizeBeforeCreate + 1);
        Graph testGraph = graphs.get(graphs.size() - 1);
        assertThat(testGraph.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testGraph.getType()).isEqualTo(DEFAULT_TYPE);
        assertThat(testGraph.isShared()).isEqualTo(DEFAULT_SHARED);
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

        List<Graph> graphs = graphRepository.findAll();
        assertThat(graphs).hasSize(databaseSizeBeforeTest);
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

        List<Graph> graphs = graphRepository.findAll();
        assertThat(graphs).hasSize(databaseSizeBeforeTest);
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

        List<Graph> graphs = graphRepository.findAll();
        assertThat(graphs).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllGraphs() throws Exception {
        // Initialize the database
        graphRepository.saveAndFlush(graph);

        // Get all the graphs
        restGraphMockMvc.perform(get("/api/graphs?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(TestUtil.APPLICATION_JSON_UTF8))
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
            .andExpect(content().contentType(TestUtil.APPLICATION_JSON_UTF8))
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
        Graph updatedGraph = new Graph();
        updatedGraph.setId(graph.getId());
        updatedGraph.setName(UPDATED_NAME);
        updatedGraph.setType(UPDATED_TYPE);
        updatedGraph.setShared(UPDATED_SHARED);

        restGraphMockMvc.perform(put("/api/graphs")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(updatedGraph)))
                .andExpect(status().isOk());

        // Validate the Graph in the database
        List<Graph> graphs = graphRepository.findAll();
        assertThat(graphs).hasSize(databaseSizeBeforeUpdate);
        Graph testGraph = graphs.get(graphs.size() - 1);
        assertThat(testGraph.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testGraph.getType()).isEqualTo(UPDATED_TYPE);
        assertThat(testGraph.isShared()).isEqualTo(UPDATED_SHARED);
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
        List<Graph> graphs = graphRepository.findAll();
        assertThat(graphs).hasSize(databaseSizeBeforeDelete - 1);
    }
}
