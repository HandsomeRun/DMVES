import com.rabbitmq.impl.Sender;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

//TIP 要<b>运行</b>代码，请按 <shortcut actionId="Run"/> 或
// 点击装订区域中的 <icon src="AllIcons.Actions.Execute"/> 图标。
public class Main {
    public static void main(String[] args) {
        //TIP 当文本光标位于高亮显示的文本处时按 <shortcut actionId="ShowIntentionActions"/>
        // 查看 IntelliJ IDEA 建议如何修正。
        System.out.printf("Hello and welcome!");

        for (int i = 1; i <= 5; i++) {
            //TIP 按 <shortcut actionId="Debug"/> 开始调试代码。我们已经设置了一个 <icon src="AllIcons.Debugger.Db_set_breakpoint"/> 断点
            // 但您始终可以通过按 <shortcut actionId="ToggleLineBreakpoint"/> 添加更多断点。
            System.out.println("i = " + i);
        }

        Sender sender = null;
        try {
            sender = new Sender();

            Scanner sc = new Scanner(System.in);
            int cnt = 0 ;
            while(true){
                int op = sc.nextInt();
                if(op == 1) {
                    System.out.println("发送广播消息..." + cnt);
                    sender.sendBroadcast("这是一条广播消息" + cnt);
                }
                else if(op == 2){
                    System.out.println("发送公平分发消息..." + cnt);
                    sender.sendFairMessage("这是一条公平分发消息" + cnt );
                }
                else {
                    break ;
                }
                cnt ++ ;
            }

            // 等待消息处理
            Thread.sleep(2000);

            // 关闭连接
            sender.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}