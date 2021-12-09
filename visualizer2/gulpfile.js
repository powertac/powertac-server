// Generated on 2017-02-24 using generator-jhipster 4.0.6
'use strict';

var gulp = require('gulp'),
    rev = require('gulp-rev'),
    templateCache = require('gulp-angular-templatecache'),
    htmlmin = require('gulp-htmlmin'),
    imagemin = require('gulp-imagemin'),
    ngConstant = require('gulp-ng-constant'),
    rename = require('gulp-rename'),
    eslint = require('gulp-eslint'),
    del = require('del'),
    browserSync = require('browser-sync'),
    KarmaServer = require('karma').Server,
    parseConfig = require('karma').config.parseConfig,
    plumber = require('gulp-plumber'),
    changed = require('gulp-changed'),
    gulpIf = require('gulp-if');

var handleErrors = require('./gulp/handle-errors'),
    serve = require('./gulp/serve'),
    util = require('./gulp/utils'),
    copy = require('./gulp/copy'),
    inject = require('./gulp/inject'),
    build = require('./gulp/build');

var config = require('./gulp/config');

gulp.task('clean', function () {
    return del([config.dist], { dot: true });
});

gulp.task('copy:fonts', copy.fonts);

gulp.task('copy:common', copy.common);

gulp.task('copy:images', copy.images);

gulp.task('copy', gulp.series('copy:fonts', 'copy:common'));

gulp.task('images', function () {
    return gulp.src(config.app + 'content/images/**')
        .pipe(plumber({errorHandler: handleErrors}))
        .pipe(changed(config.dist + 'content/images'))
        .pipe(imagemin({optimizationLevel: 5, progressive: true, interlaced: true}))
        .pipe(rev())
        .pipe(gulp.dest(config.dist + 'content/images'))
        .pipe(rev.manifest(config.revManifest, {
            base: config.dist,
            merge: true
        }))
        .pipe(gulp.dest(config.dist))
        .pipe(browserSync.reload({stream: true}));
});

gulp.task('styles', function () {
    return gulp.src(config.app + 'content/css')
        .pipe(browserSync.reload({stream: true}));
});

gulp.task('template:index', function () {
    return gulp.src(config.app + 'index.template.html')
        .pipe(rename('index.html'))
        .pipe(gulp.dest(config.app));
});

gulp.task('template:test', function () {
    return gulp.src(config.test + 'karma.conf.template.js')
        .pipe(rename('karma.conf.js'))
        .pipe(gulp.dest(config.test));
});

gulp.task('inject:app', inject.app);

gulp.task('inject:vendor', inject.vendor);

gulp.task('inject:test', gulp.series('template:test', inject.test));

gulp.task('inject:dep', gulp.series('inject:test', 'inject:vendor'));

gulp.task('inject', gulp.series('inject:dep', 'inject:app'));

gulp.task('html', function () {
    return gulp.src(config.app + 'app/**/*.html')
        .pipe(htmlmin({collapseWhitespace: true}))
        .pipe(templateCache({
            module: 'visualizer2App',
            root: 'app/',
            moduleSystem: 'IIFE'
        }))
        .pipe(gulp.dest(config.tmp));
});

gulp.task('assets:prod', gulp.series('images', 'styles', 'html', 'copy:images', build));

gulp.task('ngconstant:dev', function () {
    var cfg = require('./src/main/resources/config/mode.json');
    if (cfg.mode === '@visualizer.mode@') {
      cfg.mode = 'research';
    }
    return ngConstant({
        name: 'visualizer2App',
        constants: {
            VERSION: util.parseVersion(),
            DEBUG_INFO_ENABLED: true,
            CONFIG: {
                mode: cfg.mode
            }
        },
        template: config.constantTemplate,
        stream: true
    })
    .pipe(rename('app.constants.js'))
    .pipe(gulp.dest(config.app + 'app/'));
});

gulp.task('ngconstant:prod', function () {
    var cfg = require('./src/main/resources/config/mode.json');
    if (cfg.mode === '@visualizer.mode@') {
      cfg.mode = 'research';
    }
    return ngConstant({
        name: 'visualizer2App',
        constants: {
            VERSION: util.parseVersion().replace(/-SNAPSHOT/, ''),
            DEBUG_INFO_ENABLED: false,
            CONFIG: {
                mode: cfg.mode
            }
        },
        template: config.constantTemplate,
        stream: true
    })
    .pipe(rename('app.constants.js'))
    .pipe(gulp.dest(config.app + 'app/'));
});

// check app for eslint errors
gulp.task('eslint', function () {
    return gulp.src(['gulpfile.js', config.app + 'app/**/*.js'])
        .pipe(plumber({errorHandler: handleErrors}))
        .pipe(eslint())
        .pipe(eslint.format())
        .pipe(eslint.failOnError());
});

// check app for eslint errors anf fix some of them
gulp.task('eslint:fix', function () {
    return gulp.src(config.app + 'app/**/*.js')
        .pipe(plumber({errorHandler: handleErrors}))
        .pipe(eslint({
            fix: true
        }))
        .pipe(eslint.format())
        .pipe(gulpIf(util.isLintFixed, gulp.dest(config.app + 'app')));
});

gulp.task('test', gulp.series('inject:test', 'ngconstant:dev', function karma(done) {
    parseConfig(
        __dirname + '/' + config.test + 'karma.conf.js',
        { singleRun: true },
        { promiseConfig: true, throwErrors: true }
    ).then(
        (karmaConfig) => {
            new KarmaServer(karmaConfig, done).start();
        }
    );
}));


gulp.task('watch', function () {
    gulp.watch('bower.json', ['install']);
    gulp.watch(['gulpfile.js', 'pom.xml'], ['ngconstant:dev']);
    gulp.watch(config.app + 'content/css/**/*.css', ['styles']);
    gulp.watch(config.app + 'content/images/**', ['images']);
    gulp.watch(config.app + 'app/**/*.js', ['inject:app']);
    gulp.watch([config.app + '*.html', config.app + 'app/**', config.app + 'i18n/**']).on('change', browserSync.reload);
});

gulp.task('install', gulp.series('template:index', 'inject:dep', 'ngconstant:dev', 'inject:app'));

gulp.task('serve', gulp.series('install', serve));

gulp.task('build', gulp.series('clean', 'template:index', 'copy', 'inject:vendor', 'ngconstant:prod', 'inject:app', 'assets:prod'));

gulp.task('default', gulp.series('serve'));
