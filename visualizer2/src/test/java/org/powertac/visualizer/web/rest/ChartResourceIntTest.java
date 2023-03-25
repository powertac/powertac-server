package org.powertac.visualizer.web.rest;

import org.powertac.visualizer.Visualizer2App;

import org.powertac.visualizer.domain.Chart;
import org.powertac.visualizer.repository.ChartRepository;
import org.powertac.visualizer.repository.UserRepository;
import org.powertac.visualizer.service.ChartService;
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

/**
 * Test class for the ChartResource REST controller.
 *
 * @see ChartResource
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Visualizer2App.class)
public class ChartResourceIntTest {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final Boolean DEFAULT_SHARED = false;
    private static final Boolean UPDATED_SHARED = true;

    @Autowired
    private ChartRepository chartRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChartService chartService;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    private MockMvc restChartMockMvc;

    private Chart chart;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        ChartResource chartResource = new ChartResource(chartService, userRepository);
        this.restChartMockMvc = MockMvcBuilders.standaloneSetup(chartResource)
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
    public static Chart createEntity(EntityManager em) {
        Chart chart = new Chart();
        chart.setName(DEFAULT_NAME);
        chart.setShared(DEFAULT_SHARED);
        return chart;
    }

    @BeforeEach
    public void initTest() {
        chart = createEntity(em);
    }

    @Test
    @Transactional
    public void createChart() throws Exception {
        int databaseSizeBeforeCreate = chartRepository.findAll().size();

        // Create the Chart

        restChartMockMvc.perform(post("/api/charts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(chart)))
            .andExpect(status().isCreated());

        // Validate the Chart in the database
        List<Chart> chartList = chartRepository.findAll();
        assertThat(chartList).hasSize(databaseSizeBeforeCreate + 1);
        Chart testChart = chartList.get(chartList.size() - 1);
        assertThat(testChart.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testChart.isShared()).isEqualTo(DEFAULT_SHARED);
    }

    @Test
    @Transactional
    public void createChartWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = chartRepository.findAll().size();

        // Create the Chart with an existing ID
        Chart existingChart = new Chart();
        existingChart.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restChartMockMvc.perform(post("/api/charts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(existingChart)))
            .andExpect(status().isBadRequest());

        // Validate the Alice in the database
        List<Chart> chartList = chartRepository.findAll();
        assertThat(chartList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkNameIsRequired() throws Exception {
        int databaseSizeBeforeTest = chartRepository.findAll().size();
        // set the field null
        chart.setName(null);

        // Create the Chart, which fails.

        restChartMockMvc.perform(post("/api/charts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(chart)))
            .andExpect(status().isBadRequest());

        List<Chart> chartList = chartRepository.findAll();
        assertThat(chartList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkSharedIsRequired() throws Exception {
        int databaseSizeBeforeTest = chartRepository.findAll().size();
        // set the field null
        chart.setShared(null);

        // Create the Chart, which fails.

        restChartMockMvc.perform(post("/api/charts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(chart)))
            .andExpect(status().isBadRequest());

        List<Chart> chartList = chartRepository.findAll();
        assertThat(chartList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllCharts() throws Exception {
        // Initialize the database
        chartRepository.saveAndFlush(chart);

        // Get all the chartList
        restChartMockMvc.perform(get("/api/charts?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(chart.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME.toString())))
            .andExpect(jsonPath("$.[*].shared").value(hasItem(DEFAULT_SHARED.booleanValue())));
    }

    @Test
    @Transactional
    public void getChart() throws Exception {
        // Initialize the database
        chartRepository.saveAndFlush(chart);

        // Get the chart
        restChartMockMvc.perform(get("/api/charts/{id}", chart.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(chart.getId().intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME.toString()))
            .andExpect(jsonPath("$.shared").value(DEFAULT_SHARED.booleanValue()));
    }

    @Test
    @Transactional
    public void getNonExistingChart() throws Exception {
        // Get the chart
        restChartMockMvc.perform(get("/api/charts/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateChart() throws Exception {
        // Initialize the database
        chartService.save(chart);

        int databaseSizeBeforeUpdate = chartRepository.findAll().size();

        // Update the chart
        Chart updatedChart = chartRepository.getOne(chart.getId());
        updatedChart.setName(UPDATED_NAME);
        updatedChart.setShared(UPDATED_SHARED);

        restChartMockMvc.perform(put("/api/charts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedChart)))
            .andExpect(status().isOk());

        // Validate the Chart in the database
        List<Chart> chartList = chartRepository.findAll();
        assertThat(chartList).hasSize(databaseSizeBeforeUpdate);
        Chart testChart = chartList.get(chartList.size() - 1);
        assertThat(testChart.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testChart.isShared()).isEqualTo(UPDATED_SHARED);
    }

    @Test
    @Transactional
    public void updateNonExistingChart() throws Exception {
        int databaseSizeBeforeUpdate = chartRepository.findAll().size();

        // Create the Chart

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restChartMockMvc.perform(put("/api/charts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(chart)))
            .andExpect(status().isCreated());

        // Validate the Chart in the database
        List<Chart> chartList = chartRepository.findAll();
        assertThat(chartList).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test
    @Transactional
    public void deleteChart() throws Exception {
        // Initialize the database
        chartService.save(chart);

        int databaseSizeBeforeDelete = chartRepository.findAll().size();

        // Get the chart
        restChartMockMvc.perform(delete("/api/charts/{id}", chart.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Chart> chartList = chartRepository.findAll();
        assertThat(chartList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Chart.class);
    }
}
