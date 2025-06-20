package services;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class JiraTicketFetcher {

    private JiraTicketFetcher() {
        // Utility class
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    private static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        try (InputStream is = java.net.URI.create(url).toURL().openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String jsonText = readAll(rd);
            return new JSONObject(jsonText);
        }
    }

    private static String buildJiraQueryUrl(String projectKey, int startAt, int maxResults) {
        return "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                + projectKey + "%22%20AND%20issuetype=%22Bug%22%20AND%20(status=%22closed%22%20OR%20status=%22resolved%22)%20AND%20resolution=%22fixed%22"
                + "&fields=key,resolutiondate&startAt=" + startAt + "&maxResults=" + maxResults;
    }

    /**
     * Recupera tutti i ticket di tipo "Bug" chiusi e fissati per il progetto specificato,
     * restituendo una mappa {ticket ID -> resolutionDate}.
     */
    public static Map<String, String> fetchFixedBugTickets(String projectKey) throws IOException {
        Map<String, String> ticketMap = new HashMap<>();
        int startAt = 0;
        int total;
        do {
            String url = buildJiraQueryUrl(projectKey, startAt, 1000);
            JSONObject json;
            try {
                json = readJsonFromUrl(url);
            } catch (JSONException e) {
                throw new IOException("Errore nella lettura della risposta JSON da JIRA", e);
            }
            JSONArray issues = json.getJSONArray("issues");
            total = json.getInt("total");
            for (int idx = 0; idx < issues.length() && startAt < total; idx++, startAt++) {
                JSONObject issue = issues.getJSONObject(idx);
                String key = issue.getString("key");
                String resolutionDate = issue.getJSONObject("fields").optString("resolutiondate", "");
                ticketMap.put(key, resolutionDate);
            }
        } while (startAt < total);
        return ticketMap;
    }
}
