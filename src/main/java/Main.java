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
                        "file_path", Map.of(
                                "type", "string",
                                "description", "The path of the file to read"))))
                .putAdditionalProperty("required", JsonValue.from(List.of("file_path")))
                .build();

        FunctionDefinition readFileFunction = FunctionDefinition.builder()
                .name("read_file")
                .description("Read the contents of a file at the given path.")
                .parameters(readFileParams)
                .build();

        ChatCompletionTool readFileTool = ChatCompletionTool.builder()
                .function(readFileFunction)
                .build();

        FunctionParameters writeFileParams = FunctionParameters.builder()
                .putAdditionalProperty("type", JsonValue.from("object"))
                .putAdditionalProperty("properties", JsonValue.from(Map.of(
                        "file_path", Map.of(
                                "type", "string",
                                "description", "The path of the file to write to"),
                        "content", Map.of(
                                "type", "string",
                                "description", "The content to write to the file"))))
                .putAdditionalProperty("required", JsonValue.from(List.of("file_path", "content")))
                .build();

        FunctionDefinition writeFileFunction = FunctionDefinition.builder()
                .name("write_file")
                .description("Write content to a file")
                .parameters(writeFileParams)
                .build();

        ChatCompletionTool writeFileTool = ChatCompletionTool.builder()
                .function(writeFileFunction)
                .build();

        ChatCompletionCreateParams.Builder builder = ChatCompletionCreateParams.builder()
                .model("anthropic/claude-haiku-4.5")
                .addUserMessage(prompt)
                .addTool(readFileTool)
                .addTool(writeFileTool);

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
                String toolName = toolCall.function().name();
                String toolResult;
                try {
                    JsonNode argsNode = mapper.readTree(toolCall.function().arguments());
                    String filePath = getFilePath(argsNode);

                    if ("read_file".equalsIgnoreCase(toolName) || "read".equalsIgnoreCase(toolName)) {
                        toolResult = Files.readString(Path.of(filePath));
                    } else if ("write_file".equalsIgnoreCase(toolName) || "write".equalsIgnoreCase(toolName)) {
                        String fileContent = argsNode.has("content") ? argsNode.get("content").asText() : "";
                        Path path = Path.of(filePath);
                        if (path.getParent() != null) {
                            Files.createDirectories(path.getParent());
                        }
                        Files.writeString(path, fileContent);
                        toolResult = "File written successfully";
                    } else {
                        toolResult = "Unknown tool: " + toolName;
                    }
                } catch (Exception e) {
                    toolResult = "Error executing tool " + toolName + ": " + e.getMessage();
                }

                // Append tool result message to conversation history
                builder.addMessage(ChatCompletionToolMessageParam.builder()
                        .toolCallId(toolCall.id())
                        .content(toolResult)
                        .build());
            }
        }
    }

    private static String getFilePath(JsonNode argsNode) {
        if (argsNode.has("file_path")) {
            return argsNode.get("file_path").asText();
        } else if (argsNode.has("path")) {
            return argsNode.get("path").asText();
        } else {
            return argsNode.asText();
        }
    }
}
