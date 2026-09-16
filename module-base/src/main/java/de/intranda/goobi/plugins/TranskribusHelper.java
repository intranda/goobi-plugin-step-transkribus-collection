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
import com.fasterxml.jackson.databind.ObjectMapper;

import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;

public class TranskribusHelper {

    public static final String DOCUMENT_ID_PROPERTY = "Transkribus Document ID";

    private static final int MAX_LOGGED_ANSWER_LENGTH = 500;

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
     * @throws InterruptedException
     * @throws IOException
     */
    public static String ingestMetsFile(String sessionId, String transkribusUrl, String metsUrl, String collectionId, long delay)
            throws IOException, InterruptedException {
        HttpResponse<String> response = Unirest.post(
                transkribusUrl + "collections/" + collectionId + "/createDocFromMetsUrl?fileName=" + metsUrl)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Cookie", "JSESSIONID=" + sessionId)
                .field("JSESSIONID", sessionId)
                .asString();
        String jobID = extractJobId(response.getStatus(), response.getBody());

        Thread.sleep(delay);
        TranskribusJob tj = requestJob(sessionId, transkribusUrl, jobID);
        if (tj.getDocId() == null) {
            throw new IOException("Transkribus job " + jobID + " did not deliver a document ID (state: " + tj.getState()
                    + "). Maybe the configured delay is too short.");
        }
        return String.valueOf(tj.getDocId());
    }

    /**
     * trigger the export of the given document as alto files
     *
     * @param documentId
     * @param sessionId
     * @param transkribusUrl
     * @param collectionId
     * @param delay
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    public static String startExport(String documentId, String sessionId, String transkribusUrl, String collectionId, long delay)
            throws InterruptedException, IOException {
        HttpResponse<String> response = Unirest.post(transkribusUrl + "collections/" + collectionId + "/export")
                .header("Content-Type", "application/json")
                .header("Cookie", "JSESSIONID=" + sessionId)
                .body("{\"commonPars\" : {\"doExportDocMetadata\" : true,\"doWriteMets\" : true,\"doWriteImages\" : true,\"doExportPageXml\" : true,\"doExportAltoXml\" : true,\"doExportSingleTxtFiles\" : false,\"doWritePdf\" : false,\"doWriteTei\" : false,\"doWriteDocx\" : false,\"doWriteOneTxt\" : false,\"useVersionStatus\" : \"Latest version\",\"writeTextOnWordLevel\" : false,\"splitIntoWordsInAltoXml\" : true},\"docs\" : {\"doc\" : [ {\"docId\" : "
                        + documentId + ",\"pageList\" : {\"pages\" : [ ]}}]}}")
                .asString();
        String jobID = extractJobId(response.getStatus(), response.getBody());

        Thread.sleep(delay);
        TranskribusJob tj = requestJob(sessionId, transkribusUrl, jobID);
        return String.valueOf(tj.getResult());

    }

    /**
     * Ask Transkribus for the status of a job
     *
     * @param sessionId
     * @param transkribusUrl
     * @param jobId
     * @return
     * @throws IOException if Transkribus does not answer with a JSON job description
     */
    private static TranskribusJob requestJob(String sessionId, String transkribusUrl, String jobId) throws IOException {
        HttpResponse<String> response = Unirest.get(transkribusUrl + "jobs/" + jobId)
                .header("Accept", "application/json")
                .header("Cookie", "JSESSIONID=" + sessionId)
                .asString();
        return parseJob(jobId, response.getStatus(), response.getBody());
    }

    /**
     * Read the job ID out of a Transkribus answer. Transkribus answers such requests with the plain numeric job ID. Anything else - an error message,
     * an HTML error page, an empty body - means that the request failed, and must not end up in the URL of the follow up request, as this would only
     * produce a misleading error much later on.
     *
     * @param status HTTP status code of the answer
     * @param body body of the answer
     * @return the job ID without surrounding whitespace
     * @throws IOException if the request failed or the answer is no job ID
     */
    static String extractJobId(int status, String body) throws IOException {
        if (status < 200 || status >= 300) {
            throw new IOException("Transkribus answered with HTTP status " + status + " instead of a job id: " + abbreviate(body));
        }
        String jobId = body == null ? "" : body.trim();
        if (!jobId.matches("\\d+")) {
            throw new IOException("Transkribus did not answer with a job id but with: " + abbreviate(body));
        }
        return jobId;
    }

    /**
     * Parse the Transkribus answer for a job status request.
     *
     * @param jobId job ID the answer belongs to
     * @param status HTTP status code of the answer
     * @param body body of the answer
     * @return the parsed job
     * @throws IOException if the request failed or the answer is no JSON job description
     */
    static TranskribusJob parseJob(String jobId, int status, String body) throws IOException {
        if (status < 200 || status >= 300) {
            throw new IOException(
                    "Transkribus answered with HTTP status " + status + " for job " + jobId + ": " + abbreviate(body));
        }
        try {
            return new ObjectMapper().readValue(body, TranskribusJob.class);
        } catch (JsonProcessingException e) {
            throw new IOException("Transkribus did not answer with a JSON job description for job " + jobId + " but with: " + abbreviate(body), e);
        }
    }

    /**
     * Shorten an answer so that it stays readable inside of an error message and the process journal
     *
     * @param body
     * @return
     */
    private static String abbreviate(String body) {
        if (body == null) {
            return "<no answer>";
        }
        String text = body.trim();
        if (text.isEmpty()) {
            return "<empty answer>";
        }
        if (text.length() > MAX_LOGGED_ANSWER_LENGTH) {
            return text.substring(0, MAX_LOGGED_ANSWER_LENGTH) + "...";
        }
        return text;
    }

}
