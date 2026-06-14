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

import com.diogonunes.jcolor.Ansi;
import com.diogonunes.jcolor.Attribute;

/**
 * Writer with built-in color code parser. Renders the local console via JColor
 * ({@code com.diogonunes.jcolor}) and telnet sessions via raw ANSI escapes.
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

	// When null, output goes to the local System.out console (colorized with
	// JColor); otherwise it goes to this telnet stream (colorized with raw ANSI).
	protected final PrintStream printStream;

	// --- CONNSTRUCTOR FOR LOCAL CONSOLE ---

	public ColorWriter() {
		printStream = null;
	}

	// --- CONNSTRUCTOR FOR TELNET CONSOLE ---

	public ColorWriter(PrintStream stream) {
		printStream = Objects.requireNonNull(stream);
	}

	// --- COLOR PARSER & WRITER ---

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		String text = new String(cbuf, off, len);
		text = text.replace((char) 160, ' ');
		if (printStream == null) {

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
				Attribute fc = null;
				Attribute bg = null;
				Attribute style = null;
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
							fc = Attribute.WHITE_TEXT();
							style = Attribute.BOLD();
							break;

						case '!':
							fc = Attribute.YELLOW_TEXT();
							style = Attribute.BOLD();
							break;

						case '\'':
							fc = Attribute.GREEN_TEXT();
							style = Attribute.BOLD();
							break;

						case '%':
							fc = Attribute.WHITE_TEXT();
							bg = null;
							style = Attribute.DIM();
							break;

						case '$':
							fc = Attribute.CYAN_TEXT();
							style = Attribute.BOLD();
							break;

						case '~':
							fc = Attribute.MAGENTA_TEXT();
							style = Attribute.BOLD();
							break;

						case '=':
							bg = Attribute.GREEN_BACK();
							break;

						case '÷':
							bg = Attribute.RED_BACK();
							break;

						default:
							System.out.print("§" + token);
							continue;
						}
						if (line.lineFeed && !st.hasMoreTokens()) {
							if (token.isEmpty()) {
								token = " ";
							}
							System.out.println(colorize(token, fc, style, bg));
						} else {
							System.out.print(colorize(token, fc, style, bg));
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
							System.out.println(token);
						} else {
							System.out.print(token);
						}
						continue;
					}
				}
			}
			if (text.indexOf('§') > -1) {
				System.out.print(Ansi.RESET);
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
			System.out.println(" ");
			return true;
		}
		return false;
	}

	// --- JCOLOR HELPER (LOCAL CONSOLE) ---

	/**
	 * Wraps {@code token} in ANSI escapes for the supplied (nullable) text color,
	 * text style, and background color using JColor. Returns the plain token when
	 * no attribute applies.
	 */
	protected String colorize(String token, Attribute textColor, Attribute textStyle, Attribute background) {
		ArrayList<Attribute> attributes = new ArrayList<>(3);
		if (textColor != null) {
			attributes.add(textColor);
		}
		if (textStyle != null) {
			attributes.add(textStyle);
		}
		if (background != null) {
			attributes.add(background);
		}
		if (attributes.isEmpty()) {
			return token;
		}
		return Ansi.colorize(token, attributes.toArray(new Attribute[0]));
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