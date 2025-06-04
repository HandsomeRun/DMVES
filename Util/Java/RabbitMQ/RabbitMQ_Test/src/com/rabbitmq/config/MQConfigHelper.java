package com.rabbitmq.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

public class MQConfigHelper {
    private static final String CONFIG_FILE = "src/config.json";
    private static MQConfigHelper instance;
    private final JsonObject config;

    private MQConfigHelper() throws IOException {
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            this.config = new Gson().fromJson(reader, JsonObject.class);
        }
    }

    public static synchronized MQConfigHelper getInstance() throws IOException {
        if (instance == null) {
            instance = new MQConfigHelper();
        }
        return instance;
    }

    public JsonObject getRabbitMQConfig() {
        return config.getAsJsonObject("rabbitmq");
    }

    public JsonObject getExchangeConfig(String type) {
        return config.getAsJsonObject("exchanges").getAsJsonObject(type);
    }

    public JsonObject getQueueConfig(String type) {
        return config.getAsJsonObject("queues").getAsJsonObject(type);
    }

    public String getRoutingKey(String type) {
        return config.getAsJsonObject("routingKeys").get(type).getAsString();
    }
} 