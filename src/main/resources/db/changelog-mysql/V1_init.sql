-- liquibase formatted sql

/*
 Navicat Premium Dump SQL

 Source Server         : 模版
 Source Server Type    : SQLite
 Source Server Version : 3045000 (3.45.0)
 Source Schema         : main

 Target Server Type    : SQLite
 Target Server Version : 3045000 (3.45.0)
 File Encoding         : 65001

 Date: 10/10/2024 17:18:32
*/


-- ----------------------------
-- Table structure for account
-- ----------------------------
CREATE TABLE IF NOT EXISTS `account` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `email` varchar(255),
    `password` varchar(255),
    `session_token` varchar(255),
    `access_token` varchar(255),
    `create_time` datetime,
    `update_time` datetime,
    `shared` tinyint,
    `refresh_token` varchar(255),
    `user_id` bigint NOT NULL,
    `account_type` tinyint NOT NULL DEFAULT 1,
    `auto` tinyint NOT NULL DEFAULT 0,
    `user_limit` int NOT NULL DEFAULT 4,
    `name` varchar(255),
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for car_apply
-- ----------------------------
CREATE TABLE IF NOT EXISTS `car_apply` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `share_id` bigint NOT NULL,
    `account_id` bigint NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for redemption
-- ----------------------------
CREATE TABLE IF NOT EXISTS `redemption` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `target_user_name` varchar(255),
    `account_id` bigint NOT NULL,
    `user_id` bigint NOT NULL,
    `duration` int NOT NULL,
    `time_unit` tinyint NOT NULL DEFAULT 1,
    `create_time` datetime NOT NULL,
    `code` varchar(255) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for share
-- ----------------------------
CREATE TABLE IF NOT EXISTS `share` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `account_id` bigint,
    `unique_name` varchar(255),
    `password` varchar(255),
    `comment` text,
    `expires_at` datetime,
    `parent_id` bigint NOT NULL DEFAULT 1,
    `avatar_url` varchar(255),
    `trust_level` int,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_account_shares` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for share_claude_config
-- ----------------------------
CREATE TABLE IF NOT EXISTS `share_claude_config` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `share_id` bigint NOT NULL,
    `oauth_token` varchar(255),
    `expires_at` datetime,
    `account_id` bigint NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for share_gpt_config
-- ----------------------------
CREATE TABLE IF NOT EXISTS `share_gpt_config` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `share_id` bigint NOT NULL,
    `share_token` text,
    `expires_in` int,
    `expires_at` datetime,
    `site_limit` varchar(255),
    `gpt4_limit` int,
    `gpt35_limit` int,
    `show_userinfo` tinyint,
    `show_conversations` tinyint,
    `refresh_everyday` tinyint,
    `temporary_chat` tinyint,
    `account_id` bigint NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for share_api_config
-- ----------------------------
CREATE TABLE IF NOT EXISTS `share_api_config` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `share_id` bigint NOT NULL,
    `account_id` bigint NOT NULL,
    `api_proxy` varchar(255),
    `api_key` varchar(255),
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

