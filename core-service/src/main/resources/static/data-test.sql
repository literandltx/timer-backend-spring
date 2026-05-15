-- Init User table
INSERT INTO users (password, email) VALUES ('password123', 'sometest@example.com');

-- Init Label table
INSERT INTO labels (name, color, user_id) VALUES ('Test Label', '#CCCCCC', 1);
