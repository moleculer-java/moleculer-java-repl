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

import static services.moleculer.repl.ColorWriter.YELLOW;

import java.io.File;
import java.io.PrintWriter;

import services.moleculer.ServiceBroker;
import services.moleculer.context.CallOptions;
import services.moleculer.service.Name;
import services.moleculer.stream.PacketStream;

/**
 * Sends a file to the specified action.
 */
@Name("stream")
public class Stream extends Call {

	@Override
	public String getDescription() {
		return "Sends a file to the specified action";
	}

	@Override
	public String getUsage() {
		return "stream <actionName> <filePath>";
	}

	@Override
	public int getNumberOfRequiredParameters() {
		return 2;
	}
	
	@Override
	public void onCommand(ServiceBroker broker, PrintWriter out, String[] parameters) throws Exception {
		String name = parameters[0].replace('\"', ' ').replace('\'', ' ').trim();
		StringBuilder path = new StringBuilder();
		for (int i = 1; i < parameters.length; i++) {
			path.append(parameters[i].replace('\"', ' ').replace('\'', ' ').trim());
			path.append(' ');
		}
		String filePath = path.toString().trim();
		out.println(YELLOW + ">> Call '" + name + "' with file: " + filePath);
		long start = System.nanoTime();
		File file = new File(filePath);
		if (!file.isFile()) {
			out.println();
			out.println("File not found!");
			return;
		}
		PacketStream stream = broker.createStream();		
		broker.call(name, stream, CallOptions.timeout(60000)).then(rsp -> {
			long duration = System.nanoTime() - start;
			Thread.sleep(100);
			dumpResponse(out, rsp, duration);			
		}).catchError(error -> {
			error.printStackTrace(out);
		});
		stream.transferFrom(file).then(finished -> {
			out.println();
			out.println("File transfer finished!");
		}).catchError(error -> {
			error.printStackTrace(out);
		});
	}
	
}
