package io.scalecube.seed;

import io.scalecube.app.decoration.Logo;
import io.scalecube.app.packages.PackageInfo;
import io.scalecube.config.ConfigRegistry;
import io.scalecube.config.ConfigRegistrySettings;
import io.scalecube.config.audit.Slf4JConfigEventListener;
import io.scalecube.config.source.ClassPathConfigSource;
import io.scalecube.config.source.SystemEnvironmentConfigSource;
import io.scalecube.config.source.SystemPropertiesConfigSource;
import io.scalecube.net.Address;
import io.scalecube.services.Microservices;
import io.scalecube.services.ServiceEndpoint;
import io.scalecube.services.discovery.ScalecubeServiceDiscovery;
import io.scalecube.services.discovery.api.ServiceDiscovery;
import java.nio.file.Path;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Seed Node server. */
public class SeedRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(SeedRunner.class);

  private static final String DECORATOR =
      "#######################################################################";

  /**
   * Main runner.
   *
   * @param args program arguments
   */
  public static void main(String[] args) {
    ConfigRegistry configRegistry = ConfigBootstrap.configRegistry();

    Config config =
        configRegistry
            .objectProperty(Config.class.getPackage().getName(), Config.class)
            .value()
            .orElseThrow(() -> new IllegalStateException("Couldn't load config"));

    LOGGER.info(DECORATOR);
    LOGGER.info("Starting Seed, {}", config);
    LOGGER.info(DECORATOR);

    Microservices.builder()
        .discovery(defServiceDiscovery(config))
        .start()
        .doOnNext(SeedRunner::newLogo)
        .block()
        .onShutdown()
        .block();
  }

  private static Function<ServiceEndpoint, ServiceDiscovery> defServiceDiscovery(Config config) {
    return endpoint ->
        new ScalecubeServiceDiscovery(endpoint)
            .options(opts -> opts.memberAlias(config.memberAlias()))
            .transport(opts -> opts.port(config.discoveryPort()))
            .membership(opts -> opts.seedMembers(config.seedAddresses()));
  }

  private static void newLogo(Microservices microservices) {
    Logo.from(new PackageInfo())
        .ip(microservices.discovery().address().host())
        .port("" + microservices.discovery().address().port())
        .draw();
  }

  public static class Config {

    Integer discoveryPort;
    List<String> seeds;
    String memberAlias;

    public Integer discoveryPort() {
      return discoveryPort;
    }

    public String memberAlias() {
      return memberAlias;
    }

    List<Address> seedAddresses() {
      return seeds.stream().map(Address::from).collect(Collectors.toList());
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", Config.class.getSimpleName() + "[", "]")
          .add("discoveryPort=" + discoveryPort)
          .add("seeds=" + seeds)
          .add("memberAlias=" + memberAlias)
          .toString();
    }
  }

  static class ConfigBootstrap {

    private static final Pattern CONFIG_FILE_PATTERN =
        Pattern.compile("(.*)config(.*)?\\.properties");
    private static final Predicate<Path> CONFIG_PATH_PREDICATE =
        path -> CONFIG_FILE_PATTERN.matcher(path.getFileName().toString()).matches();

    static ConfigRegistry configRegistry() {
      return ConfigRegistry.create(
          ConfigRegistrySettings.builder()
              .addListener(new Slf4JConfigEventListener())
              .addLastSource("sys_prop", new SystemPropertiesConfigSource())
              .addLastSource("env_var", new SystemEnvironmentConfigSource())
              .addLastSource("cp", new ClassPathConfigSource(CONFIG_PATH_PREDICATE))
              .jmxEnabled(false)
              .build());
    }
  }
}
