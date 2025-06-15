package rabbitmq.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rabbitmq.config.RabbitMQConfig;

import java.io.FileReader;
import java.io.IOException;

public class MQConfigHelper {
    private static MQConfigHelper instance;
    private com.rabbitmq.config.RabbitMQConfig config;

    private MQConfigHelper() {
        loadConfig();
    }

    public static synchronized MQConfigHelper getInstance() {
        if (instance == null) {
            instance = new MQConfigHelper();
        }
        return instance;
    }

    private void loadConfig() {
        try (FileReader reader = new FileReader("Util/Java/RabbitMQ/RabbitMQ/config.json")) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject rabbitmqConfig = jsonObject.getAsJsonObject("rabbitmq");
            
            Gson gson = new Gson();
            config = gson.fromJson(rabbitmqConfig, com.rabbitmq.config.RabbitMQConfig.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load RabbitMQ configuration", e);
        }
    }

    public RabbitMQConfig getConfig() {
        return config;
    }
} 