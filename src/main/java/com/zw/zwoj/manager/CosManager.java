package com.zw.zwoj.manager;


import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.zw.zwoj.config.CosClientConfig;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;

@Component
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    //上传对象
    public PutObjectResult putObject(String key,String localFilePath){
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(),key,
                new File(localFilePath));
        return cosClient.putObject(putObjectRequest);
    }


    public PutObjectResult putObject(String key,File file){
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(),key,
                file);
        return cosClient.putObject(putObjectRequest);
    }
}
