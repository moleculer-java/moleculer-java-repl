/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2018 Andras Berkes [andras.berkes@programmer.net]<br>
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

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import services.moleculer.ServiceBroker;
import services.moleculer.repl.Command;
import services.moleculer.service.Name;
import services.moleculer.transporter.Transporter;

/**
 * Turn on/off debug log of Transporter.
 */
@Name("debug")
public class Debug extends Command {

	public Debug() {
		option("all, -a", "show heartbeat/gossip messages");
	}

	@Override
	public String getDescription() {
		return "Turn on/off debug log of Transporter";
	}

	@Override
	public String getUsage() {
		return "debug <on|off>";
	}

	@Override
	public int getNumberOfRequiredParameters() {
		return 1;
	}

	@Override
	public void onCommand(ServiceBroker broker, PrintWriter out, String[] parameters) throws Exception {
		Transporter transporter = broker.getConfig().getTransporter();
		if (transporter == null) {
			out.println("Debug mode can't be enabled without a Transporter.");
			out.println();
			return;
		}
		List<String> params = Arrays.asList(parameters);
		boolean all = params.contains("--all") || params.contains("-a");
		boolean on = params.contains("on");
		if (on) {
			if (transporter.isDebug() && (!all || transporter.isDebugHeartbeats())) {
				out.println("Debug messages have been enabled earlier.");
			} else {
				out.println("The transporter debug mode has been enabled.");
				if (all) {
					out.println("Heartbeat/gossip messages will also be displayed.");
				}
				transporter.setDebug(true);
				if (all) {
					transporter.setDebugHeartbeats(true);
				}
			}
		} else {
			if (!transporter.isDebug() && !transporter.isDebugHeartbeats()) {
				out.println("Debug messages have been disabled earlier.");
			} else {
				transporter.setDebug(false);
				transporter.setDebugHeartbeats(false);
				out.println("The transporter debug mode has been disabled.");
			}
		}
	}

}