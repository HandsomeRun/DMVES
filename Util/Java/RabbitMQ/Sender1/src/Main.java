import com.rabbitmq.impl.Sender;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

//TIP 要<b>运行</b>代码，请按 <shortcut actionId="Run"/> 或
// 点击装订区域中的 <icon src="AllIcons.Actions.Execute"/> 图标。
public class Main {




    public static void main(String[] args) {

        String broadcastExchange = "broadcast.exchange";
        String faircastExchange = "fair.exchange" ;
        String queueNameF = "fair.queue" ;
        String fairRoutingKey = "fair.routing.key";

        Sender sender = new Sender();

        sender.initExchange(broadcastExchange ,Sender.MQ_FANOUT ) ;
        sender.initExchange(faircastExchange , Sender.MQ_DIRECT);

        Scanner sc = new Scanner(System.in) ;
        int cnt = 0 ;
        while(true){
            int op = sc.nextInt();
            if(op == 1) {
                System.out.println("发送广播消息...");
                sender.sendBroadcastMessage("broadcast.exchange" , "这是一条广播消息" + cnt);
            }
            else if(op == 2){
                System.out.println("发送公平分发消息...");
                sender.sendFairMessage("fair.exchange" ,"fair.routing.key" ,"这是一条公平分发消息" + cnt);
            }
            else {
                break ;
            }
            cnt ++ ;
        }

        // 等待消息处理
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        sender.close();
        sc.close();

    }
}