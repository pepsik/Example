package org.pepsik.core.security;

import org.pepsik.core.models.entities.Reworked.Comment;
import org.pepsik.core.models.entities.Reworked.Post;
import org.pepsik.core.services.Reworked.CommentService;
import org.pepsik.core.services.Reworked.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Created by pepsik on 10/16/2015.
 */
@Component
public class SecurityService {

    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;

    public boolean canUpdatePost(Long postId) {
        Post post = postService.findPostById(postId);
        if (post != null) {
            return SecurityContextHolder.getContext().getAuthentication().getName().equals(post.getOwner().getUsername());
        } else {
            return true;
        }
    }

    public boolean canDeletePost(Long postId) {
        return canUpdatePost(postId);
    }

    public boolean canUpdateComment(Long commentId) {
        Comment comment = commentService.findCommentById(commentId);
        if (comment != null) {
            return SecurityContextHolder.getContext().getAuthentication().getName().equals(comment.getOwner().getUsername());
        } else {
            return true;
        }
    }

    public boolean canDeleteComment(Long commentId) {
        return canUpdateComment(commentId);
    }
}
