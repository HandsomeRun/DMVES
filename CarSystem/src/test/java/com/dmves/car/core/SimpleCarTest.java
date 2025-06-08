package com.dmves.car.core;

import com.dmves.car.core.message.*;
import com.dmves.car.core.model.*;
import org.junit.*;
import static org.junit.Assert.*;

/**
 * 小车子系统简单测试类
 */
public class SimpleCarTest {

    private Car car;
    private Message message;

    @Before
    public void setUp() {
        // 初始化小车
        car = new Car();
        car.setCarId("test-car-001");
        car.setCarPosition(new Point(0, 0));
        car.setCarStatus(CarStatusEnum.FREE);
    }

    @Test
    public void testCarCreation() {
        // 测试小车是否正确创建
        assertNotNull(car);
        assertEquals("test-car-001", car.getCarId());
        assertEquals(0, car.getCarPosition().getX(), 0.001);
        assertEquals(0, car.getCarPosition().getY(), 0.001);
        assertEquals(CarStatusEnum.FREE, car.getCarStatus());
    }

    @Test
    public void testCarMovement() {
        // 测试小车移动
        Point newPosition = new Point(10, 20);
        car.setCarPosition(newPosition);

        assertEquals(10, car.getCarPosition().getX(), 0.001);
        assertEquals(20, car.getCarPosition().getY(), 0.001);
    }

    @Test
    public void testCarStatusChange() {
        // 测试小车状态变更
        car.setCarStatus(CarStatusEnum.RUNNING);
        assertEquals(CarStatusEnum.RUNNING, car.getCarStatus());

        car.setCarStatus(CarStatusEnum.NAVIGATING);
        assertEquals(CarStatusEnum.NAVIGATING, car.getCarStatus());
    }

    @Test
    public void testMessageCreation() {
        // 测试消息创建
        message = new Message();
        message.setType(MessageType.MOVE_REQUEST);
        message.setContent("(10,20)");
        message.setSender("test-sender");
        message.setReceiver("test-car-001");
        message.setTimestamp(System.currentTimeMillis());

        assertNotNull(message);
        assertEquals(MessageType.MOVE_REQUEST, message.getType());
        assertEquals("(10,20)", message.getContent());
        assertEquals("test-sender", message.getSender());
        assertEquals("test-car-001", message.getReceiver());
    }

    @Test
    public void testPointParsing() {
        // 测试点位解析
        String pointStr = "(15,25)";
        Point point = new Point(15, 25);

        assertNotNull(point);
        assertEquals(15, point.getX(), 0.001);
        assertEquals(25, point.getY(), 0.001);

        // 测试点位转字符串
        String backToStr = point.toString();
        assertTrue(backToStr.contains("15") && backToStr.contains("25"));
    }

    @Test
    public void testCarTargetSetting() {
        // 测试设置目标位置
        Point target = new Point(30, 40);
        car.setCarTarget(target);

        assertNotNull(car.getCarTarget());
        assertEquals(30, car.getCarTarget().getX(), 0.001);
        assertEquals(40, car.getCarTarget().getY(), 0.001);
    }
}