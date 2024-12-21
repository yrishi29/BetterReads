package org.example.betterreadsdataloader.service;

import jakarta.annotation.PostConstruct;
import org.example.betterreadsdataloader.model.Author;
import org.example.betterreadsdataloader.model.Book;
import org.example.betterreadsdataloader.repository.AuthorRepository;
import org.example.betterreadsdataloader.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.data.annotation.Id;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SaveToDb {

    @Value("${datadump.location.author}")
    private String authorDumpLocation;
    @Value("${datadump.location.works}")
    private String worksDumpLocation;


    @Autowired
    AuthorRepository authorRepository;

    @Autowired
    BookRepository bookRepository;

    @PostConstruct
    public void start(){
//        initAuthor();
            //initWorks();
    }



    private void initAuthor() {
        Path path = Paths.get(authorDumpLocation);



        try (Stream<String> lines = Files.lines(path)) {
            lines.forEach(line -> {
                try {
                    // Extract JSON object from the line
                    int jsonStartIndex = line.indexOf("{");
                    if (jsonStartIndex == -1) {
                        System.err.println("No JSON object found in line: " + line);
                        return; // Skip this line
                    }

                    String jsonObj = line.substring(jsonStartIndex);
                    // Parse the JSON object
                    JSONObject jsonObject = new JSONObject(jsonObj);
                    Author author = new Author();
                    author.setName(jsonObject.getString("name"));
                    author.setPersonalName(jsonObject.getString("name"));
                    author.setId(jsonObject.getString("key").replace("/authors/", ""));
                    authorRepository.save(author);

                } catch (JSONException e) {
                    System.err.println("Error parsing JSON in line: " + line);
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            System.err.println("Error reading file at location: " + authorDumpLocation);
            e.printStackTrace();
        }
    }






    private void initWorks() {
        Path path = Paths.get(worksDumpLocation);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

        try (Stream<String> lines = Files.lines(path)) {
            lines.forEach(line -> {
                String jsonString = extractJsonFromLine(line);
                if (jsonString == null) {
                    System.err.println("No JSON object found in line: " + line);
                    return; // Skip this line
                }

                try {
                    JSONObject jsonObject = new JSONObject(jsonString);
                    Book book = new Book();

                    // Set book ID
                    book.setId(jsonObject.getString("key").replace("/works/", ""));

                    // Set title
                    book.setName(jsonObject.optString("title", "Unknown Title"));

                    // Set description
                    JSONObject descriptionObj = jsonObject.optJSONObject("description");
                    if (descriptionObj != null) {
                        book.setDescription(descriptionObj.optString("value", ""));
                    }

                    // Set publish date
                    JSONObject createdObj = jsonObject.optJSONObject("created");
                    if (createdObj != null) {
                        String dateStr = createdObj.optString("value", "");
                        if (!dateStr.isEmpty()) {
                            book.setPublishedDate(LocalDate.parse(dateStr, dateTimeFormatter));
                        }
                    }

                    // Set cover IDs
                    JSONArray coversArray = jsonObject.optJSONArray("covers");
                    if (coversArray != null) {
                        List<String> coverIds = new ArrayList<>();
                        for (int i = 0; i < coversArray.length(); i++) {
                            coverIds.add(coversArray.getString(i));
                        }
                        book.setCoverIds(coverIds);
                    }

                    // Set author IDs and names
                    JSONArray authorJsonArray = jsonObject.optJSONArray("authors");
                    if (authorJsonArray != null) {
                        List<String> authorIds = new ArrayList<>();
                        List<String> authorNames = new ArrayList<>();

                        for (int i = 0; i < authorJsonArray.length(); i++) {
                            String authorId = authorJsonArray.getJSONObject(i)
                                    .getJSONObject("author")
                                    .getString("key")
                                    .replace("/authors/", "");
                            authorIds.add(authorId);

                            // Fetch author name from the repository
                            String authorName = authorRepository.findById(authorId)
                                    .map(author -> author.getName())
                                    .orElse("Unknown Author");
                            authorNames.add(authorName);
                        }

                        book.setAuthorIds(authorIds);
                        book.setAuthorNames(authorNames);
                    }

                    // Optionally save the book to your database
                    bookRepository.save(book);

                } catch (JSONException e) {
                    System.err.println("Error parsing JSON in line: " + line);
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            System.err.println("Error reading file at location: " + worksDumpLocation);
            e.printStackTrace();
        }
    }

    private String extractJsonFromLine(String line) {
        int jsonStartIndex = line.indexOf("{");
        return jsonStartIndex != -1 ? line.substring(jsonStartIndex) : null;
    }




}
