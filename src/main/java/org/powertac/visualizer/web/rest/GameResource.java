package org.powertac.visualizer.web.rest;

import com.codahale.metrics.annotation.Timed;

import org.powertac.visualizer.domain.Game;
import org.powertac.visualizer.security.SecurityUtils;
import org.powertac.visualizer.service.GameService;
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
 * REST controller for managing Game.
 */
@RestController
@RequestMapping("/api")
public class GameResource {

    private final Logger log = LoggerFactory.getLogger(GameResource.class);
        
    @Inject
    private GameService gameService;
    
    /**
     * POST  /games : Create a new game.
     *
     * @param game the game to create
     * @return the ResponseEntity with status 201 (Created) and with body the new game, or with status 400 (Bad Request) if the game has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/games",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Game> createGame(@Valid @RequestBody Game game) throws URISyntaxException {
        log.debug("REST request to save Game : {}", game);
        if (game.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("game", "idexists", "A new game cannot already have an ID")).body(null);
        }
        Game result = gameService.save(game);
        return ResponseEntity.created(new URI("/api/games/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("game", result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /games : Updates an existing game.
     *
     * @param game the game to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated game,
     * or with status 400 (Bad Request) if the game is not valid,
     * or with status 500 (Internal Server Error) if the game couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/games",
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Game> updateGame(@Valid @RequestBody Game game) throws URISyntaxException {
        log.debug("REST request to update Game : {}", game);
        if (game.getId() == null) {
            return createGame(game);
        }
        Game result = gameService.save(game);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("game", game.getId().toString()))
            .body(result);
    }

    /**
     * GET  /games : get all the games.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of games in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @RequestMapping(value = "/games",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<List<Game>> getAllGames(Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of Games");
        Page<Game> page = gameService.findAll(pageable); 
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/games");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /games/:id : get the "id" game.
     *
     * @param id the id of the game to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the game, or with status 404 (Not Found)
     */
    @RequestMapping(value = "/games/{id}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Game> getGame(@PathVariable Long id) {
        log.debug("REST request to get Game : {}", id);
        Game game = gameService.findOne(id);
        return Optional.ofNullable(game)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /games/:id : delete the "id" game.
     *
     * @param id the id of the game to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @RequestMapping(value = "/games/{id}",
        method = RequestMethod.DELETE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Void> deleteGame(@PathVariable Long id) {
        log.debug("REST request to delete Game : {}", id);
        gameService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("game", id.toString())).build();
    }

    /**
     * Get all games owned by logged in user, plus all shared files.
     */
    @RequestMapping(value = "/mygames",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<List<Game>> getMyGames() throws URISyntaxException {
        log.debug("REST request to get owned and shared games");
        String login = SecurityUtils.getCurrentUserLogin();
        List<Game> list = gameService.findByOwnerIsCurrentUserOrShared(login);
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

}
