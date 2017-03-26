package org.powertac.visualizer.repository_ptac;

import java.util.List;

import org.springframework.data.repository.Repository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface RecycleRepository<T> extends Repository<T, Long> {

  T save (T t);

  List<T> findAll ();

  T findById(long id);

  T findByName(String name);

  void recycle ();

}
