package org.powertac.visualizer.domain;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

import org.hibernate.annotations.Proxy;
import org.powertac.visualizer.domain.enumeration.GraphType;

/**
 * A Graph.
 */
@Entity
@Table(name = "graph")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Proxy(lazy = false)
public class Graph implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private GraphType type;

    @NotNull
    @Column(name = "shared", nullable = false)
    private Boolean shared;

    @ManyToOne
    private User owner;

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

    public GraphType getType() {
        return type;
    }

    public void setType(GraphType type) {
        this.type = type;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Graph graph = (Graph) o;
        if (graph.id == null || id == null) {
            return false;
        }
        return Objects.equals(id, graph.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Graph{" +
            "id=" + id +
            ", name='" + name + "'" +
            ", type='" + type + "'" +
            ", shared='" + shared + "'" +
            '}';
    }
}
