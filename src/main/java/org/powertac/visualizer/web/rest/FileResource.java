package org.powertac.visualizer.web.rest;

import com.codahale.metrics.annotation.Timed;

import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.IOUtils;
import org.powertac.visualizer.domain.File;
import org.powertac.visualizer.domain.User;
import org.powertac.visualizer.domain.enumeration.FileType;
import org.powertac.visualizer.repository.UserRepository;
import org.powertac.visualizer.security.SecurityUtils;
import org.powertac.visualizer.service.FileService;
import org.powertac.visualizer.web.rest.util.HeaderUtil;
import org.powertac.visualizer.web.rest.util.PaginationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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

    @Inject
    private FileService fileService;

    @Inject
    private UserRepository userRepository;

    /**
     * POST  /files : Create a new file.
     *
     * @param file the file to create
     * @return the ResponseEntity with status 201 (Created) and with body the new file, or with status 400 (Bad Request) if the file has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/files",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<File> createFile(@Valid @RequestBody File file) throws URISyntaxException {
        log.debug("REST request to save File : {}", file);
        if (file.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("file", "idexists", "A new file cannot already have an ID")).body(null);
        }

        String login = SecurityUtils.getCurrentUserLogin();
        User user = userRepository.findOneByLogin(login).orElse(null);

        file.setOwner(user);

        File result = fileService.save(file);
        return ResponseEntity.created(new URI("/api/files/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("file", result.getId().toString()))
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
    @RequestMapping(value = "/files",
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<File> updateFile(@Valid @RequestBody File file) throws URISyntaxException {
        log.debug("REST request to update File : {}", file);
        if (file.getId() == null) {
            return createFile(file);
        }
        File result = fileService.save(file);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("file", file.getId().toString()))
            .body(result);
    }

    /**
     * GET  /files : get all the files.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of files in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @RequestMapping(value = "/files",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
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
    @RequestMapping(value = "/files/{id}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<File> getFile(@PathVariable Long id) {
        log.debug("REST request to get File : {}", id);
        File file = fileService.findOne(id);
        return Optional.ofNullable(file)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /files/:id : delete the "id" file.
     *
     * @param id the id of the file to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @RequestMapping(value = "/files/{id}",
        method = RequestMethod.DELETE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Void> deleteFile(@PathVariable Long id) {
        log.debug("REST request to delete File : {}", id);
        File file = fileService.findOne(id);
        fileService.delete(id);
        file.getType().getFile(file.getOwner(), file.getName()).delete();
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("file", id.toString())).build();
    }

    /**
     * Get all files owned by logged in user, plus all shared files.
     */
    @RequestMapping(value = "/myfiles/{type}/",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<List<File>> getMyFiles(
            @Valid @NotNull @PathVariable String type) throws URISyntaxException {
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
     * @param name
     * @param response
     * @throws IOException
     */
    @RequestMapping(value = "/myfiles/{type}/{id}", method = RequestMethod.GET)
    @Timed
    public void getMyFile (@Valid @NotNull @PathVariable String type,
                @Valid @NotNull @PathVariable Long id,
                HttpServletResponse response) throws IOException {
        log.debug("REST request to download a file");
        FileType fileType = FileType.valueOf(type.toUpperCase());
        if (fileType == null) {
            throw new IllegalArgumentException("Unknown type " + type);
        }
        File file = fileService.findOne(id);
        java.io.File raw = fileType.getFile(file.getOwner(), file.getName());
        try (
            InputStream in = new BufferedInputStream(new FileInputStream(raw));
            OutputStream out = new BufferedOutputStream(response.getOutputStream())
        ) {
            response.setHeader("Content-Disposition", "attachment; filename=" + file.getName());
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
    @RequestMapping(value = "/myfiles/{type}/", method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<File> postFile (@Valid @NotNull @PathVariable String type,
                @RequestParam("shared") Boolean shared,
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
            throw new FileExistsException();
        }
        try (
            OutputStream out = new BufferedOutputStream(new FileOutputStream(raw))
        ) {
            byte[] bytes = part.getBytes();
            out.write(bytes);
            out.close();

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
