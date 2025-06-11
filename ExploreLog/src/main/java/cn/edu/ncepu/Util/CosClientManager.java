package cn.edu.ncepu.Util;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;

public class CosClientManager {
    private static COSClient instance;
    // 配置参数
    private static final String SECRET_ID = "AKIDtc5HDsGPIvOefPs4ze29zYqqZ1C6kDTT";      // 替换为你的 SecretId
    private static final String SECRET_KEY = "lKekafX9UZNu98rJVR71kCPM3UfbIiup";    // 替换为你的 SecretKey
    private static final String REGION = "ap-beijing";         // 存储桶所在地域

    public static synchronized COSClient getInstance() {
        if (instance == null) {
            COSCredentials cred = new BasicCOSCredentials(SECRET_ID, SECRET_KEY);
            ClientConfig config = new ClientConfig(new Region(REGION));
            instance = new COSClient(cred, config);
        }
        return instance;
    }
}