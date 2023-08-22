package com.lostsidewalk.buffy.app.post;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.PostPublisher;
import com.lostsidewalk.buffy.app.model.request.PostConfigRequest;
import com.lostsidewalk.buffy.post.*;
import com.lostsidewalk.buffy.post.StagingPost.PostPubStatus;
import com.lostsidewalk.buffy.publisher.Publisher.PubResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

@Service
public class StagingPostService {

    @Autowired
    StagingPostDao stagingPostDao;

    @Autowired
    PostPublisher postPublisher;

    public List<StagingPost> getStagingPosts(String username, List<Long> queueIds) throws DataAccessException {
        List<StagingPost> list;
        if (isEmpty(queueIds)) {
            list = stagingPostDao.findByUser(username);
        } else {
            list = stagingPostDao.findByUserAndQueueIds(username, queueIds);
        }
        if (list != null) {
            return list;
        }
        return emptyList();
    }

    public Long createPost(String username, Long queueId, PostConfigRequest postConfigRequest) throws DataAccessException, DataUpdateException {
        StagingPost stagingPost = StagingPost.from(
                "COMPOSABLE_RSS", // importer Id
                queueId,
                null, // importer desc
                null, // subscription Id
                postConfigRequest.getPostTitle(),
                postConfigRequest.getPostDesc(),
                postConfigRequest.getPostContents(),
                null, // post media
                postConfigRequest.getPostITunes(),
                postConfigRequest.getPostUrl(),
                postConfigRequest.getPostUrls(),
                null, // img url
                new Date(), // import timestamp
                null, // post hash
                username,
                postConfigRequest.getPostComment(),
                postConfigRequest.getPostRights(),
                postConfigRequest.getContributors(),
                postConfigRequest.getAuthors(),
                postConfigRequest.getPostCategories(),
                null, // publish timestamp
                postConfigRequest.getExpirationTimestamp(),
                postConfigRequest.getEnclosures(),
                null
        );
        return stagingPostDao.add(stagingPost);
    }

    public void addContent(String username, Long id, ContentObject contentObject) throws DataAccessException, DataUpdateException {
        List<ContentObject> postContents = stagingPostDao.findById(username, id).getPostContents();
        postContents.add(contentObject);
        stagingPostDao.updatePostContents(username, id, postContents);
    }

    public void addPostUrl(String username, Long id, PostUrl postUrl) throws DataAccessException, DataUpdateException {
        List<PostUrl> postUrls = stagingPostDao.findById(username, id).getPostUrls();
        postUrls.add(postUrl);
        stagingPostDao.updatePostUrls(username, id, postUrls);
    }

    public void addContributor(String username, Long id, PostPerson contributor) throws DataAccessException, DataUpdateException {
        List<PostPerson> contributors = stagingPostDao.findById(username, id).getContributors();
        contributors.add(contributor);
        stagingPostDao.updateContributors(username, id, contributors);
    }

    public void addAuthor(String username, Long id, PostPerson author) throws DataAccessException, DataUpdateException {
        List<PostPerson> authors = stagingPostDao.findById(username, id).getAuthors();
        authors.add(author);
        stagingPostDao.updateAuthors(username, id, authors);
    }

    public void addEnclosure(String username, Long id, PostEnclosure postEnclosure) throws DataAccessException, DataUpdateException {
        List<PostEnclosure> postEnclosures = stagingPostDao.findById(username, id).getEnclosures();
        postEnclosures.add(postEnclosure);
        stagingPostDao.updatePostEnclosures(username, id, postEnclosures);
    }

    public StagingPost updatePost(String username, Long id, PostConfigRequest postUpdateRequest) throws DataAccessException, DataUpdateException {
        stagingPostDao.updatePost(
                username,
                id,
                postUpdateRequest.getPostTitle(),
                postUpdateRequest.getPostDesc(),
                postUpdateRequest.getPostContents(),
                null,
                postUpdateRequest.getPostITunes(),
                postUpdateRequest.getPostUrl(),
                postUpdateRequest.getPostUrls(),
                null,
                postUpdateRequest.getPostComment(),
                postUpdateRequest.getPostRights(),
                postUpdateRequest.getContributors(),
                postUpdateRequest.getAuthors(),
                postUpdateRequest.getPostCategories(),
                postUpdateRequest.getExpirationTimestamp(),
                postUpdateRequest.getEnclosures() );
        return stagingPostDao.findById(username, id);
    }

    public List<PubResult> updatePostPubStatus(String username, Long id, PostPubStatus newStatus) throws DataAccessException, DataUpdateException {
        //
        // perform the update
        //
        stagingPostDao.updatePostPubStatus(username, id, newStatus);
        //
        // deploy the queue
        //
        Long queueId = stagingPostDao.findQueueIdByStagingPostId(username, id);
        return postPublisher.publishFeed(username, queueId);
    }

    @SuppressWarnings("UnusedReturnValue")
    public List<PubResult> updateQueuePubStatus(String username, Long id, PostPubStatus newStatus) throws DataAccessException, DataUpdateException {
        //
        // perform the update
        //
        stagingPostDao.updateQueuePubStatus(username, id, newStatus);
        //
        // deploy the queue
        //
        return postPublisher.publishFeed(username, id);
    }

    public ContentObject updatePostTitle(String username, Long id, ContentObject newPostTitle) throws DataAccessException, DataUpdateException {
        stagingPostDao.updatePostTitle(username, id, newPostTitle);
        return stagingPostDao.findById(username, id).getPostTitle();
    }

    public ContentObject updatePostDesc(String username, Long id, ContentObject newPostDesc) throws DataAccessException, DataUpdateException {
        stagingPostDao.updatePostDesc(username, id, newPostDesc);
        return stagingPostDao.findById(username, id).getPostDesc();
    }

    public PostITunes updatePostITunes(String username, Long id, PostITunes newPostITunes) throws DataAccessException, DataUpdateException {
        stagingPostDao.updatePostITunes(username, id, newPostITunes);
        return stagingPostDao.findById(username, id).getPostITunes();
    }

    public String updatePostComment(String username, Long id, String newPostComment) throws DataAccessException, DataUpdateException {
        stagingPostDao.updatePostComment(username, id, newPostComment);
        return stagingPostDao.findById(username, id).getPostComment();
    }

    public String updatePostRights(String username, Long id, String newPostRights) throws DataAccessException, DataUpdateException {
        stagingPostDao.updatePostRights(username, id, newPostRights);
        return stagingPostDao.findById(username, id).getPostRights();
    }

    public Date updateExpirationTimestamp(String username, Long id, Date newExpirationTimestamp) throws DataAccessException, DataUpdateException {
        stagingPostDao.updateExpirationTimestamp(username, id, newExpirationTimestamp);
        return stagingPostDao.findById(username, id).getExpirationTimestamp();
    }

    public void updatePostMedia(String username, Long postId, PostMedia postMedia) throws DataAccessException, DataUpdateException {
        stagingPostDao.updatePostMedia(username, postId, postMedia);
    }

    public void updatePostContent(String username, Long id, Integer contentIdx, ContentObject contentObject) throws DataAccessException, DataUpdateException {
        List<ContentObject> postContents = stagingPostDao.findById(username, id).getPostContents();
        postContents.set(contentIdx, contentObject);
        stagingPostDao.updatePostContents(username, id, postContents);
    }

    public void updatePostUrl(String username, Long id, Integer urlIdx, PostUrl postUrl) throws DataAccessException, DataUpdateException {
        List<PostUrl> postUrls = stagingPostDao.findById(username, id).getPostUrls();
        postUrls.set(urlIdx, postUrl);
        stagingPostDao.updatePostUrls(username, id, postUrls);
    }

    public void updatePostContributor(String username, Long id, Integer urlIdx, PostPerson contributor) throws DataAccessException, DataUpdateException {
        List<PostPerson> contributors = stagingPostDao.findById(username, id).getContributors();
        contributors.set(urlIdx, contributor);
        stagingPostDao.updateContributors(username, id, contributors);
    }

    public void updatePostAuthor(String username, Long id, Integer urlIdx, PostPerson author) throws DataAccessException, DataUpdateException {
        List<PostPerson> authors = stagingPostDao.findById(username, id).getAuthors();
        authors.set(urlIdx, author);
        stagingPostDao.updateAuthors(username, id, authors);
    }

    public void updatePostEnclosure(String username, Long id, Integer enclosureIdx, PostEnclosure enclosure) throws DataAccessException, DataUpdateException {
        List<PostEnclosure> enclosures = stagingPostDao.findById(username, id).getEnclosures();
        enclosures.set(enclosureIdx, enclosure);
        stagingPostDao.updatePostEnclosures(username, id, enclosures);
    }

    public StagingPost findById(String username, Long id) throws DataAccessException {
        return stagingPostDao.findById(username, id);
    }

    public void deletePost(String username, Long id) throws DataAccessException, DataUpdateException {
        stagingPostDao.deleteById(username, id);
    }

    public void clearPostITunes(String username, Long id) throws DataAccessException, DataUpdateException {
        stagingPostDao.clearPostITunes(username, id);
    }

    public void clearPostComment(String username, Long id) throws DataAccessException, DataUpdateException {
        stagingPostDao.clearPostComment(username, id);
    }

    public void clearPostRights(String username, Long id) throws DataAccessException, DataUpdateException {
        stagingPostDao.clearPostRights(username, id);
    }

    public void clearExpirationTimestamp(String username, Long id) throws DataAccessException, DataUpdateException {
        stagingPostDao.clearExpirationTimestamp(username, id);
    }

    public void clearPostMedia(String username, Long postId) throws DataAccessException, DataUpdateException {
        stagingPostDao.clearPostMedia(username, postId);
    }

    public void deletePostContent(String username, Long id, Integer contentIdx) throws DataAccessException, DataUpdateException {
        List<ContentObject> postContents = stagingPostDao.findById(username, id).getPostContents();
        postContents.remove(contentIdx.intValue());
        stagingPostDao.updatePostContents(username, id, postContents);
    }

    public void deletePostUrl(String username, Long id, Integer urlIdx) throws DataAccessException, DataUpdateException {
        List<PostUrl> postUrls = stagingPostDao.findById(username, id).getPostUrls();
        postUrls.remove(urlIdx.intValue());
        stagingPostDao.updatePostUrls(username, id, postUrls);
    }

    public void deletePostContributor(String username, Long id, Integer contributorIdx) throws DataAccessException, DataUpdateException {
        List<PostPerson> contributors = stagingPostDao.findById(username, id).getContributors();
        contributors.remove(contributorIdx.intValue());
        stagingPostDao.updateContributors(username, id, contributors);
    }

    public void deletePostAuthor(String username, Long id, Integer authorIdx) throws DataAccessException, DataUpdateException {
        List<PostPerson> authors = stagingPostDao.findById(username, id).getAuthors();
        authors.remove(authorIdx.intValue());
        stagingPostDao.updateAuthors(username, id, authors);
    }

    public void deletePostEnclosure(String username, Long id, Integer enclosureIdx) throws DataAccessException, DataUpdateException {
        List<PostEnclosure> enclosures = stagingPostDao.findById(username, id).getEnclosures();
        enclosures.remove(enclosureIdx.intValue());
        stagingPostDao.updatePostEnclosures(username, id, enclosures);
    }
}
