CREATE TABLE `provider_user`(
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `provider_id` BIGINT(20) NOT NULL,
    `user_id` BIGINT(20) NOT NULL
);