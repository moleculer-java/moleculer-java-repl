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

import java.io.PrintWriter;
import java.util.Collection;
import java.util.LinkedList;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.util.CheckedTree;

/**
 * Interface of all interactive console command implementations.
 */
public abstract class Command {

	// --- CONSTANTS ---
	
	public static final String META_REPL_HEADER = "$repl";
	
	// --- CONSTRUCTOR ---

	public Command() {
		option("help", "output usage information");
	}

	// --- BASIC METHODS ---

	public abstract String getDescription();

	public abstract String getUsage();

	public abstract int getNumberOfRequiredParameters();

	public abstract void onCommand(ServiceBroker broker, PrintWriter out, String[] parameters) throws Exception;

	// --- OPTION HANDLING ---

	protected LinkedList<String[]> options = new LinkedList<>();

	protected void option(String option, String description) {
		options.add(new String[] { option, description });
	}

	// --- FLAG PARSER ---

	protected Tree parseFlags(String[] parameters) throws Exception {
		return parseFlags(1, parameters, null);
	}

	protected Tree parseFlags(int from, String[] parameters, Collection<String> knownParams) {
		Tree flags = new Tree();
		String name = null;
		if (from < parameters.length) {
			String param;
			boolean skipNext = false;
			for (int i = from; i < parameters.length; i++) {
				param = parameters[i];
				if (name == null) {
					if (param.startsWith("--") && param.length() > 2) {
						name = param.substring(2);
						if (knownParams == null || knownParams.contains(name)) {
							flags.put("lastIndex", i);
							skipNext = false;
						} else {
							skipNext = true;
						}
					}
				} else {
					if (param.startsWith("--") && param.length() > 2) {
						flags.putObject(name, null);
						name = param.substring(2);
						if (knownParams == null || knownParams.contains(name)) {
							flags.put("lastIndex", i);
							skipNext = false;
						} else {
							skipNext = true;
						}						
					} else {
						flags.put(name, param);
						if (!skipNext) {
							flags.put("lastIndex", i);
						}
						name = null;
						skipNext = false;
					}
				}
			}
			if (name != null) {
				flags.putObject(name, null);
			}
		}
		return flags;
	}

	// --- CONCATENATE ARGUMENTS ---

	protected Tree getPayload(String[] parameters) throws Exception {
		return getPayload(1, parameters);
	}

	protected Tree getPayload(int from, String[] parameters) throws Exception {
		if (parameters.length > from) {
			if (parameters[from].startsWith("'") || parameters[from].startsWith("{")
					|| parameters[from].startsWith("[")) {

				// JSON format
				StringBuilder tmp = new StringBuilder();
				for (int i = from; i < parameters.length; i++) {
					if (tmp.length() != 0) {
						tmp.append(' ');
					}
					tmp.append(parameters[i]);
				}
				String json = tmp.toString();
				if (json.startsWith("'")) {
					json = json.substring(1);
				}
				if (json.endsWith("'")) {
					json = json.substring(0, json.length() - 1);
				}
				Tree payload = new Tree(json.trim());
				payload.getMeta().put(META_REPL_HEADER, true);
				return payload;
			}
			Tree payload = new Tree();
			String name = null;
			for (int i = from; i < parameters.length; i++) {
				String p = parameters[i];
				if (name == null) {
					if (p.startsWith("--")) {
						p = p.substring(2);
					}
					name = p;
				} else {
					if ("true".equals(p)) {
						payload.put(name, true);
					} else if ("false".equals(p)) {
						payload.put(name, false);
					} else {
						try {
							if (p.contains(".")) {
								payload.put(name, Double.parseDouble(p));
							} else {
								payload.put(name, Integer.parseInt(p));
							}
						} catch (Exception notNumeric) {
							if ((p.startsWith("\"") && p.endsWith("\"")) || (p.startsWith("'") && p.endsWith("'"))) {
								p = p.replace('\"', ' ').replace('\'', ' ').trim();
							}
							payload.put(name, p);
						}
					}
					name = null;
				}
			}
			if (name != null && payload.isEmpty()) {
				if ("true".equals(name)) {
					payload = new CheckedTree(true);
				} else if ("false".equals(name)) {
					payload = new CheckedTree(false);
				} else {
					try {
						if (name.contains(".")) {
							payload = new CheckedTree(Double.parseDouble(name));
						} else {
							payload = new CheckedTree(Integer.parseInt(name));
						}
					} catch (Exception notNumeric) {
						if ((name.startsWith("\"") && name.endsWith("\""))
								|| (name.startsWith("'") && name.endsWith("'"))) {
							name = name.replace('\"', ' ').replace('\'', ' ').trim();
						}
						payload = new CheckedTree(name);
					}
				}
			}
			payload.getMeta().put(META_REPL_HEADER, true);
			return payload;
		}
		Tree payload = new Tree();
		payload.getMeta().put(META_REPL_HEADER, true);
		return payload;
	}

	// --- FORMATTERS ---

	protected void printChars(StringBuilder out, char c, int repeats) {
		for (int i = 0; i < repeats; i++) {
			out.append(c);
		}
	}

}