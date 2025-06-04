package com.rabbitmq;

import com.rabbitmq.impl.Receiver;
import com.rabbitmq.impl.Sender;
import com.rabbitmq.interfaces.MessageHandler;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

public class Main {
    public static void main(String[] args) {
        try {
            // 创建发送者和接收者
            Sender sender = new Sender();
//            Receiver receiver1 = new Receiver();
//            Receiver receiver2 = new Receiver();

            // 设置消息处理器
            MessageHandler handler = message -> {
                System.out.println("收到消息: " + message + " me ");
            };

            // 启动接收者
//            receiver1.receiveBroadcast(new MessageHandler() {
//                @Override
//                public void handle(String message) {
//                    System.out.println("[1] 收到消息: " + message);
//                }
//            });
//            receiver2.receiveBroadcast(new MessageHandler() {
//                @Override
//                public void handle(String message) {
//                    System.out.println("[2] 收到消息: " + message);
//                }
//            });
//            receiver1.receiveFairMessage(new MessageHandler() {
//                @Override
//                public void handle(String message) {
//                    System.out.println("[1] 收到消息: " + message);
//                }
//            });
//            receiver2.receiveFairMessage(new MessageHandler() {
//                @Override
//                public void handle(String message) {
//                    System.out.println("[2] 收到消息: " + message);
//                }
//            });

            // 发送消息
//            System.out.println("发送广播消息...");
//            sender.sendBroadcast("这是一条广播消息");
//
//            System.out.println("发送公平分发消息...");
//            sender.sendFairMessage("这是一条公平分发消息");

            Scanner sc = new Scanner(System.in);
            
            while(true){
                int op = sc.nextInt();
                if(op == 1) {
                    System.out.println("发送广播消息...");
                    sender.sendBroadcast("这是一条广播消息");
                }
                else if(op == 2){
                    System.out.println("发送公平分发消息...");
                    sender.sendFairMessage("这是一条公平分发消息");
                }
                else {
                    break ;
                }
            }

            // 等待消息处理
            Thread.sleep(2000);

            // 关闭连接
            sender.close();
//            receiver1.close();
//            receiver2.close();

        } catch (IOException | TimeoutException | InterruptedException e) {
            e.printStackTrace();
        }
    }
} 