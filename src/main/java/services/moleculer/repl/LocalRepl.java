/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2017 Andras Berkes [andras.berkes@programmer.net]<br>
 * Based on Moleculer Framework for NodeJS [https://moleculer.services].
 * <br><br>
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:<br>
 * <br>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.<br>
 * <br>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package services.moleculer.repl;

import static io.datatree.dom.PackageScanner.scan;
import static services.moleculer.util.CommonUtils.nameOf;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;

/**
 * Local interactive console (uses System.in and System.out). To start console,
 * type<br>
 * 
 * <pre>
 * ServiceBroker broker = ServiceBroker.builder().build();
 * broker.start();
 * broker.repl();
 * </pre>
 * 
 * Start telnet-based console:
 * 
 * <pre>
 * broker.repl(false);
 * </pre>
 * 
 * @see RemoteRepl
 */
@Name("Local REPL Console")
public class LocalRepl extends Repl implements Runnable {

	// --- PROPERTIES ---

	/**
	 * Java package(s) where the custom (user-defined) commands are located.
	 */
	protected String[] packagesToScan = {};

	// --- MAP OF THE REGISTERED COMMANDS ---

	protected ConcurrentHashMap<String, Command> commands = new ConcurrentHashMap<>(64);

	protected ConcurrentHashMap<String, Command> postDefinedCommands = new ConcurrentHashMap<>();
	
	// --- VARIABLES ---

	protected ExecutorService executor;

	protected String lastCommand = "help";

	protected LocalReader reader;

	protected ColorWriter colorWriter = new ColorWriter();

	// --- CONSTRUCTORS ---

	public LocalRepl() {
	}

	public LocalRepl(String... packagesToScan) {
		this.packagesToScan = packagesToScan;
	}

	// --- START CONSOLE INSTANCE ---

	/**
	 * Initializes console instance.
	 *
	 * @param broker
	 *            parent ServiceBroker
	 */
	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);

		// We ensure that the first command will run fast
		if (isEnabled()) {
			broker.getConfig().getExecutor().execute(() -> {
				try {
					onCommand(new PrintWriter(new Writer() {

						@Override
						public void write(char[] cbuf, int off, int len) throws IOException {
						}

						@Override
						public void flush() throws IOException {
						}

						@Override
						public void close() throws IOException {
						}

					}), "info");
				} catch (Exception ignored) {
				}
			});
		}
	}

	// --- START READING INPUT ---

	@Override
	protected void startReading() {

		// Find commands
		commands.clear();

		// Load built-in commands
		load("Actions", "Broadcast", "BroadcastLocal", "Call", "Clear", "Close", "DCall", "Emit", "Env", "Events",
				"Exit", "Find", "Gc", "Info", "Memory", "Nodes", "Props", "Services", "Threads", "Bench", "Debug",
				"Ping");

		// Load custom commands
		if (packagesToScan != null && packagesToScan.length > 0) {
			for (String packageName : packagesToScan) {
				if (!packageName.isEmpty()) {
					try {
						LinkedList<String> classNames = scan(packageName);
						for (String className : classNames) {
							if (className.indexOf('$') > -1) {
								continue;
							}
							className = packageName + '.' + className;
							Class<?> type = Class.forName(className);
							if (Command.class.isAssignableFrom(type)) {
								Command command = (Command) type.newInstance();
								String name = nameOf(command, false).toLowerCase();
								commands.put(name, command);

								// Log
								logger.info("Command \"" + name + "\" registered.");
							}
						}
					} catch (Throwable cause) {
						logger.warn("Unable to scan Java package!", cause);
					}
				}
			}
		}

		// Put user-defined commands
		commands.putAll(postDefinedCommands);
		
		// Start standard input reader
		if (executor != null) {
			try {
				executor.shutdownNow();
			} catch (Exception ignored) {
			}
		}
		executor = Executors.newSingleThreadExecutor();
		executor.execute(this);

		// Log start
		showStartMessage();
	}
	
	protected void load(String... commands) {
		try {
			for (String command : commands) {
				String className = "services.moleculer.repl.commands." + command;
				Command impl = (Command) Class.forName(className).newInstance();
				String name = nameOf(impl, false).toLowerCase();
				this.commands.put(name, impl);
			}
		} catch (Throwable cause) {
			logger.warn("Unable to load command!", cause);
		}
	}

	protected void showStartMessage() {
		logger.info(nameOf(this, true) + " started. Type \"help\" for list of commands.");
	}

	public void addCommand(Command command) {
		String name = nameOf(command, false).toLowerCase();
		postDefinedCommands.put(name, command);
	}

	// --- COMMAND READER LOOP ---

	@Override
	public void run() {
		try {
			Thread.sleep(1000);
			while (executor != null && !Thread.currentThread().isInterrupted()) {
				reader = new LocalReader();
				reader.start();
				if (reader == null) {
					return;
				}
				reader.join();
				String command = reader.getLine();
				if (command.length() > 2048) {
					char first = command.charAt(0);
					boolean sameChars = true;
					for (int i = 1; i < command.length() - 1; i++) {
						if (command.charAt(i) != first) {
							sameChars = false;
							break;
						}
					}
					if (sameChars) {
						
						// Something is wrong (eg. System.in is redirected)
						stopReading();
						logger.info(nameOf(this, false) + " interrupted.");
						return;
					}
				}
				reader = null;
				if (command.length() > 0) {
					if ("r".equalsIgnoreCase(command) || "repeat".equalsIgnoreCase(command)) {
						command = lastCommand;
					} else if ("q".equalsIgnoreCase(command)) {
						command = "exit";
					}
					onCommand(new PrintWriter(colorWriter), command);
					lastCommand = command;
				}
			}
		} catch (InterruptedException i) {

			// Interrupt

		} catch (Throwable cause) {

			// Never happens
			cause.printStackTrace();
		}
	}

	// --- COMMAND PROCESSOR ---

	protected void onCommand(PrintWriter out, String command) throws Exception {
		try {
			if (command == null) {
				return;
			}
			command = command.trim();
			if (command.length() == 0) {
				return;
			}
			String[] tokens = parseLine(command);
			String cmd = tokens[0].toLowerCase();
			out.println();
			if (tokens.length > 1 && tokens[1].equals("--help")) {
				printCommandHelp(out, tokens[0]);
				return;
			}
			if ("help".equals(cmd) || "?".equals(cmd)) {
				boolean telnet = false;
				if (tokens.length > 1) {
					if (tokens[1].equals("telnet")) {
						telnet = true;
					} else {
						printCommandHelp(out, tokens[1]);
						return;
					}
				}
				printHelp(out, telnet);
				out.println("  Type \"repeat\" or \"r\"  to repeat the execution of the last command.");
				out.println();
				return;
			}
			Command impl = commands.get(cmd);
			if (impl == null) {
				out.println("The \"" + cmd + "\" command is unknown.");

				// Suggest a command
				if (cmd.length() > 1) {
					int maxCount = 0;
					String suggestion = "";
					char[] chars = cmd.toCharArray();
					for (String test : commands.keySet()) {
						int count = 0;
						for (char c : chars) {
							if (test.indexOf(c) > -1) {
								count++;
							}
						}
						if ((count > maxCount) || (count == maxCount && test.length() < suggestion.length())) {
							maxCount = count;
							suggestion = test;
						}
					}
					if (100 * maxCount / cmd.length() > 70) {
						out.println("Type \"help\" to list of all supported commands.");
						out.println("Do you mean \"" + suggestion + "\"?");
						out.println();
						printCommandHelp(out, suggestion);
						return;
					}
				}

				// Show help
				out.println();
				printHelp(out, false);
				out.println();
				return;
			}
			String[] args = new String[tokens.length - 1];
			System.arraycopy(tokens, 1, args, 0, args.length);
			if (impl.getNumberOfRequiredParameters() > args.length) {
				out.println("Unable to call \"" + cmd + "\" command!");
				out.println("Too few command parameters (" + args.length + " < " + impl.getNumberOfRequiredParameters()
						+ ")!");
				out.println();
				printCommandHelp(out, cmd);
				return;
			}
			impl.onCommand(broker, out, args);
			out.println();
		} catch (Exception cause) {
			out.println("Command execution failed!");
			cause.printStackTrace(out);
			out.println();
		}
	}

	protected String[] parseLine(String command) {
		char delimiter = ' ';
		boolean inToken = false;
		StringBuilder writer = new StringBuilder(100);
		LinkedList<String> tokens = new LinkedList<>();
		for (int i = 0; i < command.length(); i++) {
			char c = command.charAt(i);
			if (inToken) {
				if (c == delimiter) {
					tokens.add(writer.toString());
					writer.setLength(0);
					if (c == '-') {
						writer.append(c);
					}
					inToken = false;
					continue;
				}
				writer.append(c);
			} else {
				if (c == '\'') {
					delimiter = '\'';
					inToken = true;
				} else {
					if (c == '"') {
						delimiter = '"';
						inToken = true;
					} else if (c == ' ') {

						// Skipping
					} else {
						delimiter = ' ';
						writer.append(c);
						inToken = true;
					}
				}
			}
			if (i == command.length() - 1 && writer.length() != 0) {
				tokens.add(writer.toString());
			}
		}
		String[] array = new String[tokens.size()];
		tokens.toArray(array);
		return array;
	}

	protected void printHelp(PrintWriter out, boolean telnet) {
		String[] names = new String[commands.size()];
		commands.keySet().toArray(names);
		Arrays.sort(names, String.CASE_INSENSITIVE_ORDER);
		TextTable table = new TextTable(false, "Command", "Description");
		table.setPadding(2);
		for (String name : names) {
			if (!telnet && name.equals("close")) {
				continue;
			}
			Command impl = commands.get(name);
			table.addRow(impl.getUsage(), impl.getDescription());
		}
		out.println("Commands:");
		out.println();
		out.println(table);
	}

	protected void printCommandHelp(PrintWriter out, String name) {
		Command impl = commands.get(name);
		if (impl == null) {
			out.println("The \"" + name + "\" command is unknown!");
			out.println("Type \"help\" for more information.");
			return;
		}
		out.println("  Usage: " + impl.getUsage());
		out.println();
		out.println("  " + impl.getDescription());
		out.println();
		out.println("  Options:");
		out.println();
		TextTable table = new TextTable(false, "Option", "Description");
		table.setPadding(2);
		for (String[] option : impl.options) {
			table.addRow("  --" + option[0], option[1]);
		}
		out.println(table);
		out.println();
	}

	// --- STOP READING INPUT ---

	@Override
	protected void stopReading() {
		if (reader != null) {
			LocalReader r = reader;
			reader = null;
			try {
				r.interrupt();
			} catch (Exception ignored) {
			}
		}
		if (executor != null) {
			try {
				executor.shutdownNow();
			} catch (Exception ignored) {
			}
			executor = null;
		}
		commands.clear();
	}

	// --- GETTERS AND SETTERS ---

	public String[] getPackagesToScan() {
		return packagesToScan;
	}

	public void setPackagesToScan(String... packagesToScan) {
		this.packagesToScan = packagesToScan;
	}

}