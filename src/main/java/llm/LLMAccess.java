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

    public ChatLanguageModel geminiModel;
    public ChatLanguageModel mistralModel;
    public ChatLanguageModel openaiModel;

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
                geminiModel = VertexAiGeminiChatModel.builder()
                        .project(geminiProject)
                        .location(geminiLocation)
                        //      .temperature(1.0f)  // between 0 and 2; default 1.0 for pro-1.5
                        //       .topK(40) // some models have a three-stage sampling process. topK; then topP; then temperature
                        .topP(0.94f)  // 1.5 default is 0.64; this the sum of probability of tokens to sample from
                        //     .maxOutputTokens(1000)  // max replay size (max is 8192)
                        .modelName("gemini-1.5-pro")
                        .build();
            } catch (Error e) {
                System.out.println("Error creating Gemini model: " + e.getMessage());
            }
        }

        if (mistralToken != null && !mistralToken.isEmpty()) {
            mistralModel = MistralAiChatModel.builder()
                    .modelName(MistralAiChatModelName.MISTRAL_MEDIUM_LATEST)
                    .apiKey(mistralToken)
                    .build();
        }

        if (openaiToken != null && !openaiToken.isEmpty()) {
            openaiModel = OpenAiChatModel.builder()
                    .modelName(OpenAiChatModelName.GPT_4_O_MINI)
                    .apiKey(openaiToken)
                    .build();
        }
    }
}
