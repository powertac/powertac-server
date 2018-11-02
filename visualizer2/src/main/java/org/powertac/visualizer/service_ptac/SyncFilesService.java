package org.powertac.visualizer.service_ptac;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.powertac.visualizer.domain.Game;
import org.powertac.visualizer.domain.User;
import org.powertac.visualizer.domain.enumeration.FileType;
import org.powertac.visualizer.service.FileService;
import org.powertac.visualizer.service.GameService;
import org.powertac.visualizer.service.UserService;
import org.powertac.visualizer.service_ptac.VisualizerService.VisualizerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SyncFilesService {

    static private Logger log = LoggerFactory.getLogger(SyncFilesService.class);

    @Autowired
    private VisualizerService visualizerService;

    @Autowired
    private UserService userService;

    @Autowired
    private FileService fileService;

    @Autowired
    private GameService gameService;

    @Scheduled(fixedDelay = 30000, initialDelay = 60000)
    @Transactional()
    public void syncFileSystem() {
        if (visualizerService.getState().equals(VisualizerState.RUNNING)
        || visualizerService.getState().equals(VisualizerState.WAITING)) {
            log.debug("Skipping sync, game in progress");
            return;
        }

        long t = System.currentTimeMillis();
        log.debug("Starting at " + new java.util.Date());

        sync(FileType.TRACE, FileType.DIRECTORY_LOG, new String[] {".trace"});
        sync(FileType.STATE, FileType.DIRECTORY_LOG, new String[] {".state"});
        sync(FileType.BOOT, FileType.DIRECTORY_BOOT, new String[] {".xml"});
        sync(FileType.SEED, FileType.DIRECTORY_SEED, new String[] {".state"});
        sync(FileType.CONFIG, FileType.DIRECTORY_CONFIG, new String[] {".properties", ".props"});
        sync(FileType.WEATHER, FileType.DIRECTORY_WEATHER, new String[] {".xml"});

        t = System.currentTimeMillis() - t;
        log.debug("Finished after " + t + " milliseconds");
    }


    private boolean sync(FileType type, String typedir, String[] suffixes) {
        int additions = 0, deletions = 0;
        File root = new File(FileType.DIRECTORY_ROOT);
        log.trace("Syncing type " + type + "...");
        for (File userdir : root.listFiles()) {
            if (!userdir.isDirectory()) {
                // regular file in root dir, skip
                continue;
            }

            String login = userdir.getName();
            User user = userService.getUserByLogin(login).orElse(null);
            if (user == null) {
                // not a user dir (or user was deleted?)
                continue;
            }
            log.trace("Syncing user " + login + "...");

            // get the DB's current list for this user and type
            List<org.powertac.visualizer.domain.File> expectedList =
                fileService.findByOwnerIsCurrentUser(login, type)
                .stream()
                .sorted(new Comparator<org.powertac.visualizer.domain.File>() {
                    @Override
                    public int compare(org.powertac.visualizer.domain.File o1,
                                        org.powertac.visualizer.domain.File o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                })
                .collect(Collectors.toList());

            userdir = new File(userdir, typedir);
            if (!userdir.exists() || !userdir.isDirectory()) {
                // FS doesn't contain expected subdir -- remove all from DB
                for (org.powertac.visualizer.domain.File expected: expectedList) {
                    log.debug("Going to delete file " + expected.getName());
                    for (Game game: gameService.findByAssociatedFile(expected)) {
                        log.debug("  ... also have to clear refs in Game " + game.getName());
                        clearFileRefsFromGame(game, expected.getId());
                    }
                    fileService.delete(expected);
                }
                continue;
            }

            log.trace("Entering " + userdir.getAbsolutePath());

            // Get the FS's current list for this user and type
            List<File> foundList = new LinkedList<>();
            for (File log : userdir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                  for (String suffix : suffixes) {
                    if (name.endsWith(suffix)) {
                      return true;
                    }
                  }
                  return false;
                }
            })) {
                if (log.isFile()) {
                    foundList.add(log);
                }
            }
            foundList.sort(new Comparator<File>() {
                @Override
                public int compare (File o1, File o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });

            log.trace("Expected for user " + login + ":");

            for (org.powertac.visualizer.domain.File file : expectedList) {
                log.trace("    " + file.getName());
            }
            log.trace("Found for user " + login + ":");
            for (File file : foundList) {
                log.trace("    " + file.getName());
            }

            // Now sync by going through both lists in order
            Iterator<org.powertac.visualizer.domain.File> expectedIt = expectedList.iterator();
            Iterator<File> foundIt = foundList.iterator();
            org.powertac.visualizer.domain.File expected = expectedIt.hasNext() ? expectedIt.next() : null;
            File found = foundIt.hasNext() ? foundIt.next() : null;
            while (expected != null || found != null) {
                if (expected == null) {
                    log.debug("Creating for user " + login + ": " + found.getName());
                    fileService.createFile(type, found.getName(), user);
                    found = foundIt.hasNext() ? foundIt.next() : null;
                    additions++;
                } else if (found == null) {
                    log.debug("Deleting for user " + login + ": " + expected.getName());
                    for (Game game: gameService.findByAssociatedFile(expected)) {
                        log.debug("  ... also have to clear refs in Game " + game.getName());
                        clearFileRefsFromGame(game, expected.getId());
                    }
                    fileService.delete(expected);
                    expected = expectedIt.hasNext() ? expectedIt.next() : null;
                    deletions++;
                } else {
                    int cmp = expected.getName().compareTo(found.getName());
                    if (cmp > 0) {
                        log.debug("Creating for user " + login + ": " + found.getName());
                        fileService.createFile(type, found.getName(), user);
                        found = foundIt.hasNext() ? foundIt.next() : null;
                        additions++;
                    } else if (cmp < 0) {
                        log.debug("Deleting for user " + login + ": " + expected.getName());
                        for (Game game: gameService.findByAssociatedFile(expected)) {
                            log.debug("  ... also have to clear refs in Game " + game.getName());
                            clearFileRefsFromGame(game, expected.getId());
                        }
                        fileService.delete(expected);
                        expected = expectedIt.hasNext() ? expectedIt.next() : null;
                        deletions++;
                    } else {
                        found = foundIt.hasNext() ? foundIt.next() : null;
                        expected = expectedIt.hasNext() ? expectedIt.next() : null;
                    }
                }
            }
        }

        log.trace(additions + " additions, " + deletions + " deletions");

        return additions != 0 || deletions != 0;
    }

    private void clearFileRefsFromGame(Game game, Long fileId) {
        boolean changed = false;
        if (game.getTraceFileId() == fileId) {
            game.setTraceFile(null);
            changed = true;
        }
        if (game.getStateFileId() == fileId) {
            game.setStateFile(null);
            changed = true;
        }
        if (game.getSeedFileId() == fileId) {
            game.setSeedFile(null);
            changed = true;
        }
        if (game.getConfigFileId() == fileId) {
            game.setConfigFile(null);
            changed = true;
        }
        if (game.getWeatherFileId() == fileId) {
            game.setWeatherFile(null);
            changed = true;
        }
        if (game.getBootFileId() == fileId) {
            game.setBootFile(null);
            changed = true;
        }
        if (changed) {
            gameService.save(game);
        }
    }
}
