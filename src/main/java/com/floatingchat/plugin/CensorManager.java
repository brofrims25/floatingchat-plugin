package com.floatingchat.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Menyensor kata-kata terlarang yang diatur lewat config.yml (censor.words).
 * Mendukung deteksi trik bypass sederhana seperti "a-n-j-i-n-g" atau "an j i ng".
 */
public class CensorManager {

    private final ConfigManager configManager;
    private List<Pattern> patterns = new ArrayList<>();

    public CensorManager(ConfigManager configManager) {
        this.configManager = configManager;
        reload();
    }

    public void reload() {
        patterns = new ArrayList<>();
        for (String word : configManager.getCensorWords()) {
            if (word == null || word.trim().isEmpty()) continue;
            patterns.add(buildPattern(word.trim()));
        }
    }

    private Pattern buildPattern(String word) {
        StringBuilder regex = new StringBuilder();
        // Sisipkan pemisah opsional (spasi, titik, strip, underscore, bintang) di antara tiap huruf
        // supaya trik seperti "a-n-j-i-n-g" atau "a n j i n g" tetap terdeteksi.
        String separator = "[\\s._\\-*]{0,2}";
        for (int i = 0; i < word.length(); i++) {
            regex.append(Pattern.quote(String.valueOf(word.charAt(i))));
            if (i != word.length() - 1 && configManager.isCensorDetectBypassTricks()) {
                regex.append(separator);
            }
        }
        int flags = configManager.isCensorIgnoreCase() ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0;
        return Pattern.compile(regex.toString(), flags);
    }

    /**
     * Mengembalikan pesan yang sudah disensor sesuai konfigurasi.
     */
    public String censor(String message) {
        if (!configManager.isCensorEnabled() || message == null || message.isEmpty()) {
            return message;
        }

        String result = message;
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(result);
            StringBuilder sb = new StringBuilder();
            int lastEnd = 0;
            while (matcher.find()) {
                sb.append(result, lastEnd, matcher.start());
                String matched = matcher.group();
                sb.append(replacementFor(matched));
                lastEnd = matcher.end();
            }
            sb.append(result.substring(lastEnd));
            result = sb.toString();
        }
        return result;
    }

    private String replacementFor(String matchedWord) {
        String replacement = configManager.getCensorReplacement();
        if ("AUTO_STARS".equalsIgnoreCase(replacement)) {
            StringBuilder stars = new StringBuilder();
            for (int i = 0; i < matchedWord.length(); i++) stars.append('*');
            return stars.toString();
        }
        return replacement;
    }
}
