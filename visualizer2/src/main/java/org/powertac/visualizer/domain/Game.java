package org.powertac.visualizer.domain;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.hibernate.annotations.Proxy;
import org.powertac.visualizer.domain.enumeration.GameType;

/**
 * A Game.
 */
@Entity
@Table(name = "game")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Proxy(lazy = false)
public class Game implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private GameType type;

    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @NotNull
    @Column(name = "shared", nullable = false)
    private Boolean shared;

    @Column(name = "date")
    private ZonedDateTime date;

    @Size(max = 4095)
    @Column(name = "brokers", length = 4095)
    private String brokers;

    @ManyToOne
    private User owner;

    @ManyToOne
    private File traceFile;

    @ManyToOne
    private File stateFile;

    @ManyToOne
    private File seedFile;

    @ManyToOne
    private File bootFile;

    @ManyToOne
    private File configFile;

    @ManyToOne
    private File weatherFile;

    @Transient
    private List<String> brokerList;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public GameType getType() {
        return type;
    }

    public void setType(GameType type) {
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

    public ZonedDateTime getDate() {
        return date;
    }

    public void setDate(ZonedDateTime date) {
        this.date = date;
    }

    public String getBrokers() {
        return brokers;
    }

    public List<String> getBrokerList() {
        if (brokerList == null && brokers != null) {
            brokerList = Arrays.asList(brokers.split("\\s*,\\s*"));
        }
        return brokerList;
    }

    public void setBrokers(String brokers) {
        this.brokers = brokers;
        this.brokerList = null;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User user) {
        this.owner = user;
    }

    public File getTraceFile() {
        return traceFile;
    }

    public String getTraceFileName() {
        return getFileName(getTraceFile());
    }

    public String getTraceFilePath() {
        return getFilePath(getTraceFile());
    }

    public Long getTraceFileId() {
        return getFileId(getTraceFile());
    }

    public void setTraceFile(File file) {
        this.traceFile = file;
    }

    public File getStateFile() {
        return stateFile;
    }

    public String getStateFileName() {
        return getFileName(getStateFile());
    }

    public String getStateFilePath() {
        return getFilePath(getStateFile());
    }

    public Long getStateFileId() {
        return getFileId(getStateFile());
    }

    public void setStateFile(File file) {
        this.stateFile = file;
    }

    public File getSeedFile() {
        return seedFile;
    }

    public String getSeedFileName() {
        return getFileName(getSeedFile());
    }

    public String getSeedFilePath() {
        return getFilePath(getSeedFile());
    }

    public Long getSeedFileId() {
        return getFileId(getSeedFile());
    }

    public void setSeedFile(File file) {
        this.seedFile = file;
    }

    public File getBootFile() {
        return bootFile;
    }

    public String getBootFileName() {
        return getFileName(getBootFile());
    }

    public String getBootFilePath() {
        return getFilePath(getBootFile());
    }

    public Long getBootFileId() {
        return getFileId(getBootFile());
    }

    public void setBootFile(File file) {
        this.bootFile = file;
    }

    public File getConfigFile() {
        return configFile;
    }

    public String getConfigFileName() {
        return getFileName(getConfigFile());
    }

    public String getConfigFilePath() {
        return getFilePath(getConfigFile());
    }

    public Long getConfigFileId() {
        return getFileId(getConfigFile());
    }

    public void setConfigFile(File file) {
        this.configFile = file;
    }

    public File getWeatherFile() {
        return weatherFile;
    }

    public String getWeatherFileName() {
        return getFileName(getWeatherFile());
    }

    public String getWeatherFilePath() {
        return getFilePath(getWeatherFile());
    }

    public Long getWeatherFileId() {
        return getFileId(getWeatherFile());
    }

    public void setWeatherFile(File file) {
        this.weatherFile = file;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Game game = (Game) o;
        if (game.id == null || id == null) {
            return false;
        }
        return Objects.equals(id, game.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Game{" +
            "id=" + id +
            ", type='" + type + "'" +
            ", name='" + name + "'" +
            ", shared='" + shared + "'" +
            ", date='" + date + "'" +
            ", brokers='" + brokers + "'" +
            '}';
    }

    private String getFileName(File file) {
        return file == null ? null : file.getName();
    }

    private String getFilePath(File file) {
        return file == null ? null : file.getPath();
    }

    private Long getFileId(File file) {
        return file == null ? null : file.getId();
    }
}
