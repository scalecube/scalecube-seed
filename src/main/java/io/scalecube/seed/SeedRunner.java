package io.scalecube.seed;

import io.scalecube.config.ConfigRegistry;
import io.scalecube.config.ConfigRegistrySettings;
import io.scalecube.config.audit.Slf4JConfigEventListener;
import io.scalecube.config.source.ClassPathConfigSource;
import io.scalecube.config.source.SystemEnvironmentConfigSource;
import io.scalecube.config.source.SystemPropertiesConfigSource;
import io.scalecube.services.Microservices;
import io.scalecube.services.transport.api.Address;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
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
   * @throws InterruptedException exception thrown
   */
  public static void main(String[] args) throws InterruptedException {
    LOGGER.info("Reading seed configuration");
    ConfigRegistry configRegistry = ConfigBootstrap.configRegistry();

    Config config =
        configRegistry
            .objectProperty("io.scalecube.seed", Config.class)
            .value()
            .orElseThrow(() -> new IllegalStateException("Couldn't load config"));

    LOGGER.info(DECORATOR);
    LOGGER.info("Starting Seed with {}", config);
    LOGGER.info(DECORATOR);

    Microservices.builder()
        .discovery(
            options ->
                options
                    .seeds(
                        Arrays.stream(config.seedAddresses())
                            .map(address -> Address.create(address.host(), address.port()))
                            .toArray(Address[]::new))
                    .port(config.discoveryPort)
                    .memberHost(config.memberHost)
                    .memberPort(config.memberPort))
        .startAwait();

    Thread.currentThread().join();
  }

  public static class Config {

    Integer discoveryPort;
    List<String> seeds;
    String memberHost;
    Integer memberPort;

    /**
     * Returns seeds as an {@link Address}'s array.
     *
     * @return {@link Address}'s array
     */
    Address[] seedAddresses() {
      return Optional.ofNullable(seeds)
          .map(
              seeds ->
                  seeds
                      .stream()
                      .filter(s -> !s.isEmpty())
                      .map(Address::from)
                      .toArray(Address[]::new))
          .orElse(new Address[0]);
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder("Config{");
      sb.append(", discoveryPort=").append(discoveryPort);
      sb.append(", seeds=").append(seeds);
      sb.append(", memberHost=").append(memberHost);
      sb.append(", memberPort=").append(memberPort);
      sb.append('}');
      return sb.toString();
    }
  }

  static class ConfigBootstrap {

    private static final Pattern CONFIG_PATTERN = Pattern.compile("(.*)config(.*)?\\.properties");
    private static final Predicate<Path> PATH_PREDICATE =
        path -> CONFIG_PATTERN.matcher(path.toString()).matches();

    /**
     * ConfigRegistry method factory.
     *
     * @return configRegistry
     */
    static ConfigRegistry configRegistry() {
      return ConfigRegistry.create(
          ConfigRegistrySettings.builder()
              .addListener(new Slf4JConfigEventListener())
              .addLastSource("sys_prop", new SystemPropertiesConfigSource())
              .addLastSource("env_var", new SystemEnvironmentConfigSource())
              .addLastSource("cp", new ClassPathConfigSource(PATH_PREDICATE))
              .jmxEnabled(false)
              .build());
    }
  }
}
