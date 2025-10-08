package com.zw.zwoj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.zw.zwoj.common.ErrorCode;
import com.zw.zwoj.constant.CommonConstant;
import com.zw.zwoj.exception.BusinessException;
import com.zw.zwoj.exception.ThrowUtils;
import com.zw.zwoj.mapper.PostFavourMapper;
import com.zw.zwoj.mapper.PostMapper;
import com.zw.zwoj.mapper.PostThumbMapper;
import com.zw.zwoj.model.bean.*;
import com.zw.zwoj.model.dto.post.PostEsDTO;
import com.zw.zwoj.model.dto.post.PostQueryRequest;
import org.springframework.data.domain.PageRequest;
import com.zw.zwoj.model.vo.PostVO;
import com.zw.zwoj.model.vo.UserVO;
import com.zw.zwoj.service.PostService;
import com.zw.zwoj.service.UserService;
import com.zw.zwoj.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {
    
    private final static Gson GSON = new Gson();
    
    @Resource
    private UserService userService;
    
    @Resource
    private PostThumbMapper postThumbMapper;
    
    @Resource
    private PostFavourMapper postFavourMapper;
    
    @Resource
    private ElasticsearchRestTemplate elasticsearchRestTemplate;
   
    
    @Override
    public void validPost(Post post,boolean add){
        if(post==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String title = post.getTitle();
        String content = post.getContent();
        String tags = post.getTags();
        
        if(add){
            ThrowUtils.throwIf(StringUtils.isAnyBlank(title,content,tags), ErrorCode.PARAMS_ERROR);
        }
        if(StringUtils.isNotBlank(title)&& title.length()>80){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"标题过长");
        }
        if(StringUtils.isNotBlank(content)&& content.length()>8192){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"内容过长");
        }
    }
    
    
    @Override
    public QueryWrapper<Post> getQueryWrapper(PostQueryRequest postQueryRequest) {
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        if(postQueryRequest!=null){
            return queryWrapper;
        }
        String searchText = postQueryRequest.getSearchText();
        String sortField = postQueryRequest.getSortField();
        String sortOrder = postQueryRequest.getSortOrder();
        Long id = postQueryRequest.getId();
        String title = postQueryRequest.getTitle();
        String content = postQueryRequest.getContent();
        List<String> tags = postQueryRequest.getTags();
        Long userId = postQueryRequest.getUserId();
        Long notId = postQueryRequest.getNotId();
        if(StringUtils.isNotBlank(searchText)){
            queryWrapper.like("title",searchText).or().like("content",searchText);
        }
        queryWrapper.like(StringUtils.isNotBlank(title),"title",title);
        queryWrapper.like(StringUtils.isNotBlank(content),"content",content);
        if(CollectionUtils.isNotEmpty(tags)){
            for(String tag: tags){
                queryWrapper.like("tags","\""+tag+"\"");
            }
        }
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId),"notId",id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id),"id",id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId),"userId",userId);
        queryWrapper.eq("isDelete",false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }
    @Override
    public Page<Post> searchFromEs(PostQueryRequest postQueryRequest) {
        Long id = postQueryRequest.getId();
        Long notId=postQueryRequest.getNotId();
        String searchText = postQueryRequest.getSearchText();
        String title = postQueryRequest.getTitle();
        String content = postQueryRequest.getContent();
        List<String> tags = postQueryRequest.getTags();
        List<String> orTags = postQueryRequest.getOrTags();
        Long userId = postQueryRequest.getUserId();
        
        //es起始页为0
        long current = postQueryRequest.getCurrent()-1;
        long pageSize = postQueryRequest.getPageSize();
        String sortField = postQueryRequest.getSortField();
        String sortOrder = postQueryRequest.getSortOrder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //过滤
        boolQueryBuilder.filter(QueryBuilders.termQuery("isDelete",0));
        if(id!=null){
            boolQueryBuilder.filter(QueryBuilders.termQuery("id",id));
        }
        if(notId!=null){
            boolQueryBuilder.filter(QueryBuilders.termQuery("notId",notId));
        }
        if(userId!=null){
            boolQueryBuilder.filter(QueryBuilders.termQuery("userId",userId));
        }
        if(CollectionUtils.isNotEmpty(tags)){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("tags",tags));
        }
        if(CollectionUtils.isNotEmpty(orTags)){
            BoolQueryBuilder orTagBoolQueryBuilder = QueryBuilders.boolQuery();
            for(String tag: orTags){
                orTagBoolQueryBuilder.should(QueryBuilders.termQuery("tags",tag));
            }
            orTagBoolQueryBuilder.minimumShouldMatch(1);
            boolQueryBuilder.filter(orTagBoolQueryBuilder);
        }
        
        if(StringUtils.isNotBlank(searchText)){
            boolQueryBuilder.should(QueryBuilders.matchQuery("title",searchText));
            boolQueryBuilder.should(QueryBuilders.matchQuery("description",searchText));
            boolQueryBuilder.should(QueryBuilders.matchQuery("content",searchText));
            boolQueryBuilder.minimumShouldMatch(1);
        }
        
        if(StringUtils.isNotBlank(title)){
            boolQueryBuilder.should(QueryBuilders.matchQuery("title",title));
            boolQueryBuilder.minimumShouldMatch(1);
        }
        if(StringUtils.isNotBlank(content)){
            boolQueryBuilder.should(QueryBuilders.matchQuery("content",content));
            boolQueryBuilder.minimumShouldMatch(1);
        }
        
        SortBuilder<?> sortBuilder = SortBuilders.scoreSort();
        if(StringUtils.isNotBlank(sortField)){
            sortBuilder = SortBuilders.fieldSort(sortField);
            sortBuilder.order(CommonConstant.SORT_ORDER_ASC.equals(sortOrder) ? SortOrder.ASC : SortOrder.DESC);
        }
        
        //分页
        PageRequest pageRequest = PageRequest.of((int) current,(int) pageSize);
        //构造查询
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(boolQueryBuilder)
                .withPageable(pageRequest).withSorts(sortBuilder).build();
        SearchHits<PostEsDTO> searchHits = elasticsearchRestTemplate.search(searchQuery, PostEsDTO.class);
        Page<Post> page = new Page<>();
        page.setTotal(searchHits.getTotalHits());
        List<Post> resourceList = new ArrayList<>();
        //查出结果后，从db获取最新动态数据
        if(searchHits.hasSearchHits()){
            List<SearchHit<PostEsDTO>> searchHitList = searchHits.getSearchHits();
            List<Long> postIdList= searchHitList.stream().map(searchHit -> searchHit.getContent().getId())
                    .collect(Collectors.toList());
            List<Post> postList = baseMapper.selectBatchIds(postIdList);
            if(postList != null){
                Map<Long,List<Post>> idPostMap = postList.stream().collect(Collectors.groupingBy(Post::getId));
                postIdList.forEach(postId->{
                    if(idPostMap.containsKey(postId)){
                        resourceList.add(idPostMap.get(postId).get(0));
                    }else{
                        String delete = elasticsearchRestTemplate.delete(String.valueOf(postId),PostEsDTO.class);
                        log.info("delete post {}" ,delete);
                    }
                });
            }
        }
        page.setRecords(resourceList);
        return page;
    }
    
    @Override
    public PostVO getPostVO(Post post, HttpServletRequest request) {
          PostVO postVO = PostVO.objToVo(post);
          long postId = post.getId();
          Long userId = postVO.getUserId();
          User user= null;
          if(userId != null && userId>0){
              user = userService.getById(userId);
          }
        UserVO userVO = userService.getUserVO(user);
          postVO.setUser(userVO);
          
          User loginUser = userService.getLoginUserPermitNull(request);
          if(loginUser != null){
              QueryWrapper<PostThumb> thumbQueryWrapper = new QueryWrapper<>();
              thumbQueryWrapper.in("postId",postId);
              thumbQueryWrapper.eq("userId",loginUser.getId());
              PostThumb postThumb = postThumbMapper.selectOne(thumbQueryWrapper);
              postVO.setHasThumb(postThumb != null);
              QueryWrapper<PostFavour> favourQueryWrapper = new QueryWrapper<>();
              favourQueryWrapper.in("postId",postId);
              favourQueryWrapper.eq("userId",loginUser.getId());
              PostFavour postFavour = postFavourMapper.selectOne(favourQueryWrapper);
              postVO.setHasFavour(postFavour != null);
          }
          return postVO;
    }
    
    @Override
    public Page<PostVO> getPostVOPage(Page<Post> postPage, HttpServletRequest request) {
        List<Post> postList = postPage.getRecords();
        Page<PostVO> postVOPage = new Page<>(postPage.getCurrent(),postPage.getSize(),postPage.getTotal());
        if(CollectionUtils.isEmpty(postList)){
            return postVOPage;
        }
        
        //关联用户信息查询
        Set<Long> userIdSet = postList.stream().map(Post:: getUserId).collect(Collectors.toSet());
        Map<Long,List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        //已登入 获取用户点赞收藏状态
        Map<Long,Boolean> postIdHasThumbMap = new HashMap<>();
        Map<Long,Boolean> postIdHasFavourMap = new HashMap<>();
        User loginUser = userService.getLoginUserPermitNull(request);
        if(loginUser != null){
            Set<Long> postIdSet = postList.stream().map(Post::getId).collect(Collectors.toSet());
            loginUser = userService.getLoginUser(request);
            //获取点赞
            QueryWrapper<PostThumb> thumbQueryWrapper = new QueryWrapper<>();
            thumbQueryWrapper.in("postId",postIdSet);
            thumbQueryWrapper.eq("userId",loginUser.getId());
            List<PostThumb> postPostThumbList = postThumbMapper.selectList(thumbQueryWrapper);
            postPostThumbList.forEach(postThumb-> postIdHasThumbMap.put(postThumb.getPostId(),true));
            //获取收藏
            QueryWrapper<PostFavour> postFavourQueryWrapper = new QueryWrapper<>();
            postFavourQueryWrapper.in("postId",postIdSet);
            postFavourQueryWrapper.eq("userId",loginUser.getId());
            List<PostFavour> postFavourList = postFavourMapper.selectList(postFavourQueryWrapper);
            postFavourList.forEach(postFavour ->postIdHasFavourMap.put(postFavour.getPostId(),true));
        }
        //填充信息
        List<PostVO> postVOList = postList.stream().map(post ->{
            PostVO postVO = PostVO.objToVo(post);
            Long userId = post.getUserId();
            User user = null;
            if(userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            postVO.setUser(userService.getUserVO(user));
            postVO.setHasThumb(postIdHasThumbMap.getOrDefault(post.getId(),false));
            postVO.setHasFavour(postIdHasFavourMap.getOrDefault(post.getId(),false));
            return postVO;
        }).collect(Collectors.toList());
        postVOPage.setRecords(postVOList);
        return postVOPage;
    }
    
}
