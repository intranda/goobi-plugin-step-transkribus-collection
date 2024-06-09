package de.intranda.goobi.plugins;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class TranskribusHelper {

    /**
     * Check if the URL works
     * 
     * @param url
     * @return
     * @throws IOException
     */
    public static boolean checkUrl(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        //connection.setRequestMethod("HEAD");
        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            return false;
        } else {
            return true;
        }
    }
}
