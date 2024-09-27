package llm;

import db.MySQLLink;
import dev.langchain4j.service.AiServices;
import traveller.Empire;
import traveller.World;

import java.sql.SQLException;
import java.util.List;

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
            
            Your job is also to write a 30 word synopsis about the Empire, summarising what is known to the players. 
            This should have be flavourful and make use of the known cultural traits.\s
            Your synopsis should where possible avoid using the keywords in the cultural traits directly.
            
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
            Culturally this society is Progressive, Enterprising, Expansionist, Chaotic, Belligerent, Harmonious
            
            Name: Dingiir Hegemonic Union
            Description: A volatile alliance of ambitious power blocs, constantly pushing outward and competing for supremacy. Despite their internal rivalries, they present a unified front to outsiders, driven by a shared vision of dominance and relentless progress.
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
        EmpireNameGenerator openAINamer = AiServices.builder(EmpireNameGenerator.class)
                .chatLanguageModel(llm.openaiModel)
                .systemMessageProvider(_ -> systemPrompt)
                .build();
        EmpireNameGenerator geminiNamer = AiServices.builder(EmpireNameGenerator.class)
                .chatLanguageModel(llm.geminiModel)
                .systemMessageProvider(_ -> systemPrompt)
                .build();
        EmpireNameGenerator mistralNamer = AiServices.builder(EmpireNameGenerator.class)
                .chatLanguageModel(llm.mistralModel)
                .systemMessageProvider(_ -> systemPrompt)
                .build();


        int count = 0;
        for (World world : worlds) {
            if (world.getPopExponent() > 0) {
                String userMessage = "Planet: " + world.getName() + "\n" +
                        world.keywordDescription();

                Empire empire = new Empire(world.getEmpire());
                if (!empire.getName().equals(world.getName())) {
                    System.out.println("Empire already named: " + empire.getName());
                    continue;
                }
                System.out.println(userMessage);
                EmpireDescription empireDescription;
                try {
                    if (count % 3 == 0)
                        empireDescription = openAINamer.generateName(userMessage);
                    else if (count % 3 == 1)
                        empireDescription = mistralNamer.generateName(userMessage);
                    else
                        empireDescription = geminiNamer.generateName(userMessage);
                } catch (Exception e) {
                    System.out.println("Error generating name: " + e.getMessage());
                    empireDescription = new EmpireDescription("Domain of " + world.getName(), "A powerful empire that controls the planet " + world.getName());
                }
                String query = "";
                System.out.println(empireDescription);
                try {
                    // It is possible that empireDescription.name or empireDescription.description contains a single quote
                    // which we need to escape
                    String escapedName = empireDescription.name.replace("'", "''");
                    String escapedDescription = empireDescription.description.replace("'", "''");
                    query = "UPDATE empires SET Name = '" + escapedName + "', Description = '" + escapedDescription + "' WHERE id = " + world.getEmpire();
                    connection.createStatement().executeUpdate(query);
                } catch (SQLException e) {
                    System.out.println("Failed to write: " + query);
                    throw new RuntimeException(e);
                }
                count++;
                if (count > 5) break;
            }
        }
    }
}
