package com.kareem.lifeos.secondbrain;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Thin Android transport adapter for the exact upstream SecondBrain HTTP contract.
 *
 * No retrieval, graph, commitment, digest or person logic lives here. Those remain owned by the
 * pinned openintelligence-labs/secondbrain service. This class only mirrors its documented HTTP
 * surface so the LifeOS Android shell can consume it.
 */
public final class SecondBrainGateway {
    private static final String PREFS = "lifeos_secondbrain";
    private static final String KEY_BASE = "base_url";
    public static final String DEFAULT_BASE = "http://127.0.0.1:7821";
    private final Context context;

    public SecondBrainGateway(Context context) { this.context = context.getApplicationContext(); }

    public String baseUrl() {
        String value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_BASE, DEFAULT_BASE);
        if (value == null || value.trim().isEmpty()) return DEFAULT_BASE;
        value = value.trim();
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value;
    }

    public void setBaseUrl(String value) {
        if (value == null || value.trim().isEmpty()) value = DEFAULT_BASE;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_BASE, value.trim()).apply();
    }

    public boolean health() throws Exception {
        JSONObject o = get("/health");
        return o.optBoolean("ok", false);
    }

    public List<TimelineEvent> timeline(String startIso, String endIso) throws Exception {
        JSONObject body = new JSONObject().put("start", startIso).put("end", endIso);
        JSONArray xs = post("/timeline", body).optJSONArray("events");
        if (xs == null) return Collections.emptyList();
        List<TimelineEvent> out = new ArrayList<>();
        for (int i = 0; i < xs.length(); i++) {
            JSONObject x = xs.getJSONObject(i);
            out.add(new TimelineEvent(x.optString("memory_id"), x.optString("content"),
                    nullable(x, "valid_from"), x.optDouble("importance", 0)));
        }
        return out;
    }

    public PersonCard who(String name) throws Exception {
        JSONObject response = post("/who", new JSONObject().put("name", name));
        JSONArray xs = response.optJSONArray("facts");
        List<TimelineEvent> facts = new ArrayList<>();
        if (xs != null) for (int i = 0; i < xs.length(); i++) {
            JSONObject x = xs.getJSONObject(i);
            facts.add(new TimelineEvent(x.optString("memory_id"), x.optString("content"),
                    nullable(x, "valid_from"), x.optDouble("importance", 0)));
        }
        return new PersonCard(response.optString("person_id"), name, facts);
    }

    public List<Commitment> commitments(String status) throws Exception {
        JSONObject response = post("/commitments", new JSONObject().put("status", status));
        JSONArray xs = response.optJSONArray("commitments");
        if (xs == null) return Collections.emptyList();
        List<Commitment> out = new ArrayList<>();
        for (int i = 0; i < xs.length(); i++) {
            JSONObject x = xs.getJSONObject(i);
            out.add(new Commitment(x.optString("id"), x.optString("content"),
                    nullable(x, "due_at"), x.optString("status"), nullable(x, "owner_pid")));
        }
        return out;
    }

    public Digest digest(String date, String period) throws Exception {
        JSONObject response = post("/digest", new JSONObject().put("date", date).put("period", period));
        return new Digest(response.optString("period"), strings(response.optJSONArray("themes")),
                strings(response.optJSONArray("broken_promises")),
                strings(response.optJSONArray("suggested_followups")),
                strings(response.optJSONArray("cited")), response.optDouble("importance_sum", 0));
    }

    private JSONObject get(String path) throws Exception { return request("GET", path, null); }
    private JSONObject post(String path, JSONObject body) throws Exception { return request("POST", path, body); }

    private JSONObject request(String method, String path, JSONObject body) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(baseUrl() + path).openConnection();
        c.setConnectTimeout(1800); c.setReadTimeout(4000); c.setRequestMethod(method);
        c.setRequestProperty("Accept", "application/json");
        if (body != null) {
            c.setDoOutput(true); c.setRequestProperty("Content-Type", "application/json");
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = c.getOutputStream()) { os.write(bytes); }
        }
        int code = c.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
        String text = read(stream);
        c.disconnect();
        if (code < 200 || code >= 300) throw new IllegalStateException(path + " HTTP " + code + (text.isEmpty() ? "" : ": " + text));
        return text.isEmpty() ? new JSONObject() : new JSONObject(text);
    }

    private static String read(InputStream in) throws Exception {
        if (in == null) return "";
        StringBuilder b = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            for (String line; (line = r.readLine()) != null;) b.append(line);
        }
        return b.toString();
    }

    private static String nullable(JSONObject o, String key) {
        return o.isNull(key) ? null : o.optString(key, null);
    }

    private static List<String> strings(JSONArray xs) {
        if (xs == null) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        for (int i = 0; i < xs.length(); i++) { String s = xs.optString(i, ""); if (!s.isEmpty()) out.add(s); }
        return out;
    }

    public static final class TimelineEvent {
        public final String memoryId, content, validFrom; public final double importance;
        public TimelineEvent(String memoryId, String content, String validFrom, double importance) {
            this.memoryId=memoryId; this.content=content; this.validFrom=validFrom; this.importance=importance;
        }
    }
    public static final class PersonCard {
        public final String personId, name; public final List<TimelineEvent> facts;
        public PersonCard(String personId, String name, List<TimelineEvent> facts) { this.personId=personId; this.name=name; this.facts=facts; }
    }
    public static final class Commitment {
        public final String id, content, dueAt, status, ownerPid;
        public Commitment(String id, String content, String dueAt, String status, String ownerPid) {
            this.id=id; this.content=content; this.dueAt=dueAt; this.status=status; this.ownerPid=ownerPid;
        }
    }
    public static final class Digest {
        public final String period; public final List<String> themes, brokenPromises, suggestedFollowups, cited; public final double importanceSum;
        public Digest(String period,List<String> themes,List<String> brokenPromises,List<String> suggestedFollowups,List<String> cited,double importanceSum){this.period=period;this.themes=themes;this.brokenPromises=brokenPromises;this.suggestedFollowups=suggestedFollowups;this.cited=cited;this.importanceSum=importanceSum;}
    }
}
