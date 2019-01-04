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
package services.moleculer.repl.commands;

import static services.moleculer.repl.ColorWriter.CYAN;
import static services.moleculer.repl.ColorWriter.GRAY;
import static services.moleculer.repl.ColorWriter.GREEN;
import static services.moleculer.repl.ColorWriter.MAGENTA;
import static services.moleculer.repl.ColorWriter.WHITE;
import static services.moleculer.repl.ColorWriter.YELLOW;
import static services.moleculer.util.CommonUtils.getHostName;
import static services.moleculer.util.CommonUtils.nameOf;

import java.io.PrintWriter;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.datatree.Tree;
import io.datatree.dom.TreeReaderRegistry;
import io.datatree.dom.TreeWriterRegistry;
import services.moleculer.ServiceBroker;
import services.moleculer.breaker.CircuitBreaker;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.repl.Command;
import services.moleculer.repl.TextTable;
import services.moleculer.serializer.Serializer;
import services.moleculer.service.Name;
import services.moleculer.service.ServiceInvoker;
import services.moleculer.transporter.TcpTransporter;
import services.moleculer.transporter.Transporter;

/**
 * Informations (node ID, IP address, etc.) about the ServiceBroker instance.
 */
@Name("info")
public class Info extends Command {

	// --- CURRENT VERSIONS ---

	/**
	 * Current software version.
	 */
	protected static AtomicReference<String> softwareVersion = new AtomicReference<>();

	/**
	 * Current protocol version.
	 */
	protected static AtomicReference<String> protocolVersion = new AtomicReference<>();

	// --- NUMBER FORMATTER ---

	protected DecimalFormat formatter = new DecimalFormat("#.##");

	// --- METHODS ---

	@Override
	public String getDescription() {
		return "Information about the broker";
	}

	@Override
	public String getUsage() {
		return "info";
	}

	@Override
	public int getNumberOfRequiredParameters() {
		return 0;
	}

	@Override
	public void onCommand(ServiceBroker broker, PrintWriter out, String[] parameters) throws Exception {

		// Get Runtime
		Runtime r = Runtime.getRuntime();
		ServiceBrokerConfig cfg = broker.getConfig();

		// --- GENERAL INFORMATION ---

		printHeader(out, "General information");
		TextTable table = new TextTable(false, "Name", "Value");

		table.addRow(GRAY + "CPU", ": " + WHITE + "Arch: " + System.getProperty("os.arch", "unknown") + ", Cores: "
				+ r.availableProcessors());
		int cpuUsage = cfg.getMonitor().getTotalCpuPercent();
		table.addRow(GRAY + "CPU usage", ": " + WHITE + cpuUsage + "%");
		long total = r.totalMemory();
		long free = r.freeMemory();
		long used = total - free;
		int usedLen = (int) (20 * used / total);
		StringBuilder tmp = new StringBuilder(64);
		tmp.append(": ");
		tmp.append(WHITE);
		tmp.append('[');
		tmp.append(GREEN);
		if (usedLen < 1) {
			usedLen = 1;
		}
		printChars(tmp, '|', usedLen);
		tmp.append(WHITE);
		printChars(tmp, '-', 20 - usedLen);
		tmp.append("] ");
		tmp.append(GRAY);
		synchronized (formatter) {
			tmp.append(formatter.format((double) (free / (double) 1024) / (double) 1024));
		}
		tmp.append(" MB free");
		table.addRow(GRAY + "Heap", tmp.toString());
		table.addRow(GRAY + "OS", ": " + WHITE + System.getProperty("os.name", "unknown") + " (V"
				+ System.getProperty("os.version", "?") + ')');
		try {
			table.addRow(GRAY + "IP", ": " + WHITE + InetAddress.getLocalHost().getHostAddress());
		} catch (Exception ignored) {
		}
		table.addRow(GRAY + "Hostname", ": " + WHITE + getHostName());
		table.addRow("", "");

		table.addRow(GRAY + "Java VM version", ": " + WHITE + System.getProperty("java.version", "unknown") + " from "
				+ System.getProperty("java.vm.vendor", "unknown vendor"));
		table.addRow(GRAY + "Java VM type", ": " + WHITE + System.getProperty("java.vm.name", "unknown"));

		table.addRow(GRAY + "Moleculer version", ": " + WHITE + getSoftwareVersion());
		table.addRow(GRAY + "Protocol version", ": " + WHITE + getProtocolVersion());

		TimeZone zone = TimeZone.getDefault();
		int currentOffset = zone.getOffset(System.currentTimeMillis());
		String offset;
		if (currentOffset == 0) {
			offset = "+00:00";
		} else {
			long hours = TimeUnit.MILLISECONDS.toHours(currentOffset);
			long minutes = TimeUnit.MILLISECONDS.toMinutes(currentOffset);
			minutes = Math.abs(minutes - TimeUnit.HOURS.toMinutes(hours));
			offset = String.format("%+03d:%02d", hours, Math.abs(minutes));
		}
		table.addRow("", "");

		table.addRow(GRAY + "Current time",
				": " + WHITE
						+ SimpleDateFormat
								.getDateTimeInstance(SimpleDateFormat.MEDIUM, SimpleDateFormat.MEDIUM, Locale.US)
								.format(new Date())
						+ " GMT" + offset + " (" + zone.getDisplayName() + ")");

		out.println(table);

		// --- BROKER INFORMATION ---

		Transporter t = cfg.getTransporter();
		printHeader(out, "Broker information");
		table = new TextTable(false, "Name", "Value");

		String ns = cfg.getNamespace();
		if (ns == null || ns.isEmpty()) {
			table.addRow(GRAY + "Namespace", ": <None>");
		} else {
			table.addRow(GRAY + "Namespace", ": " + WHITE + ns);
		}
		table.addRow(GRAY + "Node ID", ": " + WHITE + broker.getNodeID());

		Tree info = cfg.getServiceRegistry().getDescriptor();
		Tree services = info.get("services");
		if (services != null && !services.isNull()) {
			int actionCounter = 0;
			int eventCounter = 0;
			for (Tree service : services) {
				Tree actions = service.get("actions");
				if (actions != null) {
					actionCounter += actions.size();
				}
				Tree events = service.get("events");
				if (events != null) {
					eventCounter += events.size();
				}
			}
			table.addRow(GRAY + "Services", ": " + WHITE + services.size());
			table.addRow(GRAY + "Actions", ": " + WHITE + actionCounter);
			table.addRow(GRAY + "Events", ": " + WHITE + eventCounter);
		} else {
			table.addRow(GRAY + "Services", ": " + WHITE + "0");
			table.addRow(GRAY + "Actions", ": " + WHITE + "0");
			table.addRow(GRAY + "Events", ": " + WHITE + "0");
		}

		table.addRow("", "");
		addType(table, "Strategy", cfg.getStrategyFactory());
		addType(table, "Cacher", cfg.getCacher());
		if (t == null) {
			table.addRow(GRAY + "Nodes", ": " + CYAN + "1");
		} else {
			table.addRow(GRAY + "Nodes", ": " + CYAN + t.getAllNodeIDs().size());
		}
		out.println(table);

		// --- BROKER OPTIONS ---

		printHeader(out, "Broker options");
		table = new TextTable(false, "Name", "Value");

		addType(table, "Context factory", cfg.getContextFactory());
		addType(table, "Event bus", cfg.getEventbus());
		addType(table, "System monitor", cfg.getMonitor());
		addType(table, "Service registry", cfg.getServiceRegistry());
		addType(table, "UID generator", cfg.getUidGenerator());

		ServiceInvoker si = cfg.getServiceInvoker();
		if (si instanceof CircuitBreaker) {
			table.addRow(GRAY + "Circuit breaker:", ": " + MAGENTA + "true");
			CircuitBreaker cb = (CircuitBreaker) si;
			table.addRow(GRAY + "Max failures", ": " + CYAN + cb.getMaxErrors());
			table.addRow(GRAY + "Half open time", ": " + CYAN + cb.getWindowLength());
		} else {
			table.addRow(GRAY + "Circuit breaker:", ": " + MAGENTA + "false");
		}

		table.addRow("", "");

		addType(table, "Task executor", cfg.getExecutor());
		addType(table, "Task scheduler", cfg.getScheduler());
		table.addRow("", "");

		if (t != null) {
			Serializer s = t.getSerializer();
			addType(table, "Serializer", s);
			try {
				if (s != null && "JSON".equalsIgnoreCase(s.getFormat())) {
					String readers = getAPIs(TreeReaderRegistry.getReader("json"),
							TreeReaderRegistry.getReadersByFormat("json"));
					String writers = getAPIs(TreeWriterRegistry.getWriter("json"),
							TreeWriterRegistry.getWritersByFormat("json"));
					if (readers.equals(writers)) {
						table.addRow(GRAY + "JSON implementations", ": " + WHITE + readers);
					} else {
						table.addRow(GRAY + "JSON readers", ": " + WHITE + readers);
						table.addRow(GRAY + "JSON writers", ": " + WHITE + writers);
					}
				}
			} catch (Exception ignored) {
			}
			addType(table, "Transporter", t);
			if (t instanceof TcpTransporter) {
				TcpTransporter tt = (TcpTransporter) t;
				table.addRow(GRAY + "Gossip period", ": " + CYAN + tt.getGossipPeriod());
				table.addRow(GRAY + "Max connections", ": " + CYAN + tt.getMaxConnections());
				table.addRow(GRAY + "TCP server port", ": " + CYAN + tt.getCurrentPort());
				table.addRow(GRAY + "UDP broadcasting", ": " + MAGENTA + tt.isUdpBroadcast());
				table.addRow(GRAY + "UDP multicasting",
						": " + MAGENTA + (tt.getUrls() == null || tt.getUrls().length == 0));

				if (tt.getUdpBindAddress() == null) {
					table.addRow(GRAY + "Broadcast address", ": <auto>");
				} else {
					table.addRow(GRAY + "Broadcast address", ": " + GREEN + tt.getUdpBindAddress());
				}
				table.addRow(GRAY + "Multicast address", ": " + GREEN + tt.getUdpMulticast());
				table.addRow(GRAY + "UDP port", ": " + CYAN + tt.getUdpPort());

			} else {
				table.addRow(GRAY + "Heartbeat interval", ": " + CYAN + t.getHeartbeatInterval());
				table.addRow(GRAY + "Heartbeat timeout", ": " + CYAN + t.getHeartbeatTimeout());
			}
			table.addRow(GRAY + "Offline timeout", ": " + CYAN + t.getOfflineTimeout());
		} else {
			table.addRow(GRAY + "Transporter", ": <none>");
		}
		table.addRow(GRAY + "Internal services", ": " + MAGENTA + cfg.isInternalServices());
		out.println(table);
	}

	protected String getAPIs(Object defaultAPI, Set<String> values) {
		String api = nameOf(defaultAPI, false);
		if (api.startsWith("json")) {
			api = api.substring(4);
		}
		StringBuilder list = new StringBuilder(32);
		for (String name : values) {
			int i = name.lastIndexOf(".Json");
			if (i > -1) {
				if (list.length() > 0) {
					list.append(", ");
				}
				name = name.substring(i + 5);
				list.append(name);
				if (name.equals(api)) {
					list.append(" (*)");
				}
			}
		}
		return list.toString();
	}

	protected void addType(TextTable table, String title, Object component) {
		if (component == null) {
			table.addRow(GRAY + title, GRAY + ": <none>");
		} else {
			table.addRow(GRAY + title, ": " + GREEN + nameOf(component, false));
		}
	}

	protected void printHeader(PrintWriter out, String header) {
		header = YELLOW + "  " + header;
		int len = header.length() + 2;
		StringBuilder line = new StringBuilder(len);
		line.append(YELLOW);
		for (int i = 0; i < len; i++) {
			line.append('=');
		}
		out.println(line);
		out.println(header);
		out.println(line);
		out.println();
	}

	public static final String getSoftwareVersion() {
		String version = softwareVersion.get();
		if (version == null) {
			try {
				version = (String) ServiceBroker.class.getField("SOFTWARE_VERSION").get(ServiceBroker.class);
			} catch (Throwable ignored) {
				version = ServiceBroker.SOFTWARE_VERSION;
			}
			softwareVersion.compareAndSet(null, version);
		}
		return version;
	}

	public static final String getProtocolVersion() {
		String version = protocolVersion.get();
		if (version == null) {
			try {
				version = (String) ServiceBroker.class.getField("PROTOCOL_VERSION").get(ServiceBroker.class);
			} catch (Throwable ignored) {
				version = ServiceBroker.PROTOCOL_VERSION;
			}
			protocolVersion.compareAndSet(null, version);
		}
		return version;
	}

}