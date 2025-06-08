package com.dmves.car.core;

import com.dmves.car.core.message.*;
import com.dmves.car.core.model.*;
import com.dmves.car.core.component.CarComponent;
import com.dmves.car.core.blackboard.IBlackboard;
import org.junit.*;
import static org.junit.Assert.*;

/**
 * 小车子系统集成测试类
 */
public class CarSystemTest {

    private Car car;
    private CarComponent carComponent;
    private MockBlackboard mockBlackboard;

    /**
     * 模拟黑板实现，用于测试
     */
    private class MockBlackboard implements IBlackboard {
        private final java.util.Map<String, String> data = new java.util.HashMap<>();

        public void write(String key, String message) {
            data.put(key, message);
        }

        public String read(String key) {
            return data.get(key);
        }

        public void delete(String key) {
            data.remove(key);
        }

        public void expire(String key, int seconds) {
            // 测试中不需要实现过期功能
        }

        public boolean exists(String key) {
            return data.containsKey(key);
        }
    }

    @Before
    public void setUp() {
        mockBlackboard = new MockBlackboard();

        // 设置地图信息
        mockBlackboard.write("mapWidth", "10");
        mockBlackboard.write("mapHeight", "10");
        mockBlackboard.write("mapBarrier", "0".repeat(100)); // 10x10的无障碍地图

        carComponent = new CarComponent("test-car", mockBlackboard);
        car = carComponent.getCar();

        // 初始化组件
        carComponent.initialize();
        carComponent.start();
    }

    @After
    public void tearDown() {
        // 停止组件
        carComponent.stop();
    }

    @Test
    public void testInitialization() {
        // 测试初始化
        assertNotNull(car);
        assertNotNull(carComponent);
        assertEquals("test-car", carComponent.getCarId());
        assertEquals(CarStatusEnum.FREE, carComponent.getCurrentStatus());
        assertEquals(0, carComponent.getCar().getCarPosition().getX());
        assertEquals(0, carComponent.getCar().getCarPosition().getY());
    }

    @Test
    public void testCarStatusUpdate() {
        // 测试小车状态更新
        // 从 FREE 可以转换到 SEARCHING
        assertTrue(carComponent.updateStatus(CarStatusEnum.SEARCHING));
        assertEquals(CarStatusEnum.SEARCHING, carComponent.getCurrentStatus());

        // 从 SEARCHING 可以转换到 WAIT_NAV
        carComponent.updateTarget(5, 5); // 设置目标位置
        assertTrue(carComponent.updateStatus(CarStatusEnum.WAIT_NAV));
        assertEquals(CarStatusEnum.WAIT_NAV, carComponent.getCurrentStatus());

        // 从 WAIT_NAV 可以转换到 NAVIGATING
        assertTrue(carComponent.updateStatus(CarStatusEnum.NAVIGATING));
        assertEquals(CarStatusEnum.NAVIGATING, carComponent.getCurrentStatus());

        // 从 NAVIGATING 可以转换到 RUNNING
        carComponent.updatePath("RDLU"); // 设置路径
        assertTrue(carComponent.updateStatus(CarStatusEnum.RUNNING));
        assertEquals(CarStatusEnum.RUNNING, carComponent.getCurrentStatus());

        // 从 RUNNING 可以转换到 FREE
        assertTrue(carComponent.updateStatus(CarStatusEnum.FREE));
        assertEquals(CarStatusEnum.FREE, carComponent.getCurrentStatus());
    }

    @Test
    public void testCarPathExecution() {
        // 测试路径执行
        // 先确保小车处于 FREE 状态
        carComponent.updateStatus(CarStatusEnum.FREE);

        // 先切换到SEARCHING状态
        assertTrue("应该能够切换到SEARCHING状态", carComponent.updateStatus(CarStatusEnum.SEARCHING));

        // 设置目标位置
        carComponent.updateTarget(2, 2);
        String path = "RRDD"; // 简单的路径：右右下下，最终应该到达(2,2)

        // 确保目标位置已设置
        assertNotNull("目标位置不应为空", carComponent.getCar().getCarTarget());
        assertEquals("目标X坐标应该是2", 2, carComponent.getCar().getCarTarget().getX());
        assertEquals("目标Y坐标应该是2", 2, carComponent.getCar().getCarTarget().getY());

        // 现在可以转换到WAIT_NAV状态
        assertTrue("应该能够切换到WAIT_NAV状态", carComponent.updateStatus(CarStatusEnum.WAIT_NAV));
        assertTrue("应该能够切换到NAVIGATING状态", carComponent.updateStatus(CarStatusEnum.NAVIGATING));

        // 设置路径并执行
        carComponent.updatePath(path);
        assertEquals("路径应该被正确设置", path, carComponent.getCar().getCarPath());

        // 更新状态为 RUNNING
        assertTrue("应该能够切换到RUNNING状态", carComponent.updateStatus(CarStatusEnum.RUNNING));

        // 执行第一步移动
        assertTrue("第一步移动应该成功", carComponent.move("R"));
        assertEquals("X坐标应该增加1", 1, carComponent.getCar().getCarPosition().getX());
        assertEquals("Y坐标应该保持不变", 0, carComponent.getCar().getCarPosition().getY());

        // 执行剩余的路径
        assertTrue("路径执行应该成功开始", carComponent.executePath("RDD"));

        // 等待路径执行完成（增加等待时间，确保路径执行完成）
        try {
            // 由于每步移动间隔100ms，3步移动需要至少300ms，我们等待2秒以确保完成
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("测试被中断");
        }

        // 验证最终状态
        assertEquals("执行完成后应该保持RUNNING状态", CarStatusEnum.RUNNING, carComponent.getCurrentStatus());

        // 验证最终位置
        Point finalPosition = carComponent.getCar().getCarPosition();
        assertEquals("最终X坐标应该是2", 2, finalPosition.getX());
        assertEquals("最终Y坐标应该是2", 2, finalPosition.getY());

        // 验证是否到达目标位置
        Point target = carComponent.getCar().getCarTarget();
        assertEquals("应该到达目标X坐标", target.getX(), finalPosition.getX());
        assertEquals("应该到达目标Y坐标", target.getY(), finalPosition.getY());
    }

    @Test
    public void testCarMovement() {
        // 测试小车移动
        carComponent.updateStatus(CarStatusEnum.FREE);

        // 向右移动
        assertTrue(carComponent.move("R"));
        assertEquals(1, carComponent.getCar().getCarPosition().getX());
        assertEquals(0, carComponent.getCar().getCarPosition().getY());

        // 向下移动
        assertTrue(carComponent.move("D"));
        assertEquals(1, carComponent.getCar().getCarPosition().getX());
        assertEquals(1, carComponent.getCar().getCarPosition().getY());

        // 向左移动
        assertTrue(carComponent.move("L"));
        assertEquals(0, carComponent.getCar().getCarPosition().getX());
        assertEquals(1, carComponent.getCar().getCarPosition().getY());

        // 向上移动
        assertTrue(carComponent.move("U"));
        assertEquals(0, carComponent.getCar().getCarPosition().getX());
        assertEquals(0, carComponent.getCar().getCarPosition().getY());
    }

    @Test
    public void testCarBoundaryValues() {
        // 测试边界值
        carComponent.updateStatus(CarStatusEnum.FREE);

        // 测试无效的移动方向
        assertFalse("无效方向应该移动失败", carComponent.move("X"));
        assertEquals(0, carComponent.getCar().getCarPosition().getX());
        assertEquals(0, carComponent.getCar().getCarPosition().getY());

        // 测试空路径
        assertFalse("空路径应该执行失败", carComponent.executePath(""));
        assertEquals(0, carComponent.getCar().getCarPosition().getX());
        assertEquals(0, carComponent.getCar().getCarPosition().getY());

        // 测试边界移动
        // 确保地图大小正确设置
        assertEquals("10", mockBlackboard.read("mapWidth"));
        assertEquals("10", mockBlackboard.read("mapHeight"));

        // 更新位置到边界
        carComponent.updatePosition(9, 9);

        // 验证当前位置
        assertEquals("X坐标应该是9", 9, carComponent.getCar().getCarPosition().getX());
        assertEquals("Y坐标应该是9", 9, carComponent.getCar().getCarPosition().getY());
    }

    @Test
    public void testCompleteWorkflow() {
        // 测试完整工作流
        // 初始状态检查
        assertEquals(CarStatusEnum.FREE, carComponent.getCurrentStatus());
        assertEquals(0, carComponent.getCar().getCarPosition().getX());
        assertEquals(0, carComponent.getCar().getCarPosition().getY());

        // 先切换到SEARCHING状态
        assertTrue(carComponent.updateStatus(CarStatusEnum.SEARCHING));

        // 设置目标位置
        carComponent.updateTarget(2, 2);
        assertNotNull(carComponent.getCar().getCarTarget());
        assertEquals(2, carComponent.getCar().getCarTarget().getX());
        assertEquals(2, carComponent.getCar().getCarTarget().getY());

        // 状态转换
        assertTrue(carComponent.updateStatus(CarStatusEnum.WAIT_NAV));
        assertTrue(carComponent.updateStatus(CarStatusEnum.NAVIGATING));

        // 设置路径
        String path = "RRDD";
        carComponent.updatePath(path);
        assertEquals(path, carComponent.getCar().getCarPath());

        // 执行路径
        assertTrue(carComponent.updateStatus(CarStatusEnum.RUNNING));

        // 执行第一步移动
        assertTrue(carComponent.move("R"));
        assertEquals(1, carComponent.getCar().getCarPosition().getX());
        assertEquals(0, carComponent.getCar().getCarPosition().getY());

        // 执行剩余的路径
        assertTrue(carComponent.executePath("RDD"));

        // 等待路径执行
        try {
            Thread.sleep(2000); // 等待足够长的时间确保路径执行完成
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 验证最终位置
        Point position = carComponent.getCar().getCarPosition();
        assertTrue("X坐标应该大于0", position.getX() > 0);
        assertTrue("Y坐标应该大于0", position.getY() > 0);
    }

    @Test
    public void testCarColor() {
        // 测试小车颜色设置
        String newColor = "red";
        car.setCarColor(newColor);
        assertEquals(newColor, car.getCarColor());
    }

    @Test
    public void testBlackboardCommunication() {
        // 测试与黑板的通信
        String testKey = "test_key";
        String testMessage = "test_message";

        // 写入黑板
        mockBlackboard.write(testKey, testMessage);

        // 验证读取
        assertEquals(testMessage, mockBlackboard.read(testKey));

        // 验证存在性检查
        assertTrue(mockBlackboard.exists(testKey));

        // 删除并验证
        mockBlackboard.delete(testKey);
        assertFalse(mockBlackboard.exists(testKey));
    }
}