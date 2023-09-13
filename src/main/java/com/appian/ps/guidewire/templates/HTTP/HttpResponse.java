package com.appian.ps.guidewire.templates.HTTP;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.appian.connectedsystems.templateframework.sdk.configuration.Document;

public class HttpResponse {
    private final Map<String, Object> response;
    private final int statusCode;
    private final String statusLine;
    private List<Document> documents;
    private String contentType;
    private Map<String, Object> headers;

    public HttpResponse(int statusCode, String statusLine, Map<String, Object> response, String contentType, Map<String, Object> headers) {
        this.statusCode = statusCode;
        this.statusLine = statusLine;
        this.response = response;
        this.contentType = contentType;
        this.headers = headers;
    }

    public HttpResponse(int statusCode, String statusLine, Map<String, Object> response, String contentType, Map<String,
        Object> headers, List<Document> documents) {
        this.response = response;
        this.statusLine = statusLine;
        this.statusCode = statusCode;
        this.contentType = contentType;
        this.headers = headers;
        this.documents = documents;
    }

    public void addDocuments(Document document) {
        this.documents.add(document);
    }

    public List<Document> getDocuments() {
        return this.documents != null ? documents : null;
    }
    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusLine(){
        return statusLine;
    }

    public Map<String, Object> getResponse(){
        return response;
    }

    public Map<String,Object> getCombinedResponse() {

        Map<String,Object> formattedHTTPResponse = new HashMap<>();
        formattedHTTPResponse.put("body", response);
        formattedHTTPResponse.put("statusCode", statusCode);
        formattedHTTPResponse.put("statusLine", statusLine);
        formattedHTTPResponse.put("contentType", contentType);
        formattedHTTPResponse.put("headers", headers);

        // If files were returned from the http response, add them to Appian response in designer
        if (documents == null) return formattedHTTPResponse;
        if (documents.size() == 1) {
            documents.forEach(doc -> formattedHTTPResponse.put("Document", doc));
        } else {
            AtomicInteger index = new AtomicInteger(1);
            documents.forEach(doc -> formattedHTTPResponse.put("Document" + index.getAndIncrement(), doc));
        }

        return formattedHTTPResponse;
    }
}
