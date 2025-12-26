package com.zw.zwoj.model.dto.postthumb;

import lombok.Data;

import java.io.Serializable;

@Data
public class PostThumbAddRequest implements Serializable {
    private Long postId;
    
    private static final long serialVersionUID = 1L;
}
