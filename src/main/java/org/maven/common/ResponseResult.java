package org.maven.common;

import java.io.Serial;
import java.util.HashMap;

public class ResponseResult extends HashMap<String, Object> {

    @Serial
    private static final long serialVersionUID = 1L;

    public ResponseResult() {
    }

    public ResponseResult(int code, String message) {
        put("code", code);
        put("message", message);
    }

    public static ResponseResult success() {
        return new ResponseResult(200, "操作成功");
    }

    public static ResponseResult success(String message) {
        return new ResponseResult(200, message);
    }

    public static ResponseResult error() {
        return new ResponseResult(500, "操作失败");
    }

    public static ResponseResult error(String message) {
        return new ResponseResult(500, message);
    }

    @Override
    public ResponseResult put(String key, Object value) {
        super.put(key, value);
        return this;
    }
}
