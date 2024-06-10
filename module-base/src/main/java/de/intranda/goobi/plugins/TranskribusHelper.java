package de.intranda.goobi.plugins;

import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.http.client.ClientProtocolException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;

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
        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Login into Transkribus and get back a session ID
     * 
     * @param transkribusUrl
     * @param user
     * @param pw
     * @return
     * @throws URISyntaxException
     * @throws ClientProtocolException
     * @throws IOException
     * @throws JDOMException
     */
    public static String getSessionId(String transkribusUrl, String user, String pw)
            throws URISyntaxException, ClientProtocolException, IOException, JDOMException {

        HttpResponse<String> response = Unirest.post(transkribusUrl + "auth/login")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .field("user", user)
                .field("pw", pw)
                .asString();
        String xml = response.getBody();

        // parse response as xml
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(new StringReader(xml));

        // Get sessionId element of of it
        Element sessionIdElement = doc.getRootElement().getChild("sessionId");
        if (sessionIdElement != null) {
            return sessionIdElement.getTextTrim();
        } else {
            throw new IOException("No session ID available for user " + user + ".");
        }
    }

    /**
     * Ingest the public available METS file to the given Transkribus collection and pass back the document ID
     * 
     * @param sessionId
     * @param transkribusUrl
     * @param metsUrl
     * @param collectionId
     * @return
     * @throws JsonProcessingException
     * @throws JsonMappingException
     * @throws InterruptedException
     * @throws URISyntaxException
     * @throws ClientProtocolException
     * @throws IOException
     */
    public static String ingestMetsFile(String sessionId, String transkribusUrl, String metsUrl, String collectionId)
            throws JsonMappingException, JsonProcessingException, InterruptedException {
        HttpResponse<String> response = Unirest.post(
                transkribusUrl + "collections/" + collectionId + "/createDocFromMetsUrl?fileName=" + metsUrl)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Cookie", "JSESSIONID=" + sessionId)
                .field("JSESSIONID", sessionId)
                .asString();
        String jobID = response.getBody();

        Thread.sleep(3000);
        HttpResponse<String> response2 = Unirest.get(transkribusUrl + "jobs/" + jobID)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Cookie", "JSESSIONID=" + sessionId)
                .asString();

        ObjectMapper om = new ObjectMapper();
        TranskribusJob tj = om.readValue(response2.getBody(), TranskribusJob.class);
        return String.valueOf(tj.getDocId());
    }

}
