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

import static services.moleculer.repl.ColorWriter.GREEN;
import static services.moleculer.repl.ColorWriter.WHITE;

import java.io.PrintWriter;
import java.text.DecimalFormat;

import services.moleculer.ServiceBroker;
import services.moleculer.repl.Command;
import services.moleculer.service.Name;

/**
 * Invokes the garbage collector.
 */
@Name("gc")
public class Gc extends Command {

	// --- NUMBER FORMATTER ---

	protected DecimalFormat formatter = new DecimalFormat("#.##");

	// --- METHODS ---

	@Override
	public String getDescription() {
		return "Invoke garbage collector";
	}

	@Override
	public String getUsage() {
		return "gc";
	}

	@Override
	public int getNumberOfRequiredParameters() {
		return 0;
	}

	@Override
	public void onCommand(ServiceBroker broker, PrintWriter out, String[] parameters) throws Exception {
		Runtime runtime = Runtime.getRuntime();
		long before = runtime.totalMemory() - runtime.freeMemory();
		System.runFinalization();
		System.gc();
		long after = runtime.totalMemory() - runtime.freeMemory();
		long freed = before - after;
		if (freed < 0) {
			freed = 0;
		}
		int percent = (int) (100 * freed / before);
		out.println("Garbage collection report:");
		out.println();
		long max = before;
		if (after > max) {
			max = after;
		}
		printMemory(out, "  before  - ", before, max, 27);
		printMemory(out, "  after   - ", after, max, 27);
		printMemory(out, "  freed   - ", freed, max, 27);
		out.print("  percent - ");
		out.print(Integer.toString(percent));
		out.println("%");
	}

	protected void printMemory(PrintWriter out, String title, long value, long max, int offset) throws Exception {
		StringBuilder tmp = new StringBuilder();
		tmp.append(title);
		tmp.append(WHITE);
		synchronized (formatter) {
			tmp.append(formatter.format((double) (value / (double) 1024) / (double) 1024));
		}
		tmp.append(" Mbytes");
		printChars(tmp, ' ', offset - tmp.length());
		tmp.append(GREEN);
		printChars(tmp, '|', (int) ((77 - offset) * value / max));
		out.println(tmp.toString());
	}

}