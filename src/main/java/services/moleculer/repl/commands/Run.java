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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.repl.Command;
import services.moleculer.repl.Repl;
import services.moleculer.service.Name;
import services.moleculer.service.Service;
import services.moleculer.service.ServiceRegistry;
import services.moleculer.util.CommonUtils;

/**
 * Loads a "script" file and executes all non-empty lines as command. Skips rows
 * that begin with a hashmark, double slash or star character (comment markers).
 * The script file must be UTF-8 encoded! Sample: <br>
 * <br>
 * run /scripts/commands.txt
 */
@Name("run")
public class Run extends Command {

	@Override
	public String getDescription() {
		return "Runs commands from a script file";
	}

	@Override
	public String getUsage() {
		return "run <path-to-file>";
	}

	@Override
	public int getNumberOfRequiredParameters() {
		return 1;
	}

	@Override
	public void onCommand(ServiceBroker broker, PrintWriter out, String[] parameters) throws Exception {

		// Script path
		StringBuilder tmp = new StringBuilder();
		for (String p : parameters) {
			tmp.append(p).append(' ');
		}
		String path = tmp.toString().replace('\"', ' ').replace('\'', ' ').trim();

		// Load file
		FileInputStream in = null;
		String script;
		try {
			out.println("Loading script file from \"" + path + "\"...");
			in = new FileInputStream(path);
			byte[] bytes = CommonUtils.readFully(in);
			script = new String(bytes, StandardCharsets.UTF_8);
			in = null;
			out.println("Script file loaded successfully.");
		} catch (Exception e) {
			out.println("Unable to load file \"" + path + "\"!");
			out.println();
			e.printStackTrace(out);
			return;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception ignored) {
				}
			}
		}

		// Get REPL Service
		Repl repl = getService(broker, Repl.class);
		if (repl == null) {
			out.println("Unable to find REPL Service!");
			out.println();
			return;
		}
		
		// Loop on lines
		BufferedReader reader = new BufferedReader(new StringReader(script));
		String line;
		int count = 0;
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (line.isEmpty() || line.startsWith("#") || line.startsWith("*") || line.startsWith("//")) {
				continue;
			}
			out.println(line);
			repl.onCommand(out, line);
			count++;
		}
		out.println(count + " commands executed.");
		out.println();		
	}

	@SuppressWarnings("unchecked")
	private static final <T extends Service> T getService(ServiceBroker broker, Class<T> type) {
		ServiceRegistry registry = broker.getConfig().getServiceRegistry();
		Tree info = registry.getDescriptor();
		for (Tree service : info.get("services")) {
			String name = service.get("name", "");
			if (name != null && !name.isEmpty()) {
				Service instance = broker.getLocalService(name);
				if (type.isAssignableFrom(instance.getClass())) {
					return (T) instance;
				}
			}
		}
		return null;
	}

}