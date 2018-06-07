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

import static services.moleculer.repl.ColorWriter.CYAN;
import static services.moleculer.repl.ColorWriter.GRAY;
import static services.moleculer.repl.ColorWriter.GREEN;
import static services.moleculer.repl.ColorWriter.MAGENTA;
import static services.moleculer.repl.ColorWriter.WHITE;
import static services.moleculer.repl.ColorWriter.YELLOW;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

import io.datatree.dom.Config;
import io.datatree.dom.builtin.JsonBuiltin;
import io.datatree.dom.converters.DataConverterRegistry;

/**
 * JSON serializer with color code parser. Generates "colorized" JSON string.
 */
public class JsonColorizer extends JsonBuiltin {

	// --- IMPLEMENTED WRITER METHOD ---

	@Override
	public String toString(Object value, Object meta, boolean pretty, boolean insertMeta) {
		if (value == null) {
			return "";
		}
		StringBuilder builder = builders.poll();
		if (builder == null) {
			builder = new StringBuilder(512);
		} else {
			builder.setLength(0);
		}
		builder.append(GRAY);
		toColorizedString(builder, value, insertMeta ? meta : null, pretty ? 1 : 0);
		final String json = builder.toString();
		if (builders.size() > Config.POOL_SIZE) {
			return json;
		}
		builders.add(builder);
		return json;
	}

	// --- PROTECTED UTILITIES ---

	@SuppressWarnings("rawtypes")
	protected static final void toColorizedString(StringBuilder builder, Object value, Object meta, int indent) {

		// Null value
		if (value == null) {
			builder.append(WHITE).append(NULL).append(GRAY);
			return;
		}

		// Numeric values
		if (value instanceof Number) {
			builder.append(YELLOW).append(value).append(GRAY);
			return;
		}

		// Boolean values
		if (value instanceof Boolean) {
			builder.append(MAGENTA).append(value).append(GRAY);
			return;
		}

		// String values
		if (value instanceof String) {
			builder.append(GREEN);
			appendString(builder, value, true);
			builder.append(GRAY);
			return;
		}

		// Map
		if (value instanceof Map) {
			Map map = (Map) value;
			int max = map.size();
			int pos = 0;
			int newIndent = indent == 0 ? 0 : indent + 1;
			builder.append('{');
			if (indent != 0) {
				appendIndent(builder, indent);
			}
			for (Object child : map.entrySet()) {
				Map.Entry entry = (Map.Entry) child;
				appendString(builder, entry.getKey(), false);
				builder.append(':');
				toColorizedString(builder, entry.getValue(), null, newIndent);
				if (++pos < max || meta != null) {
					builder.append(',');
					if (indent != 0) {
						appendIndent(builder, indent);
					}
				}
			}
			if (meta != null) {
				appendString(builder, Config.META, false);
				builder.append(':');
				toColorizedString(builder, meta, null, newIndent);
			}
			if (indent != 0) {
				appendIndent(builder, indent - 1);
			}
			builder.append('}');
			return;

		}

		// List or Set
		if (value instanceof Collection) {
			builder.append('[');
			if (indent != 0) {
				appendIndent(builder, indent);
			}
			Collection array = (Collection) value;
			int max = array.size();
			int pos = 0;
			int newIndent = indent == 0 ? 0 : indent + 1;
			for (Object child : array) {
				toColorizedString(builder, child, null, newIndent);
				if (++pos < max) {
					builder.append(',');
					if (indent != 0) {
						appendIndent(builder, indent);
					}
				}
			}
			if (indent != 0) {
				appendIndent(builder, indent - 1);
			}
			builder.append(']');
			return;
		}

		// Byte array
		if (value instanceof byte[]) {
			builder.append(CYAN);
			builder.append('"');
			builder.append(DataConverterRegistry.convert(String.class, value));
			builder.append('"');
			builder.append(GRAY);
			return;
		}

		// Array
		if (value.getClass().isArray()) {
			builder.append('[');
			if (indent != 0) {
				appendIndent(builder, indent);
			}
			int max = Array.getLength(value);
			int newIndent = indent == 0 ? 0 : indent + 1;
			for (int i = 0; i < max; i++) {
				toColorizedString(builder, Array.get(value, i), null, newIndent);
				if (i < max - 1) {
					builder.append(',');
					if (indent != 0) {
						appendIndent(builder, indent);
					}
				}
			}
			if (indent != 0) {
				appendIndent(builder, indent - 1);
			}
			builder.append(']');
			return;
		}

		// Other types
		appendString(builder, value, true);
	}

}
