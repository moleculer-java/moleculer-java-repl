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
import static services.moleculer.repl.ColorWriter.GREEN;
import static services.moleculer.repl.ColorWriter.YELLOW;
import static services.moleculer.repl.ColorWriter.GRAY;
import static services.moleculer.util.CommonUtils.formatNamoSec;

import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.repl.Command;
import services.moleculer.service.Name;
import services.moleculer.stream.PacketStream;

/**
* Calls the specified action.
*/
@Name("call")
public class Call extends Command {

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
		String name = parameters[0].replace('\"', ' ').replace('\'', ' ').trim();
		Tree params = getPayload(parameters);
		out.println(YELLOW + ">> Call '" + name + "' with params: " + params.toString("colorized-json", false));
		long start = System.nanoTime();
		Tree rsp = broker.call(name, params).waitFor(15, TimeUnit.SECONDS);
		dumpResponse(out, rsp, System.nanoTime() - start);
	}

	protected static void dumpResponse(PrintWriter out, Tree rsp, long duration) {
		out.println();
		out.println(CYAN + "Execution time: " + formatNamoSec(duration));
		out.println();
		out.println(GREEN + "Response:");
		out.println();
		if (rsp == null) {
			out.println(GRAY + "'null' response");
		} else {
			if (rsp.getType() == PacketStream.class) {
				PacketStream stream = (PacketStream) rsp.asObject();
				stream.onPacket((bytes, error, closed) -> {
					if (bytes != null && bytes.length > 0) {
						out.println();
						out.println(GREEN + bytes.length + " bytes received:");
						out.println();
						writeHexBytes(out, bytes);
						out.println();
					}
					if (error != null) {
						error.printStackTrace(out);
					}
					if (closed) {
						out.println("Stream closed successfully.");
					}
				});
			} else {
				out.println(rsp.toString("colorized-json", true, true));
			}
		}
	}
	
	protected static void writeHexBytes(PrintWriter out, byte[] bytes) {
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