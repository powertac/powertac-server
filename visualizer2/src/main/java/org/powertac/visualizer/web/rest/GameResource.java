package org.powertac.visualizer.web.rest;

import com.codahale.metrics.annotation.Timed;

import org.apache.commons.io.FileExistsException;

import org.powertac.visualizer.config.Constants;
import org.powertac.visualizer.domain.File;
import org.powertac.visualizer.domain.Game;
import org.powertac.visualizer.domain.User;
import org.powertac.visualizer.domain.enumeration.FileType;
import org.powertac.visualizer.repository.UserRepository;
import org.powertac.visualizer.security.SecurityUtils;
import org.powertac.visualizer.service.GameService;
import org.powertac.visualizer.service_ptac.EmbeddedService;
import org.powertac.visualizer.service_ptac.VisualizerService;
import org.powertac.visualizer.service_ptac.VisualizerService.VisualizerState;
import org.powertac.visualizer.web.rest.util.HeaderUtil;
import org.powertac.visualizer.web.rest.util.PaginationUtil;

import io.github.jhipster.web.util.ResponseUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing Game.
 */
@RestController
@RequestMapping("/api")
public class GameResource {

    private final Logger log = LoggerFactory.getLogger(GameResource.class);


    private static final String ENTITY_NAME = "game";

    private final GameService gameService;
    private UserRepository userRepository;

    @Autowired
    private VisualizerService visualizerService;

    @Autowired
    private EmbeddedService embeddedService;

    public GameResource(GameService gameService, UserRepository userRepository) {
        this.gameService = gameService;
        this.userRepository = userRepository;
    }


    /**
     * POST /games : Create a new game.
     *
     * @param game the game to create
     * @return the ResponseEntity with status 201 (Created) and with body the new game, or with status 400 (Bad Request) if the game has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/games")
    @Timed
    public ResponseEntity<Game> createGame(@Valid @RequestBody Game game) throws URISyntaxException {
        log.debug("REST request to save Game : {}", game);
        if (game.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert(ENTITY_NAME, "idexists", "A new game cannot already have an ID")).body(null);
        }
        String login = SecurityUtils.getCurrentUserLogin();
        User user = userRepository.findOneByLogin(login).orElse(null);

        game.setOwner(user);
        if (game.getDate() == null) {
            game.setDate(ZonedDateTime.now());
        }

        Game result = gameService.save(game);
        return ResponseEntity.created(new URI("/api/games/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT /games : Updates an existing game.
     *
     * @param game the game to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated game,
     * or with status 400 (Bad Request) if the game is not valid,
     * or with status 500 (Internal Server Error) if the game couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/games")
    @Timed
    public ResponseEntity<Game> updateGame(@Valid @RequestBody Game game) throws URISyntaxException {
        log.debug("REST request to update Game : {}", game);
        if (game.getId() == null) {
            return createGame(game);
        }
        if (game.getDate() == null) {
            game.setDate(ZonedDateTime.now());
        }
        Game result = gameService.save(game);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, game.getId().toString()))
            .body(result);
    }

    /**
     * GET /games : get all the games.
     *
     * @param pageable
     *            the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of games in
     *         body
     * @throws URISyntaxException
     *             if there is an error to generate the pagination HTTP headers
     */
    @GetMapping("/games")
    @Timed
    public ResponseEntity<List<Game>> getAllGames(Pageable pageable) throws URISyntaxException {
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
    @GetMapping("/games/{id}")
    @Timed
    public ResponseEntity<Game> getGame(@PathVariable Long id) {
        log.debug("REST request to get Game : {}", id);
        Optional<Game> game = gameService.findOne(id);
        return ResponseUtil.wrapOrNotFound(game);
    }

    /**
     * DELETE /games/:id : delete the "id" game.
     *
     * @param id the id of the game to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/games/{id}")
    @Timed
    public ResponseEntity<Void> deleteGame(@PathVariable Long id) {
        log.debug("REST request to delete Game : {}", id);
        Optional<Game> game = gameService.findOne(id);
        game.ifPresent(gameService::delete);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

    /**
     * Get all games owned by logged in user, plus all shared files.
     */
    @GetMapping("/mygames")
    @Timed
    public ResponseEntity<List<Game>> getMyGames() throws URISyntaxException {
        log.debug("REST request to get owned and shared games");
        String login = SecurityUtils.getCurrentUserLogin();
        List<Game> list = gameService.findByOwnerIsCurrentUserOrShared(login);
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    @PostMapping("/bootgame")
    @Timed
    public ResponseEntity<Game> bootGame(@Valid @RequestBody Game game,
            @RequestParam("overwrite") @Valid @NotNull Boolean overwrite)
            throws URISyntaxException, FileExistsException {
        log.debug("REST request to start a boot game");

        if (visualizerService.getMode().equals(Constants.MODE_TOURNAMENT)) {
            throw new IllegalStateException("Not available in tournament mode");
        }
        if (visualizerService.getState().equals(VisualizerState.RUNNING)) {
            throw new IllegalStateException("Visualizer already has a game running");
        }

        String login = SecurityUtils.getCurrentUserLogin();
        User user = userRepository.findOneByLogin(login).orElse(null);

        List<Game> check = gameService.findByNameAndType(login, game.getName(), game.getType());
        if (check.size() > 0) {
            if (overwrite) {
                for (Game g : check) {
                    gameService.delete(g);
                }
            } else {
                return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("game", "nameexists", "A boot game already exists for this name"))
                    .body(null);
            }
        }

        game.setOwner(user);
        if (game.getDate() == null) {
            game.setDate(ZonedDateTime.now());
        }

        String error = embeddedService.runBootGame(game, user);
        if (error != null) {
            embeddedService.closeGame();
            throw new RuntimeException(error);
        }

        game = gameService.save(game);

        return ResponseEntity.created(new URI("/api/games/" + game.getId()))
                .headers(HeaderUtil.createAlert(game.getType() + " game '" + game.getName() + "' started", null))
                .body(game);
    }

    @PostMapping("/simgame")
    @Timed
    public ResponseEntity<Game> simGame(@Valid @RequestBody Game game,
            @RequestParam("overwrite") @Valid @NotNull Boolean overwrite)
            throws URISyntaxException {
        log.debug("REST request to start a sim game");

        if (visualizerService.getMode().equals(Constants.MODE_TOURNAMENT)) {
            throw new IllegalStateException("Not available in tournament mode");
        }
        if (visualizerService.getState().equals(VisualizerState.RUNNING)) {
            throw new IllegalStateException("Visualizer already has a game running");
        }

        String login = SecurityUtils.getCurrentUserLogin();
        User user = userRepository.findOneByLogin(login).orElse(null);

        List<Game> check = gameService.findByNameAndType(login, game.getName(), game.getType());
        if (check.size() > 0) {
            if (overwrite) {
                for (Game g : check) {
                    gameService.delete(g);
                }
            } else {
                return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("game", "nameexists", "A sim game already exists for this name"))
                    .body(null);
            }
        }

        game.setOwner(user);
        if (game.getDate() == null) {
            game.setDate(ZonedDateTime.now());
        }

        String error = embeddedService.runSimGame(game, user);
        if (error != null) {
            embeddedService.closeGame();
            throw new RuntimeException(error);
        }

        game = gameService.save(game);

        return ResponseEntity.created(new URI("/api/games/" + game.getId()))
                .headers(HeaderUtil.createAlert(game.getType() + " game '" + game.getName() + "' started", null))
                .body(game);
    }

    @PostMapping("/replaygame_internal")
    @Timed
    public ResponseEntity<Void> replayGameInternal(@Valid @RequestBody File file)
             throws URISyntaxException, IOException {
        log.debug("REST request to replay an internal game");

        if (!file.getType().equals(FileType.STATE)) {
            throw new IllegalArgumentException("Expected a state file");
        }
        if (visualizerService.getMode().equals(Constants.MODE_TOURNAMENT)) {
            throw new IllegalStateException("Not available in tournament mode");
        }
        if (visualizerService.getState().equals(VisualizerState.RUNNING)) {
            throw new IllegalStateException("Visualizer already has a game running");
        }

        InputStream source = new FileInputStream(new java.io.File(file.getPath()));
        String error = embeddedService.runReplayGame(source);
        if (error != null) {
            throw new RuntimeException(error);
        }

        return ResponseEntity.ok()
                .headers(HeaderUtil.createAlert("Replaying game from '" + file.getPath() + "'", null))
                .body(null);
    }

    @PostMapping("/replaygame_external")
    @Timed
    public ResponseEntity<Void> replayGameExternal(@Valid @RequestBody String url)
             throws URISyntaxException, MalformedURLException, IOException {
        log.debug("REST request to replay an external game");

        if (visualizerService.getMode().equals(Constants.MODE_TOURNAMENT)) {
            throw new IllegalStateException("Not available in tournament mode");
        }
        if (visualizerService.getState().equals(VisualizerState.RUNNING)) {
            throw new IllegalStateException("Visualizer already has a game running");
        }

        InputStream source = URI.create(url).toURL().openStream();
        String error = embeddedService.runReplayGame(source);
        if (error != null) {
            throw new RuntimeException(error);
        }

        return ResponseEntity.ok()
                .headers(HeaderUtil.createAlert("Replaying game from '" + url + "'", null))
                .body(null);
    }

    @PostMapping("/closegame")
    @Timed
    public ResponseEntity<Void> closeGame() throws IllegalStateException {
        log.debug("REST request to start a game");
        if (visualizerService.getMode().equals(Constants.MODE_TOURNAMENT)) {
            throw new IllegalStateException("Not available in tournament mode");
        }

        embeddedService.closeGame();

        return ResponseEntity.ok()
                .headers(HeaderUtil.createAlert("Game stopped", null))
                .body(null);
    }

}

