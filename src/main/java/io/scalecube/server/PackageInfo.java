package io.scalecube.server;

import io.scalecube.transport.Address;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Information provider about the environment and the package of this instance.
 *
 */
public class PackageInfo {

  private final Properties properties = new Properties();

  /**
   * Runtime Environment information provider.
   */
  public PackageInfo() {
    if (System.getenv("SC_HOME") != null) {
      String path = System.getenv("SC_HOME");

      try {
        FileInputStream in = new FileInputStream(path + "package.properties");
        properties.load(in);
      } catch (IOException e) {
        System.out.println("cannot open file: " + path + "package.properties cause:" + e.getCause());
        defaultProps();
      }
    } else {
      InputStream stream = PackageInfo.class.getResourceAsStream("package.properties");
      if (stream != null) {
        try {
          properties.load(stream);
        } catch (IOException e) {
          defaultProps();
        }
      } else {
        defaultProps();
      }
    }
  }

  private void defaultProps() {
    properties.put("artifactId", "Development");
    properties.put("version", "Development");
    properties.put("groupId", "Development");
    properties.put("version", "Development");
    properties.put("version", "Development");
  }

  public String version() {
    return properties.getProperty("version");
  }

  public String groupId() {
    return properties.getProperty("groupId");
  }

  public String artifactId() {
    return properties.getProperty("artifactId");
  }

  public static String java() {
    return System.getProperty("java.version");
  }

  public static String os() {
    return System.getProperty("os.name");
  }

  /**
   * returns host name of the current running host.
   * 
   * @return host name.
   */
  public static String hostname() {
    String result = getVariable("HOSTNAME", "unknown");
    if ("unknown".equals(result)) {
      return getHostName("unknown");
    } else {
      return result;
    }
  }

  private static String getHostName(String defaultValue) {
    String hostname = defaultValue;
    try {
      InetAddress addr = InetAddress.getLocalHost();
      hostname = addr.getHostName();
    } catch (UnknownHostException ex) {
      hostname = defaultValue;
    }
    return hostname;
  }

  public static String pid() {
    String vmName = ManagementFactory.getRuntimeMXBean().getName();
    int pids = vmName.indexOf("@");
    String pid = vmName.substring(0, pids);
    return pid;
  }


  /**
   * Returns API Gateway API.
   */
  public static int gatewayPort() {
    String port = getVariable("API_GATEWAY_PORT", "8081");
    return Integer.valueOf(port);
  }

  /**
   * Resolve seed address from environment variable or system property.
   * 
   * @return seed address as string for example localhost:4801.
   */
  public static Address[] seedAddress() {
    String list = getVariable("SC_SEED_ADDRESS", null);
    if (list != null && !list.isEmpty()) {
      String[] hosts = list.split(",");
      List<Address> seedList = Arrays.asList(hosts).stream().filter(predicate -> !predicate.isEmpty())
          .map(mapper -> mapper.trim())
          .map(hostAndPort -> {
            return Address.from(hostAndPort);
          }).collect(Collectors.toList());
      return seedList.toArray(new Address[seedList.size()]);
    } else {
      return null;
    }
  }

  private static String getVariable(String name, String defaultValue) {
    if (System.getenv(name) != null) {
      return System.getenv(name);
    }
    if (System.getProperty(name) != null) {
      return System.getProperty(name);
    }
    return defaultValue;
  }

}
