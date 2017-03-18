package org.powertac.visualizer.repository;

import org.powertac.visualizer.domain.View;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data JPA repository for the View entity.
 */
public interface ViewRepository extends JpaRepository<View,Long> {

    @Query("select view from View view where view.owner.login = :login")
    List<View> findByOwnerIsCurrentUser(@Param("login") String login);

    @Query("select view from View view where view.shared = TRUE or view.owner.login = :login")
    List<View> findByOwnerIsCurrentUserOrShared(@Param("login") String login);

}
