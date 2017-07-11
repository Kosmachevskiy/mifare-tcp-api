package app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@SpringBootApplication
public class MifareTcpApiApplication {

	@Autowired
	private Environment environment;

	private boolean demoMode;
	private String key;
	private int block;
	private int port;
	private int terminalIndex;

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(MifareTcpApiApplication.class);
		application.setBannerMode(Banner.Mode.OFF);
		application.run(args);
	}

	@PostConstruct
	public void init() {
		if (isNull(environment.getProperty("mode")))
			demoMode = false;
		else
			demoMode = environment.getProperty("mode").equals("demo") ? true : false;
		key = nonNull(environment.getProperty("key")) ? (environment.getProperty("key")) : "FFFFFFFFFFFF";
		block = nonNull(environment.getProperty("block")) ? Integer.parseInt(environment.getProperty("block")) : 60;
		port = nonNull(environment.getProperty("port")) ? Integer.parseInt(environment.getProperty("port")) : 6969;
		terminalIndex = nonNull(environment.getProperty("terminal")) ? Integer.parseInt(environment.getProperty("terminal")) : 0;

		System.out.println("Application starts with following parameters:");
		System.out.println("-Demo mode: " + demoMode);
		System.out.println("-Access key: " + key);
		System.out.println("-TCP server port: " + port);
		System.out.println("-Block number: " + block);
		System.out.println("-Automatic terminal selection: " + (terminalIndex == 0 ? true : false));
	}

	@Bean
	public SmartCartService smartCartService() {
		if (demoMode)
			return new FakeSmartCartService();
		else {
			return new SmartCartServiceImpl(block, key, terminalIndex);
		}
	}

	@Bean
	public Server server() {
		return new Server(port);
	}
}
