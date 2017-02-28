package org.powertac.visualizer.repository;

import org.powertac.visualizer.domain.File;
import org.powertac.visualizer.domain.Game;
import org.powertac.visualizer.domain.enumeration.GameType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data JPA repository for the Game entity.
 */
public interface GameRepository extends JpaRepository<Game,Long> {

    @Query("select game from Game game where game.owner.login = ?#{principal.username}")
    List<Game> findByOwnerIsCurrentUser();

    @Query("select game from Game game where game.owner.login = ?#{principal.username} or game.shared = TRUE")
    List<Game> findByOwnerIsCurrentUserOrShared();

    @Query("select game from Game game where game.shared = TRUE or game.owner.login = :login")
    List<Game> findByOwnerIsCurrentUserOrShared(@Param("login") String login);

    @Query("select game from Game game where (game.owner.login = ?#{principal.username} or game.shared = TRUE)"
            + " and game.name = ?1 and game.type = ?2")
    List<Game> findByNameAndType(String name, GameType type);

    @Query("select game from Game game where"
            + " (game.shared = TRUE or game.owner.login = :login)"
            + " and game.name = :name and game.type = :type")
    List<Game> findByNameAndType(@Param("login") String login, @Param("name") String name, @Param("type") GameType type);

    @Query("select game from Game game where game.traceFile = ?1 or game.stateFile = ?1 or game.seedFile = ?1"
           + " or game.configFile = ?1 or game.bootFile = ?1 or game.weatherFile = ?1")
    List<Game> findByAssociatedFile(File file);

}
