package org.powertac.visualizer.repository;

import org.powertac.visualizer.domain.Graph;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data JPA repository for the Graph entity.
 */
public interface GraphRepository extends JpaRepository<Graph,Long> {

    @Query("select graph from Graph graph where graph.owner.login = :login")
    List<Graph> findByOwnerIsCurrentUser(@Param("login") String login);

    @Query("select graph from Graph graph where graph.shared = TRUE or graph.owner.login = :login")
    List<Graph> findByOwnerIsCurrentUserOrShared(@Param("login") String login);
}
