package _3_advanced;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;

import javax.sql.DataSource;

import org.mariadb.jdbc.MariaDbDataSource;

import _2_naive.Naive_RAG_Example;
import static dev.langchain4j.internal.Utils.getOrDefault;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.localai.LocalAiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import shared.Assistant;
import static shared.Utils.startConversationWith;
import static shared.Utils.toPath;

public class _10_Advanced_RAG_SQL_Database_Retreiver_Example {
    static String GEMINI_API_KEY = getOrDefault(System.getenv("GEMINI_API_KEY"), "demoKey");
    static String DB_URL = getOrDefault(System.getenv("DB_URL"), "jdbc:mariadb://localhost:3306/openmrs");
    static String DB_USER = getOrDefault(System.getenv("DB_USER"), "root");
    static String DB_PASSWORD = getOrDefault(System.getenv("DB_PASSWORD"), "password");
    static String MODEL_NAME = "llama3.2";
    static String BASE_URL = "http://localhost:11435";
  

    /**
     * Please refer to {@link Naive_RAG_Example} for a basic context.
     * <p>
     * Advanced RAG in LangChain4j is described here: https://github.com/langchain4j/langchain4j/pull/538
     * <p>
     * This example demonstrates how to use SQL database content retriever.
     * <p>
     * WARNING! Although fun and exciting, {@link SqlDatabaseContentRetriever} is dangerous to use!
     * Do not ever use it in production! The database user must have very limited READ-ONLY permissions!
     * Although the generated SQL is somewhat validated (to ensure that the SQL is a SELECT statement),
     * there is no guarantee that it is harmless. Use it at your own risk!
     * <p>
     * In this example we will use an in-memory H2 database with 3 tables: customers, products and orders.
     * See "resources/sql" directory for more details.
     * <p>
     * This example requires "langchain4j-experimental-sql" dependency.
     */

    public static void main(String[] args) {

        Assistant assistant = createAssistant();

        // You can ask questions such as "How many customers do we have?" and "What is our top seller?".
        startConversationWith(assistant);
    }

    private static Assistant createAssistant() {

        DataSource dataSource = createDataSource();
        ChatLanguageModel chatLanguageModel2 = GoogleAiGeminiChatModel.builder()
                .apiKey(GEMINI_API_KEY)
                .modelName("gemini-2.0-flash")
                .logRequestsAndResponses(true)
                .build();

        ChatLanguageModel chatLanguageModel = OllamaChatModel.builder()
              .baseUrl(BASE_URL)
              .modelName(MODEL_NAME)
              .logRequests(true)
              .logResponses(true)
              .timeout(Duration.ofMinutes(5))  // Set 5-minute timeout
              .build();

        ChatLanguageModel chatLanguageModel3 = LocalAiChatModel.builder()
            .baseUrl("http://localhost:8080/v1")
            .modelName("gpt-4")
            //.maxTokens(3)
            .logRequests(true)
            .logResponses(true)
            .temperature(0.0)
            .timeout(Duration.ofMinutes(5))  // Set 5-minute timeout
            .build();
        ContentRetriever contentRetriever = SqlDatabaseContentRetriever.builder()
                .dataSource(dataSource)
                .sqlDialect("MySQL")
                .chatLanguageModel(chatLanguageModel)
                .build();
        return AiServices.builder(Assistant.class)
                .chatLanguageModel(chatLanguageModel)
                .contentRetriever(contentRetriever)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    private static DataSource createDataSource() {

        DataSource dataSource = setupDataSource();

        return dataSource;
    }

    private static DataSource setupDataSource() {
        MariaDbDataSource dataSource = new MariaDbDataSource();

        try {
            dataSource.setUrl(DB_URL);
            dataSource.setUser(DB_USER);
            dataSource.setPassword(DB_PASSWORD);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return dataSource;
    }

    private static String read(String path) {
        try {
            return new String(Files.readAllBytes(toPath(path)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void execute(String sql, DataSource dataSource) {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            for (String sqlStatement : sql.split(";")) {
                statement.execute(sqlStatement.trim());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}