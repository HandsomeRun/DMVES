package com.rabbitmq;

import com.rabbitmq.impl.Receiver;
import com.rabbitmq.impl.Sender;
import com.rabbitmq.interfaces.MessageHandler;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

public class Main {
    public static void main(String[] args) {
//        try {
//            Scanner sc = new Scanner(System.in);
//            Sender sender = new Sender() ;
//
//            String broadcastExchange = "broadcast.exchange";
//            String faircastExchange = "fair.exchange" ;
//
//            sender.initExchange("broadcast.exchange" , "fanout");
//            sender.initExchange("fair.exchange" ,"direct" );
//
//
//            Receiver breceiver = new Receiver();
//            breceiver.initExchange("broadcast.exchange" , "fanout");
////            String queueName = breceiver.getQueueNameFromExchange("broadcast.exchange");
////            breceiver.bindQueueToExchange(queueName , broadcastExchange , "");
//
//            breceiver.receiveBroadcastMessage(broadcastExchange, new MessageHandler() {
//                @Override
//                public void handleMessage(String message) {
//                    System.out.println("[1 fanout] : " + message);
//                }
//            });
//
//            Receiver freceiver = new Receiver();
//            freceiver.initExchange(faircastExchange , "direct");
//            String queueNameF = "fair.queue" ;
//            freceiver.initQueue(queueNameF);
//            freceiver.bindQueueToExchange(queueNameF,faircastExchange,"fair.routing.key");
//
//            freceiver.receiveFairMessage(faircastExchange, queueNameF, new MessageHandler() {
//                @Override
//                public void handleMessage(String message) {
//                    System.out.println("[1 fair] : " + message);
//                }
//            });
//
//            int cnt = 0 ;
//            while(true){
//                int op = sc.nextInt();
//                if(op == 1) {
//                    System.out.println("发送广播消息...");
//                    sender.sendBroadcastMessage("broadcast.exchange" , "这是一条广播消息" + cnt);
//                }
//                else if(op == 2){
//                    System.out.println("发送公平分发消息...");
//                    sender.sendFairMessage("fair.exchange" ,"fair.routing.key" ,"这是一条公平分发消息" + cnt);
//                }
//                else {
//                    break ;
//                }
//            }
//
//            // 等待消息处理
//            Thread.sleep(2000);
//            sender.close();
//            sc.close();
//
//        } catch ( InterruptedException e) {
//            e.printStackTrace();
//        }

        Receiver receiver = new Receiver();
        receiver.DMVESReceiverMessage(Receiver.ControlName, Receiver.TargetName, new MessageHandler() {
            @Override
            public void handleMessage(String message) {
                System.out.println("[2 fair] : " + message);
            }
        });

        Receiver receiver1 = new Receiver();
        receiver1.DMVESReceiverMessage(Receiver.ControlName, Receiver.ViewName, new MessageHandler() {
            @Override
            public void handleMessage(String message) {
                System.out.println("[2 fanout] : " + message);
            }
        });
    }
} 