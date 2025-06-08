package com.rabbitmq.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileReader;
import java.io.IOException;

public class MQConfigHelper {
    private static MQConfigHelper instance;
    private RabbitMQConfig config;

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
        try (FileReader reader = new FileReader("src/config.json")) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject rabbitmqConfig = jsonObject.getAsJsonObject("rabbitmq");
            
            Gson gson = new Gson();
            config = gson.fromJson(rabbitmqConfig, RabbitMQConfig.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load RabbitMQ configuration", e);
        }
    }

    public RabbitMQConfig getConfig() {
        return config;
    }
} 