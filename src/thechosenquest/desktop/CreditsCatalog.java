package thechosenquest.desktop;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Loads the release-owned credits data that is also shipped with the game. */
final class CreditsCatalog {
    static final class Entry {
        final String section;
        final String title;
        final String creator;
        final String license;
        final String source;
        final String notes;

        Entry(String section, String title, String creator, String license,
              String source, String notes) {
            this.section = section;
            this.title = title;
            this.creator = creator;
            this.license = license;
            this.source = source;
            this.notes = notes;
        }
    }

    private static final String CREDITS_RESOURCE = "/assets/credits/credits.tsv";
    private static final String TESTERS_RESOURCE = "/assets/credits/beta-testers.txt";

    private CreditsCatalog() { }

    static List<Entry> entries() {
        InputStream stream = CreditsCatalog.class.getResourceAsStream(CREDITS_RESOURCE);
        if (stream == null) return Collections.emptyList();
        List<Entry> entries = new ArrayList<Entry>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream,
            java.nio.charset.StandardCharsets.UTF_8));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0 || line.startsWith("#")) continue;
                String[] fields = line.split("\\t", -1);
                if (fields.length < 6) continue;
                entries.add(new Entry(fields[0], fields[1], fields[2], fields[3],
                    fields[4], fields[5]));
            }
        } catch (IOException ignored) {
            return Collections.emptyList();
        } finally {
            try { reader.close(); } catch (IOException ignored) { }
        }
        return Collections.unmodifiableList(entries);
    }

    static List<String> betaTesters() {
        InputStream stream = CreditsCatalog.class.getResourceAsStream(TESTERS_RESOURCE);
        if (stream == null) return Collections.emptyList();
        List<String> testers = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream,
            java.nio.charset.StandardCharsets.UTF_8));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0 && !line.startsWith("#")) testers.add(line);
            }
        } catch (IOException ignored) {
            return Collections.emptyList();
        } finally {
            try { reader.close(); } catch (IOException ignored) { }
        }
        return Collections.unmodifiableList(testers);
    }
}
