import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.ollama.OllamaChatModel;

public class TestModel {
    public static void main(String[] args) {
        OllamaChatModel model = OllamaChatModel.builder().baseUrl("http://localhost:11434").modelName("llama3:8b-instruct-q4_K_M").build();
        System.out.println(model.generate(java.util.Collections.singletonList(dev.langchain4j.data.message.UserMessage.from("hello"))).content().text());
    }
}
