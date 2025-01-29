package org.powertac.visualizer.domain;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Proxy;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;

/**
 * A Chart.
 */
@Entity
@Table(name = "chart")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Proxy(lazy = false)
public class Chart implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @NotNull
    @Column(name = "shared", nullable = false)
    private Boolean shared;

    @ManyToOne
    private User owner;

    @ManyToMany
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    @JoinTable(name = "chart_graph",
               joinColumns = @JoinColumn(name="charts_id", referencedColumnName="id"),
               inverseJoinColumns = @JoinColumn(name="graphs_id", referencedColumnName="id"))
    private Set<Graph> graphs = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean isShared() {
        return shared;
    }

    public void setShared(Boolean shared) {
        this.shared = shared;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User user) {
        this.owner = user;
    }

    public Set<Graph> getGraphs() {
        return graphs;
    }

    public void setGraphs(Set<Graph> graphs) {
        this.graphs = graphs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Chart chart = (Chart) o;
        if (chart.id == null || id == null) {
            return false;
        }
        return Objects.equals(id, chart.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Chart{" +
            "id=" + id +
            ", name='" + name + "'" +
            ", shared='" + shared + "'" +
            '}';
    }
}
