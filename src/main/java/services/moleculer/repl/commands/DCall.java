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
import static services.moleculer.util.CommonUtils.formatNamoSec;

import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.context.CallOptions;
import services.moleculer.repl.Command;
import services.moleculer.service.Name;

/**
* Direct call an action.
*/
@Name("dcall")
public class DCall extends Command {

	@Override
	public String getDescription() {
		return "Direct call an action";
	}

	@Override
	public String getUsage() {
		return "dcall <nodeID> <actionName> [jsonParams]";
	}

	@Override
	public int getNumberOfRequiredParameters() {
		return 2;
	}

	@Override
	public void onCommand(ServiceBroker broker, PrintWriter out, String[] parameters) throws Exception {
		String nodeID = parameters[0];
		String name = parameters[1].replace('\"', ' ').replace('\'', ' ').trim();
		Tree params = getPayload(2, parameters);
		out.println(YELLOW + ">> Call '" + name + "' on '" + nodeID + "' with params: " + params.toString("colorized-json", false));
		long start = System.nanoTime();
		Tree rsp = broker.call(name, params, CallOptions.nodeID(nodeID)).waitFor(15, TimeUnit.SECONDS);
		long duration = System.nanoTime() - start;
		out.println();
		out.println(CYAN + "Execution time: " + formatNamoSec(duration));
		out.println();
		out.println(GREEN + "Response:");
		out.println();
		if (rsp == null) {
			out.println("'null' response");
		} else {
			out.println(rsp.toString("colorized-json", true, true));
		}
	}

}