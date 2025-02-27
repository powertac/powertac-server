package org.powertac.visualizer.web.rest;

import com.codahale.metrics.annotation.Timed;

import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.IOUtils;
import org.powertac.visualizer.domain.File;
import org.powertac.visualizer.domain.Game;
import org.powertac.visualizer.domain.User;
import org.powertac.visualizer.domain.enumeration.FileType;
import org.powertac.visualizer.repository.UserRepository;
import org.powertac.visualizer.security.SecurityUtils;
import org.powertac.visualizer.service.FileService;
import org.powertac.visualizer.service.GameService;
import org.powertac.visualizer.service_ptac.SyncFilesService;
import org.powertac.visualizer.web.rest.util.HeaderUtil;
import org.powertac.visualizer.web.rest.util.PaginationUtil;

import io.github.jhipster.web.util.ResponseUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing File.
 */
@RestController
@RequestMapping("/api")
public class FileResource {

    private final Logger log = LoggerFactory.getLogger(FileResource.class);

    private static final String ENTITY_NAME = "file";

    private final FileService fileService;
    private final GameService gameService;
    private final UserRepository userRepository;

    public FileResource(FileService fileService, GameService gameService, UserRepository userRepository) {
        this.fileService = fileService;
        this.gameService = gameService;
        this.userRepository = userRepository;
    }

    @Autowired
    private SyncFilesService syncFilesService;

    /**
     * POST  /files : Create a new file.
     *
     * @param file the file to create
     * @return the ResponseEntity with status 201 (Created) and with body the new file, or with status 400 (Bad Request) if the file has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/files")
    @Timed
    public ResponseEntity<File> createFile(@Valid @RequestBody File file) throws URISyntaxException {
        log.debug("REST request to save File : {}", file);
        if (file.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert(ENTITY_NAME, "idexists", "A new file cannot already have an ID")).body(null);
        }

        String login = SecurityUtils.getCurrentUserLogin();
        User user = userRepository.findOneByLogin(login).orElse(null);

        file.setOwner(user);

        File result = fileService.save(file);
        return ResponseEntity.created(new URI("/api/files/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /files : Updates an existing file.
     *
     * @param file the file to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated file,
     * or with status 400 (Bad Request) if the file is not valid,
     * or with status 500 (Internal Server Error) if the file couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/files")
    @Timed
    public ResponseEntity<File> updateFile(@Valid @RequestBody File file) throws URISyntaxException {
        log.debug("REST request to update File : {}", file);
        if (file.getId() == null) {
            return createFile(file);
        }
        File result = fileService.save(file);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, file.getId().toString()))
            .body(result);
    }

    /**
     * GET  /files : get all the files.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of files in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @GetMapping("/files")
    @Timed
    public ResponseEntity<List<File>> getAllFiles(Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of Files");
        Page<File> page = fileService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/files");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /files/:id : get the "id" file.
     *
     * @param id the id of the file to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the file, or with status 404 (Not Found)
     */
    @GetMapping("/files/{id}")
    @Timed
    public ResponseEntity<File> getFile(@PathVariable Long id) {
        log.debug("REST request to get File : {}", id);
        Optional<File> file = fileService.findOne(id);
        return ResponseUtil.wrapOrNotFound(file);
    }

    /**
     * DELETE  /files/:id : delete the "id" file.
     *
     * @param id the id of the file to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/files/{id}")
    @Timed
    public ResponseEntity<Void> deleteFile(@PathVariable Long id) {
        log.debug("REST request to delete File : {}", id);
        Optional<File> file = fileService.findOne(id);
        if (file.isPresent()) {
            File ff = file.get();
            fileService.delete(ff);
            ff.getType().getFile(ff.getOwner(), ff.getName()).delete();
        }
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

    /**
     * Get all files owned by logged in user, plus all shared files.
     */
    @GetMapping("/myfiles/{type}/")
    @Timed
    public ResponseEntity<List<File>> getMyFiles(
            @Valid @NotNull @PathVariable String type) throws URISyntaxException {
        syncFilesService.syncFileSystem();
        FileType fileType = FileType.valueOf(type.toUpperCase());
        log.debug("REST request to get owned and shared files, type = " + type);
        String login = SecurityUtils.getCurrentUserLogin();
        List<File> list = fileService.findByOwnerIsCurrentUserOrShared(login, fileType);
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    /**
     * Download a file.
     * TODO document.
     *
     * @param type
     * @param id
     * @param response
     * @throws IOException
     */
    @GetMapping("/myfiles/{type}/{id}")
    @Timed
    public void getMyFile (@Valid @NotNull @PathVariable String type,
                @Valid @NotNull @PathVariable Long id,
                HttpServletResponse response) throws IOException {
        log.debug("REST request to download a file");
        FileType fileType = FileType.valueOf(type.toUpperCase());
        if (fileType == null) {
            throw new IllegalArgumentException("Unknown type " + type);
        }
        Optional<File> file = fileService.findOne(id);
        if (file.isEmpty()) {
            return;
        }
        File ff = file.get();
        java.io.File raw = fileType.getFile(ff.getOwner(), ff.getName());
        try (
            InputStream in = new BufferedInputStream(new FileInputStream(raw));
            OutputStream out = new BufferedOutputStream(response.getOutputStream())
        ) {
            response.setHeader("Content-Disposition", "attachment; filename=" + ff.getName());
            response.setHeader("Content-Type", fileType.getContentType());
            IOUtils.copy(in, out);
        }
    }

    /**
     * Upload a file.
     * TODO document.
     *
     * @param part
     * @param type
     * @param shared
     * @throws FileExistsException
     * @throws IOException
     * @throws URISyntaxException
     */
    @PostMapping("/myfiles/{type}/")
    @Timed
    public ResponseEntity<File> postFile (@Valid @NotNull @PathVariable String type,
                @RequestParam("shared") Boolean shared,
                @RequestParam("overwrite") Boolean overwrite,
                @Valid @NotNull @RequestParam("file") MultipartFile part)
                throws IOException, URISyntaxException {
        String name = part.getOriginalFilename();
        log.debug("REST request to upload a " + type.toString() + " file: " +
                name + " @ " + part.getSize() + " bytes.");
        String login = SecurityUtils.getCurrentUserLogin();
        User user = userRepository.findOneByLogin(login).orElse(null);

        FileType fileType = FileType.valueOf(type.toUpperCase());
        java.io.File raw = fileType.getFile(user, name);
        if (raw.exists()) {
          if (overwrite) {
            List<File> files = fileService.findByOwnerIsCurrentUser(login, fileType);
            for (File file: files) {
              if (file.getName().equals(name)) {
                for (Game game : gameService.findByAssociatedFile(file)) {
                  switch(fileType) {
                    case BOOT: game.setBootFile(null); break;
                    case CONFIG: game.setConfigFile(null); break;
                    case SEED: game.setSeedFile(null); break;
                    case WEATHER: game.setWeatherFile(null); break;
                    case STATE: game.setStateFile(null); break;
                    case TRACE: game.setTraceFile(null); break;
                    default:
                      throw new IllegalArgumentException("Can't overwrite " + type + " file");
                  }
                  gameService.save(game);
                }
                fileService.delete(file);
                break;
              }
            }
          } else {
            throw new FileExistsException();
          }
        }
        try (
            OutputStream out = new FileOutputStream(raw);
            InputStream in = part.getInputStream()
        ) {
            byte[] buf = new byte[65536];
            while (true) {
              int len = in.read(buf);
              if (len > 0) {
                out.write(buf, 0, len);
              } else {
                break;
              }
            }

            File file = new File();
            file.setType(fileType);
            file.setName(name);
            file.setOwner(user);
            file.setShared(shared);
            file = fileService.save(file);
            return ResponseEntity.created(new URI("/api/files/" + file.getId()))
                .headers(HeaderUtil.createEntityCreationAlert("file", file.getId().toString()))
                .body(file);
        }
    }

}
