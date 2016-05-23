package org.powertac.visualizer.repository;

import org.powertac.visualizer.domain.View;

import org.springframework.data.jpa.repository.*;

import java.util.List;

/**
 * Spring Data JPA repository for the View entity.
 */
public interface ViewRepository extends JpaRepository<View,Long> {

    @Query("select view from View view where view.owner.login = ?#{principal.username}")
    List<View> findByOwnerIsCurrentUser();

}
