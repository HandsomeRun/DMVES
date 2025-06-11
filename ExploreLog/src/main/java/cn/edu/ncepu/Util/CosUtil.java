package cn.edu.ncepu.Util;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.*;

import java.io.*;

public class CosUtil {

    private static final String BUCKET_NAME = "ylin-bucket-1363644423"; // 存储桶名称

    /**
     * 上传filePath下的所有日志
     *
     * @param filePath 对应需要上传的文件夹路径
     */
    public static void upload(String filePath) {
        // 初始化 COS 客户端
        COSClient cosClient = CosClientManager.getInstance();

        try {
            // 构建上传请求
            File localFolder = new File(filePath); // 本地名称
            if (localFolder.exists() && localFolder.isDirectory()) {
                File[] files = localFolder.listFiles();  // 获取文件数组
                if (files != null) {
                    // 遍历所有文件
                    for (File file : files) {
                        if (file.isFile()) {
                            // 构建上传请求
                            ObjectMetadata metadata = new ObjectMetadata();
                            metadata.setContentLength(file.length()); // 动态设置文件长度
                            PutObjectRequest putRequest = new PutObjectRequest(
                                    BUCKET_NAME,
                                    filePath + "/" + file.getName(),
                                    file
                            );
                            putRequest.setMetadata(metadata);

                            // 执行上传
                            PutObjectResult result = cosClient.putObject(putRequest);
                            System.out.println("文件上传成功！ETag: " + result.getETag());

                        }
                    }
                    // 最后删除空文件夹
                    deleteFolder(localFolder);
                }
            } else {
                System.out.println("路径不存在或不是一个文件夹");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static BufferedReader readRunLog(String filePath) {
        COSClient cosClient = CosClientManager.getInstance();
        File tempFile = null;
        try {
            // 构建临时文件路径
            tempFile = File.createTempFile("cos-temp-", ".log", new File("logs/temp"));

            // 执行下载到临时文件
            GetObjectRequest getObjectRequest = new GetObjectRequest(BUCKET_NAME, filePath + "/runLog.log");
            COSObject cosObject = cosClient.getObject(getObjectRequest);
            try (InputStream inputStream = cosObject.getObjectContent();
                 FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
            System.out.println("文件已下载至临时文件：" + tempFile.getAbsolutePath());

            return new CloseableBufferedReader(new BufferedReader(new InputStreamReader(new FileInputStream(tempFile)))
                    , tempFile);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 递归删除文件夹
    private static void deleteFolder(File folder) {
        if (folder.exists()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteFolder(file); // 递归删除子文件夹
                    } else {
                        file.delete(); // 删除文件
                    }
                }
            }
            folder.delete(); // 删除空文件夹
            System.out.println("文件夹已删除: " + folder.getAbsolutePath());
        }
    }

    // 自定义包装类，实现资源自动清理
    private static class CloseableBufferedReader extends BufferedReader implements Closeable {
        private final File tempFile;

        public CloseableBufferedReader(Reader reader, File tempFile) {
            super(reader);
            this.tempFile = tempFile;
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                // 删除临时文件
                if (tempFile != null && tempFile.exists()) {
                    if (!tempFile.delete()) {
                        System.err.println("无法删除临时文件: " + tempFile.getAbsolutePath());
                    }
                }
            }
        }
    }
}
