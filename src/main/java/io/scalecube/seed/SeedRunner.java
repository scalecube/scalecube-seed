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
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Function;
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
            .orElseThrow(() -> new IllegalStateException("Couldn't load config: " + Config.class));

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

    private int discoveryPort = 4801;
    private List<String> seeds = Collections.emptyList();
    private String memberAlias = "seed";

    public int discoveryPort() {
      return discoveryPort;
    }

    public String memberAlias() {
      return memberAlias;
    }

    public List<Address> seedAddresses() {
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

    static ConfigRegistry configRegistry() {
      return ConfigRegistry.create(
          ConfigRegistrySettings.builder()
              .jmxEnabled(false)
              .addListener(new Slf4JConfigEventListener())
              .addLastSource("environment", new SystemEnvironmentConfigSource())
              .addLastSource("system", new SystemPropertiesConfigSource())
              .addLastSource(
                  "classpath",
                  ClassPathConfigSource.createWithPattern(
                      "config.properties", Collections.emptyList()))
              .build());
    }
  }
}
