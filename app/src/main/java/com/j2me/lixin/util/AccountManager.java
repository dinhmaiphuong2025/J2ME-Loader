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
import java.util.List;

import com.j2me.lixin.config.Config;

public class AccountManager {
	public static final String DEFAULT_ACCOUNT = "Mặc định";

	public static class Account {
		private String name;
		private final String dir;

		public Account(String name, String dir) {
			this.name = name;
			this.dir = (dir != null) ? dir : "";
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDir() {
			return dir;
		}

		public boolean isDefault() {
			return dir == null || dir.isEmpty();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Account account = (Account) o;
			return name.equals(account.name);
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}
	}

	public static String sanitizeAccountName(String name) {
		if (name == null) return "";
		String sanitized = name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
		return sanitized.isEmpty() ? DEFAULT_ACCOUNT : sanitized;
	}

	private static File getAccountFile(String appPath) {
		File appDir = new File(appPath);
		return new File(Config.getConfigsDir(), appDir.getName() + ".accounts");
	}

	public static List<Account> getAccountList(String appPath) {
		List<Account> accounts = new ArrayList<>();
		File file = getAccountFile(appPath);
		if (file.exists()) {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
				String line;
				int lineIndex = 0;
				while ((line = reader.readLine()) != null) {
					line = line.trim();
					if (!line.isEmpty()) {
						if (line.contains("|")) {
							String[] parts = line.split("\\|", 2);
							String name = parts[0].trim();
							String dir = parts.length > 1 ? parts[1].trim() : "";
							if (!name.isEmpty()) {
								accounts.add(new Account(name, dir));
							}
						} else {
							// Legacy format
							String dir = (lineIndex == 0) ? "" : line;
							accounts.add(new Account(line, dir));
						}
						lineIndex++;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (accounts.isEmpty()) {
			accounts.add(new Account(DEFAULT_ACCOUNT, ""));
			saveAccountList(appPath, accounts);
		}
		return accounts;
	}

	public static void saveAccountList(String appPath, List<Account> accounts) {
		File file = getAccountFile(appPath);
		File parent = file.getParentFile();
		if (parent != null && !parent.exists()) {
			parent.mkdirs();
		}
		try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
			for (Account acc : accounts) {
				writer.println(acc.getName() + "|" + acc.getDir());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static List<String> getAccounts(String appPath) {
		List<Account> list = getAccountList(appPath);
		List<String> names = new ArrayList<>();
		for (Account acc : list) {
			names.add(acc.getName());
		}
		return names;
	}

	public static Account getAccountByName(String appPath, String name) {
		List<Account> list = getAccountList(appPath);
		for (Account acc : list) {
			if (acc.getName().equals(name)) {
				return acc;
			}
		}
		return new Account(name, sanitizeAccountName(name));
	}

	public static Account addAccount(String appPath, String accountName) {
		if (accountName == null) return null;
		accountName = sanitizeAccountName(accountName);
		List<Account> accounts = getAccountList(appPath);
		for (Account acc : accounts) {
			if (acc.getName().equalsIgnoreCase(accountName)) {
				return acc;
			}
		}
		Account newAcc = new Account(accountName, accountName);
		accounts.add(newAcc);
		saveAccountList(appPath, accounts);
		return newAcc;
	}

	public static boolean renameAccount(String appPath, String oldName, String newName) {
		if (oldName == null || newName == null) return false;
		newName = sanitizeAccountName(newName);
		if (newName.isEmpty() || oldName.equals(newName)) return false;

		List<Account> accounts = getAccountList(appPath);
		Account target = null;
		for (Account acc : accounts) {
			if (acc.getName().equals(oldName)) {
				target = acc;
				break;
			}
		}
		if (target != null) {
			target.setName(newName);
			saveAccountList(appPath, accounts);
			return true;
		}
		return false;
	}
}
