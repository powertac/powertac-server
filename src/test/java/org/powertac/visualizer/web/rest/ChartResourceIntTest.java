package org.powertac.visualizer.web.rest;

import org.powertac.visualizer.Visualizer2App;
import org.powertac.visualizer.domain.Chart;
import org.powertac.visualizer.repository.ChartRepository;
import org.powertac.visualizer.repository.UserRepository;
import org.powertac.visualizer.service.ChartService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.hamcrest.Matchers.hasItem;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
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


/**
 * Test class for the ChartResource REST controller.
 *
 * @see ChartResource
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Visualizer2App.class)
@WebAppConfiguration
@IntegrationTest
public class ChartResourceIntTest {

    private static final String DEFAULT_NAME = "A";
    private static final String UPDATED_NAME = "B";

    private static final Boolean DEFAULT_SHARED = false;
    private static final Boolean UPDATED_SHARED = true;

    @Inject
    private ChartRepository chartRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private ChartService chartService;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    private MockMvc restChartMockMvc;

    private Chart chart;

    @PostConstruct
    public void setup() {
        MockitoAnnotations.initMocks(this);
        ChartResource chartResource = new ChartResource();
        ReflectionTestUtils.setField(chartResource, "chartService", chartService);
        ReflectionTestUtils.setField(chartResource, "userRepository", userRepository);
        this.restChartMockMvc = MockMvcBuilders.standaloneSetup(chartResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    @Before
    public void initTest() {
        chart = new Chart();
        chart.setName(DEFAULT_NAME);
        chart.setShared(DEFAULT_SHARED);
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
        List<Chart> charts = chartRepository.findAll();
        assertThat(charts).hasSize(databaseSizeBeforeCreate + 1);
        Chart testChart = charts.get(charts.size() - 1);
        assertThat(testChart.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testChart.isShared()).isEqualTo(DEFAULT_SHARED);
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

        List<Chart> charts = chartRepository.findAll();
        assertThat(charts).hasSize(databaseSizeBeforeTest);
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

        List<Chart> charts = chartRepository.findAll();
        assertThat(charts).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllCharts() throws Exception {
        // Initialize the database
        chartRepository.saveAndFlush(chart);

        // Get all the charts
        restChartMockMvc.perform(get("/api/charts?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
        Chart updatedChart = new Chart();
        updatedChart.setId(chart.getId());
        updatedChart.setName(UPDATED_NAME);
        updatedChart.setShared(UPDATED_SHARED);

        restChartMockMvc.perform(put("/api/charts")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(updatedChart)))
                .andExpect(status().isOk());

        // Validate the Chart in the database
        List<Chart> charts = chartRepository.findAll();
        assertThat(charts).hasSize(databaseSizeBeforeUpdate);
        Chart testChart = charts.get(charts.size() - 1);
        assertThat(testChart.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testChart.isShared()).isEqualTo(UPDATED_SHARED);
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
        List<Chart> charts = chartRepository.findAll();
        assertThat(charts).hasSize(databaseSizeBeforeDelete - 1);
    }
}
