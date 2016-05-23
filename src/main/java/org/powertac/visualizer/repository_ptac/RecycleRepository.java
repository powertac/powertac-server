package org.powertac.visualizer.repository_ptac;

import java.util.List;

import org.springframework.data.repository.Repository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface RecycleRepository<T> extends Repository<T, Long> {

  <S extends T> S save (S ts);

  List<T> findAll ();

  T findById(long id);

  T findByName(String name);

  void recycle ();

}
