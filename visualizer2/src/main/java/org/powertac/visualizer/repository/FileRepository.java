package org.powertac.visualizer.repository;

import org.powertac.visualizer.domain.File;
import org.powertac.visualizer.domain.enumeration.FileType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data JPA repository for the File entity.
 */
public interface FileRepository extends JpaRepository<File,Long> {

    @Query("select file from File file where file.owner.login = :login"
    + " and (:type = NULL or file.type = :type)")
    List<File> findByOwnerIsCurrentUser(@Param("login") String login, @Param("type") FileType type);

    @Query("select file from File file where"
    + " (file.shared = TRUE or file.owner.login = :login)"
    + " and (:type = NULL or file.type = :type)")
    List<File> findByOwnerIsCurrentUserOrShared(@Param("login") String login, @Param("type") FileType type);

}
