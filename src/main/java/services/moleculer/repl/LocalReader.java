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

/**
 * System.in reader daemon.
 */
public class LocalReader extends Thread {

	// --- LINE ---

	protected StringBuilder line = new StringBuilder(80);

	// --- FIRST READING ---
	
	protected final boolean showPrompt;
	
	// --- CONSTRUCTOR ---

	protected LocalReader(boolean showPrompt) {
		this.showPrompt = showPrompt;
		setDaemon(true);
	}

	// --- READER LOOP ---

	public void run() {
		try {
			int c = ' ';
			if (showPrompt) {
				System.out.print("mol $ ");
			}
			for (;;) {
				c = System.in.read();
				if (c == '\n') {
					return;
				}
				if (c == '\r') {
					continue;
				}
				line.append((char) c);
				if (line.length() > 2048) {
					break;
				}
			}
		} catch (Throwable ignored) {
		}
	}

	// --- GET THE ENTERED LINE ---

	protected String getLine() {
		return line.toString().trim();
	}

}