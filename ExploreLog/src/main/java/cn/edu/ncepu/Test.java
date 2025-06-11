package cn.edu.ncepu;

import cn.edu.ncepu.Util.CosUtil;

public class Test {
    // 配置参数
    private static final String SECRET_ID = "AKIDtc5HDsGPIvOefPs4ze29zYqqZ1C6kDTT";      // 替换为你的 SecretId
    private static final String SECRET_KEY = "lKekafX9UZNu98rJVR71kCPM3UfbIiup";    // 替换为你的 SecretKey
    private static final String REGION = "ap-beijing";         // 存储桶所在地域
    private static final String BUCKET_NAME = "ylin-bucket-1363644423"; // 存储桶名称
    private static final String KEY = "logs/a.log";                     // 对象键（即文件路径+名称）
    private static final String DOWNLOAD_PATH = "D:/Users/71707/Desktop/a.log";  // 本地保存路径

    public static void main(String[] args) {

        CosUtil.upload("logs/2025-06-10-23_27_13");
//        // 1. 初始化 COS 客户端
//        COSCredentials cred = new BasicCOSCredentials(SECRET_ID, SECRET_KEY);
//        ClientConfig clientConfig = new ClientConfig(new Region(REGION));
//        COSClient cosClient = new COSClient(cred, clientConfig);
//
//        try {
//            // 2. 创建文件元数据（可选，设置文件属性）
//            ObjectMetadata metadata = new ObjectMetadata();
//            metadata.setContentLength(0); // 若文件内容为空，需显式设置长度
//
//            // 3. 构建上传请求
//            File localFile = new File(KEY); // 本地文件路径（需提前创建 a.log 文件）
//            PutObjectRequest putObjectRequest = new PutObjectRequest(
//                    BUCKET_NAME,
//                    KEY,
//                    localFile
//            );
//            putObjectRequest.setMetadata(metadata);
//
//            // 4. 执行上传
//            com.qcloud.cos.model.PutObjectResult result = cosClient.putObject(putObjectRequest);
//            System.out.println("文件上传成功！ETag: " + result.getETag());
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        try {
//            // 2. 构建下载请求
//            GetObjectRequest getObjectRequest = new GetObjectRequest(BUCKET_NAME, KEY);
//
//            // 3. 执行下载
//            COSObject cosObject = cosClient.getObject(getObjectRequest);
//            try (InputStream inputStream = cosObject.getObjectContent();
//                 FileOutputStream fos = new FileOutputStream(DOWNLOAD_PATH)) {
//                byte[] buffer = new byte[1024];
//                int bytesRead;
//                while ((bytesRead = inputStream.read(buffer)) != -1) {
//                    fos.write(buffer, 0, bytesRead);
//                }
//                System.out.println("文件下载成功：" + DOWNLOAD_PATH);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            // 4. 关闭客户端
//            cosClient.shutdown();
//        }

    }
//    public static void main(String[] args) {
//        UUID uuid = UUID.randomUUID();
//        ISender sender = new Sender();
//        sender.initExchange("exchange.ExploreLog", Sender.MQ_DIRECT);
//
//        // 建立Redis连接
//        RedisUtil redisUtil = RedisUtil.getInstance();
//        try {
//            redisUtil.getJedis(uuid);
//        } catch (Exception e) {
//            System.out.println(e.getMessage());
//        }
//
//        // 初始化Redis中的内容
//        redisUtil.setInt("mapHeight", 5);
//        redisUtil.setInt("mapWidth", 5);
//        redisUtil.setMap("mapBarrier", new int[][]{
//                {0, 1, 0, 0, 1}
//                , {0, 0, 0, 1, 1}
//                , {1, 1, 0, 0, 1}
//                , {0, 0, 0, 0, 0}
//                , {1, 0, 1, 0, 0}});
//        redisUtil.setMap("mapExplore", new int[][]{
//                {0, 1, 0, 0, 1}
//                , {1, 1, 1, 1, 1}
//                , {1, 1, 1, 0, 1}
//                , {0, 1, 1, 1, 0}
//                , {1, 0, 1, 1, 0}});
//        redisUtil.setInt("carNum", 3);
//        redisUtil.setCar(new Car(1, CarStatusEnum.RUNNING, new Point(1, 1), new Point(3, 3)
//                , "1243", CarAlgorithmEnum.BFS, 3, "rgb(255,0,255)"
//                , System.currentTimeMillis()));
//        redisUtil.setCar(new Car(2, CarStatusEnum.RUNNING, new Point(2, 2), new Point(4, 1)
//                , "1243", CarAlgorithmEnum.BFS, 3, "rgb(255,0,255)"
//                , System.currentTimeMillis()));
//        redisUtil.setCar(new Car(3, CarStatusEnum.RUNNING, new Point(5, 6), new Point(1, 3)
//                , "1243", CarAlgorithmEnum.BFS, 3, "rgb(255,0,255)"
//                , System.currentTimeMillis()));
//
//        long nowTime = System.currentTimeMillis();
//        sender.sendFairMessage("exchange.ExploreLog"
//                , "exploreLog.fair.routing.key"
//                , new ExploreMessage("Start", String.valueOf(nowTime)).toJson());
//
//        sender.sendFairMessage("exchange.ExploreLog"
//                , "exploreLog.fair.routing.key"
//                , new ExploreMessage("Run", String.valueOf(nowTime+500)).toJson());
//
//        sender.sendFairMessage("exchange.ExploreLog"
//                , "exploreLog.fair.routing.key"
//                , new ExploreMessage("Run", String.valueOf(nowTime+1000)).toJson());
//
//        sender.sendFairMessage("exchange.ExploreLog"
//                , "exploreLog.fair.routing.key"
//                , new ExploreMessage("End", String.valueOf(500001)).toJson());
//
//    }
}
