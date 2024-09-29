package llm;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.mistralai.MistralAiChatModelName;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.vertexai.VertexAiGeminiChatModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;

import java.io.File;
import java.io.FileWriter;

public class LLMAccess {

    public ChatLanguageModel geminiLargeModel, geminiSmallModel;
    public ChatLanguageModel mistralLargeModel, mistralSmallModel;
    public ChatLanguageModel openaiLargeModel, openaiSmallModel;

    String mistralToken = System.getenv("MISTRAL_TOKEN");
    String geminiProject = System.getenv("GEMINI_PROJECT");
    String openaiToken = System.getenv("OPENAI_TOKEN");

    String geminiLocation = "europe-west2";

    LLM_MODEL modelType;

    public enum LLM_MODEL {
        GEMINI,
        MISTRAL,
        OPENAI
    }

    public LLMAccess() {
        if (geminiProject != null && !geminiProject.isEmpty()) {
            try {
                geminiLargeModel = VertexAiGeminiChatModel.builder()
                        .project(geminiProject)
                        .location(geminiLocation)
                        .temperature(1.5f)  // between 0 and 2; default 1.0 for pro-1.5
                        //       .topK(40) // some models have a three-stage sampling process. topK; then topP; then temperature
                        .topP(1.0f)  // 1.5 default is 0.64; this the sum of probability of tokens to sample from
                        //     .maxOutputTokens(1000)  // max replay size (max is 8192)
                        .modelName("gemini-1.5-pro")
                        .build();
                // $3.75 per million characters output, $1.25 per million characters input (about 4-5 characters per token)
                // (from Oct 7)  $1.25 / $0.3125

                geminiSmallModel = VertexAiGeminiChatModel.builder()
                        .project(geminiProject)
                        .location(geminiLocation)
                        .temperature(1.5f)  // between 0 and 2; default 1.0 for pro-1.5
                        //       .topK(40) // some models have a three-stage sampling process. topK; then topP; then temperature
                        .topP(1.0f)  // 1.5 default is 0.64; this the sum of probability of tokens to sample from
                        //     .maxOutputTokens(1000)  // max replay size (max is 8192)
                        .modelName("gemini-1.5-flash")
                        .build();
                // $0.075 per million characters output, $0.01875 per million characters input

            } catch (Error e) {
                System.out.println("Error creating Gemini model: " + e.getMessage());
            }
        }

        if (mistralToken != null && !mistralToken.isEmpty()) {
            mistralLargeModel = MistralAiChatModel.builder()
                    .modelName(MistralAiChatModelName.MISTRAL_LARGE_LATEST)
                    .temperature(1.0) // default 0.7 in range 0.0 to 1.0
                    .topP(1.0)  // this is the default
                    .apiKey(mistralToken)
                    .build();
            // $2 per million input tokens, $6 per million output tokens

            mistralSmallModel = MistralAiChatModel.builder()
                    .modelName(MistralAiChatModelName.MISTRAL_SMALL_LATEST)
                    .temperature(1.0) // default 0.7 in range 0.0 to 1.0
                    .topP(1.0)  // this is the default
                    .apiKey(mistralToken)
                    .build();
            // $0.2 per million input tokens, $0.6 per million output tokens

            // Mistral NeMo not yet on Langchain4J...and this is cheaper.
        }

        if (openaiToken != null && !openaiToken.isEmpty()) {
            openaiSmallModel = OpenAiChatModel.builder()
                    .modelName(OpenAiChatModelName.GPT_4_O_MINI)
                    .temperature(1.0) // between 0 and 2; default is 1.0
                    .topP(1.0)  // this is the default
                    .apiKey(openaiToken)
                    .build();
            // $0.15 per million input tokens, $0.6 per million output tokens

            openaiLargeModel = OpenAiChatModel.builder()
                    .modelName(OpenAiChatModelName.GPT_4_O)
                    .temperature(1.0) // between 0 and 2; default is 1.0
                    .topP(1.0)  // this is the default
                    .apiKey(openaiToken)
                    .build();
            // $5 per million input tokens, $15 per million output tokens
        }
    }
}
