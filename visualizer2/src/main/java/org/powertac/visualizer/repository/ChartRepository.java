package org.powertac.visualizer.repository;

import org.powertac.visualizer.domain.Chart;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the Chart entity.
 */
public interface ChartRepository extends JpaRepository<Chart,Long> {

    @Query("select chart from Chart chart where chart.owner.login = :login")
    List<Chart> findByOwnerIsCurrentUser(@Param("login") String login);

    @Query("select chart from Chart chart where chart.shared = TRUE or chart.owner.login = :login")
    List<Chart> findByOwnerIsCurrentUserOrShared(@Param("login") String login);

    @Query("select distinct chart from Chart chart left join fetch chart.graphs")
    List<Chart> findAllWithEagerRelationships();

    @Query("select chart from Chart chart left join fetch chart.graphs where chart.id =:id")
    Optional<Chart> findOneWithEagerRelationships(@Param("id") Long id);

}
