package org.maven.config;

import com.jcraft.jsch.UserInfo;
import lombok.Data;

@Data
public class SftpUserInfo implements UserInfo {
    private final String password;

    public SftpUserInfo(String password) {
        this.password = password;
    }

    public String getPassphrase() {
        return null;
    }

    public boolean promptPassword(String message) {
        return true;
    }

    public boolean promptPassphrase(String message) {
        return false;
    }

    public boolean promptYesNo(String message) {
        return true;
    }

    public void showMessage(String message) {
    }
}
