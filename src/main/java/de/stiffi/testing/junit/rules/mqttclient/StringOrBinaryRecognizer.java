package de.stiffi.testing.junit.rules.mqttclient;

import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

public class StringOrBinaryRecognizer {
    public static boolean looksLikeUtf8String(byte[] data) throws UnsupportedEncodingException {
        Pattern p = Pattern.compile("\\A(\n" +
                "  [\\x09\\x0A\\x0D\\x20-\\x7E]             # ASCII\\n" +
                "| [\\xC2-\\xDF][\\x80-\\xBF]               # non-overlong 2-byte\n" +
                "|  \\xE0[\\xA0-\\xBF][\\x80-\\xBF]         # excluding overlongs\n" +
                "| [\\xE1-\\xEC\\xEE\\xEF][\\x80-\\xBF]{2}  # straight 3-byte\n" +
                "|  \\xED[\\x80-\\x9F][\\x80-\\xBF]         # excluding surrogates\n" +
                "|  \\xF0[\\x90-\\xBF][\\x80-\\xBF]{2}      # planes 1-3\n" +
                "| [\\xF1-\\xF3][\\x80-\\xBF]{3}            # planes 4-15\n" +
                "|  \\xF4[\\x80-\\x8F][\\x80-\\xBF]{2}      # plane 16\n" +
                ")*\\z", Pattern.COMMENTS);

        String phonyString = new String(data, "ISO-8859-1");
        return p.matcher(phonyString).matches();
    }
}
