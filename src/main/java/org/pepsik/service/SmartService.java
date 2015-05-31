package org.pepsik.service;

import org.pepsik.model.*;
import org.pepsik.model.Post;
import org.pepsik.model.Profile;

import java.util.List;
import java.util.Set;

/**
 * Created by pepsik on 4/9/15.
 */
public interface SmartService {

    List<Post> getAllPosts();

    List<Post> getPostsByPage(int pageIndex);

    void saveFavorite(long postId);

    List<Favorite> getFavorites(String username);

    void removeFavorite(long id);

    List<String> getPagination(int pageIndex, long postCount);

    long getPostsCount();

    User getUser(long id);

    User getUser(String username);

    void saveUser(User user);

    void deleteUser(long id);

    void saveProfile(Profile profile);

    Profile getProfile(String username);

    void deleteProfile(String username);

    Post getPost(long id);

    void savePost(Post thread);

    void deletePost(long id);

    List<Post> getUserPosts(String username, int page);

    long getUserPostsCount(User user);

    Comment getComment(long id);

    void saveComment(Comment message);

    void deleteComment(long id);

    boolean isExistComment(long id);

    boolean isExistUsername(String username);

    boolean isExistUsername(long id);

    boolean isExistPost(long id);

    long getPagesCount(long postCount);

    void saveTag(Tag tag);

    Tag getTag(String name);

    boolean isExistTag(String tag);
}
