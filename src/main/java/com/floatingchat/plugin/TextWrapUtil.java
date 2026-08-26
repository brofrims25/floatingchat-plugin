package com.floatingchat.plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Memecah teks panjang menjadi beberapa baris berdasarkan jumlah karakter maksimal per baris,
 * tanpa memotong kata di tengah (word wrap), lalu menggabungkannya kembali dengan newline.
 * Perataan (kiri/tengah/kanan) ditangani langsung oleh TextDisplay.TextAlignment di Minecraft,
 * jadi di sini kita cukup menyusun barisnya.
 */
public class TextWrapUtil {

    private TextWrapUtil() {}

    public static String wrap(String text, int maxCharsPerLine, int maxLines) {
        if (text == null || text.isEmpty()) return "";
        if (maxCharsPerLine <= 0) maxCharsPerLine = 40;

        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();

        String[] words = text.split(" ");
        for (String word : words) {
            // Kata tunggal yang lebih panjang dari batas baris tetap dipotong paksa
            while (word.length() > maxCharsPerLine) {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder();
                }
                lines.add(word.substring(0, maxCharsPerLine));
                word = word.substring(maxCharsPerLine);
            }

            if (currentLine.length() == 0) {
                currentLine.append(word);
            } else if (currentLine.length() + 1 + word.length() <= maxCharsPerLine) {
                currentLine.append(' ').append(word);
            } else {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        if (maxLines > 0 && lines.size() > maxLines) {
            lines = new ArrayList<>(lines.subList(0, maxLines));
            int lastIndex = lines.size() - 1;
            String last = lines.get(lastIndex);
            String trimmed = last.length() > 3 ? last.substring(0, last.length() - 3) : last;
            lines.set(lastIndex, trimmed + "...");
        }

        return String.join("\n", lines);
    }
}
