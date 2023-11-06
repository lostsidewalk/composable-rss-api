package com.lostsidewalk.buffy.app.post;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataConflictException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.model.v1.request.*;
import com.lostsidewalk.buffy.post.*;
import com.lostsidewalk.buffy.post.StagingPost.PostPubStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newArrayListWithExpectedSize;
import static java.util.Collections.singleton;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.*;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

@SuppressWarnings({"NestedMethodCall", "ChainedMethodCall"})
@Slf4j
@Service
public class StagingPostService {

    @Autowired
    StagingPostDao stagingPostDao;

    public final List<StagingPost> getStagingPosts(String username, List<Long> queueIds, PostPubStatus ... statuses) throws DataAccessException {
        List<StagingPost> list;
        if (isEmpty(queueIds)) {
            list = stagingPostDao.findByUser(username);
        } else {
            list = stagingPostDao.findByUserAndQueueIds(username, queueIds);
        }
        if (ArrayUtils.isNotEmpty(statuses)) {
            list = list.stream()
                    .filter(stagingPost -> stagingPost.getPostPubStatus() != null)
                    .filter(stagingPost -> ArrayUtils.contains(statuses, stagingPost.getPostPubStatus()))
                    .collect(toList());
        }
        return list;
    }

    private static List<ContentObject> convertContentObjectsToModel(Collection<? extends ContentObjectConfigRequest> postContentConfigRequests) {
        List<ContentObject> postContents = null;
        if (isNotEmpty(postContentConfigRequests)) {
            int size = size(postContentConfigRequests);
            postContents = new ArrayList<>(size);
            for (ContentObjectConfigRequest r : postContentConfigRequests) {
                String ident = randomAlphanumeric(8);
                String type = r.getType();
                String value = r.getValue();
                ContentObject contentObject = ContentObject.from(ident, type, value);
                postContents.add(contentObject);
            }
        }
        return postContents;
    }

    private static List<PostUrl> convertPostUrlsToModel(Collection<? extends PostUrlConfigRequest> postUrlConfigRequests) {
        List<PostUrl> postUrls = null;
        if (isNotEmpty(postUrlConfigRequests)) {
            int size = size(postUrlConfigRequests);
            postUrls = new ArrayList<>(size);
            for (PostUrlConfigRequest r : postUrlConfigRequests) {
                String ident = randomAlphanumeric(8);
                String title = r.getTitle();
                String type = r.getType();
                String href = r.getHref();
                String hreflang = r.getHreflang();
                String rel = r.getRel();
                PostUrl postUrl = PostUrl.from(ident, title, type, href, hreflang, rel);
                postUrls.add(postUrl);
            }
        }
        return postUrls;
    }

    private static List<PostPerson> convertPersonsToModel(Collection<? extends PostPersonConfigRequest> personConfigRequests) {
        List<PostPerson> persons = null;
        if (isNotEmpty(personConfigRequests)) {
            int size = size(personConfigRequests);
            persons = new ArrayList<>(size);
            for (PostPersonConfigRequest r : personConfigRequests) {
                String ident = randomAlphanumeric(8);
                persons.add(PostPerson.from(ident, r.getName(), r.getEmail(), r.getUri()));
            }
        }
        return persons;
    }

    private static List<PostEnclosure> convertEnclosuresToModel(Collection<? extends PostEnclosureConfigRequest> postEnclosureConfigRequests) {
        List<PostEnclosure> enclosures = null;
        if (isNotEmpty(postEnclosureConfigRequests)) {
            int size = size(postEnclosureConfigRequests);
            enclosures = new ArrayList<>(size);
            for (PostEnclosureConfigRequest r : postEnclosureConfigRequests) {
                enclosures.add(PostEnclosure.from(randomAlphanumeric(8), r.getUrl(), r.getType(), r.getLength()));
            }
        }
        return enclosures;
    }

    public final Long createPost(String username, Long queueId, PostConfigRequest postConfigRequest) throws DataAccessException, DataUpdateException, DataConflictException {
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

    public final String addContent(String username, Long id, ContentObjectConfigRequest contentObjectConfigRequest) throws DataAccessException, DataUpdateException {
        String ident = randomAlphanumeric(8);
        ContentObject newContentObject = ContentObject.from(
                ident,
                contentObjectConfigRequest.getType(),
                contentObjectConfigRequest.getValue()
        );
        StagingPost byId = stagingPostDao.findById(username, id);
        List<ContentObject> postContents;
        List<ContentObject> immutablePostContents = byId.getPostContents();
        if (immutablePostContents == null) {
            postContents = new ArrayList<>(singleton(newContentObject));
        } else {
            postContents = newArrayList(immutablePostContents);
            postContents.add(newContentObject);
        }
        stagingPostDao.updatePostContents(username, id, postContents);
        return ident;
    }

    public final String addPostUrl(String username, Long id, PostUrlConfigRequest postUrlConfigRequest) throws DataAccessException, DataUpdateException {
        String ident = randomAlphanumeric(8);
        PostUrl newPostUrl = PostUrl.from(
                ident,
                postUrlConfigRequest.getTitle(),
                postUrlConfigRequest.getType(),
                postUrlConfigRequest.getHref(),
                postUrlConfigRequest.getHreflang(),
                postUrlConfigRequest.getRel()
        );
        StagingPost byId = stagingPostDao.findById(username, id);
        List<PostUrl> postUrls;
        List<PostUrl> immutablePostUrls = byId.getPostUrls();
        if (immutablePostUrls == null) {
            postUrls = new ArrayList<>(singleton(newPostUrl));
        } else {
            postUrls = newArrayList(immutablePostUrls);
            postUrls.add(newPostUrl);
        }
        stagingPostDao.updatePostUrls(username, id, postUrls);
        return ident;
    }

    public final String addContributor(String username, Long id, PostPersonConfigRequest contributorConfigRequest) throws DataAccessException, DataUpdateException {
        String ident = randomAlphanumeric(8);
        PostPerson newContributor = PostPerson.from(
                ident,
                contributorConfigRequest.getName(),
                contributorConfigRequest.getEmail(),
                contributorConfigRequest.getUri()
        );
        StagingPost byId = stagingPostDao.findById(username, id);
        List<PostPerson> postContributors;
        List<PostPerson> immutableContributors = byId.getContributors();
        if (immutableContributors == null) {
            postContributors = new ArrayList<>(singleton(newContributor));
        } else {
            postContributors = newArrayList(immutableContributors);
            postContributors.add(newContributor);
        }
        stagingPostDao.updateContributors(username, id, postContributors);
        return ident;
    }

    public final String addAuthor(String username, Long id, PostPersonConfigRequest authorConfigRequest) throws DataAccessException, DataUpdateException {
        String ident = randomAlphanumeric(8);
        PostPerson newAuthor = PostPerson.from(
                ident,
                authorConfigRequest.getName(),
                authorConfigRequest.getEmail(),
                authorConfigRequest.getUri()
        );
        StagingPost byId = stagingPostDao.findById(username, id);
        List<PostPerson> postAuthors;
        List<PostPerson> immutableAuthors = byId.getAuthors();
        if (immutableAuthors == null) {
            postAuthors = new ArrayList<>(singleton(newAuthor));
        } else {
            postAuthors = newArrayList(immutableAuthors);
            postAuthors.add(newAuthor);
        }
        stagingPostDao.updateAuthors(username, id, postAuthors);
        return ident;
    }

    public final String addEnclosure(String username, Long id, PostEnclosureConfigRequest postEnclosureConfigRequest) throws DataAccessException, DataUpdateException {
        String ident = randomAlphanumeric(8);
        PostEnclosure newEnclosure = PostEnclosure.from(
                ident,
                postEnclosureConfigRequest.getUrl(),
                postEnclosureConfigRequest.getType(),
                postEnclosureConfigRequest.getLength()
        );
        StagingPost byId = stagingPostDao.findById(username, id);
        List<PostEnclosure> postEnclosures;
        List<PostEnclosure> immutableEnclosures = byId.getEnclosures();
        if (immutableEnclosures == null) {
            postEnclosures = new ArrayList<>(singleton(newEnclosure));
        } else {
            postEnclosures = newArrayList(immutableEnclosures);
            postEnclosures.add(newEnclosure);
        }
        stagingPostDao.updatePostEnclosures(username, id, postEnclosures);
        return ident;
    }

    @SuppressWarnings("unused")
    public final void addCategory(String username, Long id, String category) throws DataAccessException, DataUpdateException {
        StagingPost byId = stagingPostDao.findById(username, id);
        List<String> postCategories;
        List<String> immutableCategories = byId.getPostCategories();
        if (immutableCategories == null) {
            postCategories = new ArrayList<>(singleton(category));
        } else if (!immutableCategories.contains(category)) {
            postCategories = newArrayList(immutableCategories);
            postCategories.add(category);
        } else {
            postCategories = newArrayList(immutableCategories);
        }
        stagingPostDao.updatePostCategories(username, id, postCategories);
    }

    public final StagingPost updatePost(String username, Long id, PostConfigRequest postUpdateRequest, Boolean mergeUpdate) throws DataAccessException, DataUpdateException {
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

    public final StagingPost updatePostPubStatus(String username, Long id, PostPubStatus newStatus) throws DataAccessException, DataUpdateException {
        //
        // perform the update
        //
        stagingPostDao.updatePostPubStatus(username, id, newStatus);
        return stagingPostDao.findById(username, id);
    }

    public final List<StagingPost> updatePostPubStatus(String username, List<Long> ids, PostPubStatus newStatus) throws DataAccessException, DataUpdateException {
        stagingPostDao.updatePostPubStatus(username, ids, newStatus);
        return stagingPostDao.findByIds(username, ids);
    }

    public final StagingPost updatePostTitle(String username, Long id, ContentObject newPostTitle, Boolean mergeUpdate) throws DataAccessException, DataUpdateException {
        stagingPostDao.updatePostTitle(mergeUpdate, username, id, newPostTitle);
        return stagingPostDao.findById(username, id);
    }

    public final StagingPost updatePostDesc(String username, Long id, ContentObject newPostDesc, Boolean mergeUpdate) throws DataAccessException, DataUpdateException {
        stagingPostDao.updatePostDesc(mergeUpdate, username, id, newPostDesc);
        return stagingPostDao.findById(username, id);
    }

    public final StagingPost updatePostITunes(String username, Long id, PostITunes newPostITunes, Boolean mergeUpdate) throws DataAccessException, DataUpdateException {
        stagingPostDao.updatePostITunes(mergeUpdate, username, id, newPostITunes);
        return stagingPostDao.findById(username, id);
    }

    public final StagingPost updatePostComment(String username, Long id, String newPostComment) throws DataAccessException, DataUpdateException {
        stagingPostDao.updatePostComment(username, id, newPostComment);
        return stagingPostDao.findById(username, id);
    }

    public final StagingPost updatePostRights(String username, Long id, String newPostRights) throws DataAccessException, DataUpdateException {
        stagingPostDao.updatePostRights(username, id, newPostRights);
        return stagingPostDao.findById(username, id);
    }

    public final StagingPost updatePostCategories(String username, Long id, List<String> newPostCategories) throws DataAccessException, DataUpdateException {
        stagingPostDao.updatePostCategories(username, id, newPostCategories);
        return stagingPostDao.findById(username, id);
    }

    public final StagingPost updateExpirationTimestamp(String username, Long id, Date newExpirationTimestamp) throws DataAccessException, DataUpdateException {
        stagingPostDao.updateExpirationTimestamp(username, id, newExpirationTimestamp);
        return stagingPostDao.findById(username, id);
    }

    public final StagingPost updatePostMedia(String username, Long id, PostMedia postMedia, Boolean mergeUpdate) throws DataAccessException, DataUpdateException {
        stagingPostDao.updatePostMedia(mergeUpdate, username, id, postMedia);
        return stagingPostDao.findById(username, id);
    }

    //
    //
    //

    public final StagingPost updateContent(String username, Long id, String contentIdent, ContentObjectConfigRequest contentObject, Boolean mergeUpdate) throws DataAccessException, DataUpdateException {
        StagingPost byId = stagingPostDao.findById(username, id);
        List<ContentObject> postContents = byId.getPostContents();
        ContentObject oldContent = postContents.stream()
                .filter(content -> content.getIdent().equals(contentIdent))
                .findFirst()
                .orElseThrow(() -> new DataAccessException(getClass().getSimpleName(), "updateContent", "Content not found by Ident=" + contentIdent, username, id, contentIdent, contentIdent));
        oldContent.update(contentObject.getType(), contentObject.getValue());
        stagingPostDao.updatePostContents(username, id, postContents);
        return byId;
    }

    public final StagingPost updateContents(String username, Long id, Iterable<? extends ContentObjectConfigRequest> contentObjectConfigRequests, Boolean mergeUpdate) throws DataAccessException, DataUpdateException {
        List<ContentObject> contents = newArrayListWithExpectedSize(size(contentObjectConfigRequests));
        for (ContentObjectConfigRequest contentConfigRequest : contentObjectConfigRequests) {
            ContentObject newContent = ContentObject.from(
                    randomAlphanumeric(8),
                    contentConfigRequest.getType(),
                    contentConfigRequest.getValue()
            );
            contents.add(newContent);
        }
        stagingPostDao.updatePostContents(username, id, contents);
        return stagingPostDao.findById(username, id);
    }

    public final StagingPost updatePostUrl(String username, Long id, String postUrlIdent, PostUrlConfigRequest postUrl, Boolean mergeUpdate) throws DataAccessException, DataUpdateException {
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
        return stagingPostDao.findById(username, id);
    }

    public final StagingPost updatePostUrls(String username, Long id, Iterable<? extends PostUrlConfigRequest> postUrlConfigRequests, Boolean mergeUpdate) throws DataAccessException, DataUpdateException {
        List<PostUrl> postUrls = newArrayListWithExpectedSize(size(postUrlConfigRequests));
        for (PostUrlConfigRequest postUrlConfigRequest : postUrlConfigRequests) {
            PostUrl newPostUrl = PostUrl.from(
                    randomAlphanumeric(8),
                    postUrlConfigRequest.getTitle(),
                    postUrlConfigRequest.getType(),
                    postUrlConfigRequest.getHref(),
                    postUrlConfigRequest.getHreflang(),
                    postUrlConfigRequest.getRel()
            );
            postUrls.add(newPostUrl);
        }
        stagingPostDao.updatePostUrls(username, id, postUrls);
        return stagingPostDao.findById(username, id);
    }

    public final StagingPost updateContributor(String username, Long id, String contributorIdent, PostPersonConfigRequest contributor, Boolean mergeUpdate) throws DataAccessException, DataUpdateException {
        List<PostPerson> contributors = stagingPostDao.findById(username, id).getContributors();
        PostPerson oldContributor = contributors.stream()
                .filter(a -> a.getIdent().equals(contributorIdent))
                .findFirst()
                .orElseThrow(() -> new DataAccessException(getClass().getSimpleName(), "updateContributor", "Contributor not found by Ident=" + contributorIdent, username, id, contributorIdent, contributor));
        oldContributor.update(contributor.getName(), contributor.getEmail(), contributor.getUri());
        stagingPostDao.updateContributors(username, id, contributors);
        return stagingPostDao.findById(username, id);
    }

    public final StagingPost updateContributors(String username, Long id, Iterable<? extends PostPersonConfigRequest> contributorConfigRequests, Boolean mergeUpdate) throws DataAccessException, DataUpdateException {
        List<PostPerson> contributors = newArrayListWithExpectedSize(size(contributorConfigRequests));
        for (PostPersonConfigRequest contributorConfigRequest : contributorConfigRequests) {
            PostPerson newContributor = PostPerson.from(
                    randomAlphanumeric(8),
                    contributorConfigRequest.getName(),
                    contributorConfigRequest.getEmail(),
                    contributorConfigRequest.getUri()
            );
            contributors.add(newContributor);
        }
        stagingPostDao.updateContributors(username, id, contributors);
        return stagingPostDao.findById(username, id);
    }

    public final StagingPost updateAuthor(String username, Long id, String authorIdent, PostPersonConfigRequest author, Boolean mergeUpdate) throws DataAccessException, DataUpdateException {
        List<PostPerson> authors = stagingPostDao.findById(username, id).getAuthors();
        PostPerson oldAuthor = authors.stream()
                .filter(a -> a.getIdent().equals(authorIdent))
                .findFirst()
                .orElseThrow(() -> new DataAccessException(getClass().getSimpleName(), "updateAuthor", "Author not found by Ident=" + authorIdent, username, id, authorIdent, author));
        oldAuthor.update(author.getName(), author.getEmail(), author.getUri());
        stagingPostDao.updateAuthors(username, id, authors);
        return stagingPostDao.findById(username, id);
    }

    public final StagingPost updateAuthors(String username, Long id, Iterable<? extends PostPersonConfigRequest> authorConfigRequests, Boolean mergeUpdate) throws DataAccessException, DataUpdateException {
        List<PostPerson> authors = newArrayListWithExpectedSize(size(authorConfigRequests));
        for (PostPersonConfigRequest authorConfigRequest : authorConfigRequests) {
            PostPerson newAuthor = PostPerson.from(
                    randomAlphanumeric(8),
                    authorConfigRequest.getName(),
                    authorConfigRequest.getEmail(),
                    authorConfigRequest.getUri()
            );
            authors.add(newAuthor);
        }
        stagingPostDao.updateAuthors(username, id, authors);
        return stagingPostDao.findById(username, id);
    }

    public final StagingPost updateEnclosure(String username, Long id, String enclosureIdent, PostEnclosureConfigRequest enclosure, Boolean mergeUpdate) throws DataAccessException, DataUpdateException {
        List<PostEnclosure> enclosures = stagingPostDao.findById(username, id).getEnclosures();
        PostEnclosure oldEnclosure = enclosures.stream()
                .filter(e -> e.getIdent().equals(enclosureIdent))
                .findFirst()
                .orElseThrow(() -> new DataAccessException(getClass().getSimpleName(), "updateEnclosure", "Enclosure not found by Ident=" + enclosureIdent, username, id, enclosure, enclosure));
        oldEnclosure.update(enclosure.getUrl(), enclosure.getType(), enclosure.getLength());
        stagingPostDao.updatePostEnclosures(username, id, enclosures);
        return stagingPostDao.findById(username, id);
    }

    public final StagingPost updateEnclosures(String username, Long id, Iterable<? extends PostEnclosureConfigRequest> enclosureConfigRequests, Boolean mergeUpdate) throws DataAccessException, DataUpdateException {
        List<PostEnclosure> enclosures = newArrayListWithExpectedSize(size(enclosureConfigRequests));
        for (PostEnclosureConfigRequest enclosureConfigRequest : enclosureConfigRequests) {
            PostEnclosure newEnclosure = PostEnclosure.from(
                    randomAlphanumeric(8),
                    enclosureConfigRequest.getUrl(),
                    enclosureConfigRequest.getType(),
                    enclosureConfigRequest.getLength()
            );
            enclosures.add(newEnclosure);
        }
        stagingPostDao.updatePostEnclosures(username, id, enclosures);
        return stagingPostDao.findById(username, id);
    }

    public final StagingPost findById(String username, Long id) throws DataAccessException {
        return stagingPostDao.findById(username, id);
    }

    public final PostPerson findAuthorByIdent(String username, Long id, String authorIdent) throws DataAccessException {
        StagingPost stagingPost = findById(username, id);
        List<PostPerson> postAuthors = stagingPost.getAuthors();
        if (isNotEmpty(postAuthors)) {
            for (PostPerson a : postAuthors) {
                if (a.getIdent().equals(authorIdent)) {
                    return a;
                }
            }
        }
        return null;
    }

    public final PostPerson findContributorByIdent(String username, Long id, String contributorIdent) throws DataAccessException {
        StagingPost stagingPost = findById(username, id);
        List<PostPerson> postContributors = stagingPost.getContributors();
        if (isNotEmpty(postContributors)) {
            for (PostPerson a : postContributors) {
                if (a.getIdent().equals(contributorIdent)) {
                    return a;
                }
            }
        }
        return null;
    }

    public final ContentObject findContentByIdent(String username, Long id, String contentIdent) throws DataAccessException {
        StagingPost stagingPost = findById(username, id);
        List<ContentObject> postContents = stagingPost.getPostContents();
        if (isNotEmpty(postContents)) {
            for (ContentObject postContent : postContents) {
                if (postContent.getIdent().equals(contentIdent)) {
                    return postContent;
                }
            }
        }
        return null;
    }

    public final PostEnclosure findEnclosureByIdent(String username, Long id, String enclosureIdent) throws DataAccessException {
        StagingPost stagingPost = findById(username, id);
        List<PostEnclosure> postEnclosures = stagingPost.getEnclosures();
        if (isNotEmpty(postEnclosures)) {
            for (PostEnclosure e : postEnclosures) {
                if (e.getIdent().equals(enclosureIdent)) {
                    return e;
                }
            }
        }
        return null;
    }

    public final PostUrl findUrlByIdent(String username, Long id, String urlIdent) throws DataAccessException {
        StagingPost stagingPost = findById(username, id);
        List<PostUrl> postUrls = stagingPost.getPostUrls();
        if (isNotEmpty(postUrls)) {
            for (PostUrl e : postUrls) {
                if (e.getIdent().equals(urlIdent)) {
                    return e;
                }
            }
        }
        return null;
    }

    //

    public final void deleteByQueueId(String username, Long queueId) throws DataAccessException, DataUpdateException {
        stagingPostDao.deleteByQueueId(username, queueId);
    }

    public final void deleteById(String username, Long id) throws DataAccessException, DataUpdateException {
        stagingPostDao.deleteById(username, id);
    }

    //

    public final void clearPostITunes(String username, Long id) throws DataAccessException, DataUpdateException {
        stagingPostDao.clearPostITunes(username, id);
    }

    public final void clearPostComment(String username, Long id) throws DataAccessException, DataUpdateException {
        stagingPostDao.clearPostComment(username, id);
    }

    public final void clearPostRights(String username, Long id) throws DataAccessException, DataUpdateException {
        stagingPostDao.clearPostRights(username, id);
    }

    public final void clearExpirationTimestamp(String username, Long id) throws DataAccessException, DataUpdateException {
        stagingPostDao.clearExpirationTimestamp(username, id);
    }

    public final void clearPostMedia(String username, Long id) throws DataAccessException, DataUpdateException {
        stagingPostDao.clearPostMedia(username, id);
    }

    public final void clearPostCategories(String username, Long id) throws DataAccessException, DataUpdateException {
        stagingPostDao.clearPostCategories(username, id);
    }

    //

    public final StagingPost deletePostContents(String username, Long id) throws DataAccessException, DataUpdateException {
        stagingPostDao.clearPostContents(username, id);
        return stagingPostDao.findById(username, id);
    }

    public final StagingPost deletePostContent(String username, Long id, String contentIdent) throws DataAccessException, DataUpdateException {
        List<ContentObject> postContents = stagingPostDao.findById(username, id).getPostContents();
        if (postContents.removeIf(postContent -> postContent.getIdent().equals(contentIdent))) {
            stagingPostDao.updatePostContents(username, id, postContents);
        } else {
            throw new DataAccessException(getClass().getSimpleName(), "deleteContent", "Content not found by Ident=" + contentIdent, username, id, contentIdent);
        }
        return stagingPostDao.findById(username, id);
    }

    //

    public final StagingPost deletePostUrls(String username, Long id) throws DataAccessException, DataUpdateException {
        stagingPostDao.clearPostUrls(username, id);
        return stagingPostDao.findById(username, id);
    }

    public final StagingPost deletePostUrl(String username, Long id, String postUrlIdent) throws DataAccessException, DataUpdateException {
        List<PostUrl> postUrls = stagingPostDao.findById(username, id).getPostUrls();
        if (postUrls.removeIf(u -> u.getIdent().equals(postUrlIdent))) {
            stagingPostDao.updatePostUrls(username, id, postUrls);
        } else {
            throw new DataAccessException(getClass().getSimpleName(), "deletePostUrl", "Post URL not found by Ident=" + postUrlIdent, username, id, postUrlIdent);
        }
        return stagingPostDao.findById(username, id);
    }

    //

    public final StagingPost deleteAuthors(String username, Long id) throws DataAccessException, DataUpdateException {
        stagingPostDao.clearPostAuthors(username, id);
        return stagingPostDao.findById(username, id);
    }

    public final StagingPost deleteAuthor(String username, Long id, String authorIdent) throws DataAccessException, DataUpdateException {
        List<PostPerson> authors = stagingPostDao.findById(username, id).getAuthors();
        if (authors.removeIf(a -> a.getIdent().equals(authorIdent))) {
            stagingPostDao.updateAuthors(username, id, authors);
        } else {
            throw new DataAccessException(getClass().getSimpleName(), "deleteAuthor", "Author not found by Ident=" + authorIdent, username, id, authorIdent);
        }
        return stagingPostDao.findById(username, id);
    }

    //

    public final StagingPost deleteContributors(String username, Long id) throws DataAccessException, DataUpdateException {
        stagingPostDao.clearPostContributors(username, id);
        return stagingPostDao.findById(username, id);
    }

    public final StagingPost deleteContributor(String username, Long id, String contributorIdent) throws DataAccessException, DataUpdateException {
        List<PostPerson> contributors = stagingPostDao.findById(username, id).getContributors();
        if (contributors.removeIf(a -> a.getIdent().equals(contributorIdent))) {
            stagingPostDao.updateContributors(username, id, contributors);
        } else {
            throw new DataAccessException(getClass().getSimpleName(), "deleteContributor", "Contributor not found by Ident=" + contributorIdent, username, id, contributorIdent);
        }
        return stagingPostDao.findById(username, id);
    }

    //

    public final StagingPost deleteEnclosures(String username, Long id) throws DataAccessException, DataUpdateException {
        stagingPostDao.clearPostEnclosures(username, id);
        return stagingPostDao.findById(username, id);
    }

    public final StagingPost deleteEnclosure(String username, Long id, String enclosureIdent) throws DataAccessException, DataUpdateException {
        List<PostEnclosure> enclosures = stagingPostDao.findById(username, id).getEnclosures();
        if (enclosures.removeIf(e -> e.getIdent().equals(enclosureIdent))) {
            stagingPostDao.updatePostEnclosures(username, id, enclosures);
        } else {
            throw new DataAccessException(getClass().getSimpleName(), "deleteEnclosure", "Enclosure not found by Ident=" + enclosureIdent, username, id, enclosureIdent);
        }
        return stagingPostDao.findById(username, id);
    }

    public final long resolveQueueId(String username, Long postId) throws DataAccessException {
        return stagingPostDao.findQueueIdByStagingPostId(username, postId);
    }

    @Override
    public final String toString() {
        return "StagingPostService{" +
                "stagingPostDao=" + stagingPostDao +
                '}';
    }
}
