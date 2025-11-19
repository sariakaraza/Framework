package util;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

public class UrlPattern {
    public final String rawPattern;
    public final Pattern regex;
    public final Method method;

    public UrlPattern(String raw, Method m) {
        this.rawPattern = raw;
        this.method = m;
        String r = raw.replaceAll("\\{[^/]+}", "([^/]+)");
        this.regex = Pattern.compile("^" + r + "$");
    }
}
