package com.lostsidewalk.buffy.app.post;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataConflictException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.PostPublisher;
import com.lostsidewalk.buffy.app.model.v1.request.*;
import com.lostsidewalk.buffy.post.*;
import com.lostsidewalk.buffy.post.StagingPost.PostPubStatus;
import com.lostsidewalk.buffy.publisher.Publisher.PubResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayListWithExpectedSize;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections4.CollectionUtils.*;

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

    private static List<ContentObject> convertContentObjectsToModel(List<ContentObjectConfigRequest> postContentConfigRequests) {
        List<ContentObject> postContents = null;
        if (isNotEmpty(postContentConfigRequests)) {
            postContents = new ArrayList<>();
            for (ContentObjectConfigRequest r : postContentConfigRequests) {
                postContents.add(ContentObject.from(r.getType(), r.getValue()));
            }
        }
        return postContents;
    }

    private static List<PostUrl> convertPostUrlsToModel(List<PostUrlConfigRequest> postUrlConfigRequests) {
        List<PostUrl> postUrls = null;
        if (isNotEmpty(postUrlConfigRequests)) {
            postUrls = new ArrayList<>();
            for (PostUrlConfigRequest r : postUrlConfigRequests) {
                postUrls.add(PostUrl.from(r.getTitle(), r.getType(), r.getHref(), r.getHreflang(), r.getRel()));
            }
        }
        return postUrls;
    }

    private static List<PostPerson> convertPersonsToModel(List<PostPersonConfigRequest> personConfigRequests) {
        List<PostPerson> persons = null;
        if (isNotEmpty(personConfigRequests)) {
            persons = new ArrayList<>();
            for (PostPersonConfigRequest r : personConfigRequests) {
                persons.add(PostPerson.from(r.getName(), r.getEmail(), r.getUri()));
            }
        }
        return persons;
    }

    private static List<PostEnclosure> convertEnclosuresToModel(List<PostEnclosureConfigRequest> postEnclosureConfigRequests) {
        List<PostEnclosure> enclosures = null;
        if (isNotEmpty(postEnclosureConfigRequests)) {
            enclosures = new ArrayList<>();
            for (PostEnclosureConfigRequest r : postEnclosureConfigRequests) {
                enclosures.add(PostEnclosure.from(r.getUrl(), r.getType(), r.getLength()));
            }
        }
        return enclosures;
    }

    public Long createPost(String username, Long queueId, PostConfigRequest postConfigRequest) throws DataAccessException, DataUpdateException, DataConflictException {
        //
        StagingPost stagingPost = StagingPost.from(
                "COMPOSABLE_RSS", // importer Id
                queueId,
                null, // importer desc
                null, // subscription Id
                ofNullable(postConfigRequest.getPostTitle()).map(ContentObjectConfigRequest::toContentObject).orElse(null),
                ofNullable(postConfigRequest.getPostDesc()).map(ContentObjectConfigRequest::toContentObject).orElse(null),
                convertContentObjectsToModel(postConfigRequest.getPostContents()),
                null, // post media
                postConfigRequest.getPostITunes(),
                postConfigRequest.getPostUrl(),
                convertPostUrlsToModel(postConfigRequest.getPostUrls()),
                null, // img url
                new Date(), // import timestamp
                null, // post hash
                username,
                postConfigRequest.getPostComment(),
                postConfigRequest.getPostRights(),
                convertPersonsToModel(postConfigRequest.getContributors()),
                convertPersonsToModel(postConfigRequest.getAuthors()),
                postConfigRequest.getPostCategories(),
                null, // publish timestamp
                postConfigRequest.getExpirationTimestamp(),
                convertEnclosuresToModel(postConfigRequest.getEnclosures()),
                null
        );
        return stagingPostDao.add(stagingPost);
    }

    public String addContent(String username, Long id, ContentObjectConfigRequest contentObjectConfigRequest) throws DataAccessException, DataUpdateException {
        String contentIdent = null;
        ContentObject newContentObject = ContentObject.from(
                contentObjectConfigRequest.getType(),
                contentObjectConfigRequest.getValue()
        );
        List<ContentObject> postContents = stagingPostDao.findById(username, id).getPostContents();
        if (postContents == null) {
            postContents = new ArrayList<>(singleton(newContentObject));
        } else {
            contentIdent = newContentObject.getIdent();
            postContents.add(newContentObject);
        }
        stagingPostDao.updatePostContents(username, id, postContents);
        return contentIdent;
    }

    public String addPostUrl(String username, Long id, PostUrlConfigRequest postUrlConfigRequest) throws DataAccessException, DataUpdateException {
        String postUrlIdent = null;
        PostUrl newPostUrl = PostUrl.from(
                postUrlConfigRequest.getTitle(),
                postUrlConfigRequest.getType(),
                postUrlConfigRequest.getHref(),
                postUrlConfigRequest.getHreflang(),
                postUrlConfigRequest.getRel()
        );
        List<PostUrl> postUrls = stagingPostDao.findById(username, id).getPostUrls();
        if (postUrls == null) {
            postUrls = new ArrayList<>(singleton(newPostUrl));
        } else {
            postUrlIdent = newPostUrl.getIdent();
            postUrls.add(newPostUrl);
        }
        stagingPostDao.updatePostUrls(username, id, postUrls);
        return postUrlIdent;
    }

    public String addContributor(String username, Long id, PostPersonConfigRequest contributorConfigRequest) throws DataAccessException, DataUpdateException {
        String contributorIdent = null;
        PostPerson newContributor = PostPerson.from(
                contributorConfigRequest.getName(),
                contributorConfigRequest.getEmail(),
                contributorConfigRequest.getUri()
        );
        List<PostPerson> contributors = stagingPostDao.findById(username, id).getContributors();
        if (contributors == null) {
            contributors = new ArrayList<>(singleton(newContributor));
        } else {
            contributorIdent = newContributor.getIdent();
            contributors.add(newContributor);
        }
        stagingPostDao.updateContributors(username, id, contributors);
        return contributorIdent;
    }

    public String addAuthor(String username, Long id, PostPersonConfigRequest authorConfigRequest) throws DataAccessException, DataUpdateException {
        String authorIdent = null;
        PostPerson newAuthor = PostPerson.from(
                authorConfigRequest.getName(),
                authorConfigRequest.getEmail(),
                authorConfigRequest.getUri()
        );
        List<PostPerson> authors = stagingPostDao.findById(username, id).getAuthors();
        if (authors == null) {
            authors = new ArrayList<>(singleton(newAuthor));
        } else {
            authorIdent = newAuthor.getIdent();
            authors.add(newAuthor);
        }
        stagingPostDao.updateAuthors(username, id, authors);
        return authorIdent;
    }

    public String addEnclosure(String username, Long id, PostEnclosureConfigRequest postEnclosureConfigRequest) throws DataAccessException, DataUpdateException {
        String enclosureIdent = null;
        PostEnclosure newEnclosure = PostEnclosure.from(
                postEnclosureConfigRequest.getUrl(),
                postEnclosureConfigRequest.getType(),
                postEnclosureConfigRequest.getLength()
        );
        List<PostEnclosure> postEnclosures = stagingPostDao.findById(username, id).getEnclosures();
        if (postEnclosures == null) {
            postEnclosures = new ArrayList<>(singleton(newEnclosure));
        } else {
            enclosureIdent = newEnclosure.getIdent();
            postEnclosures.add(newEnclosure);
        }
        stagingPostDao.updatePostEnclosures(username, id, postEnclosures);
        return enclosureIdent;
    }

    public void addCategory(String username, Long id, String category) throws DataAccessException, DataUpdateException {
        List<String> postCategories = stagingPostDao.findById(username, id).getPostCategories();
        if (postCategories == null) {
            postCategories = new ArrayList<>(singleton(category));
        } else if (!postCategories.contains(category)) {
            postCategories.add(category);
        }
        stagingPostDao.updatePostCategories(username, id, postCategories);
    }

    public StagingPost updatePost(String username, Long id, PostConfigRequest postUpdateRequest, boolean mergeUpdate) throws DataAccessException, DataUpdateException {
        stagingPostDao.updatePost(
                mergeUpdate,
                username,
                id,
                ofNullable(postUpdateRequest.getPostTitle()).map(ContentObjectConfigRequest::toContentObject).orElse(null),
                ofNullable(postUpdateRequest.getPostDesc()).map(ContentObjectConfigRequest::toContentObject).orElse(null),
                convertContentObjectsToModel(postUpdateRequest.getPostContents()),
                null,
                postUpdateRequest.getPostITunes(),
                postUpdateRequest.getPostUrl(),
                convertPostUrlsToModel(postUpdateRequest.getPostUrls()),
                null,
                postUpdateRequest.getPostComment(),
                postUpdateRequest.getPostRights(),
                convertPersonsToModel(postUpdateRequest.getAuthors()),
                convertPersonsToModel(postUpdateRequest.getContributors()),
                postUpdateRequest.getPostCategories(),
                postUpdateRequest.getExpirationTimestamp(),
                convertEnclosuresToModel(postUpdateRequest.getEnclosures())
        );
        return stagingPostDao.findById(username, id);
    }

    public Map<String, PubResult> updatePostPubStatus(String username, Long id, PostPubStatus newStatus) throws DataAccessException, DataUpdateException {
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
    public Map<String, PubResult> updateQueuePubStatus(String username, Long id, PostPubStatus newStatus) throws DataAccessException, DataUpdateException {
        //
        // perform the update
        //
        stagingPostDao.updateQueuePubStatus(username, id, newStatus);
        //
        // deploy the queue
        //
        return postPublisher.publishFeed(username, id);
    }

    public ContentObject updatePostTitle(String username, Long id, ContentObject newPostTitle, boolean mergeUpdate) throws DataAccessException, DataUpdateException {
        stagingPostDao.updatePostTitle(mergeUpdate, username, id, newPostTitle);
        return stagingPostDao.findById(username, id).getPostTitle();
    }

    public ContentObject updatePostDesc(String username, Long id, ContentObject newPostDesc, boolean mergeUpdate) throws DataAccessException, DataUpdateException {
        stagingPostDao.updatePostDesc(mergeUpdate, username, id, newPostDesc);
        return stagingPostDao.findById(username, id).getPostDesc();
    }

    public PostITunes updatePostITunes(String username, Long id, PostITunes newPostITunes, boolean mergeUpdate) throws DataAccessException, DataUpdateException {
        stagingPostDao.updatePostITunes(mergeUpdate, username, id, newPostITunes);
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

    public List<String> updatePostCategories(String username, Long id, List<String> newPostCategories) throws DataAccessException, DataUpdateException {
        stagingPostDao.updatePostCategories(username, id, newPostCategories);
        return stagingPostDao.findById(username, id).getPostCategories();
    }

    public Date updateExpirationTimestamp(String username, Long id, Date newExpirationTimestamp) throws DataAccessException, DataUpdateException {
        stagingPostDao.updateExpirationTimestamp(username, id, newExpirationTimestamp);
        return stagingPostDao.findById(username, id).getExpirationTimestamp();
    }

    public void updatePostMedia(String username, Long id, PostMedia postMedia, boolean mergeUpdate) throws DataAccessException, DataUpdateException {
        stagingPostDao.updatePostMedia(mergeUpdate, username, id, postMedia);
    }

    //
    //
    //

    public void updateContent(String username, Long id, String contentIdent, ContentObjectConfigRequest contentObject, boolean mergeUpdate) throws DataAccessException, DataUpdateException {
        List<ContentObject> postContents = stagingPostDao.findById(username, id).getPostContents();
        ContentObject oldContent = postContents.stream()
                .filter(c -> c.getIdent().equals(contentIdent))
                .findFirst()
                .orElseThrow(() -> new DataAccessException(getClass().getSimpleName(), "updateContent", "Content not found by Ident=" + contentIdent, username, id, contentIdent, contentIdent));
        oldContent.update(contentObject.getType(), contentObject.getValue());
        stagingPostDao.updatePostContents(username, id, postContents);
    }

    public void updateContents(String username, Long id, List<ContentObjectConfigRequest> contentObjectConfigRequests, boolean mergeUpdate) throws DataAccessException, DataUpdateException {
        List<ContentObject> contents = newArrayListWithExpectedSize(size(contentObjectConfigRequests));
        for (ContentObjectConfigRequest contentConfigRequest : contentObjectConfigRequests) {
            ContentObject newContent = ContentObject.from(
                    contentConfigRequest.getType(),
                    contentConfigRequest.getValue()
            );
            contents.add(newContent);
        }
        stagingPostDao.updatePostContents(username, id, contents);
    }

    public void updatePostUrl(String username, Long id, String postUrlIdent, PostUrlConfigRequest postUrl, boolean mergeUpdate) throws DataAccessException, DataUpdateException {
        List<PostUrl> postUrls = stagingPostDao.findById(username, id).getPostUrls();
        PostUrl oldPostUrl = postUrls.stream()
                .filter(u -> u.getIdent().equals(postUrlIdent))
                .findFirst()
                .orElseThrow(() -> new DataAccessException(getClass().getSimpleName(), "updatePostUrl", "Post URL not found by Ident=" + postUrlIdent, username, id, postUrlIdent, postUrl));
        oldPostUrl.update(
                postUrl.getTitle(),
                postUrl.getType(),
                postUrl.getHref(),
                postUrl.getHreflang(),
                postUrl.getRel()
        );
        stagingPostDao.updatePostUrls(username, id, postUrls);
    }

    public void updatePostUrls(String username, Long id, List<PostUrlConfigRequest> postUrlConfigRequests, boolean mergeUpdate) throws DataAccessException, DataUpdateException {
        List<PostUrl> postUrls = newArrayListWithExpectedSize(size(postUrlConfigRequests));
        for (PostUrlConfigRequest postUrlConfigRequest : postUrlConfigRequests) {
            PostUrl newPostUrl = PostUrl.from(
                    postUrlConfigRequest.getTitle(),
                    postUrlConfigRequest.getType(),
                    postUrlConfigRequest.getHref(),
                    postUrlConfigRequest.getHreflang(),
                    postUrlConfigRequest.getRel()
            );
            postUrls.add(newPostUrl);
        }
        stagingPostDao.updatePostUrls(username, id, postUrls);
    }

    public void updateContributor(String username, Long id, String contributorIdent, PostPersonConfigRequest contributor, boolean mergeUpdate) throws DataAccessException, DataUpdateException {
        List<PostPerson> contributors = stagingPostDao.findById(username, id).getContributors();
        PostPerson oldContributor = contributors.stream()
                .filter(a -> a.getIdent().equals(contributorIdent))
                .findFirst()
                .orElseThrow(() -> new DataAccessException(getClass().getSimpleName(), "updateContributor", "Contributor not found by Ident=" + contributorIdent, username, id, contributorIdent, contributor));
        oldContributor.update(contributor.getName(), contributor.getEmail(), contributor.getUri());
        stagingPostDao.updateContributors(username, id, contributors);
    }

    public void updateContributors(String username, Long id, List<PostPersonConfigRequest> contributorConfigRequests, boolean mergeUpdate) throws DataAccessException, DataUpdateException {
        List<PostPerson> contributors = newArrayListWithExpectedSize(size(contributorConfigRequests));
        for (PostPersonConfigRequest contributorConfigRequest : contributorConfigRequests) {
            PostPerson newContributor = PostPerson.from(
                    contributorConfigRequest.getName(),
                    contributorConfigRequest.getEmail(),
                    contributorConfigRequest.getUri()
            );
            contributors.add(newContributor);
        }
        stagingPostDao.updateContributors(username, id, contributors);
    }

    public void updateAuthor(String username, Long id, String authorIdent, PostPersonConfigRequest author, boolean mergeUpdate) throws DataAccessException, DataUpdateException {
        List<PostPerson> authors = stagingPostDao.findById(username, id).getAuthors();
        PostPerson oldAuthor = authors.stream()
                .filter(a -> a.getIdent().equals(authorIdent))
                .findFirst()
                        .orElseThrow(() -> new DataAccessException(getClass().getSimpleName(), "updateAuthor", "Author not found by Ident=" + authorIdent, username, id, authorIdent, author));
        oldAuthor.update(author.getName(), author.getEmail(), author.getUri());
        stagingPostDao.updateAuthors(username, id, authors);
    }

    public void updateAuthors(String username, Long id, List<PostPersonConfigRequest> authorConfigRequests, boolean mergeUpdate) throws DataAccessException, DataUpdateException {
        List<PostPerson> authors = newArrayListWithExpectedSize(size(authorConfigRequests));
        for (PostPersonConfigRequest authorConfigRequest : authorConfigRequests) {
            PostPerson newAuthor = PostPerson.from(
                    authorConfigRequest.getName(),
                    authorConfigRequest.getEmail(),
                    authorConfigRequest.getUri()
            );
            authors.add(newAuthor);
        }
        stagingPostDao.updateAuthors(username, id, authors);
    }

    public void updateEnclosure(String username, Long id, String enclosureIdent, PostEnclosureConfigRequest enclosure, boolean mergeUpdate) throws DataAccessException, DataUpdateException {
        List<PostEnclosure> enclosures = stagingPostDao.findById(username, id).getEnclosures();
        PostEnclosure oldEnclosure = enclosures.stream()
                .filter(e -> e.getIdent().equals(enclosureIdent))
                .findFirst()
                .orElseThrow(() -> new DataAccessException(getClass().getSimpleName(), "updateEnclosure", "Enclosure not found by Ident=" + enclosureIdent, username, id, enclosure, enclosure));
        oldEnclosure.update(enclosure.getUrl(), enclosure.getType(), enclosure.getLength());
        stagingPostDao.updatePostEnclosures(username, id, enclosures);
    }

    public void updateEnclosures(String username, Long id, List<PostEnclosureConfigRequest> enclosureConfigRequests, boolean mergeUpdate) throws DataAccessException, DataUpdateException {
        List<PostEnclosure> enclosures = newArrayListWithExpectedSize(size(enclosureConfigRequests));
        for (PostEnclosureConfigRequest enclosureConfigRequest : enclosureConfigRequests) {
            PostEnclosure newEnclosure = PostEnclosure.from(
                    enclosureConfigRequest.getUrl(),
                    enclosureConfigRequest.getType(),
                    enclosureConfigRequest.getLength()
            );
            enclosures.add(newEnclosure);
        }
        stagingPostDao.updatePostEnclosures(username, id, enclosures);
    }

    public StagingPost findById(String username, Long id) throws DataAccessException {
        return stagingPostDao.findById(username, id);
    }

    public PostPerson findAuthorByIdent(String username, Long id, String authorIdent) throws DataAccessException {
        StagingPost s = findById(username, id);
        List<PostPerson> postAuthors = s.getAuthors();
        PostPerson postAuthor = null;
        if (isNotEmpty(postAuthors)) {
            for (PostPerson a : postAuthors) {
                if (a.getIdent().equals(authorIdent)) {
                    postAuthor = a;
                    break;
                }
            }
        }
        return postAuthor;
    }

    public PostPerson findContributorByIdent(String username, Long id, String contributorIdent) throws DataAccessException {
        StagingPost s = findById(username, id);
        List<PostPerson> postContributors = s.getContributors();
        PostPerson postContributor = null;
        if (isNotEmpty(postContributors)) {
            for (PostPerson a : postContributors) {
                if (a.getIdent().equals(contributorIdent)) {
                    postContributor = a;
                    break;
                }
            }
        }
        return postContributor;
    }

    public ContentObject findContentByIdent(String username, Long id, String contentIdent) throws DataAccessException {
        StagingPost s = findById(username, id);
        List<ContentObject> postContents = s.getPostContents();
        ContentObject postContent = null;
        if (isNotEmpty(postContents)) {
            for (ContentObject c : postContents) {
                if (c.getIdent().equals(contentIdent)) {
                    postContent = c;
                    break;
                }
            }
        }
        return postContent;
    }

    public PostEnclosure findEnclosureByIdent(String username, Long id, String enclosureIdent) throws DataAccessException {
        StagingPost s = findById(username, id);
        List<PostEnclosure> postEnclosures = s.getEnclosures();
        PostEnclosure postEnclosure = null;
        if (isNotEmpty(postEnclosures)) {
            for (PostEnclosure e : postEnclosures) {
                if (e.getIdent().equals(enclosureIdent)) {
                    postEnclosure = e;
                    break;
                }
            }
        }
        return postEnclosure;
    }

    public PostUrl findUrlByIdent(String username, Long id, String urlIdent) throws DataAccessException {
        StagingPost s = findById(username, id);
        List<PostUrl> postUrls = s.getPostUrls();
        PostUrl postUrl = null;
        if (isNotEmpty(postUrls)) {
            for (PostUrl e : postUrls) {
                if (e.getIdent().equals(urlIdent)) {
                    postUrl = e;
                    break;
                }
            }
        }
        return postUrl;
    }

    //

    public void deleteByQueueId(String username, Long queueId) throws DataAccessException, DataUpdateException {
        stagingPostDao.deleteByQueueId(username, queueId);
    }

    public void deleteById(String username, Long id) throws DataAccessException, DataUpdateException {
        stagingPostDao.deleteById(username, id);
    }

    //

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

    public void clearPostMedia(String username, Long id) throws DataAccessException, DataUpdateException {
        stagingPostDao.clearPostMedia(username, id);
    }

    public void clearPostCategories(String username, Long id) throws DataAccessException, DataUpdateException {
        stagingPostDao.clearPostCategories(username, id);
    }

    //

    public void deletePostContents(String username, Long id) throws DataAccessException, DataUpdateException {
        stagingPostDao.clearPostContents(username, id);
    }

    public void deleteContent(String username, Long id, String contentIdent) throws DataAccessException, DataUpdateException {
        List<ContentObject> postContents = stagingPostDao.findById(username, id).getPostContents();
        if (postContents.removeIf(c -> c.getIdent().equals(contentIdent))) {
            stagingPostDao.updatePostContents(username, id, postContents);
        } else {
            throw new DataAccessException(getClass().getSimpleName(), "deleteContent", "Content not found by Ident=" + contentIdent, username, id, contentIdent);
        }
    }

    //

    public void deletePostUrls(String username, Long id) throws DataAccessException, DataUpdateException {
        stagingPostDao.clearPostUrls(username, id);
    }

    public void deletePostUrl(String username, Long id, String postUrlIdent) throws DataAccessException, DataUpdateException {
        List<PostUrl> postUrls = stagingPostDao.findById(username, id).getPostUrls();
        if (postUrls.removeIf(u -> u.getIdent().equals(postUrlIdent))) {
            stagingPostDao.updatePostUrls(username, id, postUrls);
        } else {
            throw new DataAccessException(getClass().getSimpleName(), "deletePostUrl", "Post URL not found by Ident=" + postUrlIdent, username, id, postUrlIdent);
        }
    }

    //

    public void deletePostAuthors(String username, Long id) throws DataAccessException, DataUpdateException {
        stagingPostDao.clearPostAuthors(username, id);
    }

    public void deleteAuthor(String username, Long id, String authorIdent) throws DataAccessException, DataUpdateException {
        List<PostPerson> authors = stagingPostDao.findById(username, id).getAuthors();
        if (authors.removeIf(a -> a.getIdent().equals(authorIdent))) {
            stagingPostDao.updateAuthors(username, id, authors);
        } else {
            throw new DataAccessException(getClass().getSimpleName(), "deleteAuthor", "Author not found by Ident=" + authorIdent, username, id, authorIdent);
        }
    }

    //

    public void deleteContributors(String username, Long id) throws DataAccessException, DataUpdateException {
        stagingPostDao.clearPostContributors(username, id);
    }

    public void deleteContributor(String username, Long id, String contributorIdent) throws DataAccessException, DataUpdateException {
        List<PostPerson> contributors = stagingPostDao.findById(username, id).getContributors();
        if (contributors.removeIf(a -> a.getIdent().equals(contributorIdent))) {
            stagingPostDao.updateContributors(username, id, contributors);
        } else {
            throw new DataAccessException(getClass().getSimpleName(), "deleteContributor", "Contributor not found by Ident=" + contributorIdent, username, id, contributorIdent);
        }
    }

    //

    public void deleteEnclosures(String username, Long id) throws DataAccessException, DataUpdateException {
        stagingPostDao.clearPostEnclosures(username, id);
    }

    public void deleteEnclosure(String username, Long id, String enclosureIdent) throws DataAccessException, DataUpdateException {
        List<PostEnclosure> enclosures = stagingPostDao.findById(username, id).getEnclosures();
        if (enclosures.removeIf(e -> e.getIdent().equals(enclosureIdent))) {
            stagingPostDao.updatePostEnclosures(username, id, enclosures);
        } else {
            throw new DataAccessException(getClass().getSimpleName(), "deleteEnclosure", "Enclosure not found by Ident=" + enclosureIdent, username, id, enclosureIdent);
        }
    }
}
