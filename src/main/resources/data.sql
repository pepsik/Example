INSERT INTO ACCOUNT (ACCOUNT_ID, USERNAME, PASSWORD) VALUES
  (1, 'username1', '$2a$10$mVKCQ4Y9/asZM0QbbZOUUeamfA/sdg6h9l44TBz5n7mK0tje28FI.'),
  (2, 'username2', '$2a$10$G52uld.J0hMiq2l4p/4Lgem3oadOSefEKsxz9b4BvtNNbLcB3kVWm');

INSERT INTO POST (POST_ID, TITLE, TEXT, DATE, OWNER_ID) VALUES
  (1, 'Title1', 'Post1', '2015-10-11 15:15:01', 1),
  (2, 'Title2', 'Post2', '2015-10-11 15:18:56', 1),
  (3, 'Title3', 'Post3', '2015-10-11 20:07:49', 2);

INSERT INTO COMMENT (COMMENT_ID, TEXT, OWNER_ID, POST_ID, DATE) VALUES
  (1, 'Comment1', 2, 2, '2015-10-11 16:59:16'),
  (2, 'Comment2', 1, 1, '2015-10-11 17:01:55'),
  (3, 'Comment3', 1, 1, '2015-10-11 17:02:16');
