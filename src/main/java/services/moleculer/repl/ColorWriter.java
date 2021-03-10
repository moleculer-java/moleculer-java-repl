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
import java.util.ArrayList;
import java.util.Objects;
import java.util.StringTokenizer;

import com.diogonunes.jcdp.color.ColoredPrinter;
import com.diogonunes.jcdp.color.api.Ansi.Attribute;
import com.diogonunes.jcdp.color.api.Ansi.BColor;
import com.diogonunes.jcdp.color.api.Ansi.FColor;

/**
 * Writer with built-in color code parser. Compatible with JCDP 3.0.4 (and higher).
 */
public class ColorWriter extends Writer {

	// --- CONSTANTS ---

	public static final String GRAY = "§%";
	public static final String YELLOW = "§!";
	public static final String WHITE = "§+";
	public static final String GREEN = "§'";
	public static final String CYAN = "§$";
	public static final String MAGENTA = "§~";

	public static final String OK_COLOR = "§=";
	public static final String FAIL_COLOR = "§÷";

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
		String text = new String(cbuf, off, len);
		text = text.replace((char) 160, ' ');
		if (coloredPrinter != null) {

			// --- LOCAL CONSOLE ---
			
			// Loop on lines
			ArrayList<Line> lines = splitToLines(text);
			for (Line line: lines) {
				
				// Loop on tokens
				StringTokenizer st = new StringTokenizer(line.content, "§", true);
				if (printLineFeed(line, st)) {
					continue;
				}
				boolean wasDelimiter = false;
				FColor fc = FColor.WHITE;
				BColor bg = BColor.NONE;
				Attribute attr = Attribute.NONE;
				while (st.hasMoreTokens()) {
					String token = st.nextToken();
					if (token.length() > 0 && wasDelimiter) {
						wasDelimiter = false;
						char type = token.charAt(0);
						token = token.substring(1);
						if (token.isEmpty()) {
							printLineFeed(line, st);
							continue;
						}
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

						case '=':
							bg = BColor.GREEN;
							break;

						case '÷':
							bg = BColor.RED;
							break;

						default:
							coloredPrinter.print("§" + token);
							continue;
						}
						if (line.lineFeed && !st.hasMoreTokens()) {
							if (token.isEmpty()) {
								token = " ";
							}
							coloredPrinter.println(token, attr, fc, bg);
						} else {
							coloredPrinter.print(token, attr, fc, bg);							
						}
					} else if ("§".equals(token)) {
						wasDelimiter = true;
						printLineFeed(line, st);
						continue;
					} else {
						wasDelimiter = false;
						if (line.lineFeed && !st.hasMoreTokens()) {
							if (token.isEmpty()) {
								token = " ";
							}
							coloredPrinter.println(token);
						} else {
							coloredPrinter.print(token);							
						}						
						continue;
					}
				}
			}
			if (text.indexOf('§') > -1) {
				coloredPrinter.setAttribute(Attribute.NONE);
				coloredPrinter.setBackgroundColor(BColor.NONE);
				coloredPrinter.setForegroundColor(FColor.NONE);
			}
			
		} else {

			// --- TELNET CONSOLE ---

			StringTokenizer st = new StringTokenizer(text, "§", true);
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

					case '=':
						prefix = "\u001B[42m";
						break;

					case '÷':
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
			if (text.indexOf('§') > -1) {
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

	protected boolean printLineFeed(Line line, StringTokenizer st) {
		if (line.lineFeed && !st.hasMoreTokens()) {
			coloredPrinter.println(" ");
			return true;
		}
		return false;
	}
	
	protected ArrayList<Line> splitToLines(String text) {		
		ArrayList<Line> lines = new ArrayList<>();
		StringTokenizer st = new StringTokenizer(text, "\r\n", true);
		boolean wasLineFeed = false;
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if ("\r".equals(token) || "\n".equals(token)) {
				if (wasLineFeed) {
					continue;
				}
				wasLineFeed = true;
				if (lines.isEmpty()) {
					Line prev = new Line("");
					prev.lineFeed = true;
					lines.add(0, prev);
				} else {
					Line prev = lines.get(lines.size() - 1);
					prev.lineFeed = true;
				}
				continue;
			}
			lines.add(new Line(token));
			wasLineFeed = false;
		}
		return lines;
	}
	
	protected static class Line {
		
		private final String content;
		private boolean lineFeed;
		
		protected Line(String content) {
			this.content = content;
		}
		
	}
	
}