package SocImpl;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;

public class Parser {
	// Struct the MSG block
	public static String serializeObjects(String header, String methodName,
			Object[] args) {
		String serializedString = header
				+ (methodName == null ? "" : " " + methodName) + "\n";
		for (Object arg : args) {
			if (arg instanceof Integer) {
				serializedString += "<Integer>" + arg.toString()
						+ "</Integer>\n";
			} else if (arg instanceof String) {
				serializedString += "<String>" + arg.toString() + "</String>\n";
			} else if (arg instanceof Boolean) {
				serializedString += "<Boolean>" + arg.toString()
						+ "</Boolean>\n";
			} else if (arg instanceof Vector) {
				serializedString += "<Vector>";
				Iterator iterator = ((Vector) arg).iterator();
				while (iterator.hasNext()) {
					serializedString += iterator.next().toString() + "||";
				}
				serializedString += "</Vector>\n";
			} else if (arg == null) {
				serializedString += "<NULL></NULL>\n";
			}
		}
		serializedString += "END\n";
		System.out.println(serializedString);
		return serializedString;
	}

	public static Callback deserializeObjects(String header,
			String serializedString) {
		String[] lines = serializedString.split("\n");
		if (!lines[0].startsWith(header)) {
			return new Callback(null, null);
		}

		String methodName = lines[0].substring(header.length()).trim();
		Object[] args = new Object[lines.length - 2];
		if (methodName.length() == 0)
			methodName = null;

		int index = 0;
		for (int i = 1; i < lines.length - 1; i++) {
			if (lines[i].startsWith("<Integer>")) {
				args[index++] = new Integer(Integer.valueOf(lines[i].substring(
						9, lines[i].indexOf("</Integer>"))));
			} else if (lines[i].startsWith("<String>")) {
				if (lines[i].indexOf("</String>") != -1) {
					args[index++] = new String(lines[i].substring(8,
							lines[i].indexOf("</String>")));
				} else {
					String temp = lines[i].substring(8) + "\n";
					i++;
					while (lines[i].indexOf("</String>") == -1) {
						temp += lines[i] + "\n";
						i++;
					}
					temp += lines[i]
							.substring(0, lines[i].indexOf("</String>"));
					args[index++] = temp;
				}
			} else if (lines[i].startsWith("<Boolean>")) {
				args[index++] = new Boolean(Boolean.valueOf(lines[i].substring(
						9, lines[i].indexOf("</Boolean>"))));
			} else if (lines[i].startsWith("<Vector>")) {
				lines[i] = lines[i].substring(lines[i].indexOf(8),
						lines[i].indexOf("</Vector>"));
				String[] words = lines[i].substring(8).split("\\|\\|");
				args[index++] = new Vector(Arrays.asList(words));
			} else if (lines[i].startsWith("<NULL></NULL>")) {
				args[index++] = null;
			}
		}

		Object[] returnArgs = null;
		
		if (index == args.length) {
			returnArgs = args;
		} else {
			returnArgs = new Object[index];
			for (int j = 0; j < index; j++) {
				returnArgs[j] = args[j];
			}
		}

		return new Callback(methodName, returnArgs);
	}
}
