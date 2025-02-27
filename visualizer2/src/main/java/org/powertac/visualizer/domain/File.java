package org.powertac.visualizer.domain;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Objects;

import org.hibernate.annotations.Proxy;
import org.powertac.visualizer.domain.enumeration.FileType;

/**
 * A File.
 */
@Entity
@Table(name = "file")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Proxy(lazy = false)
public class File implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String separator = java.io.File.separator;

    public static final String getSafeName(String name) {
        try {
            name = name == null ? null : URLEncoder.encode(name, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // should never happen
        }
        return name;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private FileType type;

    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "name", length = 255, nullable = false)
    private String name;

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

    public FileType getType() {
        return type;
    }

    public void setType(FileType type) {
        this.type = type;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        File file = (File) o;
        if (file.id == null || id == null) {
            return false;
        }
        return Objects.equals(id, file.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "File{" +
            "id=" + id +
            ", type='" + type + "'" +
            ", name='" + name + "'" +
            ", shared='" + shared + "'" +
            '}';
    }

    public boolean exists() {
        return exists(getOwner());
    }

    public boolean exists(User user) {
        return getType().getFile(user, getName()).exists();
    }

    public boolean delete() {
        return delete(getOwner());
    }

    public boolean delete(User user) {
        return getType().getFile(user, getName()).delete();
    }

    public String getPath() {
        return getPath(getOwner());
    }

    public String getPath(User user) {
        FileType type = getType();
        java.io.File raw = type == null ? null : type.getFile(user, getName());
        return raw == null ? null : raw.getPath();
    }

}
