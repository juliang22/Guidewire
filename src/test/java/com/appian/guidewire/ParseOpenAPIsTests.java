package com.appian.guidewire;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.IOUtils;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import std.ConstantKeys;

public class ParseOpenAPIsTests implements ConstantKeys {

  public ParseOpenAPIsTests() throws IOException {
  }

  private static void addDoc(IndexWriter w, String title) throws Exception {
    Document doc = new Document();
    doc.add(new TextField("title", title, Field.Store.YES));
    w.addDocument(doc);
  }


  @Test
  public void testLuceneFuzzySearch() throws Exception {
    ClassLoader classLoader = ParseOpenAPIsTests.class.getClassLoader();
    InputStream input = classLoader.getResourceAsStream("com/appian/guidewire/templates/claimsv2.json");
    String swaggerStr = IOUtils.toString(input, StandardCharsets.UTF_8);

    JsonNode openApi = new ObjectMapper().readValue(swaggerStr, JsonNode.class);
    JsonNode paths = openApi.get("paths");



    // 1. Prepare Lucene to index some text
    StandardAnalyzer analyzer = new StandardAnalyzer();
    Directory index = new ByteBuffersDirectory();
    IndexWriterConfig config = new IndexWriterConfig(analyzer);
    IndexWriter w = new IndexWriter(index, config);
    List<String> listOfChoicesForSearch = new ArrayList<>();
    paths.fields().forEachRemaining(pathNode -> {
      String pathName = pathNode.getKey();
      JsonNode path = pathNode.getValue();
      if (PATHS_TO_REMOVE.contains(pathName)) return;

      Map<String, JsonNode> operations = new HashMap<>();
      operations.put(GET, path.get(GET));
      operations.put(POST, path.get(POST));
      operations.put(PATCH, path.get(PATCH));
      operations.put(DELETE, path.get(DELETE));
      operations.forEach((restOperation, operation) -> {
        if (operation == null || operation.size() == 0) return;
        if (operation.get(DEPRECATED) != null && operation.get(DEPRECATED).asBoolean()) return;

        // Builds up endpoint choices and choices list for search on initial run with all paths
        String summary = operation.get(SUMMARY).asText();
        String description = operation.get(DESCRIPTION).asText();
        String name = restOperation.toUpperCase() + " - " + summary;
        String value = String.join(":", "claimCenter", restOperation, pathName, "ClaimAPI", summary, description);
        listOfChoicesForSearch.add(value);

        try {
          addDoc(w, value);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      });
    });
    w.close();


    // 2. Query
    String querystr = "get"; // note the spelling mistake

    FuzzyQuery q = new FuzzyQuery(new Term("title", querystr), 1);

    // 3. Search
    int hitsPerPage = Integer.MAX_VALUE;
    DirectoryReader reader = DirectoryReader.open(index);
    IndexSearcher searcher = new IndexSearcher(reader);
    TopDocs docs = searcher.search(q, hitsPerPage);

    // 4. Display results
    System.out.println("Found " + docs.totalHits + " hits.");
    for(int i=0;i<docs.scoreDocs.length;++i) {
      int docId = docs.scoreDocs[i].doc;
      Document d = searcher.doc(docId);
      System.out.println((i + 1) + ". " + d.get("title"));
    }
    reader.close();







  }

  public String formatParsingPath(JsonNode root, String[] path) {

    StringBuilder formattedPath = new StringBuilder("/");
    for (String loc : path) {
      String formattedLoc = loc.replace("/", "~1"); // Formatting "/" to work with .at()
      formattedPath.append(formattedLoc);
      formattedPath.append("/");
    }
    return formattedPath.toString();
  }

/*  public static OpenAPI getOpenAPI(String openAPIStr) {
    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setResolve(true); // implicit
    parseOptions.setResolveFully(true);
    parseOptions.setResolveCombinators(false);
    return new OpenAPIV3Parser().readContents(openAPIStr, null, parseOptions).getOpenAPI();
  }*/

  public JsonNode getRefIfPresent(JsonNode currNode) throws IOException {
    ClassLoader classLoader = ParseOpenAPIsTests.class.getClassLoader();
    InputStream input = classLoader.getResourceAsStream("com/appian/guidewire/templates/claimsv2.json");
    String swaggerStr = IOUtils.toString(input, StandardCharsets.UTF_8);
    JsonNode openApi = new ObjectMapper().readValue(swaggerStr, JsonNode.class);


    // If no ref, just return currNode
    if (currNode == null || !currNode.has(REF)) return currNode;

    // Get Ref if it exists
    String newLoc = currNode.get(REF).asText().replace("#/", "/");
    JsonNode newNode = openApi.at(newLoc);
    if (newNode == null || newNode.isMissingNode()) return null;
    else return newNode;
  }

  // Parse through the OpenAPI spec starting at root node and traversing down path
  public JsonNode parse(JsonNode currNode, List<String> path) throws IOException {

    ClassLoader classLoader = ParseOpenAPIsTests.class.getClassLoader();
    InputStream input = classLoader.getResourceAsStream("com/appian/guidewire/templates/claimsv2.json");
    String swaggerStr = IOUtils.toString(input, StandardCharsets.UTF_8);
    JsonNode openApi = new ObjectMapper().readValue(swaggerStr, JsonNode.class);


    if (currNode == null || path.size() <= 0) return null;

    for (int i = 0; i < path.size(); i++) {
      String loc = path.get(i);
      currNode = currNode.get(loc);
      currNode = getRefIfPresent(currNode);
      if (currNode == null) return null;
    }

    currNode = getRefIfPresent(currNode);
    return currNode;
  }

  public List<JsonNode> getRefs(JsonNode arrOfRefStrs, JsonNode root) {
    if (arrOfRefStrs == null || arrOfRefStrs.size() == 0) return null;

    List<JsonNode> refNodeArr = new ArrayList<>();
    arrOfRefStrs.forEach(refNode -> {
      Optional.ofNullable(refNode.get("$ref"))
          .ifPresent(refStr -> {
            String refLocation = refStr.asText().replace("#/", "/");
            refNodeArr.add(root.at(refLocation));
          });
    });
    return refNodeArr;
  }

@Test
public void testingParsing() throws IOException {
  ClassLoader classLoader = ParseOpenAPIsTests.class.getClassLoader();
  InputStream input = classLoader.getResourceAsStream("com/appian/guidewire/templates/claimsv2.json");
  String swaggerStr = IOUtils.toString(input, StandardCharsets.UTF_8);

  JsonNode openApi = new ObjectMapper().readValue(swaggerStr, JsonNode.class);
  JsonNode paths = openApi.get("paths");
  String pathName = "/claims/{claimId}";
  JsonNode get = parse(paths, Arrays.asList(pathName, GET));
}

/*

  @Test
  public void swagger() throws IOException  {

    ClassLoader classLoader = ParseOpenAPIsTests.class.getClassLoader();
    InputStream input = classLoader.getResourceAsStream("com/appian/guidewire/templates/claimsv2.json");
    String swaggerStr = IOUtils.toString(input, StandardCharsets.UTF_8);

    // Getting OpenAPI obj
    long startTime = System.nanoTime();
    OpenAPI openAPI = Util.getOpenAPI(swaggerStr);
    System.out.println("Getting OpenAPI obj: " + (System.nanoTime() - startTime)/1000000 + " milliseconds. ");



    // Parsing OpenAPI obj
    startTime = System.nanoTime();
    Optional<Schema> schema = Optional.ofNullable(openAPI.getPaths().get("/claims/{claimId}/activities"))
        .map(PathItem::getPost)
        .map(Operation::getRequestBody)
        .map(RequestBody::getContent)
        .map(content -> content.get("application/json"))
        .map(MediaType::getSchema)
        .map(Schema::getProperties)
        .map(properties -> properties.get("data"))
        .map(data -> ((ObjectSchema)data).getProperties())
        .map(dataMap -> dataMap.get("attributes"));
    System.out.println("Getting props: " + (System.nanoTime() - startTime)/1000000 + " milliseconds. ");


    // Getting Jackson parser obj
    ObjectMapper mapper = new ObjectMapper();
    startTime = System.nanoTime();
    JsonNode map = mapper.readValue(swaggerStr, JsonNode.class);
    System.out.println("My own parsing, getting openapi parser: " + (System.nanoTime() - startTime)/1000000 + " milliseconds. ");

    // Parsing Jackson parser obj with Optionals
    startTime = System.nanoTime();
    Optional<JsonNode> allSchemas = Optional.ofNullable(map)
        .map(c -> c.get("components"))
        .map(s -> s.get("schema"));

    Optional<JsonNode> schema1 = Optional.ofNullable(map)
        .map(p -> p.get("paths"))
        .map(p -> p.get("/claims/{claimId}/activities"))
        .map(p -> p.get("post"))
        .map(p -> p.get("requestBody"))
        .map(p -> p.get("content"))
        .map(p -> p.get("application/json"))
        .map(p -> p.get("schema"));

    if (allSchemas.isPresent()) {
      allSchemas
          .map(p -> p.get("ActivityRequest"))
          .map(p -> p.get("properties"))
          .map(p -> p.get("data"));
    }

    if (allSchemas.isPresent()) {
      allSchemas
          .map(p -> p.get("ActivityData"))
          .map(p -> p.get("properties"))
          .map(p -> p.get("attributes"));
    }
    System.out.println("My own parsing, getting props: " + (System.nanoTime() - startTime)/1000000 + " milliseconds. ");


    // Parsing Jackson obj with .at()
    startTime = System.nanoTime();
    String pathName = "/claims/{claimId}/activities";
    List<String> path = Arrays.asList(PATHS, pathName, POST, REQUEST_BODY, CONTENT, APPLICATION_JSON, SCHEMA, PROPERTIES, DATA,
        PROPERTIES, ATTRIBUTES);
    JsonNode res = parse(map, path);
    System.out.println("My own parsing2, getting props: " + (System.nanoTime() - startTime)/1000000 + " milliseconds. ");



    // Compress the string
    startTime = System.nanoTime();
    String compressed = Util.compress(swaggerStr);
    System.out.println("Compression Time: " + (System.nanoTime() - startTime)/1000000 + " milliseconds." + "Str Length:" + compressed.length());

    // Decompress the string
    startTime = System.nanoTime();
    String decompressed = Util.decompress(compressed);
    System.out.println("Decompression Time: " + (System.nanoTime() - startTime)/1000000 + " milliseconds" + "Str Length:" + decompressed.length());
  }
*/




  @Test
  public void testPathVars() throws IOException {

    ClassLoader classLoader = ParseOpenAPIsTests.class.getClassLoader();
    InputStream input = classLoader.getResourceAsStream("com/appian/guidewire/templates/claimsv2.json");
    String swaggerStr = IOUtils.toString(input, StandardCharsets.UTF_8);

    JsonNode openAPI2 = new ObjectMapper().readValue(swaggerStr, JsonNode.class);
    JsonNode paths2 = Optional.ofNullable(openAPI2)
        .map(openapi -> openapi.get("paths"))
        .orElse(null);

    String pathName = "/claims/{claimId}/activities";
    Optional.ofNullable(parse(paths2, Arrays.asList(pathName, PARAMETERS)))
        .map(parameters -> getRefs(parameters, openAPI2))
        .ifPresent(refs -> refs.forEach(ref -> {
          System.out.println(ref.get("name"));
        }));
  }











  @Test
  public void rebuildingReqBodyParser() throws IOException {
    ClassLoader classLoader = ParseOpenAPIsTests.class.getClassLoader();
    InputStream input = classLoader.getResourceAsStream("com/appian/guidewire/templates/claimsv2.json");
    String swaggerStr = IOUtils.toString(input, StandardCharsets.UTF_8);

    JsonNode openApi = new ObjectMapper().readValue(swaggerStr, JsonNode.class);
    JsonNode paths = openApi.get("paths");
    String pathName = "/claims/{claimId}/check-sets";
    JsonNode post = parse(paths, Arrays.asList(pathName, POST));


    List<Object> properties = new ArrayList<>();
    JsonNode reqBody = parse(post, Arrays.asList(REQUEST_BODY));
    if(reqBody == null) {
      properties.add(ConstantKeys.getChecksumUI(CHECKSUM_IN_HEADER));
      properties.add(NO_REQ_BODY_UI);
      return;
    }

    JsonNode documentType = parse(reqBody, Arrays.asList(CONTENT,MULTIPART_FORM_DATA));
    if (documentType != null) {
      properties.add("doc prop");
    }

    properties.add(ConstantKeys.getChecksumUI(CHECKSUM_IN_REQ_BODY));

    JsonNode schema = (documentType == null) ?
        parse(reqBody, Arrays.asList(CONTENT, APPLICATION_JSON, SCHEMA, PROPERTIES, DATA, PROPERTIES, ATTRIBUTES)) :
        parse(post, Arrays.asList(RESPONSES, "201", CONTENT, APPLICATION_JSON, SCHEMA, PROPERTIES, DATA, PROPERTIES, ATTRIBUTES));


    if (schema == null) {
      properties.add(NO_REQ_BODY_UI);
      return;
    }

    JsonNode requiredNode = parse(schema, Arrays.asList(REQUIRED));

    schema.get(PROPERTIES).fields().forEachRemaining(entry -> {
      System.out.println(entry.getKey() + ": " + entry.getValue() );
    });

/*    ReqBodyUIBuilder2(schema.get(PROPERTIES), requiredNode, POST);*/



  }














}
