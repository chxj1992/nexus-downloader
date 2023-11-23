package com.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {

    private static final String initPath = "/tmp/repo";


    public static void main(String[] args) throws IOException {
        new App().download();
    }

    public void download() throws IOException {
        String token = "";
        while (true) {
            HashMap<String, Object> stringStringHashMap = readJson(token);
            Object continuationToken = stringStringHashMap.getOrDefault("continuationToken", "");
            if (continuationToken == null || StringUtils.isBlank(String.valueOf(continuationToken))) {
                System.out.println("download finished");
                break;
            }
            token = String.valueOf(continuationToken);
            List<Map> list = (List) stringStringHashMap.get("items");
            for (Map map : list) {
                List<Map> assets = (List<Map>) map.get("assets");
                for (Map dMap : assets) {
                    String url = String.valueOf(dMap.get("downloadUrl"));
                    String path = String.valueOf(dMap.get("path"));
                    genFile(url, initPath + path);
                }
            }
        }
    }


    public HashMap<String, Object> readJson(String token) throws IOException {
        OkHttpClient client = new OkHttpClient();
        String url = "https://maven.xxx.xxx/service/rest/v1/components?repository=maven-snapshots";
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


    private void genFile(final String urlStr, final String path) {
        System.out.println(path);

        URL url;
        try {
            url = new URL(urlStr);
            File t = new File(path);
            t.getParentFile().mkdirs();
            File temp = new File(path);
            FileUtils.copyURLToFile(url, temp);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}