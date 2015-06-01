package org.pepsik.service;

import org.apache.commons.collections.ListUtils;
import org.joda.time.DateTime;
import org.pepsik.model.*;
import org.pepsik.model.Post;
import org.pepsik.model.Profile;
import org.pepsik.model.support.FavoriteComparator;
import org.pepsik.model.support.PostComparator;
import org.pepsik.persistence.*;
import org.pepsik.web.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.NoResultException;
import java.util.*;

/**
 * Created by pepsik on 4/9/15.
 */

@Service
@Transactional(readOnly = true)
public class SmartServiceImpl implements SmartService {

    private static final int DEFAULT_POSTS_PER_PAGE = 7;
    private static final int DEFAULT_PAGINATION_ON_PAGE = 5;

    private static final Logger logger = LoggerFactory.getLogger(SmartServiceImpl.class);
    public static final String GUEST_NAME = "guest";

    @Autowired
    private UserDao userDao;

    @Autowired
    private ProfileDao profileDao;

    @Autowired
    private PostDao postDao;

    @Autowired
    private CommentDao commentDao;

    @Autowired
    private TagDao tagDao;

    @Autowired
    private FavoriteDao favoriteDao;

    @Override
    public List<Post> getAllPosts() {
        return postDao.getAllPosts();
    }

    @Override
    public List<Post> getPostsByPage(int pageIndex) {
        String loggedUser = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Post> postsByPage = postDao.getPostsByPage(pageIndex, DEFAULT_POSTS_PER_PAGE);
        for (Post post : postsByPage)
            post.getComments().size();
        if (loggedUser.equals(GUEST_NAME))
            return postsByPage;
        checkPostListForFavorites(postsByPage);
        return postsByPage;
    }

    @Override
    public Post getPost(long postId) {
        if (!isExistPost(postId))
            throw new ResourceNotFoundException();
        Post post = postDao.getPostById(postId);
        post.getComments().size();
        String loggedUser = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!loggedUser.equals(GUEST_NAME))
            isFavoritePost(loggedUser, post);
        return post;
    }

    @Override
    @Transactional(readOnly = false)
    @PreAuthorize("hasRole('ROLE_USER')")
    public void savePost(Post post) {
        String loggedUser = SecurityContextHolder.getContext().getAuthentication().getName();
        if (post.getTags() != null) {
            Set<Tag> postTags = post.getTags();
            Set<Tag> finalTags = new HashSet<>();
            for (Tag tag : postTags)
                if (isExistTag(tag.getName()))
                    finalTags.add(tagDao.getTag(tag.getName()));
                else {
                    tag.setCreateDate(new DateTime());
                    tag.setAuthor(getUser(loggedUser));
                    finalTags.add(tag);
                }
            post.setTags(finalTags);
        }
        if (post.getId() == 0) {
            User user = getUser(loggedUser);
            post.setUser(user);
            postDao.addPost(post);
        } else
            postDao.updatePost(post);
    }

    @Override
    @Transactional(readOnly = false)
    @PreAuthorize("(hasRole('ROLE_USER') and principal.username == this.getPost(#id).user.username) or hasRole('ROLE_ADMIN')")
    public void deletePost(long id) {
        postDao.deletePost(id);
    }

    @Override
    public boolean isExistPost(long id) {
        try {
            postDao.getPostById(id);
        } catch (NoResultException ex) {
            return false;
        }
        return true;
    }

    @Override
    public List<String> getPagination(final int pageIndex, long postCount) {
        List<String> pagination = new ArrayList<>();
        long pagesCount = postCount / DEFAULT_POSTS_PER_PAGE;
        if (postCount % DEFAULT_POSTS_PER_PAGE != 0)
            pagesCount += 1;
        if (pagesCount <= DEFAULT_PAGINATION_ON_PAGE) {
            for (int i = 1; i <= pagesCount; i++)
                pagination.add(Integer.toString(i));
            return pagination;
        }
        if (pageIndex <= 2) {
            for (int i = 1; i <= DEFAULT_PAGINATION_ON_PAGE; i++)
                pagination.add(Integer.toString(i));
            return pagination;
        }
        for (int i = pageIndex - 2; i <= pageIndex + 2; i++) {
            if (i > pagesCount)
                break;
            pagination.add(Integer.toString(i));
        }
        return pagination;
    }

    @Override
    public long getAllPostsCount() {
        return postDao.getPostCount();
    }

    @Override
    public User getUser(long id) {
        if (!isExistUsername(id))
            throw new UserNotFoundException();

        return userDao.getUserById(id);
    }

    @Override
    public User getUser(String username) {
        if (!isExistUsername(username))
            throw new UserNotFoundException();

        return userDao.getUserByUsername(username);
    }

    @Override
    @Transactional(readOnly = false)
    public void saveUser(User user) {
        if (user.getId() == 0) {
            userDao.addUser(user);
            userDao.setUserAuthority(user); // set ROLE_USER to all new accounts
        } else
            authorizedUpdateProfile(user);
    }

    @PreAuthorize("(hasRole('ROLE_USER') and principal.username == #profile.user.username)")
    @Transactional(readOnly = false)
    private void authorizedUpdateProfile(User user) {
        userDao.updateUser(user);
    }

    @Override
    @Transactional(readOnly = false)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void deleteUser(long id) {
//        userDao.deleteUser(id);
    }

    @Override
    public boolean isExistUsername(String username) {
        try {
            userDao.getUserByUsername(username);
        } catch (NoResultException ex) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isExistUsername(long id) {
        try {
            userDao.getUserById(id);
        } catch (NoResultException ex) {
            return false;
        }
        return true;
    }

    @Override
    public List<Post> getUserPosts(String username, int pageId) {
        List<Post> postList = userDao.getUserPosts(getUser(username), pageId, DEFAULT_POSTS_PER_PAGE);

        String loggedUser = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!loggedUser.equals(GUEST_NAME))
            for (Post post : postList) {
                post.getComments().size();
                isFavoritePost(loggedUser, post);
            }
        else
            for (Post post : postList)
                post.getComments().size();
        return postList;
    }

    @Override
    public long getUserPostsCount(String username) {
        return userDao.getUserPostCount(getUser(username));
    }

    @Override
    public List<Comment> getUserComments(String username, int pageId) {
        return userDao.getUserComments(getUser(username), pageId, DEFAULT_POSTS_PER_PAGE * 2); //TODO: comments default counter
    }

    @Override
    public long getUserCommentsCount(String username) {
        return userDao.getUserCommentsCount(getUser(username));
    }

//    @Override
//    public List<Favorite> getAllUserFavorites(String username) {
//        User user = getUser(username);
//        Set<Favorite> favoritesSet = user.getFavorites();
//        Comparator<Favorite> comparator = new FavoriteComparator();
//        List<Favorite> favoriteList = new LinkedList<>(favoritesSet);
//        Collections.sort(favoriteList, Collections.reverseOrder(comparator));
//        for (Favorite favorite : favoriteList) {
//            favorite.getPost().setFavorite(true);
//            favorite.getPost().getComments().size();
//        }
//        return favoriteList;
//    }

    @Override
    public List<Favorite> getUserFavorites(String username, int page) {
        List<Favorite> favorites = userDao.getUserFavorites(getUser(username), page, DEFAULT_POSTS_PER_PAGE);
        String loggedUser = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!loggedUser.equals(GUEST_NAME))
            for (Favorite favorite : favorites) {
                favorite.getPost().getComments().size();
                isFavoritePost(loggedUser, favorite.getPost());
            }
        else
            for (Favorite favorite : favorites)
                favorite.getPost().getComments().size();
        return favorites;
    }

    @Override
    public long getUserFavoritesCount(String username) {
        return userDao.getUserFavoritesCount(getUser(username));
    }

    @Override
    @Transactional(readOnly = false)
    public void saveProfile(Profile profile) {
        if (profile.getId() == 0) {
            profileDao.addProfile(profile);
            userDao.setUserAuthority(profile.getUser());
        } else
            authorizedUpdateProfile(profile);
    }

    @PreAuthorize("(hasRole('ROLE_USER') and principal.username == #profile.user.username)")
    @Transactional(readOnly = false)
    private void authorizedUpdateProfile(Profile profile) {
        profileDao.updateProfile(profile);
    }

    @Override
    public Profile getProfile(String username) {
        User user = getUser(username);
        return user.getProfile();
    }

    @Override
    @Transactional(readOnly = false)
    @PreAuthorize("(hasRole('ROLE_USER') and principal.username == this.getPost(#id).user.username) or hasRole('ROLE_ADMIN')")
    public void deleteProfile(String username) {
//        profileDao.deleteProfile(username);
    }

    @Override
    public Comment getComment(long id) {
        return commentDao.getCommentById(id);
    }

    @Override
    @Transactional(readOnly = false)
    @PreAuthorize("(hasRole('ROLE_USER') and principal.username == post.user.username) or hasRole('ROLE_ADMIN')")
    public void saveComment(Comment post) {
        if (post.getId() == 0) {
            String loggedUser = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = getUser(loggedUser);
            post.setUser(user);
            commentDao.addComment(post);
        } else
            commentDao.updateComment(post);
    }

    @Override
    @PreAuthorize("(hasRole('ROLE_USER') and principal.username == this.getComment(#id).user.username) or hasRole('ROLE_ADMIN')")
    @Transactional(readOnly = false)
    public void deleteComment(long id) {
        commentDao.deleteComment(id);
    }

    @Override
    public boolean isExistComment(long id) {
        try {
            commentDao.getCommentById(id);
        } catch (NoResultException ex) {
            return false;
        }
        return true;
    }

    @Override
    public long getPagesCount(long postCount) {
        if (postCount % DEFAULT_POSTS_PER_PAGE != 0)
            return postCount / DEFAULT_POSTS_PER_PAGE + 1;
        return postCount / DEFAULT_POSTS_PER_PAGE;
    }

    @Override
    @Transactional(readOnly = false)
    @PreAuthorize("hasRole('ROLE_USER')")
    public void saveFavorite(long postId) {
        String loggedUser = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = getUser(loggedUser);
        Post post = getPost(postId);
        if (isFavoritePost(loggedUser, post))
            throw new FavoriteExistException();
        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setPost(post);
        favorite.setAddedDate(new DateTime());
        if (!user.getFavorites().contains(favorite)) {
            post.setFavoriteCount(post.getFavoriteCount() + 1);
            favoriteDao.addFavorite(favorite);
        }
    }

    @Override
    @Transactional(readOnly = false)
    @PreAuthorize("hasRole('ROLE_USER')")
    public void removeFavorite(long postId) {
        String loggedUser = SecurityContextHolder.getContext().getAuthentication().getName();
        Post post = getPost(postId);
        if (!isFavoritePost(loggedUser, post))
            throw new FavoriteIsNotExistException();
        User user = getUser(loggedUser);
        favoriteDao.deleteFavorite(postId, user.getId());
        post.setFavoriteCount(post.getFavoriteCount() - 1);
    }

    private void checkPostListForFavorites(List<Post> pagePosts) {
        String loggedUser = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Favorite> favorites = new LinkedList<>(getUser(loggedUser).getFavorites());
        List<Post> favoritePosts = new LinkedList<>();
        for (Favorite favorite : favorites)
            favoritePosts.add(favorite.getPost());
        List<Post> matches = ListUtils.retainAll(pagePosts, favoritePosts);
        for (Post post : matches)
            post.setFavorite(true);
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    private boolean isFavoritePost(String loggedUser, Post post) {   //get all user favorites
        User user = getUser(loggedUser);
        for (Favorite favorite : user.getFavorites())
            if (favorite.getPost().equals(post)) {
                post.setFavorite(true);
                return true;
            }
        return false;
    }

    @Override
    public Tag getTag(String name) {
        if (!isExistTag(name))
            throw new TagNotFoundException();
        Tag tag = tagDao.getTag(name);
        tag.getAuthor().toString();
        if (!SecurityContextHolder.getContext().getAuthentication().getName().equals(GUEST_NAME))
            checkPostListForFavorites(tag.getPosts());
        for (Post post : tag.getPosts())
            post.getComments().size();
        Collections.sort(tag.getPosts(), Collections.reverseOrder(new PostComparator()));
        return tag;
    }

    @Override
    @Transactional(readOnly = false)
    @PreAuthorize("hasRole('ROLE_USER')")
    public void saveTag(Tag tag) {
        if (tag.getId() == 0)
            tagDao.createTag(tag);
        else
            tagDao.updateTag(tag);
    }

    @Override
    public boolean isExistTag(String tagName) {
        try {
            tagDao.getTag(tagName);
        } catch (NoResultException exception) {
            return false;
        }
        return true;
    }

}
