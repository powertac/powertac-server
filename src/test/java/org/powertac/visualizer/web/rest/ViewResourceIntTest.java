package org.powertac.visualizer.web.rest;

import org.powertac.visualizer.Visualizer2App;
import org.powertac.visualizer.domain.View;
import org.powertac.visualizer.repository.UserRepository;
import org.powertac.visualizer.repository.ViewRepository;
import org.powertac.visualizer.service.ViewService;

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
 * Test class for the ViewResource REST controller.
 *
 * @see ViewResource
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Visualizer2App.class)
@WebAppConfiguration
@IntegrationTest
public class ViewResourceIntTest {

    private static final String DEFAULT_NAME = "A";
    private static final String UPDATED_NAME = "B";
    private static final String DEFAULT_GRAPHS = "1";
    private static final String UPDATED_GRAPHS = "2";

    private static final Boolean DEFAULT_SHARED = false;
    private static final Boolean UPDATED_SHARED = true;

    @Inject
    private ViewRepository viewRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private ViewService viewService;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    private MockMvc restViewMockMvc;

    private View view;

    @PostConstruct
    public void setup() {
        MockitoAnnotations.initMocks(this);
        ViewResource viewResource = new ViewResource();
        ReflectionTestUtils.setField(viewResource, "viewService", viewService);
        ReflectionTestUtils.setField(viewResource, "userRepository", userRepository);
        this.restViewMockMvc = MockMvcBuilders.standaloneSetup(viewResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    @Before
    public void initTest() {
        view = new View();
        view.setName(DEFAULT_NAME);
        view.setGraphs(DEFAULT_GRAPHS);
        view.setShared(DEFAULT_SHARED);
    }

    @Test
    @Transactional
    public void createView() throws Exception {
        int databaseSizeBeforeCreate = viewRepository.findAll().size();

        // Create the View

        restViewMockMvc.perform(post("/api/views")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(view)))
                .andExpect(status().isCreated());

        // Validate the View in the database
        List<View> views = viewRepository.findAll();
        assertThat(views).hasSize(databaseSizeBeforeCreate + 1);
        View testView = views.get(views.size() - 1);
        assertThat(testView.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testView.getGraphs()).isEqualTo(DEFAULT_GRAPHS);
        assertThat(testView.isShared()).isEqualTo(DEFAULT_SHARED);
    }

    @Test
    @Transactional
    public void checkNameIsRequired() throws Exception {
        int databaseSizeBeforeTest = viewRepository.findAll().size();
        // set the field null
        view.setName(null);

        // Create the View, which fails.

        restViewMockMvc.perform(post("/api/views")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(view)))
                .andExpect(status().isBadRequest());

        List<View> views = viewRepository.findAll();
        assertThat(views).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkGraphsIsRequired() throws Exception {
        int databaseSizeBeforeTest = viewRepository.findAll().size();
        // set the field null
        view.setGraphs(null);

        // Create the View, which fails.

        restViewMockMvc.perform(post("/api/views")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(view)))
                .andExpect(status().isBadRequest());

        List<View> views = viewRepository.findAll();
        assertThat(views).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkSharedIsRequired() throws Exception {
        int databaseSizeBeforeTest = viewRepository.findAll().size();
        // set the field null
        view.setShared(null);

        // Create the View, which fails.

        restViewMockMvc.perform(post("/api/views")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(view)))
                .andExpect(status().isBadRequest());

        List<View> views = viewRepository.findAll();
        assertThat(views).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllViews() throws Exception {
        // Initialize the database
        viewRepository.saveAndFlush(view);

        // Get all the views
        restViewMockMvc.perform(get("/api/views?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.[*].id").value(hasItem(view.getId().intValue())))
                .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME.toString())))
                .andExpect(jsonPath("$.[*].graphs").value(hasItem(DEFAULT_GRAPHS.toString())))
                .andExpect(jsonPath("$.[*].shared").value(hasItem(DEFAULT_SHARED.booleanValue())));
    }

    @Test
    @Transactional
    public void getView() throws Exception {
        // Initialize the database
        viewRepository.saveAndFlush(view);

        // Get the view
        restViewMockMvc.perform(get("/api/views/{id}", view.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(view.getId().intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME.toString()))
            .andExpect(jsonPath("$.graphs").value(DEFAULT_GRAPHS.toString()))
            .andExpect(jsonPath("$.shared").value(DEFAULT_SHARED.booleanValue()));
    }

    @Test
    @Transactional
    public void getNonExistingView() throws Exception {
        // Get the view
        restViewMockMvc.perform(get("/api/views/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateView() throws Exception {
        // Initialize the database
        viewService.save(view);

        int databaseSizeBeforeUpdate = viewRepository.findAll().size();

        // Update the view
        View updatedView = new View();
        updatedView.setId(view.getId());
        updatedView.setName(UPDATED_NAME);
        updatedView.setGraphs(UPDATED_GRAPHS);
        updatedView.setShared(UPDATED_SHARED);

        restViewMockMvc.perform(put("/api/views")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(updatedView)))
                .andExpect(status().isOk());

        // Validate the View in the database
        List<View> views = viewRepository.findAll();
        assertThat(views).hasSize(databaseSizeBeforeUpdate);
        View testView = views.get(views.size() - 1);
        assertThat(testView.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testView.getGraphs()).isEqualTo(UPDATED_GRAPHS);
        assertThat(testView.isShared()).isEqualTo(UPDATED_SHARED);
    }

    @Test
    @Transactional
    public void deleteView() throws Exception {
        // Initialize the database
        viewService.save(view);

        int databaseSizeBeforeDelete = viewRepository.findAll().size();

        // Get the view
        restViewMockMvc.perform(delete("/api/views/{id}", view.getId())
                .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        // Validate the database is empty
        List<View> views = viewRepository.findAll();
        assertThat(views).hasSize(databaseSizeBeforeDelete - 1);
    }
}
