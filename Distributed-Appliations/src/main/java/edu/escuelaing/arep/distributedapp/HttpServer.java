package edu.escuelaing.arep.distributedapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonObject;

/**
 * Class responsible for the creation a socket server that receives requests from
 * a client and returns a response with the information of a movie.
 * @author Angie Mojica
 * @author Daniel Benavides
 */
public class HttpServer {

    private static ConcurrentHashMap<String, JsonObject> cache = new ConcurrentHashMap<>();
    private static HttpClient httpClient = new HttpClient();

    /**
     * Main method that creates a socket server and initializes the connection with
     * the client.
     * @param args Arguments of the main method.
     * @throws IOException If an input or output exception occurred.
     */
    public static void main(String[] args) throws IOException {

        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(35000);
        } catch (IOException e) {
            System.err.println("Could not listen on port: 35000.");
            System.exit(1);
        }
        boolean running = true;
        while (running) {
            Socket clientSocket = null;
            try {
                System.out.println("Listo para recibir ...");
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                System.err.println("Accept failed.");
                System.exit(1);
            }

            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String inputLine, outputLine;
            boolean firstLine = true;
            String uriStr = "";

            while ((inputLine = in.readLine()) != null) {
                if (firstLine) {
                    uriStr = inputLine.split(" ")[1];
                    firstLine = false;
                }
                System.out.println("Received: " + inputLine);
                if (!in.ready()) {
                    break;
                }
            }

            if (uriStr.startsWith("/movie")) {
                outputLine = getInfoMovie(uriStr);
            } else {
                outputLine = getHtml(uriStr);
            }

            out.println(outputLine);

            out.close();
            in.close();
            clientSocket.close();
        }
        serverSocket.close();
    }

    /**
     * Method that returns the information of a movie from the cache or from the
     * API.
     * @param uriStr URI of the request.
     * @return JsonObject with the information of the movie.
     */
    public static JsonObject getDataMovie(String uriStr) {
        String nameMovie = uriStr.split("=")[1];
        nameMovie = nameMovie.replace("%20", " ");
        JsonObject jsonResponse = null;
        if (cache.containsKey(nameMovie)) {
            jsonResponse = cache.get(nameMovie);
        } else {
            try {
                jsonResponse = httpClient.getMovieData(uriStr);
                cache.put(nameMovie, jsonResponse);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return jsonResponse;
    }

    /**
     * Method that returns the information of a movie in HTML format.
     * @param uriStr URI of the request.
     * @return String with the information of the movie in HTML format.
     */
    public static String getInfoMovie(String uriStr) {
        JsonObject jsonResponse = getDataMovie(uriStr);
        if (jsonResponse == null || !jsonResponse.get("Response").getAsBoolean()) {
            return "HTTP/1.1 404 Not Found\r\n" +
                    "Content-Type: text/html\r\n" +
                    "\r\n" +
                    "<h1>Movie not found</h1>";
        } else {
            return "HTTP/1.1 200 OK \r\n"
                    + "Content-Type: text/html\n\r" +
                    "\n\r" +
                    "        <div style=\"margin-top: 20px;\">\r\n" +
                    "            <h2 style=\"color:#333;\">" + jsonResponse.get("Title").getAsString() + "</h2>\r\n" +
                    "            <img src=\"" + jsonResponse.get("Poster").getAsString()
                    + "\" alt=\"Poster\" style=\"width: 200px;\">\r\n" +
                    "            <p style=\"color:#333;\">Year: " + jsonResponse.get("Year").getAsString() + "</p>\r\n"
                    +
                    "            <p style=\"color:#333;\">Rated: " + jsonResponse.get("Rated").getAsString()
                    + "</p>\r\n" +
                    "            <p style=\"color:#333;\">Released: " + jsonResponse.get("Released").getAsString()
                    + "</p>\r\n" +
                    "            <p style=\"color:#333;\">Runtime: " + jsonResponse.get("Runtime").getAsString()
                    + "</p>\r\n" +
                    "            <p style=\"color:#333;\">Genre: " + jsonResponse.get("Genre").getAsString()
                    + "</p>\r\n" +
                    "            <p style=\"color:#333;\">Director: " + jsonResponse.get("Director").getAsString()
                    + "</p>\r\n" +
                    "       </div>\r\n";
        }
    }

    /**
     * Method that returns the HTML code of the main page.
     * @param uriStr URI of the request.
     * @return String with the HTML code of the main page.
     */
    public static String getHtml(String uriStr) {
        String outputLine = "HTTP/1.1 200 OK \r\n" +
                "Content-Type: text/html\n\r"
                + "\n\r"
                + "<!DOCTYPE html>\r\n" +
                "<html>\r\n" +
                "    <head>\r\n" +
                "        <title>Search Movie</title>\r\n" +
                "        <meta charset=\"UTF-8\">\r\n" +
                "        <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\r\n" +
                "    </head>\r\n" +
                "    <body style=\"font-family: Arial, sans-serif; text-align: center; padding: 20px;\">\r\n" +
                "        <h1 style=\"color:#333;\">Search Movie</h1>\r\n" +
                "        <form action=\"#\" method=\"post\" style=\"margin-top: 20px;\">\r\n" +
                "            <label for=\"textInfo\" style=\"display: block; margin-bottom: 10px;\">Enter the name of the movie:</label>\r\n"
                +
                "            <input type=\"text\" id=\"nameMovie\" name=\"campoTexto\" style=\"padding: 8px; width: 200px;\">\r\n"
                +
                "            <br>\r\n" +
                "            <input type=\"button\" value=\"Search\" onclick=\"loadGetMsg()\" style=\"background-color: #007BFF; color: #fff; padding: 10px 20px; border: none; cursor: pointer; margin-top: 10px;\">\r\n"
                +
                "        </form>\r\n" +
                "        <div id=\"getrespmsg\" style=\"margin-top: 20px;\"> </div>\r\n" +
                "        <script>\n" +
                "            function loadGetMsg() {\n" +
                "                let nameMovie = document.getElementById(\"nameMovie\").value;\r\n" +
                "                const xhttp = new XMLHttpRequest();\n" +
                "                xhttp.onload = function() {\n" +
                "                    document.getElementById(\"getrespmsg\").innerHTML =\n" +
                "                    this.responseText;\n" +
                "                }\r\n" +
                "                xhttp.open(\"GET\", \"/movie?name=\" + nameMovie);\r\n" +
                "                xhttp.send();\r\n" +
                "            }\r\n" +
                "        </script>\r\n" +
                "    </body>\r\n" +
                "</html>";
        return outputLine;
    }

}
