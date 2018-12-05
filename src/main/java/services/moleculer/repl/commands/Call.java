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
import static services.moleculer.repl.ColorWriter.YELLOW;
import static services.moleculer.util.CommonUtils.formatNamoSec;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.context.CallOptions;
import services.moleculer.repl.Command;
import services.moleculer.service.Name;
import services.moleculer.stream.PacketStream;
import services.moleculer.util.CommonUtils;

/**
 * Calls the specified action.
 */
@Name("call")
public class Call extends Command {

	// First parameter is the nodeID
	protected boolean dcall;

	public Call() {
		option("load [filename]", "load params from file");
		option("stream [filename]", "send a file as stream");
		option("save [filename]", "save response to file");
		option("timeout <seconds>", "call timeout (default is 10)");
	}

	@Override
	public String getDescription() {
		return "Call an action";
	}

	@Override
	public String getUsage() {
		return "call <actionName> [jsonParams]";
	}

	@Override
	public int getNumberOfRequiredParameters() {
		return 1;
	}

	@Override
	public void onCommand(ServiceBroker broker, PrintWriter out, String[] parameters) throws Exception {

		// Check parameter sequence
		if (parameters[0].startsWith("--")) {
			String cmd = dcall ? "dcall" : "call";
			String prefix = dcall ? cmd + " node1" : cmd;
			out.println("Invalid parameter sequence! Examples of appropriate \"" + cmd + "\" commands:");
			out.println();
			out.println(prefix + " $node.actions {\"onlyLocal\":true}");
			out.println(prefix + " $node.actions --save /temp/response.json onlyLocal true");
			out.println(prefix + " $node.actions --save");
			out.println(prefix + " $node.actions --load /temp/request.json");
			out.println(prefix + " service.action --stream /temp/data.zip");
			return;
		}

		// Action name
		String nodeID = dcall ? parameters[0] : null;
		String action = dcall ? parameters[1] : parameters[0];

		// Parse flags
		Collection<String> knownParams = Arrays.asList(new String[] { "load", "stream", "save", "timeout" });
		Tree flags = parseFlags(dcall ? 2 : 1, parameters, knownParams);
		String load = flags.get("load", "");
		if ((load == null || load.isEmpty()) && flags.get("load") != null) {
			load = action + ".params.json";
		}
		String stream = flags.get("stream", "");
		String save = flags.get("save", "");
		if ((save == null || save.isEmpty()) && flags.get("save") != null) {
			save = action + ".response.json";
		}
		long timeout = flags.get("timeout", 10L);
		int lastIndex = flags.get("lastIndex", dcall ? 1 : 0);

		// Params
		Tree params = new Tree();
		if (load == null || load.isEmpty()) {
			params = getPayload(lastIndex + 1, parameters);
		} else {
			FileInputStream in = null;
			try {
				in = new FileInputStream(load);
				params = CommonUtils.readTree(in, "json");
				in = null;
			} catch (Exception e) {
				out.println("Unable to load file \"" + load + "\"!");
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
		}

		// Output file
		File file = (save == null || save.isEmpty()) ? null : new File(save);

		// Call options
		CallOptions.Options opts = CallOptions.timeout(1000L * timeout);
		if (nodeID != null) {
			opts = opts.nodeID(nodeID);
		}

		// Call action
		if (stream == null || stream.isEmpty()) {
			if (dcall) {
				out.println(YELLOW + ">> Call \"" + action + "\" on \"" + nodeID + "\" with params: "
						+ params.toString("colorized-json", false));
			} else {
				out.println(
						YELLOW + ">> Call \"" + action + "\" with params: " + params.toString("colorized-json", false));
			}
			long start = System.nanoTime();
			Tree rsp = broker.call(action, params, opts).waitFor();
			dumpResponse(out, rsp, System.nanoTime() - start, file);
		} else {
			if (dcall) {
				out.println(YELLOW + ">> Call \"" + action + "\" on \"" + nodeID + "\" with file: " + stream);
			} else {
				out.println(YELLOW + ">> Call \"" + action + "\" with file: " + stream);
			}
			final PacketStream push = (stream != null && !stream.isEmpty()) ? broker.createStream() : null;
			long start = System.nanoTime();
			broker.call(action, push, opts).then(rsp -> {
				long duration = System.nanoTime() - start;
				Thread.sleep(100);
				dumpResponse(out, rsp, duration, file);
			}).catchError(error -> {
				error.printStackTrace(out);
			});
			if (push != null) {
				push.transferFrom(new File(stream)).then(finished -> {
					out.println();
					out.println(push.getTransferedBytes() + " bytes transfered to \"" + nodeID + "\".");
				}).catchError(error -> {
					error.printStackTrace(out);
				});
			}
		}
	}

	protected void dumpResponse(PrintWriter out, Tree rsp, long duration, File file) {
		out.println();
		out.println(CYAN + "Execution time: " + formatNamoSec(duration));
		if (rsp == null) {
			out.println();
			out.println(GRAY + "\"null\" response");
		} else {
			if (rsp.getType() == PacketStream.class) {
				PacketStream stream = (PacketStream) rsp.asObject();
				final AtomicBoolean first = new AtomicBoolean(true);
				stream.onPacket((bytes, error, closed) -> {
					if (bytes != null && bytes.length > 0) {
						if (file == null) {
							out.println();
							out.println(GREEN + bytes.length + " bytes received:");
							out.println();
							writeHexBytes(out, bytes);
							out.println();
						} else {
							FileOutputStream fo = null;
							try {
								fo = new FileOutputStream(file, !first.get());
								first.compareAndSet(true, false);
								fo.write(bytes);
								fo.flush();
								out.println(GREEN + bytes.length + " bytes saved to file \"" + file.getCanonicalPath()
										+ "\".");
							} catch (Exception e) {
								out.println("Unable to save response!");
								out.println();
								e.printStackTrace(out);
							} finally {
								if (fo != null) {
									try {
										fo.close();
									} catch (Exception ignored) {
									}
								}
							}
						}
					}
					if (error != null) {
						error.printStackTrace(out);
					}
					if (closed) {
						out.println("Stream closed successfully.");
					}
				});
			} else {
				if (file == null) {
					out.println();
					out.println(GREEN + "Response:");
					out.println();
					out.println(rsp.toString("colorized-json", true, true));
				} else {
					FileOutputStream fo = null;
					try {
						fo = new FileOutputStream(file);
						fo.write(rsp.toString("json", true, true).getBytes(StandardCharsets.UTF_8));
						fo.flush();
						out.println("Response saved successfully to file \"" + file.getCanonicalPath() + "\".");
					} catch (Exception e) {
						out.println("Unable to save response!");
						out.println();
						e.printStackTrace(out);
					} finally {
						if (fo != null) {
							try {
								fo.close();
							} catch (Exception ignored) {
							}
						}
					}
				}
			}
		}
	}

	protected void writeHexBytes(PrintWriter out, byte[] bytes) {
		String s;
		int i, j;
		for (i = 0; i < bytes.length; i++) {
			s = Integer.toHexString(((int) bytes[i] & 0xff)).toUpperCase();
			if (s.length() < 2) {
				out.write('0');
			}
			out.write(s);
			out.write(' ');
			if (i > 0) {
				j = i + 1;
				if (j % 20 == 0) {
					out.println();
				} else if (j % 5 == 0) {
					out.write("| ");
				}
			}
		}
	}

}