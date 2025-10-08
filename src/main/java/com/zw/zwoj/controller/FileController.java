package com.zw.zwoj.controller;


import cn.hutool.core.io.FileUtil;
import com.zw.zwoj.common.BaseResponse;
import com.zw.zwoj.common.ErrorCode;
import com.zw.zwoj.common.ResultUtils;
import com.zw.zwoj.constant.FileConstant;
import com.zw.zwoj.exception.BusinessException;
import com.zw.zwoj.manager.CosManager;
import com.zw.zwoj.model.bean.User;
import com.zw.zwoj.model.dto.file.UploadFileRequest;
import com.zw.zwoj.model.enums.FileUploadBizEnum;
import com.zw.zwoj.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Arrays;

@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

    @Resource
    private UserService userService;

    @Resource

    private CosManager  cosManager;

    @PostMapping("/upload")
    public BaseResponse<String> uploadFile(@RequestPart("file") MultipartFile multipartFile,
                                           UploadFileRequest uploadFileRequest, HttpServletRequest request){
        String biz = uploadFileRequest.getBiz();
        FileUploadBizEnum fileUploadBizEnum = FileUploadBizEnum.getEunmByValue(biz);
        if(fileUploadBizEnum == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        validFile(multipartFile,fileUploadBizEnum);
        User loginUser = userService.getLoginUser(request);
        //文件目录，根据业务，用户来划分
        String uuid = RandomStringUtils.randomAlphanumeric(8);
        String filename = uuid + "-" + multipartFile.getOriginalFilename();
        String filepath = String.format("/%s/%s/%s",fileUploadBizEnum.getValue(),loginUser.getId(),filename);
        File file = null;
        try {
            file = File.createTempFile(filepath,null);
            multipartFile.transferTo(file);
            cosManager.putObject(filepath,file);
            //返回可访问地址
            return ResultUtils.success(FileConstant.COS_HOST +filepath);
        } catch (Exception e) {
            log.error("file upload error , filepath =" + filepath,e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"上传失败");

        }finally {
            if(file != null){
                //删除临时文件
                boolean delete = file.delete();
                if(!delete){
                    log.error("file delete error , filepath =" + filepath);
                }
            }
        }
    }

    //校验文件
    private void validFile(MultipartFile multipartFile,FileUploadBizEnum fileUploadBizEnum){
        long fileSize = multipartFile.getSize();

        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        final long ONE_MB = 1024 * 1024L;
        if(FileUploadBizEnum.USER_AVATAR.equals(fileUploadBizEnum)){
            if(fileSize > ONE_MB){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"文件大小超过了1M");
            }
            if(!Arrays.asList("jpg","jpeg","gif","png","svg","webp").contains(fileSuffix)){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"文件类型错误");
            }
        }
    }
}
