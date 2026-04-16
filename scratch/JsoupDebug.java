import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class JsoupDebug {
    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://prsindia.org/policy/monthly-policy-review"))
                .header("User-Agent", "Mozilla/5.0")
                .GET().build();
        String html = client.send(req, HttpResponse.BodyHandlers.ofString()).body();
        Document page = Jsoup.parse(html, "https://prsindia.org/policy/monthly-policy-review");
        
        System.out.println("Total links: " + page.select("a").size());
        for (Element link : page.select("a")) {
            String href = link.attr("href");
            if (href.toLowerCase().contains(".pdf")) {
                System.out.println("PDF found: " + link.text() + " -> " + link.absUrl("href"));
            }
        }
    }
}
