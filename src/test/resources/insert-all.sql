insert into `provider`(`label`) values('label_1'),('label_2');
insert into `user`(`name`) values('name_1');
insert into `order`(`number`, `user_id`) values('number_1', 1),('number_2', 1);
insert into `item`(`description`, `order_id`) values('description_3', 2),('description_1', 1),('description_2', 2);
insert into `provider_user`(`provider_id`, `user_id`) values(1, 1),(2, 1);

insert into `user`(`id`, `name`, `item_id`) values(1, 'name_1', 1)  on duplicate key update `id` = values(`id`), `name` = values(`name`), `item_id` = values(`item_id`);