package com.j2me.lixin.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.j2me.lixin.config.Config;

public class AccountManager {
	public static final String DEFAULT_ACCOUNT = "Mặc định";

	public static String sanitizeAccountName(String name) {
		if (name == null) return "";
		String sanitized = name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
		return sanitized.isEmpty() ? DEFAULT_ACCOUNT : sanitized;
	}

	private static File getAccountFile(String appPath) {
		File appDir = new File(appPath);
		return new File(Config.getConfigsDir(), appDir.getName() + ".accounts");
	}

	public static List<String> getAccounts(String appPath) {
		Set<String> accounts = new LinkedHashSet<>();
		accounts.add(DEFAULT_ACCOUNT);
		File file = getAccountFile(appPath);
		if (file.exists()) {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					line = line.trim();
					if (!line.isEmpty()) {
						accounts.add(line);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return new ArrayList<>(accounts);
	}

	public static void addAccount(String appPath, String accountName) {
		if (accountName == null) return;
		accountName = sanitizeAccountName(accountName);
		if (accountName.isEmpty() || DEFAULT_ACCOUNT.equals(accountName)) return;

		List<String> accounts = getAccounts(appPath);
		if (!accounts.contains(accountName)) {
			accounts.add(accountName);
			saveAccounts(appPath, accounts);
		}
	}

	public static void saveAccounts(String appPath, List<String> accounts) {
		File file = getAccountFile(appPath);
		File parent = file.getParentFile();
		if (parent != null && !parent.exists()) {
			parent.mkdirs();
		}
		try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
			for (String acc : accounts) {
				writer.println(acc);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
