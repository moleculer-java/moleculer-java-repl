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

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.Objects;
import java.util.StringTokenizer;

import com.diogonunes.jcdp.color.ColoredPrinter;
import com.diogonunes.jcdp.color.api.Ansi.Attribute;
import com.diogonunes.jcdp.color.api.Ansi.BColor;
import com.diogonunes.jcdp.color.api.Ansi.FColor;

public class ColorWriter extends Writer {

	// --- CONSTANTS ---
	
	public static final String GRAY = "§%";
	public static final String YELLOW = "§!";
	public static final String WHITE = "§+";
	public static final String GREEN = "§'";
	public static final String CYAN = "§$";
	public static final String MAGENTA = "§~";
	
	public static final String OK_COLOR = "§ˇ";
	public static final String FAIL_COLOR = "§^";

	protected final ColoredPrinter coloredPrinter;
	protected final PrintStream printStream;

	// --- CONNSTRUCTOR FOR LOCAL CONSOLE ---
	
	public ColorWriter() {
		coloredPrinter = new ColoredPrinter.Builder(1, false).build();
		printStream = null;
	}

	// --- CONNSTRUCTOR FOR TELNET CONSOLE ---
	
	public ColorWriter(PrintStream stream) {
		coloredPrinter = null;
		printStream = Objects.requireNonNull(stream);
	}

	// --- COLOR PARSER & WRITER ---
	
	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		String txt = new String(cbuf, off, len);
		txt = txt.replace((char) 160, ' ');
		if (coloredPrinter != null) {

			// Local output
			StringTokenizer st = new StringTokenizer(txt, "§", true);
			boolean wasDelimiter = false;
			FColor fc = FColor.WHITE;
			BColor bg = BColor.NONE;
			Attribute attr = Attribute.NONE;
			while (st.hasMoreTokens()) {
				String token = st.nextToken();
				if (token.length() > 1 && wasDelimiter) {
					wasDelimiter = false;
					char type = token.charAt(0);
					token = token.substring(1);
					switch (type) {

					case '+':
						fc = FColor.WHITE;
						attr = Attribute.LIGHT;
						break;

					case '!':
						fc = FColor.YELLOW;
						attr = Attribute.LIGHT;
						break;

					case '\'':
						fc = FColor.GREEN;
						attr = Attribute.LIGHT;
						break;

					case '%':
						fc = FColor.WHITE;
						bg = BColor.NONE;
						attr = Attribute.CLEAR;
						break;

					case '$':
						fc = FColor.CYAN;
						attr = Attribute.LIGHT;
						break;

					case '~':
						fc = FColor.MAGENTA;
						attr = Attribute.LIGHT;
						break;

					case 'ˇ':
						bg = BColor.GREEN;
						break;

					case '^':
						bg = BColor.RED;
						break;

					default:
						coloredPrinter.print("§" + token);
						continue;
					}
					coloredPrinter.print(token, attr, fc, bg);
				} else if ("§".equals(token)) {
					wasDelimiter = true;
					continue;
				} else {
					wasDelimiter = false;
					coloredPrinter.print(token);
					continue;
				}
			}
			if (txt.indexOf('§') > -1) {
				coloredPrinter.clear();
			}
		} else {
			
			// Telnet output
			StringTokenizer st = new StringTokenizer(txt, "§", true);
			boolean wasDelimiter = false;
			String prefix = null;
			while (st.hasMoreTokens()) {
				String token = st.nextToken();
				if (token.length() > 1 && wasDelimiter) {
					wasDelimiter = false;
					char type = token.charAt(0);
					token = token.substring(1);
					switch (type) {

					case '+':
						prefix = "\u001B[37m\u001B[1m";
						break;

					case '!':
						prefix = "\u001B[33m\u001B[1m";
						break;

					case '\'':
						prefix = "\u001B[32m\u001B[1m";
						break;

					case '%':
						prefix = "\u001B[0m\u001B[37m\u001B[2m";
						break;

					case '$':
						prefix = "\u001B[36m\u001B[1m";
						break;

					case '~':
						prefix = "\u001B[35m\u001B[1m";
						break;
						
					case 'ˇ':
						prefix = "\u001B[42m";
						break;

					case '^':
						prefix = "\u001B[41m";
						break;

					default:
						printStream.print("§" + token);
						continue;
					}
					if (prefix != null) {
						printStream.print(prefix);	
					}
					printStream.print(token);
				} else if ("§".equals(token)) {
					wasDelimiter = true;
					continue;
				} else {
					wasDelimiter = false;
					printStream.print(token);
					continue;
				}
			}
			if (txt.indexOf('§') > -1) {
				printStream.print("\u001B[0m");
			}
		}
	}

	@Override
	public void flush() throws IOException {
	}

	@Override
	public void close() throws IOException {
	}

}