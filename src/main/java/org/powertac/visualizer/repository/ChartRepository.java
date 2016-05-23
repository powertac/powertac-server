package org.powertac.visualizer.repository;

import org.powertac.visualizer.domain.Chart;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data JPA repository for the Chart entity.
 */
public interface ChartRepository extends JpaRepository<Chart,Long> {

    @Query("select chart from Chart chart where chart.owner.login = ?#{principal.username}")
    List<Chart> findByOwnerIsCurrentUser();

    @Query("select distinct chart from Chart chart left join fetch chart.graphs")
    List<Chart> findAllWithEagerRelationships();

    @Query("select chart from Chart chart left join fetch chart.graphs where chart.id =:id")
    Chart findOneWithEagerRelationships(@Param("id") Long id);

}
