package org.maven.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SFTP 连接
 * sftp:
 *   host: 127.0.0.1          # SFTP 服务器地址
 *   port: 22                 # SSH 端口
 *   username: newsuser       # 登录用户名
 *   password: newspass       # 登录密码（与私钥二选一）
 *   #private-key: ~/.ssh/id_rsa  # 私钥路径（与密码二选一）
 *   remote-root: /home/swan/uploads/news  # 远程根目录基准路径
 */
@Data
@Component
@ConfigurationProperties(prefix = "sftp")
public class SftpProperties {

    /** SFTP 登录用户名 */
    private String username;

    /** SFTP 登录密码 */
    private String password;

    /** 私钥路径（与密码二选一） */
    private String privateKey;

    /** SFTP 服务器地址 */
    private String host;

    /** SFTP 端口 */
    private int port = 22;

    /** 远程根目录基准路径 */
    private String remoteRoot;
}
