package org.powertac.visualizer.web.rest;

import com.codahale.metrics.annotation.Timed;
import org.powertac.visualizer.domain.Chart;
import org.powertac.visualizer.domain.User;
import org.powertac.visualizer.repository.UserRepository;
import org.powertac.visualizer.security.SecurityUtils;
import org.powertac.visualizer.service.ChartService;
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
 * REST controller for managing Chart.
 */
@RestController
@RequestMapping("/api")
public class ChartResource {

    private final Logger log = LoggerFactory.getLogger(ChartResource.class);

    private static final String ENTITY_NAME = "chart";

    private final ChartService chartService;
    private final UserRepository userRepository;

    public ChartResource(ChartService chartService, UserRepository userRepository) {
        this.chartService = chartService;
        this.userRepository = userRepository;
    }

    /**
     * POST  /charts : Create a new chart.
     *
     * @param chart the chart to create
     * @return the ResponseEntity with status 201 (Created) and with body the new chart, or with status 400 (Bad Request) if the chart has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/charts")
    @Timed
    public ResponseEntity<Chart> createChart(@Valid @RequestBody Chart chart) throws URISyntaxException {
        log.debug("REST request to save Chart : {}", chart);
        if (chart.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert(ENTITY_NAME, "idexists", "A new chart cannot already have an ID")).body(null);
        }

        String login = SecurityUtils.getCurrentUserLogin();
        User user = userRepository.findOneByLogin(login).orElse(null);

        chart.setOwner(user);

        Chart result = chartService.save(chart);
        return ResponseEntity.created(new URI("/api/charts/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /charts : Updates an existing chart.
     *
     * @param chart the chart to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated chart,
     * or with status 400 (Bad Request) if the chart is not valid,
     * or with status 500 (Internal Server Error) if the chart couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/charts")
    @Timed
    public ResponseEntity<Chart> updateChart(@Valid @RequestBody Chart chart) throws URISyntaxException {
        log.debug("REST request to update Chart : {}", chart);
        if (chart.getId() == null) {
            return createChart(chart);
        }
        Chart result = chartService.save(chart);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, chart.getId().toString()))
            .body(result);
    }

    /**
     * GET  /charts : get all the charts.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of charts in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @GetMapping("/charts")
    @Timed
    public ResponseEntity<List<Chart>> getAllCharts(Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of Charts");
        Page<Chart> page = chartService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/charts");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /charts/:id : get the "id" chart.
     *
     * @param id the id of the chart to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the chart, or with status 404 (Not Found)
     */
    @GetMapping("/charts/{id}")
    @Timed
    public ResponseEntity<Chart> getChart(@PathVariable Long id) {
        log.debug("REST request to get Chart : {}", id);
        Optional<Chart> chart = chartService.findOne(id);
        return ResponseUtil.wrapOrNotFound(chart);
    }

    /**
     * DELETE  /charts/:id : delete the "id" chart.
     *
     * @param id the id of the chart to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/charts/{id}")
    @Timed
    public ResponseEntity<Void> deleteChart(@PathVariable Long id) {
        log.debug("REST request to delete Chart : {}", id);
        Optional<Chart> chart = chartService.findOne(id);
        chart.ifPresent(chartService::delete);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

    /**
     * Get all charts owned by logged in user, plus all shared charts.
     */
    @GetMapping(value = "/mycharts")
    @Timed
    public ResponseEntity<List<Chart>> getMyGames() throws URISyntaxException {
        log.debug("REST request to get owned and shared charts");
        String login = SecurityUtils.getCurrentUserLogin();
        List<Chart> list = chartService.findByOwnerIsCurrentUserOrShared(login);
        return new ResponseEntity<>(list, HttpStatus.OK);
    }
}
