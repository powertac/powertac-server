package org.powertac.visualizer.domain;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A View.
 */
@Entity
@Table(name = "view")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class View implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "graphs", length = 255, nullable = false)
    private String graphs;

    @Transient
    private List<Long> graphIdList;

    @NotNull
    @Column(name = "shared", nullable = false)
    private Boolean shared;

    @ManyToOne
    private User owner;

    @ManyToOne
    private Chart chart;

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

    public String getGraphs() {
        return graphs;
    }

    public List<Long> getGraphIdList() {
        if (graphIdList == null && graphs != null) {
            graphIdList = Arrays.asList(graphs.split("\\s*,\\s*"))
                    .stream()
                    .map(s -> Long.parseLong(s))
                    .collect(Collectors.toList());
        }
        return graphIdList;
    }

    public void setGraphs(String graphs) {
        this.graphs = graphs;
        this.graphIdList = null;
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

    public Chart getChart() {
        return chart;
    }

    public void setChart(Chart chart) {
        this.chart = chart;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        View view = (View) o;
        if (view.id == null || id == null) {
            return false;
        }
        return Objects.equals(id, view.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "View{" +
            "id=" + id +
            ", name='" + name + "'" +
            ", graphs='" + graphs + "'" +
            ", shared='" + shared + "'" +
            '}';
    }
}
