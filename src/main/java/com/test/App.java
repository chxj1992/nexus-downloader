package com.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {

    public static void main(String[] args) throws IOException {
        String host = System.getProperty("host");
        String repoId = System.getProperty("repoId");
        String dir = System.getProperty("dir");
        if (StringUtils.isBlank(host) ) {
            System.out.println("host is required");
            return;
        }
        if (StringUtils.isBlank(repoId)) {
            System.out.println("repoId is required");
            return;
        }
        if (StringUtils.isBlank(dir)) {
            System.out.println("dir is required");
            return;
        }

        new App().download(host, repoId, dir);
    }

    public void download(String host, String repoId, String dir) throws IOException {
        String token = "";
        Map<String, Map> filteredMap = new HashMap<>();
        int counter = 0;
        while (true) {
            HashMap<String, Object> stringStringHashMap;
            try {
                stringStringHashMap = readJson(token, host, repoId);
            } catch (Exception e) {
                System.out.println("updated index failed: " + e.getMessage());
                try {
                    Thread.sleep(1000);
                } catch (Exception ignore) {
                }
                continue;
            }
            Object continuationToken = stringStringHashMap.getOrDefault("continuationToken", "");
            token = continuationToken != null ? String.valueOf(continuationToken) : null;
            if (stringStringHashMap.get("items") == null) {
                break;
            }
            List<Map> list = (List) stringStringHashMap.get("items");
            for (Map map : list) {
                String group = String.valueOf(map.get("group"));
                String name = String.valueOf(map.get("name"));
                String version = String.valueOf(map.get("version"));
                if (version.endsWith("-SNAPSHOT")) {
                    String[] versionSplit = version.split("-");
                    String version0 = versionSplit[0];
                    Long version1 = Long.parseLong(versionSplit[1].replace(".", ""));
                    Long version2 = Long.parseLong(versionSplit[2]);
                    String key = group + name + version0;
                    Map map1 = filteredMap.get(key);
                    if (map1 == null) {
                        filteredMap.put(key, map);
                    } else {
                        String tVersion = String.valueOf(map1.get("version"));
                        String[] tVersionSplit = tVersion.split("-");
                        Long tVersion1 = Long.parseLong(tVersionSplit[1].replace(".", ""));
                        Long tVersion2 = Long.parseLong(tVersionSplit[2]);
                        if (tVersion1 > version1) {
                            filteredMap.put(key, map1);
                        } else if (tVersion1.equals(version1) && tVersion2 > version2) {
                            filteredMap.put(key, map1);
                        } else {
                            System.out.println("ignore " + key + ", current: " + tVersion1 + "-" + tVersion2 + " | existed: " + version1 + "-" + version2);
                        }
                    }
                } else {
                    String key = group + name + version;
                    filteredMap.put(key, map);
                }
            }
            counter++;
            System.out.println("index update, counter: " + counter + ", resources size: " + filteredMap.size());
            if (continuationToken == null || StringUtils.isBlank(String.valueOf(continuationToken))) {
                System.out.println("index download finished, resources size: " + filteredMap.size());
                break;
            }
        }

        System.out.println("start download resources");
        int downloadCounter = 0;
        for (Map map : filteredMap.values()) {
            List<Map> assets = (List<Map>) map.get("assets");
            for (Map dMap : assets) {
                String url = String.valueOf(dMap.get("downloadUrl"));
                String path = String.valueOf(dMap.get("path"));
                if (url.endsWith(".pom") || url.endsWith(".jar")) {
                    genFile(url, dir + "/" + path);
                    downloadCounter++;
                }
            }
        }
        System.out.println("resources download finished! download counter: " + downloadCounter);
    }


    public HashMap<String, Object> readJson(String token, String host, String repoId) throws IOException {
        OkHttpClient client = new OkHttpClient();
        String url = "https://" + host + "/service/rest/v1/components?repository=" + repoId;
        if (StringUtils.isNotBlank(token)) {
            url += "&continuationToken=" + token;
        }
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(response.body().string(), HashMap.class);
        }
    }

    private void genFile(String urlStr, String path) {
        if (urlStr.startsWith("http://")) {
            urlStr = urlStr.replaceFirst("http://", "https://");
        }
        URL url;
        while (true) {
            try {
                Thread.sleep(100);
                url = new URL(urlStr);
                File t = new File(path);
                t.getParentFile().mkdirs();
                File temp = new File(path);
                FileUtils.copyURLToFile(url, temp);
                break;
            } catch (Exception e) {
                System.out.println("download file failed with exception, " + e.getMessage());
            }
        }
    }

}