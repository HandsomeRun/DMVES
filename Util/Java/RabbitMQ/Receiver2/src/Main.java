import com.rabbitmq.impl.Receiver;
import com.rabbitmq.interfaces.MessageHandler;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

//TIP 要<b>运行</b>代码，请按 <shortcut actionId="Run"/> 或
// 点击装订区域中的 <icon src="AllIcons.Actions.Execute"/> 图标。
public class Main {
    public static void main(String[] args) {

//        String broadcastExchange = "broadcast.exchange";
//        String faircastExchange = "fair.exchange" ;
//        String queueNameF = "fair.queue" ;
//        String fairRoutingKey = "fair.routing.key";
//
//
//        Receiver breceiver = new Receiver();
//        breceiver.initExchange("broadcast.exchange" , "fanout");
//
//        breceiver.receiveBroadcastMessage(broadcastExchange, new MessageHandler() {
//            @Override
//            public void handleMessage(String message) {
//                System.out.println("[1 fanout] : " + message);
//            }
//        });
//
//        Receiver freceiver = new Receiver();
//        freceiver.initExchange(faircastExchange , "direct");
//        freceiver.initQueue(queueNameF);
//        freceiver.bindQueueToExchange(queueNameF,faircastExchange,"fair.routing.key");
//
//        freceiver.receiveFairMessage(faircastExchange, queueNameF, new MessageHandler() {
//            @Override
//            public void handleMessage(String message) {
//                System.out.println("[1 fair] : " + message);
//            }
//        });

        Receiver receiver = new Receiver();
        receiver.DMVESReceiverMessage(Receiver.ControlName, Receiver.TargetName, new MessageHandler() {
            @Override
            public void handleMessage(String message) {
                System.out.println("[1 fair] : " + message);
            }
        });

        Receiver receiver1 = new Receiver();
        receiver1.DMVESReceiverMessage(Receiver.ControlName, Receiver.ViewName, new MessageHandler() {
            @Override
            public void handleMessage(String message) {
                System.out.println("[1 fanout] : " + message);
            }
        });

    }
}