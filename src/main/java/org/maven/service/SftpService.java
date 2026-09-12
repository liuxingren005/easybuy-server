package org.maven.service;

import com.jcraft.jsch.*;
import org.apache.commons.io.IOUtils;
import org.maven.config.SftpProperties;
import org.maven.config.SftpUserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.Properties;
import java.util.UUID;
import java.util.Vector;

/**
 * SFTP 文件操作服务类
 *
 * 基于 JSch 实现 SFTP 协议文件上传 / 下载 / 删除 / 列表。
 * 认证方式：密码 / RSA 私钥（二选一）
 * 生命周期：login() → 操作 → logout()
 *
 * 通过 SftpProperties 注入配置，Spring 管理 @Component 单例。
 * 连接复用 - 连接池
 *
 * 使用示例：
 *   sftpUtil.login();
 *   try {
 *       String remotePath = sftpUtil.uploadFile(file, "/data/upload");
 *   } finally {
 *       sftpUtil.logout();
 *   }
 */
@Component
public class SftpService {

    private static final Logger log = LoggerFactory.getLogger(SftpService.class);

    /** SFTP 配置属性 */
    private final SftpProperties sftpProperties;

    /** SFTP 通道（ThreadLocal 线程隔离，避免单例并发共享连接） */
    private final ThreadLocal<ChannelSftp> SFTP_HOLDER = new ThreadLocal<>();

    /** SSH 会话（ThreadLocal 线程隔离） */
    private final ThreadLocal<Session> SESSION_HOLDER = new ThreadLocal<>();

    public SftpService(SftpProperties sftpProperties) {
        this.sftpProperties = sftpProperties;
    }

    /**
     * 获取当前线程的 SFTP 通道（未 login 时报错）
     */
    private ChannelSftp channel() {
        ChannelSftp sftp = SFTP_HOLDER.get();
        if (sftp == null) {
            throw new IllegalStateException("SFTP 未连接，请先调用登录");
        }
        return sftp;
    }

    /**
     * 连接 SFTP 服务器
     * 流程：JSch → 认证 → Session → Channel("sftp") → ChannelSftp
     */
    public void login() {
        String host = sftpProperties.getHost();
        int port = sftpProperties.getPort();
        String username = sftpProperties.getUsername();
        String password = sftpProperties.getPassword();
        String privateKey = sftpProperties.getPrivateKey();

        try {
            // 同一线程若已有先关闭连接
            logout();

            JSch jsch = new JSch();
            if (privateKey != null && !privateKey.isEmpty()) {
                jsch.addIdentity(privateKey);
                log.info("sftp connect, path of private key file: {}", privateKey);
            }
            log.info("sftp connect by host:{} username:{}", host, username);

            Session session = jsch.getSession(username, host, port);
            log.info("Session is build");
            if (password != null && !password.isEmpty()) {
                session.setUserInfo(new SftpUserInfo(password));
            }

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            log.info("Session is connected");

            Channel channel = session.openChannel("sftp");
            channel.connect();
            log.info("channel is connected");

            // 当前 SFTP 会话与通道存入线程上下文
            SESSION_HOLDER.set(session);
            SFTP_HOLDER.set((ChannelSftp) channel);
            log.info("sftp server host:[{}] port:[{}] is connect successful", host, port);
        } catch (JSchException e) {
            log.error("Cannot connect to specified sftp server : {}:{} \n Exception message is: {}",
                    host, port, e.getMessage());
        }
    }

    /**
     * 关闭 SFTP 连接
     */
    public void logout() {
        ChannelSftp sftp = SFTP_HOLDER.get();
        if (sftp != null) {
            if (sftp.isConnected()) {
                sftp.disconnect();
                log.info("sftp is closed already");
            }
        }
        Session session = SESSION_HOLDER.get();
        if (session != null) {
            if (session.isConnected()) {
                session.disconnect();
                log.info("sshSession is closed already");
            }
        }
        // 清除 ThreadLocal
        SFTP_HOLDER.remove();
        SESSION_HOLDER.remove();
    }

    // 上传

    /**
     * 流式上传
     *
     * @param directory    远程目标目录
     * @param sftpFileName 远程文件名
     * @param input        输入流
     */
    public void upload(String directory, String sftpFileName, InputStream input) throws SftpException {
        ChannelSftp sftp = channel();
        try {
            sftp.cd(directory);
        } catch (SftpException e) {
            log.warn("directory is not exist, creating: {}", directory);
            sftp.mkdir(directory);
            sftp.cd(directory);
        }
        sftp.put(input, sftpFileName);
        log.info("file:{} is upload successful", sftpFileName);
    }

    /**
     * 本地文件路径上传
     *
     * @param directory  远程目录
     * @param uploadFile 本地文件路径
     */
    public void upload(String directory, String uploadFile) throws FileNotFoundException, SftpException {
        File file = new File(uploadFile);
        upload(directory, file.getName(), new FileInputStream(file));
    }

    /**
     * 字节数组上传
     *
     * @param directory    远程目录
     * @param sftpFileName 远程文件名
     * @param byteArr      字节数据
     */
    public void upload(String directory, String sftpFileName, byte[] byteArr) throws SftpException {
        upload(directory, sftpFileName, new ByteArrayInputStream(byteArr));
    }

    /**
     * 字符串上传
     *
     * @param directory    远程目录
     * @param sftpFileName 远程文件名
     * @param dataStr      文本内容
     * @param charsetName  字符编码
     */
    public void upload(String directory, String sftpFileName, String dataStr, String charsetName)
            throws UnsupportedEncodingException, SftpException {
        upload(directory, sftpFileName, new ByteArrayInputStream(dataStr.getBytes(charsetName)));
    }

    // 下载

    /**
     * 下载到本地文件
     *
     * @param directory    远程目录
     * @param downloadFile 远程文件名
     * @param saveFile     本地保存完整路径
     */
    public void download(String directory, String downloadFile, String saveFile)
            throws SftpException, FileNotFoundException {
        ChannelSftp sftp = channel();
        if (directory != null && !directory.isEmpty()) {
            sftp.cd(directory);
        }
        File file = new File(saveFile);
        sftp.get(downloadFile, new FileOutputStream(file));
        log.info("download to file: {} -> {}", downloadFile, saveFile);
    }

    /**
     * 下载到内存字节数组
     *
     * @param directory    远程目录
     * @param downloadFile 远程文件名
     * @return 文件字节内容
     */
    public byte[] download(String directory, String downloadFile) throws SftpException, IOException {
        ChannelSftp sftp = channel();
        if (directory != null && !directory.isEmpty()) {
            sftp.cd(directory);
        }
        InputStream is = sftp.get(downloadFile);
        byte[] fileData = IOUtils.toByteArray(is);
        log.info("download to memory: {} ({} bytes)", downloadFile, fileData.length);
        return fileData;
    }

    // 删除 / 列表

    /**
     * 删除远程文件
     *
     * @param directory  文件所在目录
     * @param deleteFile  要删除的文件名
     */
    public void delete(String directory, String deleteFile) throws SftpException {
        ChannelSftp sftp = channel();
        sftp.cd(directory);
        sftp.rm(deleteFile);
    }

    /**
     * 列出目录下所有文件 / 子目录
     *
     * @param directory 目标目录
     * @return Vector<ChannelSftp.LsEntry>
     */
    public Vector<?> listFiles(String directory) throws SftpException {
        return channel().ls(directory);
    }

    // MultipartFile

    /**
     * 生成 SFTP 远程唯一文件名
     *
     * @param originalFilename 原始文件名
     * @return 唯一文件名（带后缀）
     */
    public String generateFileName(String originalFilename) {
        String suffix = StringUtils.getFilenameExtension(originalFilename);
        return UUID.randomUUID() + (StringUtils.hasText(suffix) ? "." + suffix : "");
    }

    /**
     * 上传 MultipartFile 到完整远程路径
     *
     * @param file       上传文件
     * @param remotePath 完整远程路径，如 /data/upload/xxx.jpg
     */
    public void uploadMultipartFile(MultipartFile file, String remotePath) throws Exception {
        int lastIndex = remotePath.lastIndexOf("/");
        String dir = remotePath.substring(0, lastIndex);
        String fileName = remotePath.substring(lastIndex + 1);
        try (InputStream inputStream = file.getInputStream()) {
            upload(dir, fileName, inputStream);
        }
    }

    /**
     * 上传 MultipartFile 到远程根目录（自动生成唯一文件名）
     *
     * @param file       上传文件
     * @param remoteRoot 远程根目录
     * @return 完整远程路径
     */
    public String uploadFile(MultipartFile file, String remoteRoot) throws Exception {
        String originalName = file.getOriginalFilename();
        String remoteFileName = generateFileName(originalName);
        String remotePath = remoteRoot + "/" + remoteFileName;
        uploadMultipartFile(file, remotePath);
        return remotePath;
    }

    // Getter

    /**
     * 获取远程根目录
     */
    public String getRemoteRoot() {
        return sftpProperties.getRemoteRoot();
    }
}
