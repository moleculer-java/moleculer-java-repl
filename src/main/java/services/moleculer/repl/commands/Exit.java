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

import java.io.PrintWriter;

import services.moleculer.ServiceBroker;
import services.moleculer.repl.Command;
import services.moleculer.service.Name;

/**
 * "Exit application" command. Shuts down ServiceBroker then the virtual
 * machine.
 */
@Name("exit")
public class Exit extends Command {

	@Override
	public String getDescription() {
		return "Exit application";
	}

	@Override
	public String getUsage() {
		return "exit, q";
	}

	@Override
	public int getNumberOfRequiredParameters() {
		return 0;
	}

	@Override
	public synchronized void onCommand(ServiceBroker broker, PrintWriter out, String[] parameters) throws Exception {
		if (broker != null) {
			out.println("Shutting down Virtual Machine...");
			out.println();
			ServiceBroker instance = broker;
			broker = null;
			try {
				Thread safetyShutdown = new Thread(() -> {
					try {
						Thread.sleep(5000);
						Runtime.getRuntime().halt(1);
					} catch (Throwable ignored) {
					}
				}, "safetyShutdown");
				Thread.sleep(300);
				safetyShutdown.setDaemon(true);
				safetyShutdown.start();
			} catch (Throwable ignored) {
			}
			instance.stop();
		}
	}

}