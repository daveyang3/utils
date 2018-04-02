package java.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;

public class EnvironmentUtils {
	private final static Logger LOG = Logger.getLogger(EnvironmentUtils.class);
	public final static String DEV = "dev";
	public final static String PROD = "prod";
	public final static String ENV_LINK = "-";
	public final static String PROP_FILE=".properties";

	private static Map<String, String> PROPS_CAHE = new LinkedHashMap<>();

	static ResourceBundle resource = ResourceBundle.getBundle("application");

	public static String getEnv() {
		String env=resource.getString("spring.profiles.active");
		if(StringUtils.isEmpty(env)){
			env=DEV;
		}
		return env.trim();
	}

	public static String getProperty(String key) {
		return getKey(key);
	}

	public static String getKey(String key) {
		if (key == null || StringUtils.isEmpty(key.trim())) {
			return "";
		}
		String value = "";
		if (PROPS_CAHE.isEmpty()) {
			value = getAllProperties().get(key.trim());
		} else {
			value = PROPS_CAHE.get(key.trim());
		}

		return value;
	}

	public static Properties getProperties() {
		Map<String, String> map = getAllProperties();
		Properties prop = new Properties();
		prop.putAll(map);
		return prop;
	}

	public static Map<String, String> getAllProperties() {
		Map<String, String> map = new LinkedHashMap<>();
		if (PROPS_CAHE.isEmpty()) {
			Set<String> paths = getAllPropFilePathByEnv(getEnv());
			for (String p : paths) {
				Properties prop = new Properties();
				try {
					prop.load(new BufferedReader(new FileReader(p)));
					for (Object key : prop.keySet()) {
						map.put(key.toString().trim(), prop.getProperty(key.toString()).trim());
						LOG.info(key.toString().trim() + "=" + map.get(key.toString().trim()));
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			PROPS_CAHE.putAll(map);
		} else {
			map.putAll(PROPS_CAHE);
		}
		return map;
	}

	/**
	 * get all the prop file from src/main/resources
	 * 
	 * the prop file format like : <br>
	 * application-{env}.properties or {env}/a/b/application.properties
	 * 
	 * @param env
	 * @return
	 */
	private static Set<String> getAllPropFilePathByEnv(String env) {
		Set<String> set = new HashSet<>();
		String propResourcesPath = Thread.currentThread().getContextClassLoader().getResource("/").getPath();
		LOG.info("resources path:" + propResourcesPath);
		readDirFiles(propResourcesPath, set, env, false);
		return set;
	}

	/**
	 * only read the .properties file under the path
	 * @param path
	 * @param set
	 * @param env
	 * @param isTarget
	 */
	private static void readDirFiles(String path, Set<String> set, String env, boolean isTarget) {

		File file = new File(path);
		File[] tempList = file.listFiles();
		if (tempList.length > 0) {
			for (File f : tempList) {
				// get the prop file

				if (f.isFile()&&f.getName().toLowerCase().indexOf(PROP_FILE)>-1) {
					// get the prop files wtih *-{env}.properties folder
					if (isTarget || f.getName().toLowerCase().indexOf((ENV_LINK + env).toLowerCase()) > -1) {
						LOG.info("loaded [" + env + "] resources path: " + f.toString());
						set.add(f.getAbsolutePath());
					} else {
						LOG.info("Invalid resources file: " + f.toString());
					}
				}

				if (f.isDirectory()) {
					// get the prop files under {env} folder
					if (f.getName().toLowerCase().indexOf(env.toLowerCase()) > -1
							|| isContainEnv(f.getAbsolutePath(), env)) {
						readDirFiles(path + File.separator + f.getName(), set, env, true);
					} else {
						LOG.info("Invalid resources path: " + f.toString());
					}
				}
			}

		}
	}

	private static boolean isContainEnv(String path, String env) {
		boolean flag = false;
		String tmpPath = path.replaceAll("\\\\", "/");
		System.out.println(tmpPath);
		flag = Arrays.asList(tmpPath.toLowerCase().split("/")).contains(env.toLowerCase());
		return flag;
	}

	/*
	 * public static void main(String[] args) { String pa =
	 * "C:\\a\\b\\dev\\application.properties";
	 * System.out.println(isContainEnv(pa, "dev")); }
	 */
}
