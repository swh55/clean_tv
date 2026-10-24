package com.gathin.tvhome;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The weather line's data: a small {@code weather.json} next to the theme's config.json, written and
 * pushed by {@code tools/weather.py} (Open-Meteo, fetched on the computer). The launcher itself never
 * goes online, so it only reads this file; without it the line stays hidden. The shape is fixed by the
 * tool: {"temp_c": 18.4, "code": 1} — the temperature now in Celsius and Open-Meteo's WMO weather code.
 * Plain text scanning, no org.json and no Android types, so it is tested on the JVM.
 */
final class Weather {

    static final String FILE_NAME = "weather.json";
    private static final long MAX_BYTES = 4 * 1024;

    private static final Pattern TEMP = Pattern.compile("\"temp_c\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
    private static final Pattern CODE = Pattern.compile("\"code\"\\s*:\\s*(-?\\d+)");

    private Weather() {
    }

    /** The file's {tempC, code}, or null when it is missing, unreadable or not parseable. */
    static Data read(File folder) {
        File file = folder == null ? null : new File(folder, FILE_NAME);
        if (file == null || !file.isFile() || file.length() > MAX_BYTES) return null;
        try {
            // readAllLines, not readString: readString needs API 33, the launcher's minSdk is 26.
            StringBuilder text = new StringBuilder();
            for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) text.append(line);
            return parse(text.toString());
        } catch (Exception e) {
            return null;
        }
    }

    static Data parse(String text) {
        if (text == null) return null;
        Matcher t = TEMP.matcher(text);
        Matcher c = CODE.matcher(text);
        if (!t.find() || !c.find()) return null;
        try {
            return new Data(Double.parseDouble(t.group(1)), Integer.parseInt(c.group(1)));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** The string resource name for a WMO weather code, coarse buckets: the line reads "18° · Rain". */
    static String bucket(int code) {
        if (code <= 1) return "weather_clear";
        if (code == 2) return "weather_partly";
        if (code == 3 || code == 45 || code == 48) return "weather_cloudy";
        if (code >= 51 && code <= 57) return "weather_drizzle";
        if ((code >= 61 && code <= 67) || (code >= 80 && code <= 82)) return "weather_rain";
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) return "weather_snow";
        if (code >= 95 && code <= 99) return "weather_thunder";
        return "weather_cloudy";
    }

    /** The temperature now, in Celsius, and the WMO code it came with. */
    static final class Data {
        final double tempC;
        final int code;

        Data(double tempC, int code) {
            this.tempC = tempC;
            this.code = code;
        }
    }
}
