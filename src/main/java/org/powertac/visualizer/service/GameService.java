package org.powertac.visualizer.service;

import org.powertac.visualizer.domain.Game;
import org.powertac.visualizer.domain.enumeration.GameType;
import org.powertac.visualizer.repository.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

/**
 * Service Implementation for managing Game.
 */
@Service
@Transactional
public class GameService {

    private final Logger log = LoggerFactory.getLogger(GameService.class);

    @Inject
    private FileService fileService;

    @Inject
    private GameRepository gameRepository;
    
    /**
     * Save a game.
     * 
     * @param game the entity to save
     * @return the persisted entity
     */
    public Game save(Game game) {
        log.debug("Request to save Game : {}", game);
        Game result = gameRepository.save(game);
        return result;
    }

    /**
     *  Get all the games.
     *  
     *  @param pageable the pagination information
     *  @return the list of entities
     */
    @Transactional(readOnly = true) 
    public Page<Game> findAll(Pageable pageable) {
        log.debug("Request to get all Games");
        Page<Game> result = gameRepository.findAll(pageable); 
        return result;
    }

    /**
     *  Get all the games owned by this user, plus all shared games.
     *  
     *  @return the list of entities
     */
    @Transactional(readOnly = true) 
    public List<Game> findByOwnerIsCurrentUserOrShared(String login) {
        log.debug("Request to get all owned and shared Games");
        List<Game> result = gameRepository.findByOwnerIsCurrentUserOrShared(login); 
        return result;
    }

    /**
     *  Get one game by name and type.
     *
     *  @param id the id of the entity
     *  @return the entity
     */
    @Transactional(readOnly = true) 
    public List<Game> findByNameAndType(String login, String name, GameType type) {
        log.debug("Request to get Game : {}", name);
        List<Game> result = gameRepository.findByNameAndType(login, name, type);
        return result;
    }

    /**
     *  Get one game by id.
     *
     *  @param id the id of the entity
     *  @return the entity
     */
    @Transactional(readOnly = true) 
    public Game findOne(Long id) {
        log.debug("Request to get Game : {}", id);
        Game game = gameRepository.findOne(id);
        return game;
    }

    /**
     *  Delete the  game by id.
     *  
     *  @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete Game : {}", id);
        Game game = gameRepository.findOne(id);
        Long fileId = game.getStateFileId();
        if (fileId != null) {
            fileService.delete(fileId);
        }
        fileId = game.getTraceFileId();
        if (fileId != null) {
            fileService.delete(fileId);
        }
        gameRepository.delete(id);
    }
}
