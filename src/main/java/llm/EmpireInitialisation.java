package llm;

import db.MySQLLink;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import traveller.Empire;
import traveller.World;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class EmpireInitialisation {

    // the aim here is to run through the list of worlds and create names and descriptions for empires

    public static String systemPrompt = """
            You are the game-master for a Traveller role-playing campaign set in the Far Future.
            You are given the name of a planet, and some keywords about its society.
            You must invent an appropriate name for the empire (or other political body based on that planet).
            Examples of Empire names for a planet called XXX are:
            Domain of XXX
            Saeculoris Confederation
            Empire of the Seven Stars
            Grand Chancellery of XXX
            Republic of XXX
            XXX Consulate
            
            Your job is also to write a synopsis in 40 words or fewer about the Empire, summarising what is known to the players, and how it interacts with the wider galaxy.
            This should have be flavourful and make use of the known cultural traits provided.
            Your synopsis should where possible avoid using the keywords in the cultural traits directly.
            You do not need to use all the keywords, but you should use some of them. The priority is to make the description distinctive, interesting and engaging.
            Avoid using excessive adjectives. Make each one count.
            
            Here are three examples:
            Planet: Smade's World
            Culturally this society is Progressive, Enterprising, Competitive, Chaotic, Neutral, Homogeneous
            
            Name: Smade's Cladistic Exogeny
            Description: All the population are members of the Smade family, and the different lineal branches compete for political and economic office. Policy implementation is not a strong point, despite lofty goals.
            
            
            Planet: Prometheus
            Culturally this society is Reactionary, Stagnant, Stable, Organised, Peacable, Fragmented
            
            Name: The Aurelian Dynastic Accord
            Description: The planet’s noble houses cling to ancient traditions and guard their entrenched privileges fiercely. Though politically fractured, they maintain a rigid, unchanging order, with treaties and pacts binding each faction into a delicate equilibrium that has endured for centuries.
            
            
            Planet: Dingiir
            Culturally this society is Progressive, Advancing, Competitive, Strategic, Militant, Neutral, and Harmonious
            
            Name: Saeculoris Ascendancy
            Description: Ruled by meritocrats who ascend through a crucible of rigorous trials, the Saeculoris Ascendancy is a formidable power.
            Their stellar fleets are masters of calculated, decisive warfare waged beneath a banner of unwavering unity.
            
            
            Planet: Muan Ialour
            Culturally this society is Conservative, Stagnant, Expansionist, Strategic, Neutral, Neutral, and Monolithic
            
            Name: Muan Ialour Protectorates
            Description: Ruled from the obsidian throne of Muan Ialour, the Protectorates expand slowly outward, incorporating new worlds with patient, deliberate precision, like the growth of a crystal.
            """;

    record EmpireDescription(String name, String description) {
    }

    interface EmpireNameGenerator {
        EmpireDescription generateName(String userMessage);
    }

    public static void main(String[] args) {

        // we get all worlds and loop over the ones with non-zero population
        MySQLLink dbLink = new MySQLLink();
        var connection = dbLink.getConnection();
        List<World> worlds = World.getAllWorlds();
        LLMAccess llm = new LLMAccess();

        // we really need to create an AIService for each of these
        EmpireNameGenerator openAISmall = new EmpireInitialisation().getNamer(llm.openaiSmallModel);
        EmpireNameGenerator openAILarge = new EmpireInitialisation().getNamer(llm.openaiLargeModel);
        EmpireNameGenerator geminiSmall = new EmpireInitialisation().getNamer(llm.geminiSmallModel);
        EmpireNameGenerator geminiLarge = new EmpireInitialisation().getNamer(llm.geminiLargeModel);
        EmpireNameGenerator mistralSmall = new EmpireInitialisation().getNamer(llm.mistralSmallModel);
        EmpireNameGenerator mistralLarge = new EmpireInitialisation().getNamer(llm.mistralLargeModel);

        Map<Integer, EmpireNameGenerator> llmNamers = Map.of(
                0, openAISmall,
                1, openAILarge,
                2, geminiSmall,
                3, geminiLarge,
                4, mistralSmall,
                5, mistralLarge
        );
        Map<Integer, String> llmNames = Map.of(
                0, "OpenAI Small",
                1, "OpenAI Large",
                2, "Gemini Small",
                3, "Gemini Large",
                4, "Mistral Small",
                5, "Mistral Large"
        );

        int count = 0;
        for (World world : worlds) {
            if (world.getPopExponent() > 0) {

                String userMessage = "Planet: " + world.getName() + "\n" +
                        "Culturally this world is " + world.keywordDescription();

                Empire empire = new Empire(world.getEmpire());
                if (!empire.getName().equals(world.getName()) && empire.getDescription().length() > 70) {
                    System.out.println("Empire already named: " + empire.getName());
                    continue;
                }
                System.out.println(userMessage);
                EmpireDescription empireDescription;

                // we rotate through all models only if we are testing their relative performance
                EmpireNameGenerator namer = llmNamers.get(count % 6);
                String llmName = llmNames.get(count % 6);

                namer = geminiSmall;
                llmName = "Gemini Small";
                try {
                    empireDescription = namer.generateName(userMessage);
                } catch (Exception e) {
                    System.out.println("Error generating name: " + e.getMessage());
                    empireDescription = new EmpireDescription(world.getName(), "");
                }
                String query = "";
                System.out.println(empireDescription);
                try {
                    // It is possible that empireDescription.name or empireDescription.description contains a single quote
                    // which we need to escape
                    String escapedName = empireDescription.name.replace("'", "''");
                    String escapedDescription = empireDescription.description.replace("'", "''");
                    query = "UPDATE empires SET Name = '" + escapedName + "', Description = '" + escapedDescription +
                            "', LLM = '" + llmName + "', CulturalTraits = '" + world.keywordDescription() +
                            "' WHERE id = " + world.getEmpire();
                    connection.createStatement().executeUpdate(query);
                } catch (SQLException e) {
                    System.out.println("Failed to write: " + query);
                    throw new RuntimeException(e);
                }
                count++;
//                if (count % 6 == 0) {
//                    // we now pause for one second ... we have 60 RPM for Gemini Pro...so that will ensure safety
//                    try {
//                        Thread.sleep(1000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
                //        if (count >= 12)
                //            return; // for testing
            }
        }
    }

    private EmpireNameGenerator getNamer(ChatLanguageModel model) {
        return AiServices.builder(EmpireNameGenerator.class)
                .chatLanguageModel(model)
                .systemMessageProvider(_ -> systemPrompt)
                .build();
    }
}
