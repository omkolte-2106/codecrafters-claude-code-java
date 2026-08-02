import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessage;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;
import com.openai.models.chat.completions.ChatCompletionTool;
import com.openai.models.chat.completions.ChatCompletionToolMessageParam;

public class Main {
    public static void main(String[] args) {
        if (args.length < 2 || !"-p".equals(args[0])) {
            System.err.println("Usage: program -p <prompt>");
            System.exit(1);
        }

        String prompt = args[1];

        String apiKey = System.getenv("OPENROUTER_API_KEY");
        String baseUrl = System.getenv("OPENROUTER_BASE_URL");
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "https://openrouter.ai/api/v1";
        }

        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("OPENROUTER_API_KEY is not set");
        }

        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();

        FunctionParameters readFileParams = FunctionParameters.builder()
                .putAdditionalProperty("type", JsonValue.from("object"))
                .putAdditionalProperty("properties", JsonValue.from(Map.of(
                        "path", Map.of(
                                "type", "string",
                                "description", "The relative or absolute path to the file to read"))))
                .putAdditionalProperty("required", JsonValue.from(List.of("path")))
                .build();

        FunctionDefinition readFileFunction = FunctionDefinition.builder()
                .name("read_file")
                .description("Read the contents of a file at the given path.")
                .parameters(readFileParams)
                .build();

        ChatCompletionTool readFileTool = ChatCompletionTool.builder()
                .function(readFileFunction)
                .build();

        ChatCompletionCreateParams.Builder builder = ChatCompletionCreateParams.builder()
                .model("anthropic/claude-haiku-4.5")
                .addUserMessage(prompt)
                .addTool(readFileTool);

        ObjectMapper mapper = new ObjectMapper();

        while (true) {
            ChatCompletion response = client.chat().completions().create(builder.build());

            if (response.choices().isEmpty()) {
                throw new RuntimeException("no choices in response");
            }

            ChatCompletion.Choice choice = response.choices().get(0);
            ChatCompletionMessage message = choice.message();

            // Record the assistant response in conversation history
            builder.addMessage(message);

            // Exit loop if no tool calls requested
            if (message.toolCalls().isEmpty() || message.toolCalls().get().isEmpty()) {
                System.out.print(message.content().orElse(""));
                break;
            }

            // Execute each requested tool call
            for (ChatCompletionMessageToolCall toolCall : message.toolCalls().get()) {
                String fileContent;
                try {
                    JsonNode argsNode = mapper.readTree(toolCall.function().arguments());
                    String filePath;
                    if (argsNode.has("path")) {
                        filePath = argsNode.get("path").asText();
                    } else if (argsNode.has("file_path")) {
                        filePath = argsNode.get("file_path").asText();
                    } else {
                        filePath = argsNode.asText();
                    }

                    fileContent = Files.readString(Path.of(filePath));
                } catch (Exception e) {
                    fileContent = "Error reading file: " + e.getMessage();
                }

                // Append tool result message to conversation history
                builder.addMessage(ChatCompletionToolMessageParam.builder()
                        .toolCallId(toolCall.id())
                        .content(fileContent)
                        .build());
            }
        }
    }
}
