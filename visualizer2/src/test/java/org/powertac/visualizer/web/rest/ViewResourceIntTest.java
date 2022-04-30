package org.powertac.visualizer.web.rest;

import org.powertac.visualizer.Visualizer2App;

import org.powertac.visualizer.domain.View;
import org.powertac.visualizer.repository.UserRepository;
import org.powertac.visualizer.repository.ViewRepository;
import org.powertac.visualizer.service.ViewService;
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
 * Test class for the ViewResource REST controller.
 *
 * @see ViewResource
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Visualizer2App.class)
public class ViewResourceIntTest {

    private static final String DEFAULT_NAME = "A";
    private static final String UPDATED_NAME = "B";

    private static final String DEFAULT_GRAPHS = "1";
    private static final String UPDATED_GRAPHS = "2";

    private static final Boolean DEFAULT_SHARED = false;
    private static final Boolean UPDATED_SHARED = true;

    @Autowired
    private ViewRepository viewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ViewService viewService;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    private MockMvc restViewMockMvc;

    private View view;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        ViewResource viewResource = new ViewResource(viewService, userRepository);
        this.restViewMockMvc = MockMvcBuilders.standaloneSetup(viewResource)
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
    public static View createEntity(EntityManager em) {
        View view = new View();
        view.setName(DEFAULT_NAME);
        view.setGraphs(DEFAULT_GRAPHS);
        view.setShared(DEFAULT_SHARED);
        return view;
    }

    @BeforeEach
    public void initTest() {
        view = createEntity(em);
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
        List<View> viewList = viewRepository.findAll();
        assertThat(viewList).hasSize(databaseSizeBeforeCreate + 1);
        View testView = viewList.get(viewList.size() - 1);
        assertThat(testView.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testView.getGraphs()).isEqualTo(DEFAULT_GRAPHS);
        assertThat(testView.isShared()).isEqualTo(DEFAULT_SHARED);
    }

    @Test
    @Transactional
    public void createViewWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = viewRepository.findAll().size();

        // Create the View with an existing ID
        View existingView = new View();
        existingView.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restViewMockMvc.perform(post("/api/views")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(existingView)))
            .andExpect(status().isBadRequest());

        // Validate the Alice in the database
        List<View> viewList = viewRepository.findAll();
        assertThat(viewList).hasSize(databaseSizeBeforeCreate);
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

        List<View> viewList = viewRepository.findAll();
        assertThat(viewList).hasSize(databaseSizeBeforeTest);
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

        List<View> viewList = viewRepository.findAll();
        assertThat(viewList).hasSize(databaseSizeBeforeTest);
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

        List<View> viewList = viewRepository.findAll();
        assertThat(viewList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllViews() throws Exception {
        // Initialize the database
        viewRepository.saveAndFlush(view);

        // Get all the viewList
        restViewMockMvc.perform(get("/api/views?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
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
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
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
        View updatedView = viewRepository.getOne(view.getId());
        updatedView.setName(UPDATED_NAME);
        updatedView.setGraphs(UPDATED_GRAPHS);
        updatedView.setShared(UPDATED_SHARED);

        restViewMockMvc.perform(put("/api/views")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedView)))
            .andExpect(status().isOk());

        // Validate the View in the database
        List<View> viewList = viewRepository.findAll();
        assertThat(viewList).hasSize(databaseSizeBeforeUpdate);
        View testView = viewList.get(viewList.size() - 1);
        assertThat(testView.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testView.getGraphs()).isEqualTo(UPDATED_GRAPHS);
        assertThat(testView.isShared()).isEqualTo(UPDATED_SHARED);
    }

    @Test
    @Transactional
    public void updateNonExistingView() throws Exception {
        int databaseSizeBeforeUpdate = viewRepository.findAll().size();

        // Create the View

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restViewMockMvc.perform(put("/api/views")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(view)))
            .andExpect(status().isCreated());

        // Validate the View in the database
        List<View> viewList = viewRepository.findAll();
        assertThat(viewList).hasSize(databaseSizeBeforeUpdate + 1);
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
        List<View> viewList = viewRepository.findAll();
        assertThat(viewList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(View.class);
    }
}
