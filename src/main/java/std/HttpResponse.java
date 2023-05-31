package std;

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

    public HttpResponse(int statusCode, String statusLine, HashMap<String, Object> response) {
        this.statusCode = statusCode;
        this.statusLine = statusLine;
        this.response = response;
    }

    public HttpResponse(int statusCode, String statusLine, HashMap<String, Object> response, List<Document> documents) {
        this.response = response;
        this.statusLine = statusLine;
        this.statusCode = statusCode;
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

        Map<String,Object> response = new HashMap<>(getResponse());
        response.put("Status Code", statusCode);

        // If files were returned from the http response, add them to Appian response in designer
        if (documents == null) return response;
        if (documents.size() == 1) {
            documents.forEach(doc -> response.put("Document", doc));
        } else {
            AtomicInteger index = new AtomicInteger(1);
            documents.forEach(doc -> response.put("Document" + index.getAndIncrement(), doc));
        }

        return response;
    }
}
