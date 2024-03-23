# async-profiler-actuator
## Introdution
- [AsyncProfiler](https://github.com/async-profiler/async-profiler) is a proven tool for troubleshooting Java
  applications in variety of situations.
- [Spring Boot](https://github.com/spring-projects/spring-boot) is an extremely popular platform for building production
  ready Java applications.

The idea behind this small library is the integration of AsyncProfiler in form of a Spring Boot actuator.

Originally, [issue #23988](https://github.com/spring-projects/spring-boot/issues/23988) has been opened to integrate
this directly into Spring Boot back in year 2020. Later in 2024 it was suggested that this functionality is better to be
implemented as an external add-on to Spring Boot. This library attempts to do just that.

In fact, there's probably more documentation here than the actual code, goal of this project being to provide an easy way
to add this tool to your "tool-belt" and start using it.

Currently, actuator is still in a passive development phase (being a pet-project of the author), not published to any global
repositories, but the code itself might be useful enough, if you care to integrate it within your Spring Boot application.

## Usage
For usage example, please check out 
[async-profiler-actuator-demo](https://github.com/nifties-dev/async-profiler-actuator-demo) peer project.

In short:
 * Set up GitHub username and PAT environment variables as described in [Using a published package](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#using-a-published-package)
   document and add https://maven.pkg.github.com/nifties-dev/async-profiler-actuator
   repository to your build (alternatively, you can clone, build and publish this library to your own repository);
 * Add depedency to _dev.nifties.integration:async-profiler-actuator_;
 * Add _@EnableAsyncProfiler_ annotation to your Spring _@Configuration_;
 * Make sure _profiler_ actuator endpoint is exposed in _application.yml_ or _.properties_ in _management.endpoints.web.exposure_;
 * Invoke _/actuator/profiler_ via HTTP (again, see demo project's homepage for examples).
