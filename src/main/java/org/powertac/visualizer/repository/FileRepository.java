package org.powertac.visualizer.repository;

import org.powertac.visualizer.domain.File;

import org.springframework.data.jpa.repository.*;

import java.util.List;

/**
 * Spring Data JPA repository for the File entity.
 */
public interface FileRepository extends JpaRepository<File,Long> {

    @Query("select file from File file where file.owner.login = ?#{principal.username}")
    List<File> findByOwnerIsCurrentUser();

}
