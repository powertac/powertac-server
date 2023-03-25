package org.powertac.visualizer.web;

import com.codahale.metrics.annotation.Timed;
import org.apache.commons.lang3.StringUtils;
import org.powertac.visualizer.domain.PersistentToken;
import org.powertac.visualizer.repository.PersistentTokenRepository;
import org.powertac.visualizer.repository.UserRepository;
import org.powertac.visualizer.security.SecurityUtils;
import org.powertac.visualizer.service.UserService;
import org.powertac.visualizer.service.dto.UserDTO;
import org.powertac.visualizer.web.rest.vm.ManagedUserVM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing the current user's account.
 */
@Controller
@RequestMapping("/")
public class IndexResource {

    private final Logger log = LoggerFactory.getLogger(IndexResource.class);

    public IndexResource() {
    }

    @GetMapping("/")
    @Timed
    public RedirectView getRedirect() {
        return new RedirectView("index.html");
    }

}
